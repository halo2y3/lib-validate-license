package co.com.validate.license.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import com.nimbusds.jwt.JWTClaimsSet;

@ExtendWith(MockitoExtension.class)
class JweAuthenticationFilterTest {

    @Mock
    private JweService jweService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JweAuthenticationFilter jweAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDoFilterInternal_ValidToken_SetsAuthentication() throws Exception {
        // Given
        String token = "valid.jwe.token";
        String bearerToken = "Bearer " + token;
        String subject = "test-user";

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .subject(subject)
            .build();

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(jweService.validateToken(token)).thenReturn(claimsSet);

        // When
        jweAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(subject, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @ParameterizedTest(name = "No authentication when: {0}")
    @CsvSource({
        "'No authorization header',",
        "'Invalid token format', 'InvalidFormat token'",
        "'Empty bearer token', 'Bearer '"
    })
    void testDoFilterInternal_NoAuthenticationScenarios(String scenario, String authHeader) throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(authHeader);

        // When
        jweAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_ValidTokenInvalidClaims_NoAuthentication() throws Exception {
        // Given
        String token = "valid.jwe.token";
        String bearerToken = "Bearer " + token;

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(jweService.validateToken(token)).thenThrow(new RuntimeException("Invalid token"));

        // When
        jweAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_NullSubject_NoAuthentication() throws Exception {
        // Given
        String token = "valid.jwe.token";
        String bearerToken = "Bearer " + token;

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().build(); // No subject

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(jweService.validateToken(token)).thenReturn(claimsSet);

        // When
        jweAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

}
