package br.com.ramiralvesmelo.util.http;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import br.com.ramiralvesmelo.util.exception.JwtRestClientException;

/**
 * Cliente REST estático com cache de access_token (OAuth2 Client Credentials).
 * - Chame init(...) uma única vez na inicialização da aplicação.
 * - Use os métodos estáticos get/post/put/patch/delete.
 */
public final class RestClient {

    private static volatile RestTemplate restTemplate;

    private static volatile String baseUrl;
    private static volatile String tokenUri;
    private static volatile String clientId;
    private static volatile String clientSecret;
    private static volatile String scope;

    private static final AtomicReference<String> CACHED_TOKEN = new AtomicReference<>(null);
    private static final AtomicReference<Instant> TOKEN_EXPIRY = new AtomicReference<>(Instant.EPOCH);

    private RestClient() {}

    /* ==========================
       Inicialização
       ========================== */
    public static synchronized void init(Config cfg) {
        if (cfg == null) throw new IllegalArgumentException("Config não pode ser nula.");

        baseUrl = cfg.baseUrl;
        tokenUri = cfg.tokenUri;
        clientId = cfg.clientId;
        clientSecret = cfg.clientSecret;
        scope = cfg.scope;

        if (cfg.restTemplate != null) {
            restTemplate = cfg.restTemplate;
        } else {
        	var factory = new HttpComponentsClientHttpRequestFactory();
        	factory.setConnectTimeout(Duration.ofMillis(cfg.connectTimeoutMs));
        	factory.setConnectionRequestTimeout(Duration.ofMillis(cfg.connectTimeoutMs));
        	factory.setReadTimeout(Duration.ofMillis(cfg.readTimeoutMs));

        	restTemplate = new RestTemplate(factory);
        }

        // limpa token cacheado ao reinicializar
        CACHED_TOKEN.set(null);
        TOKEN_EXPIRY.set(Instant.EPOCH);
    }

    public static boolean isInitialized() {
        return restTemplate != null && baseUrl != null && tokenUri != null
                && clientId != null && clientSecret != null;
    }

    private static void ensureInitialized() {
        if (!isInitialized()) {
            throw new IllegalStateException("JwtRestClientStatic não inicializado. Chame JwtRestClientStatic.init(cfg) no boot.");
        }
    }

    /* ==========================
       Token (client_credentials)
       ========================== */
    public static synchronized String obterAccessToken() {
        ensureInitialized();
        Instant now = Instant.now();

        var tok = CACHED_TOKEN.get();
        var exp = TOKEN_EXPIRY.get();
        if (tok != null && exp != null && exp.isAfter(now.plusSeconds(30))) {
            return tok;
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            if (scope != null && !scope.isBlank()) form.add("scope", scope);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

            @SuppressWarnings("rawtypes")
			ResponseEntity<Map> resp = restTemplate.postForEntity(
                    tokenUri, new HttpEntity<>(form, headers), Map.class);

            Map<?, ?> body = resp.getBody();
            if (body == null || !body.containsKey("access_token")) {
                throw new JwtRestClientException("Resposta do token endpoint inválida (sem access_token).");
            }

            String token = Objects.toString(body.get("access_token"));
            long expiresIn = body.get("expires_in") != null
                    ? Long.parseLong(body.get("expires_in").toString())
                    : 300L;

            CACHED_TOKEN.set(token);
            TOKEN_EXPIRY.set(now.plusSeconds(expiresIn));
            return token;

        } catch (HttpStatusCodeException e) {
            throw new JwtRestClientException("Falha ao obter token: HTTP "
                    + e.getStatusCode().value() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new JwtRestClientException("Falha ao obter token: " + e.getMessage(), e);
        }
    }

    /* ==========================
       Métodos HTTP (Class<R>)
       ========================== */
    public static <R> R get(String url, Class<R> responseType) {
        return exchange(HttpMethod.GET, url, null, responseType);
    }

    public static <T, R> R post(String url, T payload, Class<R> responseType) {
        return exchange(HttpMethod.POST, url, payload, responseType);
    }

    public static <T, R> R put(String url, T payload, Class<R> responseType) {
        return exchange(HttpMethod.PUT, url, payload, responseType);
    }

    public static <T, R> R patch(String url, T payload, Class<R> responseType) {
        return exchange(HttpMethod.PATCH, url, payload, responseType);
    }

    public static <R> R delete(String url, Class<R> responseType) {
        return exchange(HttpMethod.DELETE, url, null, responseType);
    }

    public static <T, R> R delete(String url, T payload, Class<R> responseType) {
        return exchange(HttpMethod.DELETE, url, payload, responseType);
    }

    /* ==========================
       Métodos HTTP (TypeReference)
       ========================== */
    public static <R> R get(String url, ParameterizedTypeReference<R> typeRef) {
        return exchange(HttpMethod.GET, url, null, typeRef);
    }

    public static <T, R> R post(String url, T payload, ParameterizedTypeReference<R> typeRef) {
        return exchange(HttpMethod.POST, url, payload, typeRef);
    }

    public static <T, R> R put(String url, T payload, ParameterizedTypeReference<R> typeRef) {
        return exchange(HttpMethod.PUT, url, payload, typeRef);
    }

    public static <T, R> R patch(String url, T payload, ParameterizedTypeReference<R> typeRef) {
        return exchange(HttpMethod.PATCH, url, payload, typeRef);
    }

    public static <R> R delete(String url, ParameterizedTypeReference<R> typeRef) {
        return exchange(HttpMethod.DELETE, url, null, typeRef);
    }

    public static <T, R> R delete(String url, T payload, ParameterizedTypeReference<R> typeRef) {
        return exchange(HttpMethod.DELETE, url, payload, typeRef);
    }

    /* ==========================
       Núcleo (Class<R>)
       ========================== */
    private static <T, R> R exchange(HttpMethod method, String url, T payload, Class<R> responseType) {
        ensureInitialized();
        String token = obterAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);
        if (payload != null && method != HttpMethod.GET) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        HttpEntity<T> entity = new HttpEntity<>(payload, headers);
        String effectiveUrl = buildUrl(url);

        try {
            ResponseEntity<R> resp = restTemplate.exchange(
                    URI.create(effectiveUrl), method, entity, responseType);
            return resp.getBody();
        } catch (HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            throw new JwtRestClientException("Erro HTTP " + e.getStatusCode().value()
                    + " ao chamar " + effectiveUrl + ": " + body, e);
        } catch (Exception e) {
            throw new JwtRestClientException("Falha na chamada " + method + " " + effectiveUrl
                    + ": " + e.getMessage(), e);
        }
    }

    /* ==========================
       Núcleo (TypeReference)
       ========================== */
    private static <T, R> R exchange(HttpMethod method, String url, T payload, ParameterizedTypeReference<R> typeRef) {
        ensureInitialized();
        String token = obterAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);
        if (payload != null && method != HttpMethod.GET) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        HttpEntity<T> entity = new HttpEntity<>(payload, headers);
        String effectiveUrl = buildUrl(url);

        try {
            ResponseEntity<R> resp = restTemplate.exchange(
                    URI.create(effectiveUrl), method, entity, typeRef);
            return resp.getBody();
        } catch (HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            throw new JwtRestClientException("Erro HTTP " + e.getStatusCode().value()
                    + " ao chamar " + effectiveUrl + ": " + body, e);
        } catch (Exception e) {
            throw new JwtRestClientException("Falha na chamada " + method + " " + effectiveUrl
                    + ": " + e.getMessage(), e);
        }
    }

    /* ==========================
       Helpers
       ========================== */
    private static String buildUrl(String url) {
        if (url == null || url.isBlank()) return baseUrl;
        boolean absolute = url.startsWith("http://") || url.startsWith("https://");
        if (absolute) return url;
        if (baseUrl.endsWith("/") && url.startsWith("/"))
            return baseUrl.substring(0, baseUrl.length() - 1) + url;
        if (!baseUrl.endsWith("/") && !url.startsWith("/"))
            return baseUrl + "/" + url;
        return baseUrl + url;
    }

    /* ==========================
       Config
       ========================== */
    public static final class Config {
        private String baseUrl;
        private String tokenUri;
        private String clientId;
        private String clientSecret;
        private String scope;
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 15000;
        private RestTemplate restTemplate; // opcional (injete o seu, se quiser)

        public Config baseUrl(String v) { this.baseUrl = v; return this; }
        public Config tokenUri(String v) { this.tokenUri = v; return this; }
        public Config clientId(String v) { this.clientId = v; return this; }
        public Config clientSecret(String v) { this.clientSecret = v; return this; }
        public Config scope(String v) { this.scope = v; return this; }
        public Config connectTimeoutMs(int v) { this.connectTimeoutMs = v; return this; }
        public Config readTimeoutMs(int v) { this.readTimeoutMs = v; return this; }
        public Config restTemplate(RestTemplate rt) { this.restTemplate = rt; return this; }
    }
}
