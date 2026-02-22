package co.com.validate.license.controller;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.com.validate.license.model.CreateLicenseRequest;
import co.com.validate.license.model.License;
import co.com.validate.license.model.LicenseRequest;
import co.com.validate.license.model.LicenseResponse;
import co.com.validate.license.repository.LicenseRepository;
import co.com.validate.license.service.EmailService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/license")
@CrossOrigin(origins = "*")
public class LicenseRestController {

    private final LicenseRepository licenseRepository;
    private final EmailService emailService;

    @Autowired
    public LicenseRestController(LicenseRepository licenseRepository, EmailService emailService) {
        this.licenseRepository = licenseRepository;
        this.emailService = emailService;
    }
    
    @PostMapping("/create")
    public ResponseEntity<Object> create(
            @Valid @RequestBody CreateLicenseRequest createLicenseRequest) {

        // Validar duplicado
        if (licenseRepository.existsByLicenseKey(createLicenseRequest.getLicenseKey())) {
            return ResponseEntity
                    .badRequest()
                    .body("La licencia ya existe");
        }

        License license = new License();
        license.setLicenseKey(createLicenseRequest.getLicenseKey());
        license.setEmail(createLicenseRequest.getEmail());
        license.setExpirationDate(LocalDate.now().plusDays(createLicenseRequest.getValidDays()));
        license.setActive(false);

        licenseRepository.save(license);

        // Send email notification
        emailService.sendLicenseCreationEmail(
            license.getEmail(),
            license.getLicenseKey(),
            license.getExpirationDate()
        );

        return ResponseEntity.ok(license);
    }    
    
    @PostMapping("/activate")
    public ResponseEntity<Object> activate(@RequestBody LicenseRequest licenseRequest) {
    	log.info("activate: {}", licenseRequest);
    	
        Optional<License> licOptional = licenseRepository.findByLicenseKey(licenseRequest.getLicenseKey());

        License lic;
        LicenseResponse licenseResponse = new LicenseResponse();
    	
        if(licOptional.isPresent()) {
        	lic = licOptional.get();	
        }else {
        	licenseResponse.setDescription("Licencia no existe");
        	return ResponseEntity.status(403).body(licenseResponse);	
        }
        
        // Primera activación
        if (lic.getHwid() == null) {
            lic.setHwid(licenseRequest.getHwid());
            lic.setActive(true);
            licenseRepository.save(lic);
        }

        if (!lic.getHwid().equals(licenseRequest.getHwid())) {
        	licenseResponse.setDescription("Licencia usada en otro PC");
            return ResponseEntity.status(403).body(licenseResponse);
        }

        if (LocalDate.now().isAfter(lic.getExpirationDate())) {
        	licenseResponse.setDescription("Licencia vencida");
            return ResponseEntity.status(403).body(licenseResponse);
        }

        licenseResponse.setDescription("LICENCIA_OK");
        licenseResponse.setExpirationDate(lic.getExpirationDate());
        return ResponseEntity.ok(licenseResponse);
    }
}
