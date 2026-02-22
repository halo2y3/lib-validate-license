package co.com.validate.license.model;

import jakarta.validation.constraints.NotBlank;

public class TokenRequest {

    @NotBlank(message = "Subject is required")
    private String subject;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
	
}
