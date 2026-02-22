package co.com.validate.license.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import co.com.validate.license.config.BackupProperties;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private S3Client s3Client;

    @TempDir
    Path tempDir;

    private BackupProperties backupProperties;
    private BackupService backupService;

    @BeforeEach
    void setUp() {
        backupProperties = new BackupProperties();
        backupProperties.setEnabled(true);
        backupProperties.setLocalDir(tempDir.toString());
        backupProperties.setMaxFiles(3);
        BackupProperties.R2 r2 = new BackupProperties.R2();
        r2.setBucketName("test-bucket");
        r2.setAccountId("test-account");
        r2.setAccessKeyId("test-key");
        r2.setSecretAccessKey("test-secret");
        backupProperties.setR2(r2);

        backupService = new BackupService(jdbcTemplate, backupProperties);
        ReflectionTestUtils.setField(backupService, "s3Client", s3Client);
    }

    /**
     * Simula el comando BACKUP TO de H2 creando el archivo de backup en disco,
     * lo cual es necesario para que RequestBody.fromFile() funcione correctamente.
     */
    private void mockJdbcToCreateBackupFile() {
        doAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class);
            String path = sql.replaceFirst("(?i)BACKUP TO '(.+)'", "$1");
            Files.createFile(Path.of(path));
            return null;
        }).when(jdbcTemplate).execute(anyString());
    }

    @Test
    void testPerformBackup_Disabled() {
        // Arrange
        backupProperties.setEnabled(false);

        // Act
        backupService.performBackup();

        // Assert
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void testPerformBackup_Success() {
        // Arrange
        mockJdbcToCreateBackupFile();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().contents(Collections.emptyList()).build());

        // Act
        backupService.performBackup();

        // Assert
        verify(jdbcTemplate).execute(anyString());
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testPerformBackup_JdbcFails() {
        // Arrange
        doThrow(new RuntimeException("H2 backup error"))
                .when(jdbcTemplate).execute(anyString());

        // Act — no debe lanzar excepción
        backupService.performBackup();

        // Assert
        verify(jdbcTemplate).execute(anyString());
    }

    @Test
    void testPerformBackup_R2UploadFails() {
        // Arrange: el archivo debe existir para que RequestBody.fromFile() no falle
        mockJdbcToCreateBackupFile();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("R2 upload error"));

        // Act — no debe lanzar excepción
        backupService.performBackup();

        // Assert — el backup H2 sí fue ejecutado
        verify(jdbcTemplate).execute(anyString());
    }

    @Test
    void testPerformBackup_OldBackupsDeleted() {
        // Arrange: el archivo debe existir para que RequestBody.fromFile() no falle
        mockJdbcToCreateBackupFile();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // Simular 4 backups en R2 (maxFiles = 3, debe eliminar 1)
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().contents(List.of(
                        buildS3Object("licenses-old-1.zip", Instant.now().minusSeconds(400)),
                        buildS3Object("licenses-old-2.zip", Instant.now().minusSeconds(300)),
                        buildS3Object("licenses-old-3.zip", Instant.now().minusSeconds(200)),
                        buildS3Object("licenses-old-4.zip", Instant.now().minusSeconds(100))
                )).build());

        // Act
        backupService.performBackup();

        // Assert — se llamó delete exactamente una vez con el más antiguo
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals("licenses-old-1.zip", captor.getValue().key());
    }

    private S3Object buildS3Object(String key, Instant lastModified) {
        return S3Object.builder().key(key).lastModified(lastModified).build();
    }
}
