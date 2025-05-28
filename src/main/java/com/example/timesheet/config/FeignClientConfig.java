package com.example.timesheet.config;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@RequiredArgsConstructor
public class FeignClientConfig {

    private final FeignErrorDecoder errorDecoder;
    private final KeycloakTokenProvider keycloakTokenProvider; // NEW: inject this

    @Bean
    public ErrorDecoder errorDecoder() {
        return errorDecoder;
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            String token = null;

            // Try to get token from the current HTTP request
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                token = attributes.getRequest().getHeader("Authorization");
            }

            // If no token in request context, fallback to client_credentials token
            if (token == null || token.isBlank()) {
                String accessToken = keycloakTokenProvider.getAccessToken();
                if (accessToken != null) {
                    token = "Bearer " + accessToken;
                }
            }

            // Set Authorization header if token is available
            if (token != null && !token.isBlank()) {
                requestTemplate.header("Authorization", token);
            }
        };
    }
}
