package com.segula.saasgestion.service;

import com.segula.saasgestion.domain.Project;
import com.segula.saasgestion.domain.ProjectWip;
import com.segula.saasgestion.domain.WipMonthDocument;
import com.segula.saasgestion.dto.ProjectWipDto;
import com.segula.saasgestion.dto.WipMonthDocumentDto;
import com.segula.saasgestion.dto.WipSummaryDto;
import com.segula.saasgestion.dto.WipTableRowDto;
import com.segula.saasgestion.repository.ProjectRepository;
import com.segula.saasgestion.repository.ProjectResourceEntryRepository;
import com.segula.saasgestion.repository.ProjectWipRepository;
import com.segula.saasgestion.repository.WipMonthDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WipService {

    private final ProjectWipRepository wipRepo;
    private final ProjectResourceEntryRepository entryRepo;
    private final WipMonthDocumentRepository docRepo;
    private final ProjectRepository projectRepo;

    private static final String UPLOAD_DIR = "./uploads/wip/";

    // Correspondance abréviations mois → numéro
    private static final Map<String, String> MONTH_MAP = new LinkedHashMap<>();
    static {
        MONTH_MAP.put("JAN", "01"); MONTH_MAP.put("JANV", "01");
        MONTH_MAP.put("FEV", "02"); MONTH_MAP.put("FEVR", "02"); MONTH_MAP.put("FÉV", "02");
        MONTH_MAP.put("MAR", "03"); MONTH_MAP.put("MARS", "03");
        MONTH_MAP.put("AVR", "04"); MONTH_MAP.put("AVRI", "04");
        MONTH_MAP.put("MAI", "05");
        MONTH_MAP.put("JUN", "06"); MONTH_MAP.put("JUIN", "06");
        MONTH_MAP.put("JUL", "07"); MONTH_MAP.put("JUIL", "07");
        MONTH_MAP.put("AOU", "08"); MONTH_MAP.put("AOUT", "08"); MONTH_MAP.put("AOÛ", "08");
        MONTH_MAP.put("SEP", "09"); MONTH_MAP.put("SEPT", "09");
        MONTH_MAP.put("OCT", "10");
        MONTH_MAP.put("NOV", "11");
        MONTH_MAP.put("DEC", "12"); MONTH_MAP.put("DÉC", "12");
    }

    // ══════════════════════════════════════════════════════════════
    // NOUVELLE API — Dossiers mensuels + Tableau WIP
    // ══════════════════════════════════════════════════════════════

    public List<WipTableRowDto> getWipTable(Long projectId) {
        Project project = projectRepo.findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        LocalDate start = project.getStartDate() != null ? project.getStartDate() : LocalDate.now().withDayOfMonth(1);
        LocalDate end   = project.getEndDate()   != null ? project.getEndDate()   : LocalDate.now();

        List<WipTableRowDto> rows = new ArrayList<>();
        LocalDate cursor = start.withDayOfMonth(1);
        LocalDate endMonth = end.withDayOfMonth(1);

        while (!cursor.isAfter(endMonth)) {
            int y = cursor.getYear();
            int m = cursor.getMonthValue();
            String period = String.format("%d-%02d", y, m);

            BigDecimal declared = entryRepo.sumDeclaredAmountByProjectAndPeriod(projectId, period);
            if (declared == null) declared = BigDecimal.ZERO;

            List<WipMonthDocument> docs = docRepo.findByProjectIdAndYearAndMonth(projectId, y, m);

            boolean hasBlSigne = docs.stream().anyMatch(d -> "BL_SIGNE".equals(d.getDocumentType()));
            boolean hasBonCommande = docs.stream().anyMatch(d -> "BON_COMMANDE".equals(d.getDocumentType()));

            BigDecimal invoiced = BigDecimal.ZERO;
            if (hasBlSigne && hasBonCommande) {
                invoiced = docs.stream()
                    .filter(d -> "BL_SIGNE".equals(d.getDocumentType()) && d.getConfirmedAmount() != null)
                    .map(WipMonthDocument::getConfirmedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            WipTableRowDto row = new WipTableRowDto();
            row.setPeriod(period);
            row.setDeclaredAmount(declared.setScale(2, RoundingMode.HALF_UP));
            row.setInvoicedAmount(invoiced.setScale(2, RoundingMode.HALF_UP));
            row.setDelta(declared.subtract(invoiced).setScale(2, RoundingMode.HALF_UP));
            row.setDocCount(docs.size());
            row.setDocuments(docs.stream().map(this::toDocDto).collect(Collectors.toList()));
            rows.add(row);

            cursor = cursor.plusMonths(1);
        }
        return rows;
    }

    // Somme du declare/facture + montant du dernier Bon de Commande confirme (remplace
    // les precedents, ex: avenant) = reste sur la commande consommee par le projet.
    public WipSummaryDto getWipSummary(Long projectId) {
        List<WipTableRowDto> rows = getWipTable(projectId);

        BigDecimal totalDeclared = rows.stream()
            .map(WipTableRowDto::getDeclaredAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalInvoiced = rows.stream()
            .map(WipTableRowDto::getInvoicedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        WipMonthDocument lastOrder = docRepo.findByProjectIdOrderByDate(projectId).stream()
            .filter(d -> "BON_COMMANDE".equals(d.getDocumentType()) && d.getConfirmedAmount() != null)
            .reduce((first, second) -> second) // le plus recent (liste triee par annee/mois croissant)
            .orElse(null);

        WipSummaryDto summary = new WipSummaryDto();
        summary.setTotalDeclared(totalDeclared.setScale(2, RoundingMode.HALF_UP));
        summary.setTotalInvoiced(totalInvoiced.setScale(2, RoundingMode.HALF_UP));
        if (lastOrder != null) {
            summary.setTotalOrderAmount(lastOrder.getConfirmedAmount().setScale(2, RoundingMode.HALF_UP));
            summary.setOrderPeriod(String.format("%d-%02d", lastOrder.getYear(), lastOrder.getMonth()));
            summary.setRemainingOnOrder(
                lastOrder.getConfirmedAmount().subtract(totalInvoiced).setScale(2, RoundingMode.HALF_UP));
        }
        return summary;
    }

    public WipMonthDocumentDto uploadDocument(Long projectId, int year, int month,
                                               String documentType, MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();

        // Créer répertoire si nécessaire
        Path dir = Path.of(UPLOAD_DIR + projectId + "/" + year + "/" + month);
        Files.createDirectories(dir);

        String safeFileName = documentType + "_" + sanitize(file.getOriginalFilename());
        Path filePath = dir.resolve(safeFileName);
        Files.write(filePath, bytes);

        // Extraire montant si BL signé
        BigDecimal extractedAmount = null;
        if ("BL_SIGNE".equals(documentType)) {
            try (PDDocument doc = Loader.loadPDF(bytes)) {
                String text = new PDFTextStripper().getText(doc);
                extractedAmount = extractTotalAmount(text);
            } catch (Exception ignored) {}
        }

        // Remplacer le document existant du même type pour ce mois
        docRepo.findByProjectIdAndYearAndMonthAndDocumentType(projectId, year, month, documentType)
            .ifPresent(existing -> {
                try { Files.deleteIfExists(Path.of(existing.getFilePath())); } catch (Exception ignored) {}
                docRepo.delete(existing);
            });

        WipMonthDocument entity = WipMonthDocument.builder()
            .projectId(projectId)
            .year(year).month(month)
            .documentType(documentType)
            .fileName(file.getOriginalFilename())
            .filePath(filePath.toString())
            .extractedAmount(extractedAmount)
            .uploadedAt(LocalDateTime.now())
            .build();

        return toDocDto(docRepo.save(entity));
    }

    public WipMonthDocumentDto confirmAmount(Long docId, BigDecimal confirmedAmount) {
        WipMonthDocument doc = docRepo.findById(docId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + docId));
        doc.setConfirmedAmount(confirmedAmount);
        return toDocDto(docRepo.save(doc));
    }

    public void deleteDocument(Long docId) {
        WipMonthDocument doc = docRepo.findById(docId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + docId));
        try { Files.deleteIfExists(Path.of(doc.getFilePath())); } catch (Exception ignored) {}
        docRepo.delete(doc);
    }

    public byte[] downloadDocument(Long docId) throws IOException {
        WipMonthDocument doc = docRepo.findById(docId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + docId));
        return Files.readAllBytes(Path.of(doc.getFilePath()));
    }

    public String getDocumentFileName(Long docId) {
        return docRepo.findById(docId)
            .map(WipMonthDocument::getFileName)
            .orElse("document.pdf");
    }

    // ══════════════════════════════════════════════════════════════
    // ANCIENNE API — Parse BL → lignes WIP (conservée)
    // ══════════════════════════════════════════════════════════════

    public List<ProjectWipDto> parseBl(Long projectId, MultipartFile file) throws IOException {
        String text;
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            text = new PDFTextStripper().getText(doc);
        }

        String blNumber = extractBlNumber(text);
        String businessUnit = extractBusinessUnit(text);
        String fileName = file.getOriginalFilename();
        int year = extractYear(text);
        BigDecimal blAmountTotal = extractTotalAmount(text);

        List<BlLine> lines = extractLines(text, year);
        if (lines.isEmpty()) {
            String period = extractPeriodGlobal(text);
            BigDecimal days = extractAllDays(text);
            lines.add(new BlLine("Prestation", period, days));
        }

        BigDecimal totalDaysAll = lines.stream()
            .map(l -> l.days).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ProjectWipDto> result = new ArrayList<>();
        for (BlLine line : lines) {
            ProjectWipDto dto = new ProjectWipDto();
            dto.setProjectId(projectId);
            dto.setBlNumber(blNumber);
            dto.setPeriod(line.period);
            dto.setBusinessUnit(businessUnit);
            dto.setBlDays(line.days);
            dto.setFileName(fileName);
            dto.setStatus("OPEN");
            dto.setNotes(line.designation);

            if (totalDaysAll.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ratio = line.days.divide(totalDaysAll, 4, RoundingMode.HALF_UP);
                dto.setBlAmount(blAmountTotal.multiply(ratio).setScale(2, RoundingMode.HALF_UP));
            } else {
                dto.setBlAmount(blAmountTotal);
            }

            if (line.period != null) {
                BigDecimal billed = getBilledDaysForPeriod(projectId, line.period);
                dto.setBilledDays(billed);
                dto.setWipDays(billed.subtract(line.days).setScale(2, RoundingMode.HALF_UP));
                if (line.days.compareTo(BigDecimal.ZERO) > 0 && dto.getBlAmount() != null) {
                    BigDecimal rate = dto.getBlAmount().divide(line.days, 4, RoundingMode.HALF_UP);
                    dto.setWipAmount(dto.getWipDays().multiply(rate).setScale(2, RoundingMode.HALF_UP));
                }
            }
            result.add(dto);
        }
        return result;
    }

    public List<ProjectWipDto> saveAll(Long projectId, List<ProjectWipDto> dtos) {
        List<ProjectWipDto> saved = new ArrayList<>();
        for (ProjectWipDto dto : dtos) saved.add(save(projectId, dto));
        return saved;
    }

    public ProjectWipDto save(Long projectId, ProjectWipDto dto) {
        List<ProjectWip> existing = wipRepo.findByProjectId(projectId);
        String year = dto.getPeriod() != null ? dto.getPeriod().substring(0, 4)
            : String.valueOf(LocalDate.now().getYear());
        String wipId = String.format("WIP_%d_%s_%04d", projectId, year, existing.size() + 1);

        ProjectWip entity = ProjectWip.builder()
            .projectId(projectId).wipId(wipId)
            .blNumber(dto.getBlNumber()).period(dto.getPeriod())
            .businessUnit(dto.getBusinessUnit())
            .blDays(dto.getBlDays()).blAmount(dto.getBlAmount())
            .billedDays(dto.getBilledDays()).wipDays(dto.getWipDays())
            .wipAmount(dto.getWipAmount() != null ? dto.getWipAmount() : BigDecimal.ZERO)
            .fileName(dto.getFileName()).importedAt(LocalDateTime.now())
            .status(dto.getStatus() != null ? dto.getStatus() : "OPEN")
            .notes(dto.getNotes()).build();

        return toDto(wipRepo.save(entity));
    }

    public List<ProjectWipDto> getByProject(Long projectId) {
        return wipRepo.findByProjectId(projectId).stream().map(this::toDto).collect(Collectors.toList());
    }

    public ProjectWipDto update(Long id, ProjectWipDto dto) {
        ProjectWip entity = wipRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("WIP not found: " + id));
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        if (dto.getNotes() != null) entity.setNotes(dto.getNotes());
        if (dto.getBilledDays() != null) {
            entity.setBilledDays(dto.getBilledDays());
            if (entity.getBlDays() != null)
                entity.setWipDays(dto.getBilledDays().subtract(entity.getBlDays()));
        }
        return toDto(wipRepo.save(entity));
    }

    public void delete(Long id) { wipRepo.deleteById(id); }

    // ══════════════════════════════════════════════════════════════
    // HELPERS PRIVÉS
    // ══════════════════════════════════════════════════════════════

    BigDecimal extractTotalAmount(String text) {
        Pattern p = Pattern.compile("TOTAL\\s+([\\d\\s]+[,.]\\d{2})\\s*[€]?");
        Matcher m = p.matcher(text);
        BigDecimal last = BigDecimal.ZERO;
        while (m.find()) {
            try {
                last = new BigDecimal(m.group(1).replaceAll("\\s", "").replace(",", "."));
            } catch (NumberFormatException ignored) {}
        }
        return last;
    }

    private List<BlLine> extractLines(String text, int year) {
        List<BlLine> lines = new ArrayList<>();
        Pattern linePat = Pattern.compile(
            "Prestation\\s+(\\S+)\\s*\\(([A-ZÉÈÊÀÂa-zéèêàâ]{2,5})\\)[^\\d]*(\\d{1,2}[,.]\\d{2})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher m = linePat.matcher(text);
        while (m.find()) {
            String desig = "Prestation " + m.group(1) + " (" + m.group(2).toUpperCase() + ")";
            String monthNum = MONTH_MAP.get(m.group(2).toUpperCase().trim());
            if (monthNum == null) continue;
            String period = year + "-" + monthNum;
            try {
                BigDecimal days = new BigDecimal(m.group(3).replace(",", "."));
                if (days.compareTo(BigDecimal.ZERO) > 0)
                    lines.add(new BlLine(desig, period, days));
            } catch (NumberFormatException ignored) {}
        }
        return lines;
    }

    private String extractBlNumber(String text) {
        Pattern p = Pattern.compile("BON DE LIVRAISON[\\s\\-–:]+N°?\\s*:?\\s*(.+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : "";
    }

    private String extractBusinessUnit(String text) {
        Pattern p = Pattern.compile("Business Unit[:\\s]*[\\r\\n\\s]*([A-Z]\\d{4,6})");
        Matcher m = p.matcher(text);
        if (m.find()) return m.group(1).trim();
        Pattern fb = Pattern.compile("\\b([A-Z]\\d{5})\\b");
        Matcher mf = fb.matcher(text);
        return mf.find() ? mf.group(1).trim() : "";
    }

    private int extractYear(String text) {
        Matcher m = Pattern.compile("(20\\d{2})").matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) : LocalDate.now().getYear();
    }

    private String extractPeriodGlobal(String text) {
        Pattern p = Pattern.compile("P[eé]riode\\s*:?\\s*(\\d{2}-\\d{4})");
        Matcher m = p.matcher(text);
        if (m.find()) {
            String[] parts = m.group(1).split("-");
            return parts[1] + "-" + parts[0];
        }
        return null;
    }

    private BigDecimal extractAllDays(String text) {
        int from = Math.max(text.indexOf("Prestation"), 0);
        int to = text.lastIndexOf("TOTAL");
        if (to <= from) return BigDecimal.ZERO;
        String section = text.substring(from, to);
        BigDecimal total = BigDecimal.ZERO;
        Matcher m = Pattern.compile("\\b(\\d{1,2}[,.]\\d{2})\\b").matcher(section);
        while (m.find()) {
            try {
                BigDecimal v = new BigDecimal(m.group(1).replace(",", "."));
                if (v.compareTo(BigDecimal.valueOf(0.5)) >= 0 && v.compareTo(BigDecimal.valueOf(99)) <= 0)
                    total = total.add(v);
            } catch (NumberFormatException ignored) {}
        }
        return total;
    }

    private BigDecimal getBilledDaysForPeriod(Long projectId, String period) {
        BigDecimal total = entryRepo.sumBilledDaysByProjectAndPeriod(projectId, period);
        return total == null ? BigDecimal.ZERO : total.setScale(2, RoundingMode.HALF_UP);
    }

    private String sanitize(String name) {
        if (name == null) return "file.pdf";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private WipMonthDocumentDto toDocDto(WipMonthDocument e) {
        WipMonthDocumentDto d = new WipMonthDocumentDto();
        d.setId(e.getId());
        d.setYear(e.getYear()); d.setMonth(e.getMonth());
        d.setDocumentType(e.getDocumentType());
        d.setFileName(e.getFileName());
        d.setExtractedAmount(e.getExtractedAmount());
        d.setConfirmedAmount(e.getConfirmedAmount());
        d.setUploadedAt(e.getUploadedAt());
        return d;
    }

    private ProjectWipDto toDto(ProjectWip e) {
        ProjectWipDto d = new ProjectWipDto();
        d.setId(e.getId()); d.setWipId(e.getWipId()); d.setProjectId(e.getProjectId());
        d.setBlNumber(e.getBlNumber()); d.setPeriod(e.getPeriod());
        d.setBusinessUnit(e.getBusinessUnit()); d.setBlDays(e.getBlDays());
        d.setBlAmount(e.getBlAmount()); d.setBilledDays(e.getBilledDays());
        d.setWipDays(e.getWipDays()); d.setWipAmount(e.getWipAmount());
        d.setFileName(e.getFileName()); d.setImportedAt(e.getImportedAt());
        d.setStatus(e.getStatus()); d.setNotes(e.getNotes());
        return d;
    }

    private static class BlLine {
        String designation; String period; BigDecimal days;
        BlLine(String d, String p, BigDecimal j) { designation = d; period = p; days = j; }
    }
}
