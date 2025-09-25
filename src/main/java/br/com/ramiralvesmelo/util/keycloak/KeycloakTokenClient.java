package br.com.ramiralvesmelo.util.keycloak;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class KeycloakTokenClient {
    private final RestTemplate rest = new RestTemplate();
    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH; // expira no passado (força 1ª obtenção)

    public KeycloakTokenClient(String baseUrl, String realm, String clientId, String clientSecret) {
        this.tokenUrl = ensureNoTrailingSlash(baseUrl) + "/realms/" + realm + "/protocol/openid-connect/token";
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret");
    }

    public String getAccessToken() {
        // Renova 30s antes de expirar
        if (cachedToken != null && Instant.now().isBefore(expiresAt.minusSeconds(30))) {
            return cachedToken;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body =
              "grant_type=client_credentials"
            + "&client_id=" + urlEncode(clientId)
            + "&client_secret=" + urlEncode(clientSecret);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> resp = rest.exchange(tokenUrl, HttpMethod.POST, entity, Map.class);
        Map<?, ?> m = resp.getBody();

        String token = (String) m.get("access_token");
        Object raw = m.get("expires_in");
        Number expiresIn = raw instanceof Number ? (Number) raw : 300;

        this.cachedToken = token;
        this.expiresAt = Instant.now().plusSeconds(expiresIn.longValue());
        return token;
    }

    private static String ensureNoTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
