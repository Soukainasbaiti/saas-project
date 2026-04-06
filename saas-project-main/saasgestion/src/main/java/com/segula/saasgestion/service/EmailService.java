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
            ProjectCreateRequest req, String approvalToken) {

        try {
            log.info("=== DEBUT ENVOI EMAIL ADMIN via Brevo API ===");
            log.info("To: {}", adminEmail);

            String approveUrl = baseUrl + "/admin/approve/" + approvalToken;
            String rejectUrl  = baseUrl + "/admin/approve/" + approvalToken + "?action=reject";

            String html = """
                <div style='font-family:Arial,sans-serif'>
                    <h2>Nouveau projet a approuver</h2>
                    <p>Bonjour <b>%s</b>,</p>
                    <p><b>%s</b> (%s) a soumis un projet.</p>
                    <table border='1' style='border-collapse:collapse;width:100%%'>
                        <tr><td><b>Activite</b></td><td>%s</td></tr>
                        <tr><td><b>Front Financier</b></td><td>%s</td></tr>
                        <tr><td><b>Revenue Budget</b></td><td>%s EUR</td></tr>
                        <tr><td><b>Cost Budget</b></td><td>%s EUR</td></tr>
                        <tr><td><b>BU</b></td><td>%s</td></tr>
                    </table>
                    <br>
                    <a href='%s' style='background:#16a34a;color:#fff;padding:10px 20px;text-decoration:none;border-radius:5px;margin-right:10px'>Approuver</a>
                    <a href='%s' style='background:#dc2626;color:#fff;padding:10px 20px;text-decoration:none;border-radius:5px'>Rejeter</a>
                    <p style='color:#64748b;font-size:12px'>Ce lien expire dans 48 heures.</p>
                </div>
                """.formatted(
                    adminName, submitterName, submitterEmail,
                    req.getActivity(), req.getFrontFinancier(),
                    req.getRevenueBudget(), req.getCostBudget(),
                    req.getBuId(), approveUrl, rejectUrl
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
            String userEmail, String userName, String activity, String reason) {

        try {
            log.info("=== DEBUT ENVOI EMAIL REJET USER {} ===", userEmail);

            String html = """
                <div style='font-family:Arial,sans-serif'>
                    <h2>Projet non approuve</h2>
                    <p>Bonjour <b>%s</b>,</p>
                    <p>Votre projet <b>%s</b> n'a pas ete approuve.</p>
                    %s
                </div>
                """.formatted(
                    userName, activity,
                    reason != null ? "<p><b>Motif :</b> " + reason + "</p>" : ""
            );

            sendViaBrevo(userEmail, userName, "[SEGULA] Projet non approuve", html);

            log.info("=== EMAIL REJET ENVOYE a {} ===", userEmail);

        } catch (Exception e) {
            log.error("=== ERREUR EMAIL REJET === {}", e.getMessage(), e);
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
