package co.com.validate.license.security;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;

import org.springframework.stereotype.Service;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jwt.JWTClaimsSet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class JweService {

    private final JweProperties jweProperties;

    /**
     * Generates a JWE token with the given subject
     *
     * @param subject The subject (typically username or client identifier)
     * @return JWE token string
     * @throws JOSEException if encryption fails
     */
    public String generateToken(String subject) throws JOSEException {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + (jweProperties.getExpirationSeconds() * 1000));

        // Create JWT claims
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer(jweProperties.getIssuer())
                .issueTime(now)
                .expirationTime(expirationDate)
                .build();

        // Create JWE header with direct encryption algorithm and AES-256-GCM
        JWEHeader header = new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256GCM);

        // Create JWE object
        JWEObject jweObject = new JWEObject(header, new Payload(claimsSet.toJSONObject()));

        // Encrypt with direct encryption
        byte[] secretKeyBytes = jweProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);
        DirectEncrypter encrypter = new DirectEncrypter(secretKeyBytes);
        jweObject.encrypt(encrypter);

        return jweObject.serialize();
    }

    /**
     * Validates and decrypts a JWE token
     *
     * @param token JWE token string
     * @return JWTClaimsSet if valid
     * @throws ParseException 
     * @throws JOSEException 
     * @throws Exception if validation fails
     */
    public JWTClaimsSet validateToken(String token) throws SecurityException, ParseException, JOSEException {
        // Parse the JWE string
        JWEObject jweObject = JWEObject.parse(token);

        // Decrypt with direct decryption
        byte[] secretKeyBytes = jweProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);
        DirectDecrypter decrypter = new DirectDecrypter(secretKeyBytes);
        jweObject.decrypt(decrypter);

        // Extract claims
        JWTClaimsSet claimsSet = JWTClaimsSet.parse(jweObject.getPayload().toJSONObject());

        // Validate expiration
        Date expirationTime = claimsSet.getExpirationTime();
        if (expirationTime != null && expirationTime.before(new Date())) {
            throw new SecurityException("Token has expired");
        }

        // Validate issuer
        String issuer = claimsSet.getIssuer();
        if (issuer == null || !issuer.equals(jweProperties.getIssuer())) {
            throw new SecurityException("Invalid token issuer");
        }

        return claimsSet;
    }

    /**
     * Extracts subject from token without full validation (use with caution)
     *
     * @param token JWE token string
     * @return subject string or null if invalid
     */
    public String getSubjectFromToken(String token) {
        try {
            JWTClaimsSet claimsSet = validateToken(token);
            return claimsSet.getSubject();
        } catch (Exception e) {
            log.warn("Failed to extract subject from token: {}", e.getMessage());
            return null;
        }
    }

}
