package br.com.ramiralvesmelo.util.http.client;

import java.time.Duration;
import java.util.ArrayList;
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
    private static final AtomicReference<String> BASE_URL = new AtomicReference<>("");
    private static final AtomicReference<Supplier<String>> BEARER_SUPPLIER =
            new AtomicReference<>(() -> null);

    /** Configuração imutável com encapsulamento e validação via Builder. */
    public static final class Cfg {
        private final String baseUrl;
        private final Duration connectTimeout;
        private final Duration readTimeout;
        /** retorna "Bearer xxx" ou só o token */
        private final Supplier<String> bearerSupplier;
        private final List<ClientHttpRequestInterceptor> extraInterceptors;
        /** pool (opcional) */
        private final int maxTotal;
        private final int maxPerRoute;

        private Cfg(Builder b) {
            this.baseUrl = b.baseUrl;
            this.connectTimeout = b.connectTimeout;
            this.readTimeout = b.readTimeout;
            this.bearerSupplier = b.bearerSupplier;
            this.extraInterceptors = List.copyOf(b.extraInterceptors); // cópia imutável
            this.maxTotal = b.maxTotal;
            this.maxPerRoute = b.maxPerRoute;
        }

        public String getBaseUrl() { return baseUrl; }
        public Duration getConnectTimeout() { return connectTimeout; }
        public Duration getReadTimeout() { return readTimeout; }
        public Supplier<String> getBearerSupplier() { return bearerSupplier; }
        public List<ClientHttpRequestInterceptor> getExtraInterceptors() { return extraInterceptors; }
        public int getMaxTotal() { return maxTotal; }
        public int getMaxPerRoute() { return maxPerRoute; }

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private String baseUrl = "";
            private Duration connectTimeout = Duration.ofSeconds(3);
            private Duration readTimeout = Duration.ofSeconds(5);
            private Supplier<String> bearerSupplier = () -> null;
            private List<ClientHttpRequestInterceptor> extraInterceptors = new ArrayList<>();
            private int maxTotal = 200;
            private int maxPerRoute = 50;

            public Builder baseUrl(String v) { this.baseUrl = (v != null ? v : ""); return this; }
            public Builder connectTimeout(Duration v) { this.connectTimeout = Objects.requireNonNull(v, "connectTimeout"); return this; }
            public Builder readTimeout(Duration v) { this.readTimeout = Objects.requireNonNull(v, "readTimeout"); return this; }
            public Builder bearerSupplier(Supplier<String> v) { this.bearerSupplier = (v != null ? v : () -> null); return this; }
            public Builder extraInterceptors(List<ClientHttpRequestInterceptor> v) {
                this.extraInterceptors = (v != null ? new ArrayList<>(v) : new ArrayList<>()); return this;
            }
            public Builder maxTotal(int v) { this.maxTotal = v; return this; }
            public Builder maxPerRoute(int v) { this.maxPerRoute = v; return this; }

            public Cfg build() {
                if (connectTimeout.isNegative() || connectTimeout.isZero()) {
                    throw new IllegalArgumentException("connectTimeout deve ser > 0");
                }
                if (readTimeout.isNegative() || readTimeout.isZero()) {
                    throw new IllegalArgumentException("readTimeout deve ser > 0");
                }
                if (maxTotal <= 0) {
                    throw new IllegalArgumentException("maxTotal deve ser > 0");
                }
                if (maxPerRoute <= 0) {
                    throw new IllegalArgumentException("maxPerRoute deve ser > 0");
                }
                return new Cfg(this);
            }
        }
    }

    /** Inicializa com configuração imutável. */
    public static void init(Cfg cfg) {
        Objects.requireNonNull(cfg, "cfg");
        BASE_URL.set(cfg.getBaseUrl() != null ? cfg.getBaseUrl() : "");

        // 1) connect timeout
        ConnectionConfig connConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(cfg.getConnectTimeout().toMillis()))
                .build();

        // 2) pool
        PoolingHttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connConfig)
                .build();
        cm.setMaxTotal(cfg.getMaxTotal());
        cm.setDefaultMaxPerRoute(cfg.getMaxPerRoute());

        // 3) response timeout
        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMilliseconds(cfg.getReadTimeout().toMillis()))
                .build();

        // 4) HttpClient
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .build();

        // 5) RequestFactory
        HttpComponentsClientHttpRequestFactory rf = new HttpComponentsClientHttpRequestFactory(httpClient);
        rf.setConnectTimeout((int) cfg.getConnectTimeout().toMillis());
        rf.setReadTimeout((int) cfg.getReadTimeout().toMillis());

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
        BEARER_SUPPLIER.set(cfg.getBearerSupplier());
        ClientHttpRequestInterceptor auth = (req, body, ex) -> {
            Supplier<String> sup = BEARER_SUPPLIER.get();
            String token = sup != null ? sup.get() : null;
            if (token != null && !token.isBlank()) {
                req.getHeaders().set(HttpHeaders.AUTHORIZATION,
                        token.startsWith("Bearer ") ? token : "Bearer " + token);
            }
            return ex.execute(req, body);
        };
        rt.getInterceptors().add(auth);

        List<ClientHttpRequestInterceptor> extras = cfg.getExtraInterceptors();
        if (extras != null && !extras.isEmpty()) {
            rt.getInterceptors().addAll(extras);
        }

        REF.set(rt);
    }

    /** Acesso somente-leitura (opcional) ao baseUrl atual. */
    public static String getBaseUrl() {
        return BASE_URL.get();
    }

    // ===== infra interna =====
    private static RestTemplate rt() {
        RestTemplate r = REF.get();
        if (r == null) {
            throw new IllegalStateException("RestClient não inicializado. Chame RestClient.init(cfg) no boot.");
        }
        return r;
    }

    private static String url(String path) {
        String baseUrl = BASE_URL.get(); // snapshot atômico
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