package co.com.validate.license.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "backup")
@Getter
@Setter
public class BackupProperties {

    private boolean enabled = false;
    private boolean runOnStartup = false;
    private String cron = "0 0 2 * * ?";
    private String localDir = "/app/data/backups";
    private int maxFiles = 7;
    private R2 r2 = new R2();

    @Getter
    @Setter
    public static class R2 {
        private String accountId;
        private String accessKeyId;
        private String secretAccessKey;
        private String bucketName;
    }
}
