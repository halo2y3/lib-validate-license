package co.com.validate.license.model;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
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

    @Min(30)
    @Max(360)
    private int validDays;

    @AssertTrue(message = "validDays debe ser un múltiplo de 30 (1 mes = 30 días, máximo 12 meses = 360 días)")
    public boolean isValidDaysMultipleOf30() {
        return validDays % 30 == 0;
    }

}