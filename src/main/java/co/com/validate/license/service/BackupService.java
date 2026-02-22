package co.com.validate.license.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import co.com.validate.license.config.BackupProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final JdbcTemplate jdbcTemplate;
    private final BackupProperties backupProperties;

    @Autowired(required = false)
    private S3Client s3Client;

    @Scheduled(cron = "${backup.cron:0 0 2 * * ?}")
    public void performBackup() {
        if (!backupProperties.isEnabled()) {
            log.debug("Backup deshabilitado. Omitiendo.");
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String fileName = "licenses-" + timestamp + ".zip";
        Path backupDir = Path.of(backupProperties.getLocalDir());
        Path backupFile = backupDir.resolve(fileName);

        try {
            Files.createDirectories(backupDir);

            // Snapshot consistente de H2 mientras la base de datos está en ejecución
            String backupPath = backupFile.toAbsolutePath().toString().replace('\\', '/');
            jdbcTemplate.execute("BACKUP TO '" + backupPath + "'");
            log.info("Backup H2 creado: {}", backupFile);

            // Subir a Cloudflare R2 (best-effort)
            try {
                uploadToR2(backupFile, fileName);
            } catch (Exception e) {
                log.error("Error al subir backup a Cloudflare R2: {}", e.getMessage(), e);
            }

            // Eliminar archivo local
            Files.deleteIfExists(backupFile);
            log.debug("Archivo local de backup eliminado: {}", backupFile);

            // Limpiar backups antiguos en R2 (best-effort)
            try {
                cleanOldBackups();
            } catch (Exception e) {
                log.error("Error al limpiar backups antiguos en R2: {}", e.getMessage(), e);
            }

            log.info("Backup completado exitosamente: {}", fileName);

        } catch (Exception e) {
            log.error("Backup fallido: {}", e.getMessage(), e);
        }
    }

    private void uploadToR2(Path backupFile, String fileName) {
        if (s3Client == null) {
            log.warn("Cloudflare R2 no configurado. Backup no subido.");
            return;
        }

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(backupProperties.getR2().getBucketName())
                .key(fileName)
                .build();

        s3Client.putObject(request, RequestBody.fromFile(backupFile));
        log.info("Backup subido a Cloudflare R2: {}", fileName);
    }

    private void cleanOldBackups() {
        if (s3Client == null) return;

        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(backupProperties.getR2().getBucketName())
                .prefix("licenses-")
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);
        List<S3Object> objects = new ArrayList<>(response.contents());
        objects.sort(Comparator.comparing(S3Object::lastModified));

        int maxFiles = backupProperties.getMaxFiles();
        if (objects.size() > maxFiles) {
            List<S3Object> toDelete = objects.subList(0, objects.size() - maxFiles);
            for (S3Object obj : toDelete) {
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(backupProperties.getR2().getBucketName())
                        .key(obj.key())
                        .build();
                s3Client.deleteObject(deleteRequest);
                log.info("Backup antiguo eliminado de R2: {}", obj.key());
            }
        }
    }
}
