package com.distributed.scheduler.security.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Custom JWT converter that extracts roles from Keycloak tokens.
 *
 * <p>Keycloak stores realm-level roles in {@code realm_access.roles} and
 * client-level roles in {@code resource_access.<client-id>.roles}.
 *
 * <p>This converter merges standard OAuth2 scopes (e.g. SCOPE_read) with
 * Keycloak roles mapped to Spring Security's standard {@code ROLE_<ROLE_NAME>}
 * format (e.g. ROLE_ADMIN, ROLE_DEVELOPER, ROLE_OPERATOR).
 */
@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Value("${app.security.jwt.principal-attribute:preferred_username}")
    private String principalAttribute;

    @Value("${app.security.jwt.resource-id:job-scheduler-client}")
    private String resourceId;

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
                defaultAuthoritiesConverter.convert(jwt).stream(),
                extractKeycloakRoles(jwt).stream()
        ).collect(Collectors.toSet());

        String principalClaimValue = getPrincipalClaimName(jwt);
        return new JwtAuthenticationToken(jwt, authorities, principalClaimValue);
    }

    private String getPrincipalClaimName(Jwt jwt) {
        String claimName = JwtClaimNames.SUB;
        if (principalAttribute != null && jwt.hasClaim(principalAttribute)) {
            claimName = principalAttribute;
        }
        return jwt.getClaimAsString(claimName);
    }

    @SuppressWarnings("unchecked")
    private Collection<? extends GrantedAuthority> extractKeycloakRoles(Jwt jwt) {
        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();

        // 1. Extract Realm Roles (realm_access.roles)
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            Collection<String> realmRoles = (Collection<String>) realmAccess.get("roles");
            realmRoles.stream()
                    .map(role -> normalizeRole(role))
                    .map(SimpleGrantedAuthority::new)
                    .forEach(grantedAuthorities::add);
        }

        // 2. Extract Client Resource Roles (resource_access.<client-id>.roles)
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null && resourceAccess.containsKey(resourceId)) {
            Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(resourceId);
            if (clientAccess != null && clientAccess.containsKey("roles")) {
                Collection<String> clientRoles = (Collection<String>) clientAccess.get("roles");
                clientRoles.stream()
                        .map(role -> normalizeRole(role))
                        .map(SimpleGrantedAuthority::new)
                        .forEach(grantedAuthorities::add);
            }
        }

        return grantedAuthorities;
    }

    private String normalizeRole(String role) {
        String upperRole = role.toUpperCase();
        return upperRole.startsWith("ROLE_") ? upperRole : "ROLE_" + upperRole;
    }
}
