package co.com.validate.license.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import co.com.validate.license.model.License;
import co.com.validate.license.repository.LicenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseExpirationScheduler {

    private final LicenseRepository licenseRepository;
    private final EmailService emailService;

    @Value("${scheduler.expiration-warning.enabled:true}")
    private boolean schedulerEnabled;

    /**
     * Scheduled task that runs daily at 9:00 AM to check for licenses expiring tomorrow
     * Cron expression: "0 0 9 * * ?" = every day at 9:00 AM
     *
     * Format: second, minute, hour, day of month, month, day of week
     */
    @Scheduled(cron = "${scheduler.expiration-warning.cron:0 0 9 * * ?}")
    public void checkExpiringLicenses() {
        if (!schedulerEnabled) {
            log.debug("License expiration scheduler is disabled");
            return;
        }

        log.info("Starting scheduled task: checking licenses expiring tomorrow");

        try {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            List<License> expiringLicenses = licenseRepository.findByExpirationDateAndActiveTrue(tomorrow);

            if (expiringLicenses.isEmpty()) {
                log.info("No licenses expiring tomorrow");
                return;
            }

            log.info("Found {} license(s) expiring tomorrow ({})", expiringLicenses.size(), tomorrow);

            int successCount = 0;
            int failureCount = 0;

            for (License license : expiringLicenses) {
                if (sendExpirationWarning(license)) {
                    successCount++;
                } else {
                    failureCount++;
                }
            }

            log.info("Scheduled task completed. Success: {}, Failures: {}", successCount, failureCount);

        } catch (Exception e) {
            log.error("Error executing scheduled task for license expiration check", e);
        }
    }

    private boolean sendExpirationWarning(License license) {
        try {
            log.info("Sending expiration warning for license: {} to email: {}",
                    license.getLicenseKey(), license.getEmail());

            emailService.sendLicenseExpirationWarning(
                license.getEmail(),
                license.getLicenseKey(),
                license.getExpirationDate()
            );

            return true;
        } catch (Exception e) {
            log.error("Failed to process expiration warning for license: {}. Error: {}",
                    license.getLicenseKey(), e.getMessage(), e);
            return false;
        }
    }
}
