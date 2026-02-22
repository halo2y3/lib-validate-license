package co.com.validate.license.model;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LicenseResponse {
	
	private LocalDate expirationDate;
	private String description;
}