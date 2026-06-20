package com.segula.saasgestion.service;

import com.segula.saasgestion.dto.ProjectCreateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.brevo.api-key}")
    private String brevoApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    @Async
    public void sendApprovalRequestToAdmin(
            String adminEmail, String adminName,
            String submitterName, String submitterEmail,
            ProjectCreateRequest req, String approvalToken, String projectName,
            String pmName, String buId, String buTrigram,
            String industryName, String customerName,
            String discName, String engName) {

        try {
            log.info("=== DEBUT ENVOI EMAIL ADMIN via Brevo API ===");
            log.info("To: {}", adminEmail);

            String approveUrl = baseUrl + "/admin/approve/" + approvalToken;

            // Calcul marge
            double rev  = req.getRevenueBudget()  != null ? req.getRevenueBudget().doubleValue()  : 0;
            double cost = req.getCostBudget()      != null ? req.getCostBudget().doubleValue()     : 0;
            String marge = rev > 0
                ? String.format("%.0f%%", ((rev - cost) / rev) * 100)
                : "—";

            // Perimeter = Discipline : Fonction
            String perimeter = discName;
            if (req.getFunctionName() != null && !req.getFunctionName().isBlank()) {
                perimeter = discName + " : " + req.getFunctionName();
            }

            // FO/BO abrégé
            String foBo = "Back Office".equalsIgnoreCase(req.getTechnicalOffice()) ? "BO" : "FO";

            // Dates
            String startDate = req.getStartDate() != null ? req.getStartDate().toString() : "—";
            String endDate   = req.getEndDate()   != null ? req.getEndDate().toString()   : "—";

            String tdStyle = "padding:6px 10px;border:1px solid #ccc;";
            String thStyle = tdStyle + "background:#f1f5f9;font-weight:600;width:200px;";

            String html = """
                <div style='font-family:Arial,sans-serif;max-width:650px;margin:0 auto'>
                    <div style='background:#1e3a5f;padding:16px 24px;border-radius:8px 8px 0 0'>
                        <h2 style='color:#fff;margin:0;font-size:18px'>Demande de creation de ligne projet</h2>
                    </div>
                    <div style='padding:20px;background:#fff;border:1px solid #e2e8f0;border-top:none'>
                        <p>Bonjour <b>%s</b>,</p>
                        <p>Ci-apres une demande de creation de ligne projet <b>"%s"</b> soumise par <b>%s</b> (%s) :</p>
                        <table style='border-collapse:collapse;width:100%%'>
                            <tr><td style='%s'>Chef de projet</td><td style='%s'>%s</td></tr>
                            <tr><td style='%s'>ID BU</td><td style='%s'>%s</td></tr>
                            <tr><td style='%s'>BU</td><td style='%s'>%s</td></tr>
                            <tr><td style='%s'>Indus</td><td style='%s'>%s</td></tr>
                            <tr><td style='%s'>Client final</td><td style='%s'>%s</td></tr>
                            <tr><td style='%s'>Entite de facturation</td><td style='%s'>%s</td></tr>
                            <tr><td style='%s'>Perimeter</td><td style='%s'>%s</td></tr>
                            <tr><td style='%s'>Nom d activite (ou projet)</td><td style='%s'>%s</td></tr>
                            <tr><td style='%s'>Start date</td><td style='%s'>%s</td></tr>
                            <tr><td style='%s'>End date</td><td style='%s'>%s</td></tr>
                            <tr><td style='%s'>Major Project</td><td style='%s'>%s</td></tr>
                            <tr><td style='%s'>Engagement</td><td style='%s'>%s</td></tr>
                            <tr><td style='%s'>FO/BO</td><td style='%s'>%s</td></tr>
                            <tr><td style='%s'>Previsions Ventes</td><td style='%s'>%s EUR</td></tr>
                            <tr><td style='%s'>Previsions Couts</td><td style='%s'>%s EUR</td></tr>
                            <tr><td style='%s'>Marge Projet</td><td style='%s'>%s</td></tr>
                        </table>
                        <br>
                        <p>Merci.<br>Cordialement.</p>
                        <div style='margin-top:24px'>
                            <a href='%s' style='background:#16a34a;color:#fff;padding:12px 24px;text-decoration:none;border-radius:6px;font-weight:600'>
                                Approuver / Rejeter le projet
                            </a>
                        </div>
                        <p style='color:#64748b;font-size:12px;margin-top:16px'>Ce lien expire dans 48 heures.</p>
                    </div>
                </div>
                """.formatted(
                    adminName, projectName, submitterName, submitterEmail,
                    thStyle, tdStyle, pmName,
                    thStyle, tdStyle, buId,
                    thStyle, tdStyle, buTrigram,
                    thStyle, tdStyle, industryName,
                    thStyle, tdStyle, customerName,
                    thStyle, tdStyle, req.getFrontFinancier(),
                    thStyle, tdStyle, perimeter,
                    thStyle, tdStyle, req.getActivity(),
                    thStyle, tdStyle, startDate,
                    thStyle, tdStyle, endDate,
                    thStyle, tdStyle, req.isMajorProject() ? "Oui" : "Non",
                    thStyle, tdStyle, engName,
                    thStyle, tdStyle, foBo,
                    thStyle, tdStyle, req.getRevenueBudget(),
                    thStyle, tdStyle, req.getCostBudget(),
                    thStyle, tdStyle, marge,
                    approveUrl
            );

            sendViaBrevo(adminEmail, adminName,
                    "[SEGULA] Nouveau projet a approuver - " + req.getActivity(), html);

            log.info("=== EMAIL ADMIN ENVOYE AVEC SUCCES a {} ===", adminEmail);

        } catch (Exception e) {
            log.error("=== ERREUR ENVOI EMAIL ADMIN === {}", e.getMessage(), e);
        }
    }

    @Async
    public void sendApprovalConfirmationToUser(
            String userEmail, String userName, String activity) {

        try {
            log.info("=== DEBUT ENVOI EMAIL CONFIRMATION USER {} ===", userEmail);

            String html = """
                <div style='font-family:Arial,sans-serif'>
                    <h2>Projet approuve</h2>
                    <p>Bonjour <b>%s</b>,</p>
                    <p>Votre projet <b>%s</b> a ete approuve.</p>
                    <p><a href='%s'>Voir le dashboard</a></p>
                </div>
                """.formatted(userName, activity, baseUrl);

            sendViaBrevo(userEmail, userName, "[SEGULA] Projet approuve", html);

            log.info("=== EMAIL CONFIRMATION ENVOYE a {} ===", userEmail);

        } catch (Exception e) {
            log.error("=== ERREUR EMAIL CONFIRMATION === {}", e.getMessage(), e);
        }
    }

    @Async
    public void sendRejectionToUser(
            String userEmail, String userName, String activity, String reason, String editUrl) {

        try {
            log.info("=== DEBUT ENVOI EMAIL REJET USER {} ===", userEmail);

            String html = """
                <div style='font-family:Arial,sans-serif'>
                    <h2>Projet non approuve</h2>
                    <p>Bonjour <b>%s</b>,</p>
                    <p>Votre projet <b>%s</b> n'a pas ete approuve.</p>
                    %s
                    <br>
                    <p>Vous pouvez modifier votre projet et le soumettre a nouveau :</p>
                    <a href='%s' style='background:#2563eb;color:#fff;padding:10px 20px;text-decoration:none;border-radius:5px;display:inline-block'>
                        Modifier et resoumettre le projet
                    </a>
                    <p style='color:#64748b;font-size:12px;margin-top:16px'>Ce lien expire dans 48 heures.</p>
                </div>
                """.formatted(
                    userName, activity,
                    reason != null ? "<p><b>Motif :</b> " + reason + "</p>" : "",
                    editUrl
            );

            sendViaBrevo(userEmail, userName, "[SEGULA] Projet non approuve - modification requise", html);

            log.info("=== EMAIL REJET ENVOYE a {} ===", userEmail);

        } catch (Exception e) {
            log.error("=== ERREUR EMAIL REJET === {}", e.getMessage(), e);
        }
    }

    @Async
    public void sendOnePagerSubmittedToAdmin(
            String adminEmail, String adminName,
            String pmName, Long projectId,
            String projectName, java.math.BigDecimal revenueBudget, java.math.BigDecimal costBudget) {

        try {
            log.info("=== ENVOI EMAIL One Pager SUBMITTED à admin {} ===", adminEmail);

            double rev  = revenueBudget  != null ? revenueBudget.doubleValue()  : 0;
            double cost = costBudget     != null ? costBudget.doubleValue()      : 0;
            String marge = rev > 0
                ? String.format("%.0f%%", ((rev - cost) / rev) * 100)
                : "—";
            String dashboardUrl = baseUrl + "/projects/" + projectId + "/management";

            String tdStyle = "padding:6px 10px;border:1px solid #ccc;";
            String thStyle = tdStyle + "background:#f1f5f9;font-weight:600;width:200px;";

            String html = """
                <div style='font-family:Arial,sans-serif;max-width:620px;margin:0 auto'>
                    <div style='background:#1e3a5f;padding:16px 24px;border-radius:8px 8px 0 0'>
                        <h2 style='color:#fff;margin:0;font-size:18px'>One Pager soumis pour validation</h2>
                    </div>
                    <div style='padding:20px;background:#fff;border:1px solid #e2e8f0;border-top:none'>
                        <p>Bonjour <b>%s</b>,</p>
                        <p>Le PM <b>%s</b> a soumis le One Pager du projet suivant pour votre validation :</p>
                        <table style='border-collapse:collapse;width:100%%'>
                            <tr><td style='%s'>Projet</td><td style='%s'>%s</td></tr>
                            <tr><td style='%s'>Chef de projet</td><td style='%s'>%s</td></tr>
                            <tr><td style='%s'>Previsions CA</td><td style='%s'>%.0f EUR</td></tr>
                            <tr><td style='%s'>Previsions Couts</td><td style='%s'>%.0f EUR</td></tr>
                            <tr><td style='%s'>Marge Projet</td><td style='%s'>%s</td></tr>
                        </table>
                        <br>
                        <p>Cliquez ci-dessous pour accéder au projet et valider ou rejeter le One Pager :</p>
                        <div style='margin-top:16px'>
                            <a href='%s' style='background:#1e3a5f;color:#fff;padding:12px 24px;text-decoration:none;border-radius:6px;font-weight:600'>
                                Voir et valider le projet
                            </a>
                        </div>
                        <p style='color:#64748b;font-size:12px;margin-top:16px'>Connectez-vous sur la plateforme SaaS Gestion pour approuver ou rejeter ce One Pager.</p>
                    </div>
                </div>
                """.formatted(
                    adminName,
                    pmName,
                    thStyle, tdStyle, projectName,
                    thStyle, tdStyle, pmName,
                    thStyle, tdStyle, rev,
                    thStyle, tdStyle, cost,
                    thStyle, tdStyle, marge,
                    dashboardUrl
            );

            sendViaBrevo(adminEmail, adminName,
                "[SEGULA] One Pager à valider — " + projectName, html);

            log.info("=== EMAIL One Pager SUBMITTED envoyé à {} ===", adminEmail);

        } catch (Exception e) {
            log.error("=== ERREUR EMAIL One Pager SUBMITTED === {}", e.getMessage(), e);
        }
    }

    private void sendViaBrevo(String toEmail, String toName, String subject, String html) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);

        Map<String, Object> body = Map.of(
            "sender",  Map.of("name", "SEGULA SaaS", "email", fromAddress),
            "to",      List.of(Map.of("email", toEmail, "name", toName)),
            "subject", subject,
            "htmlContent", html
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(BREVO_API_URL, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Brevo API erreur: " + response.getStatusCode() + " - " + response.getBody());
        }
    }
}
