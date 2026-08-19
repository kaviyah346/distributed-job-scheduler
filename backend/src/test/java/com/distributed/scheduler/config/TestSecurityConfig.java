package com.distributed.scheduler.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.Map;

/**
 * Provides a mock JwtDecoder for tests so that integration tests run
 * without requiring a live Keycloak server running on localhost:8180.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        return token -> Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .header("typ", "JWT")
                .subject("test-user-id")
                .claim("preferred_username", "testuser")
                .claim("email", "testuser@scheduler.io")
                .claim("realm_access", Map.of("roles", java.util.List.of("ADMIN", "DEVELOPER", "OPERATOR")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
