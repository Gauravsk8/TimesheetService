package com.example.timesheet.config;


import com.example.timesheet.common.security.CustomAccessDeniedHandler;
import com.example.timesheet.common.security.CustomEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.web.SecurityFilterChain;

import static com.example.timesheet.utils.SecurityUtils.csrfConfig;

@Configuration
public class SecurityConfig {

    private final CustomEntryPoint customEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    public SecurityConfig(CustomEntryPoint customEntryPoint,
                          CustomAccessDeniedHandler customAccessDeniedHandler) {
        this.customEntryPoint = customEntryPoint;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
    }

    @Value("${keycloak.enabled:true}")
    private boolean keycloakEnabled;

    @Bean
    public JwtDecoder jwtDecoder() {
        String issuerUri = "http://localhost:8081/realms/timesheet";
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())

                //  Enable CSRF with token repository (Cookie-based)
                .csrf(csrfConfig())


                //  Permit Swagger endpoints
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // Custom exception handling
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )

                //  Enable OAuth2 Resource Server with JWT
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(customEntryPoint)
                        .jwt(Customizer.withDefaults())
                );

        return http.build();
    }
}
