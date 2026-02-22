package co.com.validate.license.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.nimbusds.jwt.JWTClaimsSet;

@SpringBootTest
@ActiveProfiles("test")
class JweServiceTest {

    @Autowired
    private JweService jweService;

    @Autowired
    private JweProperties jweProperties;

    @Test
    void testGenerateToken_Success() throws Exception {
        // Given
        String subject = "test-user";

        // When
        String token = jweService.generateToken(subject);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length >= 3, "JWE token should have multiple parts");
    }

    @Test
    void testValidateToken_Success() throws Exception {
        // Given
        String subject = "test-user";
        String token = jweService.generateToken(subject);

        // When
        JWTClaimsSet claimsSet = jweService.validateToken(token);

        // Then
        assertNotNull(claimsSet);
        assertEquals(subject, claimsSet.getSubject());
        assertEquals(jweProperties.getIssuer(), claimsSet.getIssuer());
        assertNotNull(claimsSet.getIssueTime());
        assertNotNull(claimsSet.getExpirationTime());
    }

    @Test
    void testValidateToken_InvalidToken_ThrowsException() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertThrows(Exception.class, () -> {
            jweService.validateToken(invalidToken);
        });
    }

    @Test
    void testValidateToken_WrongSecret_ThrowsException() throws Exception {
        // Given
        String subject = "test-user";
        String token = jweService.generateToken(subject);

        // Change the secret key
        String originalSecret = jweProperties.getSecretKey();
        jweProperties.setSecretKey("different-secret-key-that-is-32-characters-long-for-aes");

        // When & Then
        assertThrows(Exception.class, () -> {
            jweService.validateToken(token);
        });

        // Restore original secret
        jweProperties.setSecretKey(originalSecret);
    }

    @Test
    void testGetSubjectFromToken_Success() throws Exception {
        // Given
        String subject = "test-user";
        String token = jweService.generateToken(subject);

        // When
        String extractedSubject = jweService.getSubjectFromToken(token);

        // Then
        assertEquals(subject, extractedSubject);
    }

    @Test
    void testGetSubjectFromToken_InvalidToken_ReturnsNull() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        String subject = jweService.getSubjectFromToken(invalidToken);

        // Then
        assertNull(subject);
    }

    @Test
    void testTokenContainsCorrectClaims() throws Exception {
        // Given
        String subject = "test-user";

        // When
        String token = jweService.generateToken(subject);
        JWTClaimsSet claims = jweService.validateToken(token);

        // Then
        assertEquals(subject, claims.getSubject());
        assertEquals(jweProperties.getIssuer(), claims.getIssuer());
        assertNotNull(claims.getIssueTime());
        assertNotNull(claims.getExpirationTime());

        // Verify expiration is in the future
        assertTrue(claims.getExpirationTime().getTime() > System.currentTimeMillis());
    }
    
}
