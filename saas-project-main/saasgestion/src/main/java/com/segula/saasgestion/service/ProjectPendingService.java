package com.segula.saasgestion.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.segula.saasgestion.domain.AppUser;
import com.segula.saasgestion.domain.ProjectPending;
import com.segula.saasgestion.dto.ProjectCreateRequest;
import com.segula.saasgestion.dto.ProjectDetailDto;
import com.segula.saasgestion.repository.AppUserRepository;
import com.segula.saasgestion.repository.ProjectPendingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectPendingService {

    private final ProjectPendingRepository pendingRepo;
    private final AppUserRepository        userRepo;
    private final ProjectService           projectService;
    private final EmailService             emailService;

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

        // Notifier tous les admins
        List<AppUser> admins = userRepo.findAllActiveAdmins();
        if (admins.isEmpty()) {
            log.warn("Aucun admin actif trouvé pour l'approbation du projet");
        }
        for (AppUser admin : admins) {
            emailService.sendApprovalRequestToAdmin(
                    admin.getEmail(), admin.getFullName(),
                    submitter.getFullName(), submitter.getEmail(),
                    req, token
            );
        }

        log.info("Projet soumis pour approbation par userId={}, token={}", submitterId, token);
        return token;
    }

    // ── Approuver un projet ───────────────────────────────────────
    @Transactional
    public ProjectDetailDto approve(String token, Long adminId) {
        ProjectPending pending = getPendingOrThrow(token);

        AppUser admin = userRepo.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin introuvable"));

        ProjectCreateRequest req = deserialize(pending.getPayload());

        // Créer le projet réel en BD
        ProjectDetailDto created = projectService.create(req);

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

        AppUser admin = userRepo.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin introuvable"));

        ProjectCreateRequest req = deserialize(pending.getPayload());

        pending.setStatus("REJECTED");
        pending.setReviewedBy(admin);
        pending.setReviewedAt(OffsetDateTime.now());
        pendingRepo.save(pending);

        AppUser submitter = pending.getSubmittedBy();
        emailService.sendRejectionToUser(
                submitter.getEmail(), submitter.getFullName(),
                req.getActivity(), reason
        );

        log.info("Projet rejeté par adminId={}, token={}", adminId, token);
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
