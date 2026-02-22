package co.com.validate.license.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@test.com");
    }

    @Test
    void testSendLicenseCreationEmail_Success() {
        // Arrange
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
        String email = "user@example.com";
        String licenseKey = "TEST-LICENSE-123";
        LocalDate expirationDate = LocalDate.now().plusDays(365);

        // Mock MimeMessage creation
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailService.sendLicenseCreationEmail(email, licenseKey, expirationDate);

        // Assert
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendLicenseCreationEmail_EmailDisabled() {
        // Arrange
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);
        String email = "user@example.com";
        String licenseKey = "TEST-LICENSE-123";
        LocalDate expirationDate = LocalDate.now().plusDays(365);

        // Act
        emailService.sendLicenseCreationEmail(email, licenseKey, expirationDate);

        // Assert
        verify(mailSender, never()).send(any(MimeMessage.class));
        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void testSendLicenseCreationEmail_MailSenderThrowsException() {
        // Arrange
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
        String email = "user@example.com";
        String licenseKey = "TEST-LICENSE-123";
        LocalDate expirationDate = LocalDate.now().plusDays(365);

        // Mock MimeMessage creation
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        doThrow(new RuntimeException("Mail server error"))
            .when(mailSender).send(any(MimeMessage.class));

        // Act - should not throw exception
        emailService.sendLicenseCreationEmail(email, licenseKey, expirationDate);

        // Assert - exception was caught and logged
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendLicenseExpirationWarning_Success() {
        // Arrange
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
        String email = "user@example.com";
        String licenseKey = "TEST-LICENSE-123";
        LocalDate expirationDate = LocalDate.now().plusDays(1);

        // Mock MimeMessage creation
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailService.sendLicenseExpirationWarning(email, licenseKey, expirationDate);

        // Assert
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendLicenseExpirationWarning_EmailDisabled() {
        // Arrange
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);
        String email = "user@example.com";
        String licenseKey = "TEST-LICENSE-123";
        LocalDate expirationDate = LocalDate.now().plusDays(1);

        // Act
        emailService.sendLicenseExpirationWarning(email, licenseKey, expirationDate);

        // Assert
        verify(mailSender, never()).send(any(MimeMessage.class));
        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void testSendLicenseExpirationWarning_MailSenderThrowsException() {
        // Arrange
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
        String email = "user@example.com";
        String licenseKey = "TEST-LICENSE-123";
        LocalDate expirationDate = LocalDate.now().plusDays(1);

        // Mock MimeMessage creation
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        doThrow(new RuntimeException("Mail server error"))
            .when(mailSender).send(any(MimeMessage.class));

        // Act - should not throw exception
        emailService.sendLicenseExpirationWarning(email, licenseKey, expirationDate);

        // Assert - exception was caught and logged
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
