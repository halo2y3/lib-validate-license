package co.com.validate.license.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import co.com.validate.license.model.License;
import co.com.validate.license.repository.LicenseRepository;

@ExtendWith(MockitoExtension.class)
class LicenseExpirationSchedulerTest {

    @Mock
    private LicenseRepository licenseRepository;

    @Mock
    private EmailService emailService;

    private LicenseExpirationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new LicenseExpirationScheduler(licenseRepository, emailService);
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", true);
    }

    @Test
    void testCheckExpiringLicenses_WithExpiringLicenses() {
        // Arrange
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        License license1 = new License();
        license1.setId(1L);
        license1.setLicenseKey("LICENSE-001");
        license1.setEmail("user1@example.com");
        license1.setExpirationDate(tomorrow);
        license1.setActive(true);

        License license2 = new License();
        license2.setId(2L);
        license2.setLicenseKey("LICENSE-002");
        license2.setEmail("user2@example.com");
        license2.setExpirationDate(tomorrow);
        license2.setActive(true);

        List<License> expiringLicenses = Arrays.asList(license1, license2);

        when(licenseRepository.findByExpirationDateAndActiveTrue(tomorrow))
            .thenReturn(expiringLicenses);

        // Act
        scheduler.checkExpiringLicenses();

        // Assert
        verify(licenseRepository).findByExpirationDateAndActiveTrue(tomorrow);
        verify(emailService, times(2)).sendLicenseExpirationWarning(
            any(String.class),
            any(String.class),
            eq(tomorrow)
        );
        verify(emailService).sendLicenseExpirationWarning("user1@example.com", "LICENSE-001", tomorrow);
        verify(emailService).sendLicenseExpirationWarning("user2@example.com", "LICENSE-002", tomorrow);
    }

    @Test
    void testCheckExpiringLicenses_NoExpiringLicenses() {
        // Arrange
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        when(licenseRepository.findByExpirationDateAndActiveTrue(tomorrow))
            .thenReturn(Collections.emptyList());

        // Act
        scheduler.checkExpiringLicenses();

        // Assert
        verify(licenseRepository).findByExpirationDateAndActiveTrue(tomorrow);
        verify(emailService, never()).sendLicenseExpirationWarning(
            any(String.class),
            any(String.class),
            any(LocalDate.class)
        );
    }

    @Test
    void testCheckExpiringLicenses_SchedulerDisabled() {
        // Arrange
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", false);

        // Act
        scheduler.checkExpiringLicenses();

        // Assert
        verify(licenseRepository, never()).findByExpirationDateAndActiveTrue(any(LocalDate.class));
        verify(emailService, never()).sendLicenseExpirationWarning(
            any(String.class),
            any(String.class),
            any(LocalDate.class)
        );
    }

    @Test
    void testCheckExpiringLicenses_EmailServiceThrowsException() {
        // Arrange
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        License license1 = new License();
        license1.setId(1L);
        license1.setLicenseKey("LICENSE-001");
        license1.setEmail("user1@example.com");
        license1.setExpirationDate(tomorrow);
        license1.setActive(true);

        License license2 = new License();
        license2.setId(2L);
        license2.setLicenseKey("LICENSE-002");
        license2.setEmail("user2@example.com");
        license2.setExpirationDate(tomorrow);
        license2.setActive(true);

        List<License> expiringLicenses = Arrays.asList(license1, license2);

        when(licenseRepository.findByExpirationDateAndActiveTrue(tomorrow))
            .thenReturn(expiringLicenses);

        // Make first email fail, second should still be sent
        doThrow(new RuntimeException("Email service error"))
            .when(emailService).sendLicenseExpirationWarning(eq("user1@example.com"), any(), any());

        // Act - should not throw exception
        scheduler.checkExpiringLicenses();

        // Assert - both attempts should be made despite first failure
        verify(licenseRepository).findByExpirationDateAndActiveTrue(tomorrow);
        verify(emailService).sendLicenseExpirationWarning("user1@example.com", "LICENSE-001", tomorrow);
        verify(emailService).sendLicenseExpirationWarning("user2@example.com", "LICENSE-002", tomorrow);
    }

    @Test
    void testCheckExpiringLicenses_RepositoryThrowsException() {
        // Arrange
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        when(licenseRepository.findByExpirationDateAndActiveTrue(tomorrow))
            .thenThrow(new RuntimeException("Database error"));

        // Act - should not throw exception
        scheduler.checkExpiringLicenses();

        // Assert
        verify(licenseRepository).findByExpirationDateAndActiveTrue(tomorrow);
        verify(emailService, never()).sendLicenseExpirationWarning(
            any(String.class),
            any(String.class),
            any(LocalDate.class)
        );
    }
}
