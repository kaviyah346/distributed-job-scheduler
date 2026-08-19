package com.distributed.scheduler.security;

import com.distributed.scheduler.config.TestSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class SecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ─── 1. Public Endpoints ──────────────────────────────────────────────────

    @Test
    @DisplayName("SEC-1: Swagger UI and OpenAPI docs are publicly accessible without authentication")
    void testSwaggerAndOpenApiEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.components.securitySchemes.BearerAuth").exists());

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    // ─── 2. Unauthenticated Rejections ────────────────────────────────────────

    @Test
    @DisplayName("SEC-2: API endpoints reject unauthenticated requests with 401 Unauthorized")
    void testUnauthenticatedApiRequestsReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/queues"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/dlq"))
                .andExpect(status().isUnauthorized());
    }

    // ─── 3. Authenticated Access ──────────────────────────────────────────────

    @Test
    @DisplayName("SEC-3: Valid JWT Bearer token grants access to secured endpoints")
    void testAuthenticatedRequestWithValidJwtSucceeds() throws Exception {
        mockMvc.perform(get("/api/v1/projects")
                        .with(jwt().jwt(j -> j
                                .claim("preferred_username", "developer")
                                .claim("realm_access", Map.of("roles", java.util.List.of("DEVELOPER")))
                        ).authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ─── 4. Role-based Authorization ──────────────────────────────────────────

    @Test
    @DisplayName("SEC-4: DEVELOPER role can create Retry Policies, but OPERATOR role is forbidden (403)")
    void testRoleAuthorizationForRetryPolicyCreation() throws Exception {
        Map<String, Object> policyPayload = Map.of(
                "name", "sec-test-policy-" + UUID.randomUUID().toString().substring(0, 6),
                "strategy", "FIXED",
                "maxRetries", 3,
                "initialIntervalSeconds", 5,
                "maxIntervalSeconds", 30,
                "backoffMultiplier", 1.0
        );

        // 1. DEVELOPER can create retry policy -> 201 Created
        mockMvc.perform(post("/api/v1/retry-policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(policyPayload))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").exists());

        // 2. OPERATOR (without DEVELOPER/ADMIN role) cannot create retry policies -> 403 Forbidden
        Map<String, Object> forbiddenPayload = Map.of(
                "name", "sec-forbidden-policy-" + UUID.randomUUID().toString().substring(0, 6),
                "strategy", "FIXED"
        );
        mockMvc.perform(post("/api/v1/retry-policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forbiddenPayload))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SEC-5: OPERATOR role can perform DLQ operations, but DEVELOPER role is forbidden (403)")
    void testRoleAuthorizationForDlqOperations() throws Exception {
        UUID fakeJobId = UUID.randomUUID();

        // 1. DEVELOPER role cannot delete/purge DLQ -> 403 Forbidden
        mockMvc.perform(delete("/api/v1/dlq/jobs/" + fakeJobId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
                .andExpect(status().isForbidden());

        // 2. OPERATOR role has permission (will proceed to service; returns 404 for non-existent job, not 403)
        mockMvc.perform(delete("/api/v1/dlq/jobs/" + fakeJobId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR"))))
                .andExpect(status().isNotFound());
    }

    // ─── 5. Dashboard Stats & Worker List Endpoints ───────────────────────────

    @Test
    @DisplayName("SEC-6: Authenticated user can fetch global Dashboard KPI stats")
    void testDashboardStatsEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/stats")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalJobs").exists())
                .andExpect(jsonPath("$.data.totalProjects").exists())
                .andExpect(jsonPath("$.data.totalQueues").exists())
                .andExpect(jsonPath("$.data.activeWorkers").exists())
                .andExpect(jsonPath("$.data.statusBreakdown").exists());
    }

    @Test
    @DisplayName("SEC-7: Authenticated user can fetch Worker node status list")
    void testWorkersListEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/workers")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
}
