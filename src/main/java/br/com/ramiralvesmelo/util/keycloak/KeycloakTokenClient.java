package br.com.ramiralvesmelo.util.keycloak;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Cliente mínimo para obter access_token no Keycloak usando client_credentials.
 * Uso utilitário: instanciar manualmente e chamar getAccessToken().
 */
@Getter
@Setter
@RequiredArgsConstructor
public class KeycloakTokenClient {

	private final RestTemplate rest = new RestTemplate();

	@NonNull
	private final String baseUrl; // Ex.: http://localhost:8081
	@NonNull
	private final String realm; // Ex.: app-api-realm
	@NonNull
	private final String clientId;
	@NonNull
	private final String clientSecret;

	// cache simples de token (evita pedir toda hora)
	private volatile String cachedToken;
	private volatile Instant expiresAt = Instant.EPOCH;

	/** Monta dinamicamente a URL do token. */
	public String getTokenUrl() {
		return ensureNoTrailingSlash(baseUrl) + "/realms/" + realm + "/protocol/openid-connect/token";
	}

	public HttpHeaders authHeaders() {
		HttpHeaders h = new HttpHeaders();
		h.setBearerAuth(this.getAccessToken());
		h.setContentType(MediaType.APPLICATION_JSON);
		return h;
	}

	/** Retorna o access_token; renova 30s antes de expirar. */
	public String getAccessToken() {
		if (cachedToken != null && Instant.now().isBefore(expiresAt.minusSeconds(30))) {
			return cachedToken;
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		String body = "grant_type=client_credentials" + "&client_id=" + urlEncode(clientId) + "&client_secret="
				+ urlEncode(clientSecret);

		ResponseEntity<Map<String, Object>> resp = rest.exchange(getTokenUrl(), HttpMethod.POST,
				new HttpEntity<>(body, headers),
				new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
				});
		Map<String, Object> m = resp.getBody();

		String token = (String) m.get("access_token");
		Object raw = m.get("expires_in");
		Number expiresIn = (raw instanceof Number) ? (Number) raw : Integer.valueOf(300);

		this.cachedToken = token;
		this.expiresAt = Instant.now().plusSeconds(expiresIn.longValue());
		return token;
	}

	/** Helper: remove barra final com tolerância a null/blank. */
	public static String ensureNoTrailingSlash(String s) {
		if (s == null || s.isBlank())
			return s;
		return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
	}

	private static String urlEncode(String s) {
		return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
	}
}
