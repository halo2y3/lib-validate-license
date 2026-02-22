package co.com.validate.license.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JweAuthenticationFilter jweAuthenticationFilter;
    private final JweAuthenticationEntryPoint jweAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF as we're using JWE tokens (and for H2 console)
            .csrf(csrf -> csrf.disable())

            // Allow H2 console frames
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
            )

            // Enable CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Set session management to stateless
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Set unauthorized requests exception handler
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jweAuthenticationEntryPoint)
            )

            // Set permissions on endpoints
            .authorizeHttpRequests(auth -> auth
                // Allow H2 console access (disable in production)
                .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()
                // Allow token generation endpoint (for testing/development)
                .requestMatchers(new AntPathRequestMatcher("/api/auth/**")).permitAll()
                // Require authentication for all /api/license endpoints
                .requestMatchers(new AntPathRequestMatcher("/api/license/**")).authenticated()
                // Allow all other requests (if any)
                .anyRequest().permitAll()
            );

        // Add JWE authentication filter
        http.addFilterBefore(jweAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
