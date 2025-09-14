package br.com.ramiralvesmelo.util.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/***
 * Classe que traduz o JWT do Keycloak para o GrantedAuthority do Spring Security.
 * Ela garante que as roles que vêm do Keycloak sejam entendidas pelo Spring como perfis de acesso
 * 
 * Exp.: admin => ROLE_admin
 * 
 * @PreAuthorize("hasRole('admin')") 
 */
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	private final String clientId;

	public KeycloakRoleConverter(String clientId) {
		this.clientId = clientId;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		// Recupera as roles de nível de "realm" (globais no Keycloak)
		// O claim "realm_access" contém um mapa, e dentro dele a chave "roles" é uma
		// coleção de strings
		var realmRoles = Optional.ofNullable((Map<String, Object>) jwt.getClaims().get("realm_access"))
				.map(m -> (Collection<String>) m.get("roles")).orElseGet(List::of); // se não houver, retorna lista
																					// vazia

		// Recupera as roles de nível de "resource" (específicas de um cliente
		// registrado no Keycloak)
		// O claim "resource_access" contém um mapa onde cada clientId tem suas roles
		var resourceRoles = Optional.ofNullable((Map<String, Object>) jwt.getClaims().get("resource_access"))
				.map(m -> (Map<String, Object>) m.get(clientId)) // acessa as roles do cliente específico
				.map(m -> (Collection<String>) m.get("roles")).orElseGet(List::of); // se não houver, retorna lista
																					// vazia

		// Junta as roles de realm e resource em um único Stream
		// remove duplicadas (.distinct())
		// prefixa cada role com "ROLE_" para se adequar ao padrão do Spring Security
		// converte em SimpleGrantedAuthority (implementação de GrantedAuthority)
		// e retorna como lista
		return Stream.concat(realmRoles.stream(), resourceRoles.stream()).distinct()
				.map(r -> new SimpleGrantedAuthority("ROLE_" + r)).map(GrantedAuthority.class::cast).toList();

	}
}