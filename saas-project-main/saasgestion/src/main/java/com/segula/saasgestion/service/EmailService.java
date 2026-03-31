package com.segula.saasgestion.service;

import com.segula.saasgestion.dto.ProjectCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Async
    public void sendApprovalRequestToAdmin(
            String adminEmail, String adminName,
            String submitterName, String submitterEmail,
            ProjectCreateRequest req, String approvalToken) {

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(adminEmail);
            helper.setSubject("[SEGULA] Nouveau projet à approuver - " + req.getActivity());

            String approveUrl = baseUrl + "/admin/approve/" + approvalToken + "?action=approve";
            String rejectUrl  = baseUrl + "/admin/approve/" + approvalToken + "?action=reject";

            String html = """
                <div style='font-family:Arial,sans-serif'>
                    <h2>Nouveau projet à approuver</h2>
                    <p>Bonjour <b>%s</b>,</p>
                    <p><b>%s</b> (%s) a soumis un projet.</p>

                    <table border='1' style='border-collapse:collapse;width:100%%'>
                        <tr><td><b>Activité</b></td><td>%s</td></tr>
                        <tr><td><b>Front Financier</b></td><td>%s</td></tr>
                        <tr><td><b>Revenue Budget</b></td><td>%s EUR</td></tr>
                        <tr><td><b>Cost Budget</b></td><td>%s EUR</td></tr>
                        <tr><td><b>BU</b></td><td>%s</td></tr>
                    </table>

                    <br>
                    <a href='%s' style='background:#16a34a;color:#fff;padding:10px 20px;text-decoration:none;border-radius:5px;margin-right:10px'>
                        Approuver
                    </a>
                    <a href='%s' style='background:#dc2626;color:#fff;padding:10px 20px;text-decoration:none;border-radius:5px'>
                        Rejeter
                    </a>

                    <p style='color:#64748b;font-size:12px'>Ce lien expire dans 48 heures.</p>
                </div>
            """.formatted(
                adminName, submitterName, submitterEmail,
                req.getActivity(), req.getFrontFinancier(),
                req.getRevenueBudget(), req.getCostBudget(),
                req.getBuId(), approveUrl, rejectUrl
            );

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email approbation envoyé à {}", adminEmail);

        } catch (Exception e) {
            log.error("Erreur email admin {} : {}", adminEmail, e.getMessage());
        }
    }

    @Async
    public void sendApprovalConfirmationToUser(
            String userEmail, String userName, String activity) {

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(userEmail);
            helper.setSubject("[SEGULA] Projet approuvé");

            String html = """
                <div style='font-family:Arial,sans-serif'>
                    <h2>Projet approuvé</h2>
                    <p>Bonjour <b>%s</b>,</p>
                    <p>Votre projet <b>%s</b> a été approuvé.</p>
                    <p><a href='%s'>Voir le dashboard</a></p>
                </div>
            """.formatted(userName, activity, baseUrl);

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            log.error("Erreur email confirmation user {} : {}", userEmail, e.getMessage());
        }
    }

    @Async
    public void sendRejectionToUser(
            String userEmail, String userName, String activity, String reason) {

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(userEmail);
            helper.setSubject("[SEGULA] Projet non approuvé");

            String html = """
                <div style='font-family:Arial,sans-serif'>
                    <h2>Projet non approuvé</h2>
                    <p>Bonjour <b>%s</b>,</p>
                    <p>Votre projet <b>%s</b> n'a pas été approuvé.</p>
                    %s
                </div>
            """.formatted(
                userName, activity,
                reason != null ? "<p><b>Motif :</b> " + reason + "</p>" : ""
            );

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            log.error("Erreur email rejet user {} : {}", userEmail, e.getMessage());
        }
    }
}