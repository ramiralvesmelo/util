package br.com.ramiralvesmelo.util.security.oauth2;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import lombok.Getter;

/**
 * Cliente mínimo e genérico para obter access_token via OAuth2 (grant: client_credentials).
 * - Suporta autenticação do client por BASIC AUTH (header) ou POST (no corpo).
 * - Faz cache do token e renova 30s antes de expirar.
 */
@Getter
public class OAuth2TokenClient {

    public enum ClientAuthMethod { BASIC, POST }

    private final RestTemplate rest = new RestTemplate();

    /** Endpoint completo do token, ex.: https://idp/realms/xxx/protocol/openid-connect/token */
    private final String tokenEndpoint;

    /** Client credentials */
    private final String clientId;
    private final String clientSecret; // pode ser null para clients públicos
    private final ClientAuthMethod authMethod;

    /** Parâmetros opcionais */
    private final String scope;     // ex.: "read write" (separado por espaço)
    private final String audience;  // ex.: "api://default" (conforme IdP)

    // cache simples
    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public OAuth2TokenClient(
            String tokenEndpoint,
            String clientId,
            String clientSecret,
            ClientAuthMethod authMethod,
            String scope,
            String audience
    ) {
        this.tokenEndpoint = Objects.requireNonNull(tokenEndpoint, "tokenEndpoint");
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.clientSecret = clientSecret; // pode ser null
        this.authMethod = authMethod != null ? authMethod : ClientAuthMethod.BASIC;
        this.scope = scope;
        this.audience = audience;
    }

    /** Retorna o access_token; renova 30s antes de expirar. */
    public String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt.minusSeconds(30))) {
            return cachedToken;
        }
        synchronized (this) {
            if (cachedToken != null && Instant.now().isBefore(expiresAt.minusSeconds(30))) {
                return cachedToken;
            }
            return fetchAndCacheToken();
        }
    }

    /** Headers com Bearer pronto para chamadas HTTP. */
    public HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(getAccessToken());
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    /** Cria HttpEntity com Authorization: Bearer pronto. */
    public <T> HttpEntity<T> entity(T body) {
        return new HttpEntity<>(body, authHeaders());
    }

    /** Cria HttpEntity vazio apenas com Authorization: Bearer. */
    public HttpEntity<Void> entity() {
        return new HttpEntity<>(authHeaders());
    }    
    
    // ======================== internals ========================

    private String fetchAndCacheToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        StringBuilder body = new StringBuilder("grant_type=client_credentials");

        // client auth
        if (authMethod == ClientAuthMethod.BASIC) {
            headers.set(HttpHeaders.AUTHORIZATION, basicAuth(clientId, clientSecret));
        } else {
            body.append("&client_id=").append(urlEncode(clientId));
            if (clientSecret != null && !clientSecret.isBlank()) {
                body.append("&client_secret=").append(urlEncode(clientSecret));
            }
        }

        // opcionais
        if (scope != null && !scope.isBlank()) {
            body.append("&scope=").append(urlEncode(scope));
        }
        if (audience != null && !audience.isBlank()) {
            body.append("&audience=").append(urlEncode(audience));
        }

        ResponseEntity<Map<String, Object>> resp = rest.exchange(
                tokenEndpoint,
                HttpMethod.POST,
                new HttpEntity<>(body.toString(), headers),
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
        );

        Map<String, Object> m = resp.getBody();
        if (m == null || m.get("access_token") == null) {
            throw new IllegalStateException("Resposta do servidor OAuth2 sem access_token.");
        }

        String token = String.valueOf(m.get("access_token"));
        Number expiresIn = toNumber(m.get("expires_in"), 300);

        this.cachedToken = token;
        this.expiresAt = Instant.now().plusSeconds(expiresIn.longValue());
        return token;
    }

    private static Number toNumber(Object raw, int def) {
        if (raw instanceof Number n) return n;
        try { return Integer.valueOf(String.valueOf(raw)); } catch (Exception e) { return def; }
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String basicAuth(String user, String pass) {
        String up = user + ":" + (pass == null ? "" : pass);
        String b64 = Base64.getEncoder().encodeToString(up.getBytes(StandardCharsets.UTF_8));
        return "Basic " + b64;
    }
}