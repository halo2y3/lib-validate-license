package co.com.validate.license;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.Generated;

@SpringBootApplication
@EnableScheduling
public class RunServer extends SpringBootServletInitializer{

	@Generated
	@Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(RunServer.class);
    }
	
	@Generated
	public static void main(String[] args) {
		SpringApplication.run(RunServer.class);
	}
}