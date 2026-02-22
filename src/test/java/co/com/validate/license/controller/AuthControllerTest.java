package co.com.validate.license.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.nimbusds.jose.JOSEException;

import co.com.validate.license.security.JweAuthenticationEntryPoint;
import co.com.validate.license.security.JweAuthenticationFilter;
import co.com.validate.license.security.JweService;
import co.com.validate.license.security.SecurityConfig;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JweAuthenticationFilter.class, JweAuthenticationEntryPoint.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JweService jweService;

    @Test
    void testGenerateToken_Success() throws Exception {
        // Given
        String subject = "test-user";
        String token = "generated.jwe.token.here";

        when(jweService.generateToken(subject)).thenReturn(token);

        // When & Then
        mockMvc.perform(post("/api/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\": \"" + subject + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value(token))
            .andExpect(jsonPath("$.type").value("Bearer"))
            .andExpect(jsonPath("$.subject").value(subject));

        verify(jweService).generateToken(subject);
    }

    @ParameterizedTest(name = "Bad request when: {0}")
    @CsvSource(delimiter = '|', value = {
        "Empty subject | '{\"subject\": \"\"}'",
        "Null subject | '{}'",
        "Invalid JSON | 'invalid json'"
    })
    void testGenerateToken_InvalidInput_ReturnsBadRequest(String scenario, String jsonContent) throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
            .andExpect(status().isBadRequest());

        verify(jweService, never()).generateToken(anyString());
    }

    @Test
    void testGenerateToken_ServiceThrowsException_ReturnsInternalServerError() throws Exception {
        // Given
        String subject = "test-user";

        when(jweService.generateToken(subject)).thenThrow(new JOSEException("Encryption failed"));

        // When & Then
        mockMvc.perform(post("/api/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\": \"" + subject + "\"}"))
            .andExpect(status().isInternalServerError());

        verify(jweService).generateToken(subject);
    }

}
