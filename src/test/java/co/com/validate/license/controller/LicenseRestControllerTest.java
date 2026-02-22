package co.com.validate.license.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import co.com.validate.license.model.License;
import co.com.validate.license.repository.LicenseRepository;
import co.com.validate.license.security.JweAuthenticationEntryPoint;
import co.com.validate.license.security.JweAuthenticationFilter;
import co.com.validate.license.security.JweService;
import co.com.validate.license.security.SecurityConfig;
import co.com.validate.license.service.EmailService;

@WebMvcTest(LicenseRestController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JweAuthenticationFilter.class, JweAuthenticationEntryPoint.class})
class LicenseRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LicenseRepository licenseRepository;

    @MockBean
    private JweService jweService;

    @MockBean
    private EmailService emailService;

    @Test
    @WithMockUser
    void testCreateLicense_Success() throws Exception {
        // Given
        String licenseKey = "ABC-123-XYZ";
        String email = "user@example.com";
        int validDays = 365;

        License savedLicense = new License();
        savedLicense.setId(1L);
        savedLicense.setLicenseKey(licenseKey);
        savedLicense.setEmail(email);
        savedLicense.setExpirationDate(LocalDate.now().plusDays(validDays));
        savedLicense.setActive(false);

        when(licenseRepository.existsByLicenseKey(licenseKey)).thenReturn(false);
        when(licenseRepository.save(any(License.class))).thenReturn(savedLicense);

        // When & Then
        mockMvc.perform(post("/api/license/create")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"licenseKey\": \"" + licenseKey + "\", \"email\": \"" + email + "\", \"validDays\": " + validDays + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.licenseKey").value(licenseKey))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.active").value(false));

        verify(licenseRepository).existsByLicenseKey(licenseKey);
        verify(licenseRepository).save(any(License.class));
        verify(emailService).sendLicenseCreationEmail(eq(email), eq(licenseKey), any(LocalDate.class));
    }

    @Test
    @WithMockUser
    void testCreateLicense_DuplicateKey_ReturnsBadRequest() throws Exception {
        // Given
        String licenseKey = "ABC-123-XYZ";
        String email = "user@example.com";
        int validDays = 365;

        when(licenseRepository.existsByLicenseKey(licenseKey)).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/license/create")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"licenseKey\": \"" + licenseKey + "\", \"email\": \"" + email + "\", \"validDays\": " + validDays + "}"))
            .andExpect(status().isBadRequest());

        verify(licenseRepository).existsByLicenseKey(licenseKey);
        verify(licenseRepository, never()).save(any(License.class));
        verify(emailService, never()).sendLicenseCreationEmail(anyString(), anyString(), any(LocalDate.class));
    }

    @Test
    void testCreateLicense_NoAuthentication_ReturnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/license/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"licenseKey\": \"ABC-123\", \"email\": \"user@example.com\", \"validDays\": 365}"))
            .andExpect(status().isUnauthorized());

        verify(licenseRepository, never()).existsByLicenseKey(anyString());
        verify(emailService, never()).sendLicenseCreationEmail(anyString(), anyString(), any(LocalDate.class));
    }

    @Test
    @WithMockUser
    void testActivateLicense_FirstActivation_Success() throws Exception {
        // Given
        String licenseKey = "ABC-123-XYZ";
        String hwid = "HARDWARE-ID-12345";

        License license = new License();
        license.setId(1L);
        license.setLicenseKey(licenseKey);
        license.setExpirationDate(LocalDate.now().plusDays(365));
        license.setActive(false);
        license.setHwid(null);

        when(licenseRepository.findByLicenseKey(licenseKey)).thenReturn(Optional.of(license));
        when(licenseRepository.save(any(License.class))).thenReturn(license);

        // When & Then
        mockMvc.perform(post("/api/license/activate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"licenseKey\": \"" + licenseKey + "\", \"hwid\": \"" + hwid + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.description").value("LICENCIA_OK"));

        verify(licenseRepository).findByLicenseKey(licenseKey);
        verify(licenseRepository).save(any(License.class));
    }

    @Test
    @WithMockUser
    void testActivateLicense_LicenseNotFound_ReturnsForbidden() throws Exception {
        // Given
        String licenseKey = "NONEXISTENT-KEY";
        String hwid = "HARDWARE-ID-12345";

        when(licenseRepository.findByLicenseKey(licenseKey)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/api/license/activate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"licenseKey\": \"" + licenseKey + "\", \"hwid\": \"" + hwid + "\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.description").value("Licencia no existe"));

        verify(licenseRepository).findByLicenseKey(licenseKey);
        verify(licenseRepository, never()).save(any(License.class));
    }

    @Test
    @WithMockUser
    void testActivateLicense_WrongHwid_ReturnsForbidden() throws Exception {
        // Given
        String licenseKey = "ABC-123-XYZ";
        String originalHwid = "ORIGINAL-HWID";
        String differentHwid = "DIFFERENT-HWID";

        License license = new License();
        license.setId(1L);
        license.setLicenseKey(licenseKey);
        license.setExpirationDate(LocalDate.now().plusDays(365));
        license.setActive(true);
        license.setHwid(originalHwid);

        when(licenseRepository.findByLicenseKey(licenseKey)).thenReturn(Optional.of(license));

        // When & Then
        mockMvc.perform(post("/api/license/activate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"licenseKey\": \"" + licenseKey + "\", \"hwid\": \"" + differentHwid + "\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.description").value("Licencia usada en otro PC"));

        verify(licenseRepository).findByLicenseKey(licenseKey);
        verify(licenseRepository, never()).save(any(License.class));
    }

    @Test
    @WithMockUser
    void testActivateLicense_ExpiredLicense_ReturnsForbidden() throws Exception {
        // Given
        String licenseKey = "ABC-123-XYZ";
        String hwid = "HARDWARE-ID-12345";

        License license = new License();
        license.setId(1L);
        license.setLicenseKey(licenseKey);
        license.setExpirationDate(LocalDate.now().minusDays(1)); // Expired yesterday
        license.setActive(true);
        license.setHwid(hwid);

        when(licenseRepository.findByLicenseKey(licenseKey)).thenReturn(Optional.of(license));

        // When & Then
        mockMvc.perform(post("/api/license/activate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"licenseKey\": \"" + licenseKey + "\", \"hwid\": \"" + hwid + "\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.description").value("Licencia vencida"));

        verify(licenseRepository).findByLicenseKey(licenseKey);
        verify(licenseRepository, never()).save(any(License.class));
    }

    @Test
    @WithMockUser
    void testActivateLicense_ValidReactivation_Success() throws Exception {
        // Given
        String licenseKey = "ABC-123-XYZ";
        String hwid = "HARDWARE-ID-12345";

        License license = new License();
        license.setId(1L);
        license.setLicenseKey(licenseKey);
        license.setExpirationDate(LocalDate.now().plusDays(365));
        license.setActive(true);
        license.setHwid(hwid);

        when(licenseRepository.findByLicenseKey(licenseKey)).thenReturn(Optional.of(license));

        // When & Then
        mockMvc.perform(post("/api/license/activate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"licenseKey\": \"" + licenseKey + "\", \"hwid\": \"" + hwid + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.description").value("LICENCIA_OK"))
            .andExpect(jsonPath("$.expirationDate").exists());

        verify(licenseRepository).findByLicenseKey(licenseKey);
    }

}
