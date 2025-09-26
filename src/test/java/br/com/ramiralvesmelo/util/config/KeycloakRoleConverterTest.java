package br.com.ramiralvesmelo.util.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRoleConverterTest {

	private static Jwt jwtCom(Map<String, Object> claims) {
	    return Jwt.withTokenValue("token")
	        .headers(h -> h.put("alg", "none"))
	        .claims(c -> {
	            if (claims != null) c.putAll(claims);
	            // garante que não fique vazio
	            c.putIfAbsent("sub", "test-user");
	        })
	        .issuedAt(Instant.now())
	        .expiresAt(Instant.now().plusSeconds(60))
	        .build();
	}


    @Test
    void deveConverterRealmEResourceRolesSemDuplicar() {
        String clientId = "app-api";
        var converter = new KeycloakRoleConverter(clientId);

        var claims = Map.<String, Object>of(
            "realm_access", Map.of("roles", List.of("admin", "user", "admin")),
            "resource_access", Map.of(
                clientId, Map.of("roles", List.of("writer", "admin")) // admin duplicada
            )
        );

        var jwt = jwtCom(claims);
        var out = converter.convert(jwt);

        assertNotNull(out);
        // Esperado: ROLE_admin, ROLE_user, ROLE_writer (sem duplicar)
        assertTrue(out.stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_admin"::equals));
        assertTrue(out.stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_user"::equals));
        assertTrue(out.stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_writer"::equals));
        assertEquals(3, out.size());
    }

    @Test
    void deveRetornarSomenteRealmQuandoResourceNaoTemClient() {
        String clientId = "app-api";
        var converter = new KeycloakRoleConverter(clientId);

        var claims = Map.<String, Object>of(
            "realm_access", Map.of("roles", List.of("realm1")),
            "resource_access", Map.of(
                "outro-client", Map.of("roles", List.of("x", "y"))
            )
        );

        var out = converter.convert(jwtCom(claims));

        assertEquals(1, out.size());
        assertEquals("ROLE_realm1", out.iterator().next().getAuthority());
    }

    @Test
    void deveRetornarVazioQuandoSemClaims() {
        var converter = new KeycloakRoleConverter("qualquer");
        var out = converter.convert(jwtCom(Map.of()));
        assertNotNull(out);
        assertTrue(out.isEmpty());
    }
}
