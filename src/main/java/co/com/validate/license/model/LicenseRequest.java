package co.com.validate.license.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LicenseRequest {
	
    private String licenseKey;
    private String hwid;
    
}