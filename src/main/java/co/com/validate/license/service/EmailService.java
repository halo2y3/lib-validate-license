package co.com.validate.license.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestClient;

import co.com.validate.license.exception.EmailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final RestClient mailerSendRestClient;

    @Value("${email.from}")
    private String fromEmail;

    @Value("${email.enabled}")
    private boolean emailEnabled;

    /**
     * Sends a license creation notification email using HTML template
     *
     * @param email The recipient email address
     * @param licenseKey The generated license key
     * @param expirationDate The license expiration date
     */
    public void sendLicenseCreationEmail(String email, String licenseKey, LocalDate expirationDate) {
        if (!emailEnabled) {
            log.info("Email notifications are disabled. Skipping email to: {}", email);
            return;
        }

        try {
            String htmlContent = buildEmailContent(licenseKey, expirationDate);
            Map<String, Object> payload = buildPayload(email, "License Created Successfully - " + licenseKey, htmlContent);

            mailerSendRestClient.post().body(payload).retrieve().toBodilessEntity();
            log.info("License creation email (HTML) sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send license creation email to: {}. Error: {}", email, e.getMessage(), e);
            // Don't throw exception - email failure should not block license creation
        }
    }

    /**
     * Sends a license expiration warning email (1 day before expiration) using HTML template
     *
     * @param email The recipient email address
     * @param licenseKey The license key that will expire
     * @param expirationDate The license expiration date
     */
    public void sendLicenseExpirationWarning(String email, String licenseKey, LocalDate expirationDate) {
        if (!emailEnabled) {
            log.info("Email notifications are disabled. Skipping expiration warning to: {}", email);
            return;
        }

        try {
            String htmlContent = buildExpirationWarningContent(licenseKey, expirationDate);
            Map<String, Object> payload = buildPayload(email, "License Expiration Warning - " + licenseKey, htmlContent);

            mailerSendRestClient.post().body(payload).retrieve().toBodilessEntity();
            log.info("License expiration warning (HTML) sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send expiration warning to: {}. Error: {}", email, e.getMessage(), e);
        }
    }

    private Map<String, Object> buildPayload(String toEmail, String subject, String htmlContent) {
        return Map.of(
                "from", Map.of("email", fromEmail),
                "to", List.of(Map.of("email", toEmail)),
                "subject", subject,
                "html", htmlContent
        );
    }

    private String buildEmailContent(String licenseKey, LocalDate expirationDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try {
            ClassPathResource resource = new ClassPathResource("LicenseCreate.html");
            byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
            String htmlTemplate = new String(bytes, StandardCharsets.UTF_8);

            return htmlTemplate
                    .replace("#LICENSE_NUMBER", licenseKey)
                    .replace("#EXPIRATION_DATE", expirationDate.format(formatter));
        } catch (IOException e) {
            throw new EmailException("Error en buildEmailContent", e);
        }
    }

    private String buildExpirationWarningContent(String licenseKey, LocalDate expirationDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try {
            ClassPathResource resource = new ClassPathResource("LicenseExpiration.html");
            byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
            String htmlTemplate = new String(bytes, StandardCharsets.UTF_8);

            return htmlTemplate
                    .replace("#LICENSE_NUMBER", licenseKey)
                    .replace("#EXPIRATION_DATE", expirationDate.format(formatter));
        } catch (IOException e) {
            throw new EmailException("Error en buildExpirationWarningContent", e);
        }
    }
}
