package co.com.validate.license.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.JOSEException;

import co.com.validate.license.model.TokenRequest;
import co.com.validate.license.model.TokenResponse;
import co.com.validate.license.security.JweService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JweService jweService;

    /**
     * Generates a JWE token for testing purposes
     * In production, this should be secured and require proper authentication
     */
    @PostMapping("/token")
    public ResponseEntity<TokenResponse> generateToken(@Valid @RequestBody TokenRequest request) {
        try {
            log.info("Generating token for subject: {}", request.getSubject());
            String token = jweService.generateToken(request.getSubject());

            TokenResponse response = new TokenResponse();
            response.setToken(token);
            response.setType("Bearer");
            response.setSubject(request.getSubject());

            return ResponseEntity.ok(response);
        } catch (JOSEException e) {
            log.error("Error generating token", e);
            return ResponseEntity.status(500).build();
        }
    }

}
