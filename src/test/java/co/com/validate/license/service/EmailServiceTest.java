package co.com.validate.license.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient mailerSendRestClient;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailerSendRestClient);
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@test.com");
    }

    @Test
    void testSendLicenseCreationEmail_Success() {
        // Arrange
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
        String email = "user@example.com";
        String licenseKey = "TEST-LICENSE-123";
        LocalDate expirationDate = LocalDate.now().plusDays(365);

        // Act
        emailService.sendLicenseCreationEmail(email, licenseKey, expirationDate);

        // Assert
        verify(mailerSendRestClient, times(1)).post();
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
        verify(mailerSendRestClient, never()).post();
    }

    @Test
    void testSendLicenseCreationEmail_MailSenderThrowsException() {
        // Arrange
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
        String email = "user@example.com";
        String licenseKey = "TEST-LICENSE-123";
        LocalDate expirationDate = LocalDate.now().plusDays(365);

        when(mailerSendRestClient.post().body(any()).retrieve().toBodilessEntity())
                .thenThrow(new RuntimeException("Mail server error"));
        clearInvocations(mailerSendRestClient);

        // Act - should not throw exception
        emailService.sendLicenseCreationEmail(email, licenseKey, expirationDate);

        // Assert - exception was caught and logged
        verify(mailerSendRestClient, times(1)).post();
    }

    @Test
    void testSendLicenseExpirationWarning_Success() {
        // Arrange
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
        String email = "user@example.com";
        String licenseKey = "TEST-LICENSE-123";
        LocalDate expirationDate = LocalDate.now().plusDays(1);

        // Act
        emailService.sendLicenseExpirationWarning(email, licenseKey, expirationDate);

        // Assert
        verify(mailerSendRestClient, times(1)).post();
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
        verify(mailerSendRestClient, never()).post();
    }

    @Test
    void testSendLicenseExpirationWarning_MailSenderThrowsException() {
        // Arrange
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
        String email = "user@example.com";
        String licenseKey = "TEST-LICENSE-123";
        LocalDate expirationDate = LocalDate.now().plusDays(1);

        when(mailerSendRestClient.post().body(any()).retrieve().toBodilessEntity())
                .thenThrow(new RuntimeException("Mail server error"));
        clearInvocations(mailerSendRestClient);

        // Act - should not throw exception
        emailService.sendLicenseExpirationWarning(email, licenseKey, expirationDate);

        // Assert - exception was caught and logged
        verify(mailerSendRestClient, times(1)).post();
    }
}
