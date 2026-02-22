package co.com.validate.license.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.com.validate.license.repository.LicenseRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LicenseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        licenseRepository.deleteAll();
    }

    @Test
    void testCompleteFlow_GenerateToken_CreateLicense_ActivateLicense() throws Exception {
        // Step 1: Generate JWE token
        MvcResult tokenResult = mockMvc.perform(post("/api/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\": \"integration-test-client\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andReturn();

        String tokenResponse = tokenResult.getResponse().getContentAsString();
        JsonNode tokenJson = objectMapper.readTree(tokenResponse);
        String jweToken = tokenJson.get("token").asText();

        assertNotNull(jweToken);
        assertFalse(jweToken.isEmpty());

        // Step 2: Create a license using the token
        String licenseKey = "TEST-LICENSE-KEY-001";
        String email = "test@example.com";
        int validDays = 30;

        mockMvc.perform(post("/api/license/create")
                .header("Authorization", "Bearer " + jweToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"licenseKey\": \"" + licenseKey + "\", \"email\": \"" + email + "\", \"validDays\": " + validDays + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.licenseKey").value(licenseKey))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.active").value(false));

        // Step 3: Activate the license (first time)
        String hwid = "TEST-HARDWARE-ID-12345";

        mockMvc.perform(post("/api/license/activate")
                .header("Authorization", "Bearer " + jweToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"licenseKey\": \"" + licenseKey + "\", \"hwid\": \"" + hwid + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.description").value("LICENCIA_OK"))
            .andExpect(jsonPath("$.expirationDate").exists());

        // Step 4: Reactivate with same HWID (should succeed)
        mockMvc.perform(post("/api/license/activate")
                .header("Authorization", "Bearer " + jweToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"licenseKey\": \"" + licenseKey + "\", \"hwid\": \"" + hwid + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.description").value("LICENCIA_OK"));

        // Step 5: Try to activate with different HWID (should fail)
        mockMvc.perform(post("/api/license/activate")
                .header("Authorization", "Bearer " + jweToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"licenseKey\": \"" + licenseKey + "\", \"hwid\": \"DIFFERENT-HWID\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.description").value("Licencia usada en otro PC"));
    }

    @Test
    void testUnauthorizedAccess_WithoutToken() throws Exception {
        // Attempt to create license without token
        mockMvc.perform(post("/api/license/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"licenseKey\": \"TEST-KEY\", \"email\": \"test@example.com\", \"validDays\": 30}"))
            .andExpect(status().isUnauthorized());

        // Attempt to activate license without token
        mockMvc.perform(post("/api/license/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"licenseKey\": \"TEST-KEY\", \"hwid\": \"TEST-HWID\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testUnauthorizedAccess_WithInvalidToken() throws Exception {
        String invalidToken = "invalid.token.here";

        // Attempt to create license with invalid token
        mockMvc.perform(post("/api/license/create")
                .header("Authorization", "Bearer " + invalidToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"licenseKey\": \"TEST-KEY\", \"email\": \"test@example.com\", \"validDays\": 30}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testDuplicateLicenseKey_ReturnsBadRequest() throws Exception {
        // Generate token
        MvcResult tokenResult = mockMvc.perform(post("/api/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\": \"test-client\"}"))
            .andExpect(status().isOk())
            .andReturn();

        String jweToken = objectMapper.readTree(tokenResult.getResponse().getContentAsString())
            .get("token").asText();

        String licenseKey = "DUPLICATE-KEY";
        String email = "duplicate@example.com";

        // Create first license
        mockMvc.perform(post("/api/license/create")
                .header("Authorization", "Bearer " + jweToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"licenseKey\": \"" + licenseKey + "\", \"email\": \"" + email + "\", \"validDays\": 30}"))
            .andExpect(status().isOk());

        // Try to create duplicate
        mockMvc.perform(post("/api/license/create")
                .header("Authorization", "Bearer " + jweToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"licenseKey\": \"" + licenseKey + "\", \"email\": \"" + email + "\", \"validDays\": 30}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testActivateNonexistentLicense_ReturnsForbidden() throws Exception {
        // Generate token
        MvcResult tokenResult = mockMvc.perform(post("/api/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\": \"test-client\"}"))
            .andExpect(status().isOk())
            .andReturn();

        String jweToken = objectMapper.readTree(tokenResult.getResponse().getContentAsString())
            .get("token").asText();

        // Try to activate non-existent license
        mockMvc.perform(post("/api/license/activate")
                .header("Authorization", "Bearer " + jweToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"licenseKey\": \"NONEXISTENT-KEY\", \"hwid\": \"TEST-HWID\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.description").value("Licencia no existe"));
    }

}
