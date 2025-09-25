package br.com.ramiralvesmelo.util.http.client;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class RestHttpClient {

    private RestHttpClient() {}

    private static final AtomicReference<RestTemplate> REF = new AtomicReference<>();
    private static volatile String baseUrl = "";
    private static volatile Supplier<String> bearerSupplier = () -> null;

    public static final class Cfg {
        public String baseUrl;
        public Duration connectTimeout = Duration.ofSeconds(3);
        public Duration readTimeout = Duration.ofSeconds(5);
        /** retorna "Bearer xxx" ou só o token */
        public Supplier<String> bearerSupplier;
        public List<ClientHttpRequestInterceptor> extraInterceptors = List.of();
        /** pool (opcional) */
        public int maxTotal = 200;
        public int maxPerRoute = 50;
    }

    public static void init(Cfg cfg) {
        Objects.requireNonNull(cfg, "cfg");
        baseUrl = cfg.baseUrl != null ? cfg.baseUrl : "";

        // 1) connect timeout no ConnectionConfig (sem deprecated)
        ConnectionConfig connConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(cfg.connectTimeout.toMillis()))
                .build();

        // 2) connection manager + pool
        PoolingHttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connConfig)
                .build();
        cm.setMaxTotal(cfg.maxTotal);
        cm.setDefaultMaxPerRoute(cfg.maxPerRoute);

        // 3) response timeout por requisição
        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMilliseconds(cfg.readTimeout.toMillis()))
                .build();

        // 4) HttpClient
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .build();

        // 5) RequestFactory
        HttpComponentsClientHttpRequestFactory rf = new HttpComponentsClientHttpRequestFactory(httpClient);
        // compat: algumas versões do Spring ainda usam int aqui
        rf.setConnectTimeout((int) cfg.connectTimeout.toMillis());
        rf.setReadTimeout((int) cfg.readTimeout.toMillis());

        // 6) Jackson JavaTime
        ObjectMapper om = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MappingJackson2HttpMessageConverter jackson = new MappingJackson2HttpMessageConverter(om);

        // 7) RestTemplate
        RestTemplate rt = new RestTemplate(rf);
        rt.getMessageConverters().removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
        rt.getMessageConverters().add(0, jackson);

        // 8) Interceptor Bearer
        bearerSupplier = cfg.bearerSupplier != null ? cfg.bearerSupplier : () -> null;
        ClientHttpRequestInterceptor auth = (req, body, ex) -> {
            String token = bearerSupplier.get();
            if (token != null && !token.isBlank()) {
                req.getHeaders().set(HttpHeaders.AUTHORIZATION,
                        token.startsWith("Bearer ") ? token : "Bearer " + token);
            }
            return ex.execute(req, body);
        };
        rt.getInterceptors().add(auth);

        if (cfg.extraInterceptors != null && !cfg.extraInterceptors.isEmpty()) {
            rt.getInterceptors().addAll(cfg.extraInterceptors);
        }

        REF.set(rt);
    }

    private static RestTemplate rt() {
        RestTemplate r = REF.get();
        if (r == null) {
            throw new IllegalStateException("RestClient não inicializado. Chame RestClient.init(cfg) no boot.");
        }
        return r;
    }

    private static String url(String path) {
        if (path == null) return baseUrl;
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        if (baseUrl == null || baseUrl.isBlank()) return path;
        if (baseUrl.endsWith("/") && path.startsWith("/")) return baseUrl + path.substring(1);
        if (!baseUrl.endsWith("/") && !path.startsWith("/")) return baseUrl + "/" + path;
        return baseUrl + path;
    }

    // ==== helpers básicos (Class<T>) ====

    public static <T> T get(String path, Class<T> type) {
        return rt().getForObject(url(path), type);
    }

    public static <T> ResponseEntity<T> getEntity(String path, Class<T> type) {
        return rt().getForEntity(url(path), type);
    }

    public static <B, R> R post(String path, B body, Class<R> type) {
        return rt().postForObject(url(path), body, type);
    }

    public static <B, R> R put(String path, B body, Class<R> type) {
        ResponseEntity<R> resp = exchange(path, HttpMethod.PUT, null, body, type);
        return resp.getBody();
    }

    public static <R> R delete(String path, Class<R> type) {
        ResponseEntity<R> resp = exchange(path, HttpMethod.DELETE, null, null, type);
        return resp.getBody();
    }

    // ==== helpers tipados (ParameterizedTypeReference<R>) ====

    public static <R> R get(String path, ParameterizedTypeReference<R> typeRef) {
        ResponseEntity<R> resp = exchange(path, HttpMethod.GET, null, null, typeRef);
        return resp.getBody();
    }

    public static <B, R> R post(String path, B body, ParameterizedTypeReference<R> typeRef) {
        ResponseEntity<R> resp = exchange(path, HttpMethod.POST, null, body, typeRef);
        return resp.getBody();
    }

    public static <B, R> R put(String path, B body, ParameterizedTypeReference<R> typeRef) {
        ResponseEntity<R> resp = exchange(path, HttpMethod.PUT, null, body, typeRef);
        return resp.getBody();
    }

    public static <R> R delete(String path, ParameterizedTypeReference<R> typeRef) {
        ResponseEntity<R> resp = exchange(path, HttpMethod.DELETE, null, null, typeRef);
        return resp.getBody();
    }

    // ==== exchange genéricos ====

    public static <B, R> ResponseEntity<R> exchange(
            String path, HttpMethod method, B body, Class<R> responseType) {
        HttpEntity<B> entity = new HttpEntity<>(body);
        return rt().exchange(url(path), method, entity, responseType);
    }

    public static <B, R> ResponseEntity<R> exchange(
            String path, HttpMethod method, HttpHeaders headers, B body, Class<R> responseType) {
        HttpEntity<B> entity = new HttpEntity<>(body, headers);
        return rt().exchange(url(path), method, entity, responseType);
    }

    public static <B, R> ResponseEntity<R> exchange(
            String path, HttpMethod method, HttpHeaders headers, B body,
            ParameterizedTypeReference<R> typeRef) {
        HttpEntity<B> entity = new HttpEntity<>(body, headers);
        return rt().exchange(url(path), method, entity, typeRef);
    }
}
