package co.com.validate.license.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import co.com.validate.license.exception.EmailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

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
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("License Created Successfully - " + licenseKey);

            // Load and process HTML template
            String htmlContent = buildEmailContent(licenseKey, expirationDate);
            helper.setText(htmlContent, true); // true indicates HTML content

            mailSender.send(mimeMessage);
            log.info("License creation email (HTML) sent successfully to: {}", email);
        } catch (MessagingException e) {
            log.error("Failed to create license creation email for: {}. Error: {}", email, e.getMessage(), e);
            // Don't throw exception - email failure should not block license creation
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
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("License Expiration Warning - " + licenseKey);

            // Load and process HTML template
            String htmlContent = buildExpirationWarningContent(licenseKey, expirationDate);
            helper.setText(htmlContent, true); // true indicates HTML content

            mailSender.send(mimeMessage);
            log.info("License expiration warning (HTML) sent successfully to: {}", email);
        } catch (MessagingException e) {
            log.error("Failed to create expiration warning email for: {}. Error: {}", email, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to send expiration warning to: {}. Error: {}", email, e.getMessage(), e);
        }
    }

    private String buildEmailContent(String licenseKey, LocalDate expirationDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try {
            // Load HTML template from resources
            ClassPathResource resource = new ClassPathResource("LicenseCreate.html");
            byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
            String htmlTemplate = new String(bytes, StandardCharsets.UTF_8);

            // Replace placeholders with actual values
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
            // Load HTML template from resources
            ClassPathResource resource = new ClassPathResource("LicenseExpiration.html");
            byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
            String htmlTemplate = new String(bytes, StandardCharsets.UTF_8);

            // Replace placeholders with actual values
            return htmlTemplate
                    .replace("#LICENSE_NUMBER", licenseKey)
                    .replace("#EXPIRATION_DATE", expirationDate.format(formatter));
        } catch (IOException e) {
            throw new EmailException("Error en buildExpirationWarningContent", e);
        }
    }
}
