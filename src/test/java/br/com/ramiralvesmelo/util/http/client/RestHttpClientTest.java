package br.com.ramiralvesmelo.util.http.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

public class RestHttpClientTest {

    @AfterEach
    void cleanup() throws Exception {
        RestHttpClient.Cfg cfg = new RestHttpClient.Cfg();
        cfg.baseUrl = "http://localhost:8089/base";
        cfg.connectTimeout = Duration.ofMillis(200);
        cfg.readTimeout = Duration.ofMillis(200);
        cfg.bearerSupplier = null;
        cfg.extraInterceptors = List.of();
        RestHttpClient.init(cfg);
    }

    // ===== util: acessar/modificar o RestTemplate interno (sem classes embutidas) =====
    private RestTemplate internalRt() throws Exception {
        Field f = RestHttpClient.class.getDeclaredField("REF");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        AtomicReference<RestTemplate> ref = (AtomicReference<RestTemplate>) f.get(null);
        return ref.get();
    }

    private void setInternalRt(RestTemplate rt) throws Exception {
        Field f = RestHttpClient.class.getDeclaredField("REF");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        AtomicReference<RestTemplate> ref = (AtomicReference<RestTemplate>) f.get(null);
        ref.set(rt);
    }

    private static DefaultResponseCreator json(String j) {
        return MockRestResponseCreators.withSuccess(j, MediaType.APPLICATION_JSON);
    }

    // ==================================================================================

    @Test
    @DisplayName("Deve lançar IllegalStateException quando usado sem init()")
    void notInitialized_throws() throws Exception {
        setInternalRt(null);
        assertThrows(IllegalStateException.class, () -> RestHttpClient.get("/ping", String.class));
    }

    @Test
    @DisplayName("init aceita nulos em baseUrl/bearerSupplier/extraInterceptors")
    void init_acceptNulls_ok() throws Exception {
        RestHttpClient.Cfg cfg = new RestHttpClient.Cfg();
        cfg.baseUrl = null;
        cfg.bearerSupplier = null;
        cfg.extraInterceptors = null;
        RestHttpClient.init(cfg);

        RestTemplate rt = internalRt();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();

        server.expect(ExpectedCount.once(),
                MockRestRequestMatchers.requestTo("http://absolute-host/ok"))
              .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
              .andRespond(MockRestResponseCreators.withSuccess("OK", MediaType.TEXT_PLAIN));

        String out = RestHttpClient.get("http://absolute-host/ok", String.class);
        assertThat(out).isEqualTo("OK");
        server.verify();
    }

    @Test
    @DisplayName("url(): combinações de barras (base com/sem '/', path com/sem '/')")
    void url_combinations() throws Exception {
        RestHttpClient.Cfg cfg = new RestHttpClient.Cfg();
        cfg.baseUrl = "http://localhost:8089/base/"; // com barra final
        RestHttpClient.init(cfg);

        RestTemplate rt = internalRt();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();

        // base "/" + path "/" -> remove uma
        server.expect(ExpectedCount.once(),
                MockRestRequestMatchers.requestTo(URI.create("http://localhost:8089/base/v1")))
              .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
              .andRespond(json("true"));

        // base "/" + path sem "/" -> concatena direto
        server.expect(ExpectedCount.once(),
                MockRestRequestMatchers.requestTo(URI.create("http://localhost:8089/base/v2")))
              .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
              .andRespond(json("true"));

        assertThat(RestHttpClient.get("/v1", String.class)).isEqualTo("true");
        assertThat(RestHttpClient.get("v2", String.class)).isEqualTo("true");
        server.verify();

        // troca base para sem barra
        cfg.baseUrl = "http://localhost:8089/base";
        RestHttpClient.init(cfg);

        rt = internalRt();
        server = MockRestServiceServer.bindTo(rt).build();

        // base sem "/" + path "/" -> concatena direto
        server.expect(ExpectedCount.once(),
                MockRestRequestMatchers.requestTo(URI.create("http://localhost:8089/base/v3")))
              .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
              .andRespond(json("true"));

        // base sem "/" + path sem "/" -> insere "/"
        server.expect(ExpectedCount.once(),
                MockRestRequestMatchers.requestTo(URI.create("http://localhost:8089/base/v4")))
              .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
              .andRespond(json("true"));

        assertThat(RestHttpClient.get("/v3", String.class)).isEqualTo("true");
        assertThat(RestHttpClient.get("v4", String.class)).isEqualTo("true");
        server.verify();
    }

    @Test
    @DisplayName("path absoluto (http/https) ignora baseUrl")
    void absolute_path_ignores_base() throws Exception {
        RestHttpClient.Cfg cfg = new RestHttpClient.Cfg();
        cfg.baseUrl = "http://localhost:9999/ignored";
        RestHttpClient.init(cfg);

        RestTemplate rt = internalRt();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();

        String abs = "http://localhost:8089/direct";
        server.expect(ExpectedCount.once(),
                MockRestRequestMatchers.requestTo(abs))
              .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
              .andRespond(json("{\"ok\":true}"));

        Map<String, Object> resp = RestHttpClient.get(abs, new ParameterizedTypeReference<Map<String, Object>>() {});
        assertThat(resp).containsEntry("ok", true);
        server.verify();
    }

    @Test
    @DisplayName("Bearer sem prefixo → adiciona 'Bearer ' automaticamente")
    void bearer_without_prefix() throws Exception {
        RestHttpClient.Cfg cfg = new RestHttpClient.Cfg();
        cfg.baseUrl = "http://localhost:8089/api";
        cfg.bearerSupplier = () -> "abc123";
        RestHttpClient.init(cfg);

        RestTemplate rt = internalRt();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();

        server.expect(ExpectedCount.once(),
                MockRestRequestMatchers.requestTo("http://localhost:8089/api/ping"))
              .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
              .andExpect(req -> assertThat(req.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                  .isEqualTo("Bearer abc123"))
              .andRespond(MockRestResponseCreators.withSuccess("pong", MediaType.TEXT_PLAIN));

        String out = RestHttpClient.get("/ping", String.class);
        assertThat(out).isEqualTo("pong");
        server.verify();
    }

    @Test
    @DisplayName("Bearer com prefixo → mantém o valor")
    void bearer_with_prefix() throws Exception {
        RestHttpClient.Cfg cfg = new RestHttpClient.Cfg();
        cfg.baseUrl = "http://localhost:8089/api";
        cfg.bearerSupplier = () -> "Bearer XYZ";
        RestHttpClient.init(cfg);

        RestTemplate rt = internalRt();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();

        server.expect(ExpectedCount.once(),
                MockRestRequestMatchers.requestTo("http://localhost:8089/api/ping2"))
              .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
              .andExpect(req -> assertThat(req.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                  .isEqualTo("Bearer XYZ"))
              .andRespond(MockRestResponseCreators.withSuccess("pong2", MediaType.TEXT_PLAIN));

        String out = RestHttpClient.get("/ping2", String.class);
        assertThat(out).isEqualTo("pong2");
        server.verify();
    }

    @Test
    @DisplayName("get/post/put/delete (Class<T>) retornam corpo e usam URLs corretas")
    void class_based_methods() throws Exception {
        RestHttpClient.Cfg cfg = new RestHttpClient.Cfg();
        cfg.baseUrl = "http://localhost:8089/svc";
        RestHttpClient.init(cfg);

        RestTemplate rt = internalRt();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();

        server.expect(ExpectedCount.once(),
                MockRestRequestMatchers.requestTo("http://localhost:8089/svc/users/42"))
              .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
              .andRespond(json("\"ok-get\""));

        server.expect(ExpectedCount.once(),
                MockRestRequestMatchers.requestTo("http://localhost:8089/svc/users"))
              .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
              .andRespond(json("\"ok-post\""));

        server.expect(ExpectedCount.once(),
                MockRestRequestMatchers.requestTo("http://localhost:8089/svc/users/42"))
              .andExpect(MockRestRequestMatchers.method(HttpMethod.PUT))
              .andRespond(json("\"ok-put\""));

        server.expect(ExpectedCount.once(),
                MockRestRequestMatchers.requestTo("http://localhost:8089/svc/users/42"))
              .andExpect(MockRestRequestMatchers.method(HttpMethod.DELETE))
              .andRespond(json("\"ok-del\""));

        assertThat(RestHttpClient.get("/users/42", String.class)).isEqualTo("ok-get");
        assertThat(RestHttpClient.post("/users", Map.of("name","A"), String.class)).isEqualTo("ok-post");
        assertThat(RestHttpClient.put("/users/42", Map.of("name","B"), String.class)).isEqualTo("ok-put");
        assertThat(RestHttpClient.delete("/users/42", String.class)).isEqualTo("ok-del");

        server.verify();
    }

    @Test
    @DisplayName("get/post/put/delete (ParameterizedTypeReference<R>)")
    void typeRef_based_methods() throws Exception {
        RestHttpClient.Cfg cfg = new RestHttpClient.Cfg();
        cfg.baseUrl = "http://localhost:8089/data";
        RestHttpClient.init(cfg);

        RestTemplate rt = internalRt();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();

        server.expect(ExpectedCount.times(1),
                MockRestRequestMatchers.requestTo("http://localhost:8089/data/nums"))
              .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
              .andRespond(json("[1,2,3]"));

        server.expect(ExpectedCount.times(1),
                MockRestRequestMatchers.requestTo("http://localhost:8089/data/nums"))
              .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
              .andRespond(json("[4,5]"));

        server.expect(ExpectedCount.times(1),
                MockRestRequestMatchers.requestTo("http://localhost:8089/data/nums"))
              .andExpect(MockRestRequestMatchers.method(HttpMethod.PUT))
              .andRespond(json("[6]"));

        server.expect(ExpectedCount.times(1),
                MockRestRequestMatchers.requestTo("http://localhost:8089/data/nums"))
              .andExpect(MockRestRequestMatchers.method(HttpMethod.DELETE))
              .andRespond(json("[9]"));

        ParameterizedTypeReference<List<Integer>> type = new ParameterizedTypeReference<List<Integer>>() {};

        assertThat(RestHttpClient.get("/nums", type)).containsExactly(1,2,3);
        assertThat(RestHttpClient.post("/nums", Map.of("x",1), type)).containsExactly(4,5);
        assertThat(RestHttpClient.put("/nums", Map.of("x",2), type)).containsExactly(6);
        assertThat(RestHttpClient.delete("/nums", type)).containsExactly(9);

        server.verify();
    }

    @Test
    @DisplayName("exchange sem headers (Class<R>)")
    void exchange_noHeaders_class() throws Exception {
        RestHttpClient.Cfg cfg = new RestHttpClient.Cfg();
        cfg.baseUrl = "http://localhost:8089/ex";
        RestHttpClient.init(cfg);

        RestTemplate rt = internalRt();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();

        server.expect(ExpectedCount.once(),
                MockRestRequestMatchers.requestTo("http://localhost:8089/ex/echo"))
              .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
              .andRespond(json("\"ok\""));

        ResponseEntity<String> resp =
                RestHttpClient.exchange("/echo", HttpMethod.POST, Map.of("k","v"), String.class);
        assertThat(resp.getBody()).isEqualTo("ok");

        server.verify();
    }

    @Test
    @DisplayName("exchange com headers (Class<R>)")
    void exchange_withHeaders_class() throws Exception {
        RestHttpClient.Cfg cfg = new RestHttpClient.Cfg();
        cfg.baseUrl = "http://localhost:8089/ex2";
        RestHttpClient.init(cfg);

        RestTemplate rt = internalRt();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();

        server.expect(ExpectedCount.once(),
                MockRestRequestMatchers.requestTo("http://localhost:8089/ex2/echo2"))
              .andExpect(MockRestRequestMatchers.method(HttpMethod.PUT))
              .andExpect(req -> assertThat(req.getHeaders().getFirst("X-Test")).isEqualTo("yes"))
              .andRespond(json("\"ok2\""));

        HttpHeaders h = new HttpHeaders();
        h.add("X-Test", "yes");
        ResponseEntity<String> resp =
                RestHttpClient.exchange("/echo2", HttpMethod.PUT, h, Map.of("x",1), String.class);
        assertThat(resp.getBody()).isEqualTo("ok2");

        server.verify();
    }

    @Test
    @DisplayName("exchange com headers (ParameterizedTypeReference<R>)")
    void exchange_withHeaders_typeRef() throws Exception {
        RestHttpClient.Cfg cfg = new RestHttpClient.Cfg();
        cfg.baseUrl = "http://localhost:8089/ex3";
        RestHttpClient.init(cfg);

        RestTemplate rt = internalRt();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();

        server.expect(ExpectedCount.once(),
                MockRestRequestMatchers.requestTo("http://localhost:8089/ex3/list"))
              .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
              .andRespond(json("[10,11]"));

        ParameterizedTypeReference<List<Integer>> type =
                new ParameterizedTypeReference<List<Integer>>() {};
        ResponseEntity<List<Integer>> resp =
                RestHttpClient.exchange("/list", HttpMethod.GET, new HttpHeaders(), null, type);

        assertThat(resp.getBody()).containsExactly(10, 11);
        server.verify();
    }

    @Test
    @DisplayName("extraInterceptors são adicionados ao RestTemplate")
    void extraInterceptors_added() throws Exception {
        ClientHttpRequestInterceptor addHeader = (req, body, ex) -> {
            req.getHeaders().add("X-Extra", "1");
            return ex.execute(req, body);
        };

        RestHttpClient.Cfg cfg = new RestHttpClient.Cfg();
        cfg.baseUrl = "http://localhost:8089/inter";
        cfg.extraInterceptors = List.of(addHeader);
        RestHttpClient.init(cfg);

        RestTemplate rt = internalRt();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();

        server.expect(ExpectedCount.once(),
                MockRestRequestMatchers.requestTo("http://localhost:8089/inter/p"))
              .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
              .andExpect(req -> assertThat(req.getHeaders().getFirst("X-Extra")).isEqualTo("1"))
              .andRespond(MockRestResponseCreators.withSuccess("ok", MediaType.TEXT_PLAIN));

        String out = RestHttpClient.get("/p", String.class);
        assertThat(out).isEqualTo("ok");
        server.verify();
    }
}
