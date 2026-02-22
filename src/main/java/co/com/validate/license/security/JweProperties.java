package co.com.validate.license.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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

}
