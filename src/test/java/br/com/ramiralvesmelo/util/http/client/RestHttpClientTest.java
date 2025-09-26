package br.com.ramiralvesmelo.util.http.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

class RestHttpClientTest {

    // ==== helpers de reflexão para acessar o REF estático ====

    private static AtomicReference<RestTemplate> getRefField() throws Exception {
        Field f = RestHttpClient.class.getDeclaredField("REF");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        AtomicReference<RestTemplate> ref = (AtomicReference<RestTemplate>) f.get(null);
        return ref;
    }

    private static void setRef(RestTemplate rt) throws Exception {
        getRefField().set(rt);
    }

    private RestTemplate mockRt;

    @BeforeEach
    void setup() throws Exception {
        // inicializa com uma config válida (para testar interceptors/Jackson depois)
        RestHttpClient.Cfg cfg = new RestHttpClient.Cfg();
        cfg.baseUrl = "http://api";
        cfg.connectTimeout = Duration.ofMillis(1234);
        cfg.readTimeout = Duration.ofMillis(5678);
        cfg.bearerSupplier = () -> "abc"; // sem 'Bearer' para testar prefixo
        cfg.extraInterceptors = List.of((req, body, ex) -> ex.execute(req, body));

        RestHttpClient.init(cfg);

        // Em vários testes, vamos substituir internamente por um RestTemplate mockado
        mockRt = mock(RestTemplate.class);
    }

    @AfterEach
    void tearDown() throws Exception {
        // limpa REF para evitar interferência entre testes
        setRef(null);
    }

    // ==== init(Cfg) e aspectos estruturais ====

    @Test
    void init_deveRegistrarJacksonJavaTimeEInterceptorBearer() throws Exception {
        // Após o @BeforeEach, init já foi chamado com cfg válido
        RestTemplate current = getRefField().get();
        assertNotNull(current);

        // Deve haver um MappingJackson2HttpMessageConverter (com JavaTime)
        boolean temJackson = current.getMessageConverters().stream()
                .anyMatch(MappingJackson2HttpMessageConverter.class::isInstance);
        assertTrue(temJackson, "Deveria haver MappingJackson2HttpMessageConverter configurado");

        // Deve ter pelo menos 2 interceptors: bearer + extra
        assertTrue(current.getInterceptors().size() >= 2);
    }

    @Test
    void rt_deveFalharQuandoNaoInicializado() throws Exception {
        // zera o REF para simular cenário não inicializado
        setRef(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> RestHttpClient.get("/x", String.class));
        assertTrue(ex.getMessage().contains("não inicializado"));
    }

    // ==== URL resolution através dos helpers ====

    @Test
    void get_deveResolverUrlComBarra_noPath() throws Exception {
        setRef(mockRt);
        when(mockRt.getForObject(eq("http://api/v1/itens"), eq(String.class))).thenReturn("ok");

        String r = RestHttpClient.get("/v1/itens", String.class);
        assertEquals("ok", r);
        verify(mockRt).getForObject("http://api/v1/itens", String.class);
    }

    @Test
    void get_deveResolverUrlSemBarra_noPath() throws Exception {
        setRef(mockRt);
        when(mockRt.getForObject(eq("http://api/v1/itens"), eq(String.class))).thenReturn("ok-2");

        String r = RestHttpClient.get("v1/itens", String.class);
        assertEquals("ok-2", r);
        verify(mockRt).getForObject("http://api/v1/itens", String.class);
    }

    @Test
    void get_deveManterUrlAbsoluta() throws Exception {
        setRef(mockRt);
        String abs = "https://other/svc";
        when(mockRt.getForObject(eq(abs), eq(String.class))).thenReturn("abs");

        String r = RestHttpClient.get(abs, String.class);
        assertEquals("abs", r);
        verify(mockRt).getForObject(abs, String.class);
    }

    // ==== Métodos helpers Class<T> ====

    @Test
    void getEntity_ok() throws Exception {
        setRef(mockRt);
        ResponseEntity<String> resp = ResponseEntity.ok("X");
        when(mockRt.getForEntity("http://api/p", String.class)).thenReturn(resp);

        ResponseEntity<String> out = RestHttpClient.getEntity("/p", String.class);
        assertEquals("X", out.getBody());
    }

    @Test
    void post_ok() throws Exception {
        setRef(mockRt);
        when(mockRt.postForObject(eq("http://api/p"), eq("body"), eq(Integer.class))).thenReturn(42);

        Integer out = RestHttpClient.post("/p", "body", Integer.class);
        assertEquals(42, out);
    }

    @Test
    void put_ok() throws Exception {
        setRef(mockRt);
        ResponseEntity<Integer> resp = ResponseEntity.ok(7);
        when(mockRt.exchange(eq("http://api/p"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Integer.class)))
                .thenReturn(resp);

        Integer out = RestHttpClient.put("/p", "b", Integer.class);
        assertEquals(7, out);
    }

    @Test
    void delete_ok() throws Exception {
        setRef(mockRt);
        ResponseEntity<String> resp = ResponseEntity.ok("deleted");
        when(mockRt.exchange(eq("http://api/p"), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(String.class)))
                .thenReturn(resp);

        String out = RestHttpClient.delete("/p", String.class);
        assertEquals("deleted", out);
    }

    // ==== Métodos helpers com ParameterizedTypeReference ====

    @Test
    void getParameterized_ok() throws Exception {
        setRef(mockRt);
        ParameterizedTypeReference<List<String>> type = new ParameterizedTypeReference<>() {};
        ResponseEntity<List<String>> resp = ResponseEntity.ok(List.of("a", "b"));
        when(mockRt.exchange(eq("http://api/p"), eq(HttpMethod.GET), any(HttpEntity.class), eq(type)))
                .thenReturn(resp);

        List<String> out = RestHttpClient.get("/p", type);
        assertEquals(List.of("a", "b"), out);
    }

    @Test
    void postParameterized_ok() throws Exception {
        setRef(mockRt);
        ParameterizedTypeReference<Integer> type = new ParameterizedTypeReference<>() {};
        ResponseEntity<Integer> resp = ResponseEntity.ok(9);
        when(mockRt.exchange(eq("http://api/p"), eq(HttpMethod.POST), any(HttpEntity.class), eq(type)))
                .thenReturn(resp);

        Integer out = RestHttpClient.post("/p", "body", type);
        assertEquals(9, out);
    }

    @Test
    void putParameterized_ok() throws Exception {
        setRef(mockRt);
        ParameterizedTypeReference<String> type = new ParameterizedTypeReference<>() {};
        ResponseEntity<String> resp = ResponseEntity.ok("ok");
        when(mockRt.exchange(eq("http://api/p"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(type)))
                .thenReturn(resp);

        String out = RestHttpClient.put("/p", "B", type);
        assertEquals("ok", out);
    }

    @Test
    void deleteParameterized_ok() throws Exception {
        setRef(mockRt);
        ParameterizedTypeReference<String> type = new ParameterizedTypeReference<>() {};
        ResponseEntity<String> resp = ResponseEntity.ok("bye");
        when(mockRt.exchange(eq("http://api/p"), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(type)))
                .thenReturn(resp);

        String out = RestHttpClient.delete("/p", type);
        assertEquals("bye", out);
    }

    // ==== exchange(...) overloads ====

    @Test
    void exchange_semHeaders_ok() throws Exception {
        setRef(mockRt);
        ResponseEntity<String> resp = ResponseEntity.ok("x");
        when(mockRt.exchange(eq("http://api/path"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(resp);

        ResponseEntity<String> out = RestHttpClient.exchange("/path", HttpMethod.POST, "b", String.class);
        assertEquals("x", out.getBody());
    }

    @Test
    void exchange_comHeaders_ok() throws Exception {
        setRef(mockRt);
        HttpHeaders headers = new HttpHeaders();
        headers.set("K", "V");

        ResponseEntity<Integer> resp = ResponseEntity.ok(1);
        when(mockRt.exchange(eq("http://api/path"), eq(HttpMethod.GET), argThat(entity -> {
            // valida que os headers que passamos foram incluídos
            return "V".equals(entity.getHeaders().getFirst("K"));
        }), eq(Integer.class))).thenReturn(resp);

        ResponseEntity<Integer> out = RestHttpClient.exchange("/path", HttpMethod.GET, headers, null, Integer.class);
        assertEquals(1, out.getBody());
    }

    @Test
    void exchange_parameterized_ok() throws Exception {
        setRef(mockRt);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X", "Y");
        ParameterizedTypeReference<List<String>> type = new ParameterizedTypeReference<>() {};
        ResponseEntity<List<String>> resp = ResponseEntity.ok(List.of("k"));

        when(mockRt.exchange(eq("http://api/p2"), eq(HttpMethod.DELETE),
                argThat(entity -> "Y".equals(entity.getHeaders().getFirst("X"))), eq(type)))
                .thenReturn(resp);

        ResponseEntity<List<String>> out =
                RestHttpClient.exchange("/p2", HttpMethod.DELETE, headers, null, type);

        assertEquals(List.of("k"), out.getBody());
    }

    // ==== interceptor Bearer ====

    @Test
    void interceptorBearer_devePreencherAuthorizationQuandoTokenNaoVazio() throws Exception {
        // pega o RestTemplate montado pelo init() do @BeforeEach
        RestTemplate current = getRefField().get();
        assertNotNull(current);

        // por contrato da classe, o interceptor Bearer é adicionado antes dos extras
        List<ClientHttpRequestInterceptor> interceptors = current.getInterceptors();
        assertFalse(interceptors.isEmpty());
        ClientHttpRequestInterceptor bearer = interceptors.get(0);

        // mocks do request/execution para exercitar o interceptor diretamente
        var req = mock(org.springframework.http.HttpRequest.class);
        var headers = new HttpHeaders();
        when(req.getHeaders()).thenReturn(headers);

        ClientHttpRequestExecution exec = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse resp = mock(ClientHttpResponse.class);
        when(exec.execute(any(), any())).thenReturn(resp);

        byte[] body = "{}".getBytes();
        bearer.intercept(req, body, exec);

        assertEquals("Bearer abc", headers.getFirst(HttpHeaders.AUTHORIZATION));
        verify(exec).execute(any(), eq(body));
    }

    @Test
    void interceptorBearer_naoDefineHeaderQuandoTokenVazio() throws Exception {
        // Re-inicializa com supplier vazio
        RestHttpClient.Cfg cfg = new RestHttpClient.Cfg();
        cfg.baseUrl = "http://api";
        cfg.bearerSupplier = () -> "  "; // em branco
        RestHttpClient.init(cfg);

        RestTemplate current = getRefField().get();
        ClientHttpRequestInterceptor bearer = current.getInterceptors().get(0);

        var req = mock(org.springframework.http.HttpRequest.class);
        var headers = new HttpHeaders();
        when(req.getHeaders()).thenReturn(headers);

        ClientHttpRequestExecution exec = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse resp = mock(ClientHttpResponse.class);
        when(exec.execute(any(), any())).thenReturn(resp);

        bearer.intercept(req, new byte[0], exec);

        // não deve setar Authorization
        assertNull(headers.getFirst(HttpHeaders.AUTHORIZATION));
    }
}
