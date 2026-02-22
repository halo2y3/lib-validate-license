package co.com.validate.license.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateLicenseRequest {

    @NotBlank
    private String licenseKey;

    @NotBlank
    @Email
    private String email;

    @Min(1)
    private int validDays;

}