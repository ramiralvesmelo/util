package br.com.ramiralvesmelo.util.security.oauth2;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import br.com.ramiralvesmelo.util.security.oauth2.OAuth2TokenClient.ClientAuthMethod;

class OAuth2TokenClientTest {

    // ==== helpers de reflexão para manipular campos privados/finais ====

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Object getField(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    // Reseta o RestTemplate interno para não vazar entre testes
    @AfterEach
    void afterEach() {
        // nada obrigatório aqui (cada teste cria sua própria instância)
    }

    // ============================================================
    // Cache: usa token quando ainda falta > 30s para expirar
    // ============================================================
    @Test
    void getAccessToken_deveUsarCacheQuandoAindaValido() throws Exception {
        OAuth2TokenClient client = new OAuth2TokenClient(
                "https://idp/token", "cli", "sec", ClientAuthMethod.BASIC, null, null);

        // injeta mock do RestTemplate
        RestTemplate rt = mock(RestTemplate.class);
        setField(client, "rest", rt);

        // simula cache ainda válido (falta 100s)
        setField(client, "cachedToken", "TOK-CACHED");
        setField(client, "expiresAt", Instant.now().plusSeconds(100));

        String tok = client.getAccessToken();

        assertEquals("TOK-CACHED", tok);
        verifyNoInteractions(rt); // não bate no servidor
    }

    // ============================================================
    // Renova quando faltam <= 30s: fluxo BASIC (Authorization: Basic …)
    // ============================================================
    @Test
    void getAccessToken_deveRenovarSeFaltam30s_AuthBasic_ComScopeEAudience() throws Exception {
        OAuth2TokenClient client = new OAuth2TokenClient(
                "https://idp/realms/x/protocol/openid-connect/token",
                "my-client",
                "my-secret",
                ClientAuthMethod.BASIC,
                "read write",   // scope
                "api://default" // audience
        );

        RestTemplate rt = mock(RestTemplate.class);
        setField(client, "rest", rt);

        // Força situação de renovação (expira em 10s => dentro da janela de 30s)
        setField(client, "cachedToken", "OLD");
        setField(client, "expiresAt", Instant.now().plusSeconds(10));

        // mock da resposta do servidor de token
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("access_token", "NEW-TOKEN");
        body.put("expires_in", 60); // numérico
        ResponseEntity<Map<String, Object>> resp = ResponseEntity.ok(body);

        ArgumentCaptor<HttpEntity<String>> entityCap = ArgumentCaptor.forClass(HttpEntity.class);
        when(rt.exchange(
                eq("https://idp/realms/x/protocol/openid-connect/token"),
                eq(HttpMethod.POST),
                entityCap.capture(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(resp);

        String tok = client.getAccessToken();
        assertEquals("NEW-TOKEN", tok);

        // valida HEADER Basic
        HttpEntity<String> sent = entityCap.getValue();
        HttpHeaders h = sent.getHeaders();
        assertEquals(MediaType.APPLICATION_FORM_URLENCODED, h.getContentType());
        String auth = h.getFirst(HttpHeaders.AUTHORIZATION);
        assertNotNull(auth);
        assertTrue(auth.startsWith("Basic "));

        // confere credenciais codificadas
        String expectedBasic = "Basic " + Base64.getEncoder()
                .encodeToString("my-client:my-secret".getBytes(StandardCharsets.UTF_8));
        assertEquals(expectedBasic, auth);

        // valida corpo (grant + scope + audience) — sem client_id/client_secret (pois BASIC)
        String form = sent.getBody();
        assertNotNull(form);
        assertTrue(form.contains("grant_type=client_credentials"));
        assertTrue(form.contains("scope=read+write"));
        assertTrue(form.contains("audience=api%3A%2F%2Fdefault"));
        assertFalse(form.contains("client_id="));
        assertFalse(form.contains("client_secret="));

        // cachedToken e expiresAt devem ter sido atualizados
        assertEquals("NEW-TOKEN", getField(client, "cachedToken"));
        Instant expiresAt = (Instant) getField(client, "expiresAt");
        assertTrue(expiresAt.isAfter(Instant.now()));
    }

    // ============================================================
    // Renova com ClientAuthMethod.POST (credenciais no corpo)
    // ============================================================
    @Test
    void getAccessToken_deveUsarAuthPost_EnviandoCredenciaisNoCorpo() throws Exception {
        OAuth2TokenClient client = new OAuth2TokenClient(
                "https://idp/token", "cid", "csecret", ClientAuthMethod.POST, "s1 s2", "aud-x");

        RestTemplate rt = mock(RestTemplate.class);
        setField(client, "rest", rt);

        // força renovar
        setField(client, "cachedToken", "OLD2");
        setField(client, "expiresAt", Instant.now().plusSeconds(1)); // dentro de 30s

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("access_token", "TOK-POST");
        body.put("expires_in", "120"); // string -> cai no toNumber
        ResponseEntity<Map<String, Object>> resp = ResponseEntity.ok(body);

        ArgumentCaptor<HttpEntity<String>> entityCap = ArgumentCaptor.forClass(HttpEntity.class);
        when(rt.exchange(eq("https://idp/token"), eq(HttpMethod.POST), entityCap.capture(), any(ParameterizedTypeReference.class)))
                .thenReturn(resp);

        String tok = client.getAccessToken();
        assertEquals("TOK-POST", tok);

        HttpEntity<String> sent = entityCap.getValue();
        // não deve ter Authorization Basic
        assertNull(sent.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));

        String form = sent.getBody();
        assertNotNull(form);
        assertTrue(form.contains("grant_type=client_credentials"));
        assertTrue(form.contains("client_id=cid"));
        assertTrue(form.contains("client_secret=csecret"));
        assertTrue(form.contains("scope=s1+s2"));
        assertTrue(form.contains("audience=aud-x"));
    }

    // ============================================================
    // Erro: sem access_token na resposta
    // ============================================================
    @Test
    void getAccessToken_deveLancarQuandoRespostaNaoTemAccessToken() throws Exception {
        OAuth2TokenClient client = new OAuth2TokenClient(
                "https://idp/token", "cid", "sec", ClientAuthMethod.BASIC, null, null);

        RestTemplate rt = mock(RestTemplate.class);
        setField(client, "rest", rt);

        // força renovar
        setField(client, "cachedToken", null);
        setField(client, "expiresAt", Instant.EPOCH);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("expires_in", "abc"); // inválido -> toNumber usa default 300, mas falta access_token
        ResponseEntity<Map<String, Object>> resp = ResponseEntity.ok(body);

        when(rt.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(resp);

        IllegalStateException ex = assertThrows(IllegalStateException.class, client::getAccessToken);
        assertTrue(ex.getMessage().contains("sem access_token"));
    }

    // ============================================================
    // authHeaders() e entity()
    // ============================================================
    @Test
    void authHeadersEEntity_devemUsarBearerDoCache() throws Exception {
        OAuth2TokenClient client = new OAuth2TokenClient(
                "https://idp/token", "cid", "sec", ClientAuthMethod.BASIC, null, null);

        RestTemplate rt = mock(RestTemplate.class);
        setField(client, "rest", rt);

        // injeta token no cache (válido longe)
        setField(client, "cachedToken", "CACHED-123");
        setField(client, "expiresAt", Instant.now().plusSeconds(3600));

        HttpHeaders headers = client.authHeaders();
        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
        assertEquals("Bearer CACHED-123", headers.getFirst(HttpHeaders.AUTHORIZATION));

        var e1 = client.entity("body");
        assertEquals("body", e1.getBody());
        assertEquals("Bearer CACHED-123", e1.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));

        var e2 = client.entity();
        assertNull(e2.getBody());
        assertEquals("Bearer CACHED-123", e2.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));

        // como usou cache, não chamou o servidor
        verifyNoInteractions(rt);
    }
}
