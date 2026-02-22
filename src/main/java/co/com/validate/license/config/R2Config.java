package co.com.validate.license.config;

import java.net.URI;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "backup.enabled", havingValue = "true")
public class R2Config {

    @Bean
    public S3Client s3Client(BackupProperties props) {
        BackupProperties.R2 r2 = props.getR2();
        String endpoint = "https://" + r2.getAccountId() + ".r2.cloudflarestorage.com";
        log.info("Cloudflare R2 configurado — bucket: {}", r2.getBucketName());
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(r2.getAccessKeyId(), r2.getSecretAccessKey())))
                .region(Region.of("auto"))
                .build();
    }
}
