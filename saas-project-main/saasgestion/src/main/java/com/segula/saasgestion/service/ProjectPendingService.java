package com.segula.saasgestion.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.segula.saasgestion.domain.AppUser;
import com.segula.saasgestion.domain.ProjectPending;
import com.segula.saasgestion.dto.ProjectCreateRequest;
import com.segula.saasgestion.dto.ProjectDetailDto;
import com.segula.saasgestion.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectPendingService {

    private final ProjectPendingRepository       pendingRepo;
    private final AppUserRepository              userRepo;
    private final BuRepository                   buRepo;
    private final CustomerRepository             customerRepo;
    private final IndustryRepository             industryRepo;
    private final EngineeringDisciplineRepository disciplineRepo;
    private final EngagementRepository           engagementRepo;
    private final ProjectService                 projectService;
    private final EmailService                   emailService;

    @Value("${app.base-url}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    // ── Soumettre un projet pour approbation ──────────────────────
    @Transactional
    public String submitForApproval(ProjectCreateRequest req, Long submitterId) {
        AppUser submitter = userRepo.findById(submitterId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        String payload;
        try {
            payload = objectMapper.writeValueAsString(req);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erreur sérialisation projet", e);
        }

        String token = UUID.randomUUID().toString();

        ProjectPending pending = ProjectPending.builder()
                .payload(payload)
                .submittedBy(submitter)
                .approvalToken(token)
                .status("PENDING")
                .expiresAt(OffsetDateTime.now().plusHours(48))
                .build();

        pendingRepo.save(pending);

        // Résoudre les libellés pour l'email
        String pmName          = userRepo.findById(req.getProjectManagerId())
                                    .map(u -> u.getFullName()).orElse("—");
        String buId            = req.getBuId();
        String buTrigram       = buRepo.findById(req.getBuId())
                                    .map(b -> b.getTrigram()).orElse("—");
        String industryName    = industryRepo.findById(req.getIndustryId())
                                    .map(i -> i.getName()).orElse("—");
        String industryTrigram = industryRepo.findById(req.getIndustryId())
                                    .map(i -> i.getTrigram()).orElse("—");
        String customerName    = customerRepo.findById(req.getCustomerId())
                                    .map(c -> c.getName()).orElse("—");
        String customerTrigram = customerRepo.findById(req.getCustomerId())
                                    .map(c -> c.getTrigram()).orElse("—");
        String discName        = disciplineRepo.findById(req.getEngineeringDisciplineId())
                                    .map(d -> d.getName()).orElse("—");
        String engName         = engagementRepo.findById(req.getEngagementId())
                                    .map(e -> e.getName()).orElse("—");

        // Construire le nom du projet (même logique que ProjectService.buildName)
        String projectName = String.join(" - ",
                req.getFrontFinancier(), buId, buTrigram,
                industryTrigram, customerTrigram, req.getActivity()
        );

        // Notifier tous les admins
        List<AppUser> admins = userRepo.findAllActiveAdmins();
        if (admins.isEmpty()) {
            log.warn("Aucun admin actif trouvé pour l'approbation du projet");
        }
        for (AppUser admin : admins) {
            emailService.sendApprovalRequestToAdmin(
                    admin.getEmail(), admin.getFullName(),
                    submitter.getFullName(), submitter.getEmail(),
                    req, token, projectName,
                    pmName, buId, buTrigram, industryName, customerName,
                    discName, engName
            );
        }

        log.info("Projet soumis pour approbation par userId={}, token={}", submitterId, token);
        return token;
    }

    // ── Lister tous les projets PENDING (pour l'admin) ───────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAllPending() {
        return pendingRepo.findAllPending().stream().map(p -> {
            ProjectCreateRequest req = deserialize(p.getPayload());
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("token",       p.getApprovalToken());
            item.put("status",      p.getStatus());
            item.put("submittedBy", p.getSubmittedBy().getFullName());
            item.put("submittedAt", p.getCreatedAt());
            item.put("expiresAt",   p.getExpiresAt());
            item.put("activity",    req.getActivity());
            item.put("buId",        req.getBuId());
            item.put("frontFinancier", req.getFrontFinancier());
            item.put("revenueBudget",  req.getRevenueBudget());
            item.put("costBudget",     req.getCostBudget());
            return item;
        }).collect(java.util.stream.Collectors.toList());
    }

    // ── Approuver un projet ───────────────────────────────────────
    @Transactional
    public ProjectDetailDto approve(String token, Long adminId) {
        ProjectPending pending = getPendingOrThrow(token);

        // adminId peut être null si l'admin accède via lien email sans être connecté
        AppUser admin = (adminId != null)
                ? userRepo.findById(adminId).orElse(null)
                : null;

        ProjectCreateRequest req = deserialize(pending.getPayload());

        // Créer le projet avec createdById = submitter (le vrai créateur du projet)
        Long submitterId = pending.getSubmittedBy().getId();
        ProjectDetailDto created = projectService.create(req, submitterId);

        // Mettre à jour le statut pending
        pending.setStatus("APPROVED");
        pending.setReviewedBy(admin);
        pending.setReviewedAt(OffsetDateTime.now());
        pendingRepo.save(pending);

        // Notifier le soumetteur
        AppUser submitter = pending.getSubmittedBy();
        emailService.sendApprovalConfirmationToUser(
                submitter.getEmail(), submitter.getFullName(), req.getActivity()
        );

        log.info("Projet approuvé par adminId={}, token={}", adminId, token);
        return created;
    }

    // ── Rejeter un projet ─────────────────────────────────────────
    @Transactional
    public void reject(String token, Long adminId, String reason) {
        ProjectPending pending = getPendingOrThrow(token);

        // adminId peut être null si l'admin accède via lien email sans être connecté
        AppUser admin = (adminId != null)
                ? userRepo.findById(adminId).orElse(null)
                : null;

        ProjectCreateRequest req = deserialize(pending.getPayload());

        pending.setStatus("REJECTED");
        pending.setReviewedBy(admin);
        pending.setReviewedAt(OffsetDateTime.now());
        pendingRepo.save(pending);

        AppUser submitter = pending.getSubmittedBy();
        String editUrl = baseUrl + "/projects/edit/" + token;
        emailService.sendRejectionToUser(
                submitter.getEmail(), submitter.getFullName(),
                req.getActivity(), reason, editUrl
        );

        log.info("Projet rejeté par adminId={}, token={}", adminId, token);
    }

    // ── Récupérer un projet rejeté pour modification ─────────────
    @Transactional(readOnly = true)
    public PendingDetailView getForEdit(String token) {
        ProjectPending pending = pendingRepo.findByApprovalToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token invalide ou introuvable"));

        if ("APPROVED".equals(pending.getStatus())) {
            throw new IllegalStateException("Ce projet a été approuvé et ne peut plus être modifié.");
        }
        if (!"REJECTED".equals(pending.getStatus())) {
            throw new IllegalStateException("Ce projet n'a pas été rejeté.");
        }

        ProjectCreateRequest req = deserialize(pending.getPayload());
        return new PendingDetailView(pending, req);
    }

    // ── Supprimer définitivement un projet rejeté ─────────────────
    @Transactional
    public void deleteRejected(String token, Long userId) {
        ProjectPending pending = pendingRepo.findByApprovalToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token invalide ou introuvable"));

        if (!"REJECTED".equals(pending.getStatus())) {
            throw new IllegalStateException("Seuls les projets rejetés peuvent être supprimés.");
        }

        // Vérifier que c'est bien le soumetteur qui supprime
        if (!pending.getSubmittedBy().getId().equals(userId)) {
            throw new IllegalStateException("Vous n'êtes pas autorisé à supprimer ce projet.");
        }

        pendingRepo.delete(pending);
        log.info("Projet rejeté supprimé définitivement, token={} par userId={}", token, userId);
    }

    // ── Resoumettre un projet modifié après rejet ─────────────────
    @Transactional
    public String resubmit(String token, ProjectCreateRequest modifiedReq, Long submitterId) {
        ProjectPending original = pendingRepo.findByApprovalToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token invalide"));

        if (!"REJECTED".equals(original.getStatus())) {
            throw new IllegalStateException("Seuls les projets rejetés peuvent être resoumis.");
        }

        log.info("Resoumission du projet token={} par userId={}", token, submitterId);
        return submitForApproval(modifiedReq, submitterId);
    }

    // ── Lire les détails d'un pending (pour la page admin) ───────
    @Transactional(readOnly = true)
    public PendingDetailView getPendingDetail(String token) {
        ProjectPending pending = getPendingOrThrow(token);
        ProjectCreateRequest req = deserialize(pending.getPayload());
        return new PendingDetailView(pending, req);
    }

    // ── Utilitaires privés ────────────────────────────────────────
    private ProjectPending getPendingOrThrow(String token) {
        ProjectPending pending = pendingRepo.findByApprovalToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token invalide ou introuvable"));

        if (!"PENDING".equals(pending.getStatus())) {
            throw new IllegalStateException("Ce projet a déjà été traité (statut: " + pending.getStatus() + ")");
        }
        if (pending.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("Ce lien d'approbation a expiré");
        }
        return pending;
    }

    private ProjectCreateRequest deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, ProjectCreateRequest.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erreur désérialisation projet", e);
        }
    }

    // ── DTO interne pour la vue admin ─────────────────────────────
    public record PendingDetailView(ProjectPending pending, ProjectCreateRequest request) {}
}
