package co.com.validate.license.security;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "security.jwe")
@Getter
@Setter
public class JweProperties {

    /**
     * Secret key for JWE encryption (must be at least 256 bits / 32 characters for AES256)
     */
    private String secretKey = "default-secret-key-change-this-in-production-min32chars";

    /**
     * Token expiration time in seconds (default: 1 hour)
     */
    private long expirationSeconds = 3600;

    /**
     * Issuer of the token
     */
    private String issuer = "lib-validate-license";

    @PostConstruct
    public void validate() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                "JWE secret key is not configured. Set the JWE_SECRET_KEY environment variable (minimum 32 characters).");
        }
        if (secretKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                "JWE secret key is too short. Minimum 32 characters required for AES-256-GCM. Set JWE_SECRET_KEY.");
        }
    }

}
