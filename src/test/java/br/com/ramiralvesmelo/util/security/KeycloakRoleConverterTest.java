package br.com.ramiralvesmelo.util.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRoleConverterTest {

    @Test
    @DisplayName("Deve converter roles de realm_access e resource_access (sem duplicar) com prefixo ROLE_")
    void convertRealmAndResourceRoles() {
        Map<String, Object> realmAccess = Map.of(
            "roles", List.of("USER", "ADMIN") // maiúsculas
        );

        Map<String, Object> clientRoles = Map.of(
            "roles", List.of("admin", "manager", "ADMIN") // inclui duplicata em case diferente
        );

        Map<String, Object> resourceAccess = Map.of(
            "app-demo-api", clientRoles
        );

        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("realm_access", realmAccess)
            .claim("resource_access", resourceAccess)
            .build();

        var converter = new KeycloakRoleConverter("app-demo-api");
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        var names = authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());

        // Espera prefixo ROLE_ e união distinta
        assertTrue(names.contains("ROLE_USER"));
        assertTrue(names.contains("ROLE_ADMIN"));  // de realm
        assertTrue(names.contains("ROLE_admin"));  // de resource (minúsculo preservado)
        assertTrue(names.contains("ROLE_manager"));

        // Deve ter 4 únicas (ADMIN duplicado entre realm/resource deve ser 1)
        assertEquals(4, names.size());
    }

    @Test
    @DisplayName("Sem roles deve retornar coleção vazia")
    void emptyWhenNoRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("realm_access", Map.of())         // sem 'roles'
            .claim("resource_access", Map.of())      // sem clientId
            .build();

        var converter = new KeycloakRoleConverter("app-demo-api");
        var authorities = converter.convert(jwt);

        assertNotNull(authorities);
        assertTrue(authorities.isEmpty());
    }
}
