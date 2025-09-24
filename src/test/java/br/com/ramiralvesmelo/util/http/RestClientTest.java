package br.com.ramiralvesmelo.util.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import br.com.ramiralvesmelo.util.exception.JwtRestClientException;

@ExtendWith(MockitoExtension.class)
class RestClientTest {

    private RestTemplate rt; // mock
    private static final String BASE = "http://api.local";
    private static final String TOKEN_URI = "http://auth.local/realms/x/protocol/openid-connect/token";

    @BeforeEach
    void setup() {
        rt = mock(RestTemplate.class);

        // init com RestTemplate injetado (evita criar factory real)
        var cfg = new RestClient.Config()
                .baseUrl(BASE)
                .tokenUri(TOKEN_URI)
                .clientId("cid")
                .clientSecret("secret")
                .scope("read")
                .connectTimeoutMs(2000)
                .readTimeoutMs(4000)
                .restTemplate(rt);
        RestClient.init(cfg);
    }

    private Map<String, Object> token(String access, long expiresInSec) {
        var map = new HashMap<String, Object>();
        map.put("access_token", access);
        map.put("expires_in", expiresInSec);
        return map;
    }

    @Test
    void init_e_isInitialized_ok() {
        assertTrue(RestClient.isInitialized());
    }

    @Test
    void obterAccessToken_busca_e_cacheia() {
        // primeira vez busca
        when(rt.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(token("tok-123", 3600)));

        String t1 = RestClient.obterAccessToken();
        assertEquals("tok-123", t1);

        // segunda vez usa cache (não chama o endpoint de token novamente)
        String t2 = RestClient.obterAccessToken();
        assertEquals("tok-123", t2);

        verify(rt, times(1)).postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void obterAccessToken_refresh_quando_perto_de_expirar() {
        when(rt.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(token("tok-short", 1)))  // expira ~agora
                .thenReturn(ResponseEntity.ok(token("tok-new", 3600)));

        String t1 = RestClient.obterAccessToken();
        assertEquals("tok-short", t1);

        // chamada seguinte deve refazer a requisição (por causa do “+30s”)
        String t2 = RestClient.obterAccessToken();
        assertEquals("tok-new", t2);

        verify(rt, times(2)).postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void obterAccessToken_erro_http_wrap_em_JwtRestClientException() {
        when(rt.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "unauth"));

        JwtRestClientException ex = assertThrows(JwtRestClientException.class, RestClient::obterAccessToken);
        assertTrue(ex.getMessage().contains("Falha ao obter token"));
    }

    @Test
    void obterAccessToken_resposta_sem_access_token_dispara_excecao() {
        when(rt.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("foo", "bar")));

        JwtRestClientException ex = assertThrows(JwtRestClientException.class, RestClient::obterAccessToken);
        assertTrue(ex.getMessage().contains("sem access_token"));
    }

    @Test
    void get_ClassR_sucesso_e_headers_ok_e_url_relativa() {
        // token
        when(rt.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(token("bearer-xyz", 3600)));

        // resposta do GET
        when(rt.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        String out = RestClient.get("/v1/ping", String.class);
        assertEquals("ok", out);

        // captura dos argumentos
        ArgumentCaptor<URI> uriCap = ArgumentCaptor.forClass(URI.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<Object>> entCap = ArgumentCaptor.forClass((Class) HttpEntity.class);

        verify(rt).exchange(uriCap.capture(), eq(HttpMethod.GET), entCap.capture(), eq(String.class));

        assertEquals(URI.create(BASE + "/v1/ping"), uriCap.getValue());

        HttpHeaders hdr = entCap.getValue().getHeaders();
        assertTrue(hdr.getFirst(HttpHeaders.AUTHORIZATION).startsWith("Bearer "));
        // GET não precisa setar Content-Type
        assertNull(hdr.getContentType());
    }

    @Test
    void post_e_delete_com_body_ClassR_cabecalhos_json() {
        when(rt.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(token("bearer-abc", 3600)));

        when(rt.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("status", "created")));

        Map<String, Object> payload = Map.of("a", 1);
        Map resp = RestClient.post("/v1/create", payload, Map.class);
        assertEquals("created", resp.get("status"));

        // valida headers do POST
        ArgumentCaptor<HttpEntity<Map<String, Object>>> entCap = ArgumentCaptor.forClass((Class) HttpEntity.class);
        verify(rt).exchange(eq(URI.create(BASE + "/v1/create")), eq(HttpMethod.POST), entCap.capture(), eq(Map.class));
        HttpHeaders h = entCap.getValue().getHeaders();
        assertEquals(MediaType.APPLICATION_JSON, h.getContentType());
        assertTrue(h.getFirst(HttpHeaders.AUTHORIZATION).startsWith("Bearer "));

        // DELETE com body
        when(rt.exchange(any(URI.class), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        Void none = RestClient.delete("/v1/delete", Map.of("x", 2), Void.class);
        assertNull(none);
        verify(rt).exchange(eq(URI.create(BASE + "/v1/delete")), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    void get_TypeRef_sucesso_generics() {
        when(rt.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(token("bearer-gen", 3600)));

        ParameterizedTypeReference<List<String>> typeRef = new ParameterizedTypeReference<>() {};
        when(rt.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(List.of("a", "b")));

        List<String> out = RestClient.get("http://other/p", typeRef); // URL absoluta deve passar intacta
        assertEquals(List.of("a", "b"), out);

        // captura URI para garantir que URL absoluta não foi unida ao baseUrl
        ArgumentCaptor<URI> uriCap = ArgumentCaptor.forClass(URI.class);
        verify(rt).exchange(uriCap.capture(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class));
        assertEquals(URI.create("http://other/p"), uriCap.getValue());
    }

    @Test
    void exchange_erro_http_wrap_em_JwtRestClientException() {
        when(rt.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(token("tok-err", 3600)));

        // simula 400 na chamada do serviço
        when(rt.exchange(any(URI.class), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpClientErrorException(
                        HttpStatus.BAD_REQUEST, "bad",
                        "oops".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));

        JwtRestClientException ex = assertThrows(JwtRestClientException.class,
                () -> RestClient.put("/v1/put", Map.of("k", "v"), String.class));
        assertTrue(ex.getMessage().contains("Erro HTTP 400"));
        assertTrue(ex.getMessage().contains("/v1/put"));
    }
}
