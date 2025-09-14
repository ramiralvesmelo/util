package br.com.ramiralvesmelo.util.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRoleConverterTest {

    private static final String CLIENT_ID = "app-demo-api";

    /** Cria um Jwt “mokado” cujo getClaims() retorna exatamente o mapa passado. */
    private Jwt mockJwt(Map<String, Object> claims) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaims()).thenReturn(claims);
        return jwt;
    }

    @Test
    @DisplayName("Sem claims => vazio")
    void noClaims() {
        var converter = new KeycloakRoleConverter(CLIENT_ID);
        var jwt = mockJwt(Map.of());

        var out = converter.convert(jwt);

        assertThat(out).isEmpty();
    }

    @Test
    @DisplayName("Somente realm_access.roles => ROLE_* aplicado")
    void onlyRealmRoles() {
        var converter = new KeycloakRoleConverter(CLIENT_ID);
        var jwt = mockJwt(Map.of(
            "realm_access", Map.of("roles", List.of("admin", "user"))
        ));

        Collection<GrantedAuthority> out = converter.convert(jwt);

        assertThat(out).extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("ROLE_admin", "ROLE_user");
    }

    @Test
    @DisplayName("Somente resource_access[clientId].roles => ROLE_* aplicado")
    void onlyResourceRoles() {
        var converter = new KeycloakRoleConverter(CLIENT_ID);
        var jwt = mockJwt(Map.of(
            "resource_access", Map.of(
                CLIENT_ID, Map.of("roles", List.of("writer", "reader"))
            )
        ));

        var out = converter.convert(jwt);

        assertThat(out).extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("ROLE_writer", "ROLE_reader");
    }

    @Test
    @DisplayName("União realm + resource com duplicatas => distinct")
    void mergeAndDeduplicate() {
        var converter = new KeycloakRoleConverter(CLIENT_ID);
        var jwt = mockJwt(Map.of(
            "realm_access", Map.of("roles", List.of("admin", "user")),
            "resource_access", Map.of(
                CLIENT_ID, Map.of("roles", List.of("user", "auditor"))
            )
        ));

        var out = converter.convert(jwt);

        assertThat(out).extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("ROLE_admin", "ROLE_user", "ROLE_auditor");
    }

    @Test
    @DisplayName("Ignora resource_access de outro clientId")
    void ignoreOtherClient() {
        var converter = new KeycloakRoleConverter(CLIENT_ID);
        var jwt = mockJwt(Map.of(
            "resource_access", Map.of(
                "outro-client", Map.of("roles", List.of("x", "y"))
            )
        ));

        var out = converter.convert(jwt);

        assertThat(out).isEmpty();
    }

    @Test
    @DisplayName("Chaves presentes mas listas vazias => vazio")
    void emptyListsStillEmpty() {
        var converter = new KeycloakRoleConverter(CLIENT_ID);
        var jwt = mockJwt(Map.of(
            "realm_access", Map.of("roles", List.of()),
            "resource_access", Map.of(
                CLIENT_ID, Map.of("roles", List.of())
            )
        ));

        var out = converter.convert(jwt);

        assertThat(out).isEmpty();
    }

    @Test
    @DisplayName("Prefixo ROLE_ sempre aplicado e lista retornada é imutável")
    void rolePrefixAndUnmodifiableList() {
        var converter = new KeycloakRoleConverter(CLIENT_ID);
        var jwt = mockJwt(Map.of(
            "realm_access", Map.of("roles", List.of("ADMIN", "manager")),
            "resource_access", Map.of(
                CLIENT_ID, Map.of("roles", List.of("auditor"))
            )
        ));

        var out = converter.convert(jwt);

        assertThat(out)
            .isNotEmpty()
            .allSatisfy(ga -> assertThat(ga.getAuthority()).startsWith("ROLE_"));

        // Stream#toList (Java 21) retorna lista imutável:
        assertThrows(UnsupportedOperationException.class, () -> out.add(() -> "ROLE_test"));
    }
}
