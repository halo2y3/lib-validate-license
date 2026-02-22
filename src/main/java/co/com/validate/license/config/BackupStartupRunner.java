package co.com.validate.license.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import co.com.validate.license.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "backup.run-on-startup", havingValue = "true")
public class BackupStartupRunner implements CommandLineRunner {

    private final BackupService backupService;

    @Override
    public void run(String... args) {
        log.info("Ejecutando backup al inicio (backup.run-on-startup=true)...");
        backupService.performBackup();
    }
}
