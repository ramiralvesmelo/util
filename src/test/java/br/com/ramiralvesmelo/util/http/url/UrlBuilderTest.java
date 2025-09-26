package br.com.ramiralvesmelo.util.http.url;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.com.ramiralvesmelo.util.core.exception.UrlException;

class UrlBuilderTest {

    // =========================
    // buildAbsolute(base, path)
    // =========================
    @Test
    void buildAbsolute_deveJuntarBaseEPath_normalizandoBarras() {
        String out = UrlBuilder.buildAbsolute("https://api.exemplo.com/base/", "/v1//itens");
        // normaliza path (sem barras duplas no path)
        assertEquals("https://api.exemplo.com/base/v1/itens", out);
    }

    @Test
    void buildAbsolute_deveManterEsquemaHostQueryFragmentDaBase() throws Exception {
        String base = "http://srv.local:8080/app?x=1#frag";
        String out = UrlBuilder.buildAbsolute(base, "sub/recurso");
        URI u = new URI(out);
        assertEquals("http", u.getScheme());
        assertEquals("srv.local", u.getHost());
        assertEquals(8080, u.getPort());
        assertEquals("/app/sub/recurso", u.getPath());
        // query/fragment da base permanecem
        assertEquals("x=1", u.getQuery());
        assertEquals("frag", u.getFragment());
    }

    @Test
    void buildAbsolute_deveLancarUrlException_quandoBaseInvalida() {
        assertThrows(UrlException.class,
            () -> UrlBuilder.buildAbsolute("://base-invalida", "/x"));
    }

    // ==============================
    // buildAbsolute(base, segments…)
    // ==============================
    @Test
    void buildAbsolute_comVariosSegmentos_deveNormalizar() {
        String out = UrlBuilder.buildAbsolute("https://h.com/a", "/b/", "/c", "d/");
        assertEquals("https://h.com/a/b/c/d/", out);
    }

    // ==================================================
    // buildAbsolute(base, path, queryParams)
    // ==================================================
    @Test
    void buildAbsolute_comQueryParams_deveAdicionarSomenteValidos() throws Exception {
        Map<String, Object> qp = new LinkedHashMap<>();
        qp.put("a", 1);
        qp.put("b", "x");
        qp.put(null, "ignorar"); // chave nula ignora
        qp.put("c", null);       // valor nulo ignora

        String out = UrlBuilder.buildAbsolute("https://h.com/api", "v1/i", qp);

        // ordem preservada (LinkedHashMap): a=1&b=x
        URI u = new URI(out);
        assertEquals("/api/v1/i", u.getPath());
        assertEquals("a=1&b=x", u.getQuery());
    }

    // =========================
    // buidlUrl / buildUrl (legado)
    // =========================
    @Test
    void buidlUrl_devePreservarBarrasDuplas_noComportamentoLegado() {
        // basePath "/api/" + path "/v1/" = "/api//v1/" (dupla preservada)
        String out = UrlBuilder.buidlUrl("https://h.com/api/", "/v1/", "ABC/123");
        // sanitize troca "/" por "_"
        assertTrue(out.contains("/api//v1/ABC_123"), "deveria preservar // no path legado");
    }

    @Test
    void buildUrl_aliasDeveDelegarParaBuidlUrl() {
        String a = UrlBuilder.buidlUrl("https://h.com/x/", "/y/", "Nº 1");
        String b = UrlBuilder.buildUrl("https://h.com/x/", "/y/", "Nº 1");
        assertEquals(a, b);
    }

    // =========
    // escape()
    // =========
    @Test
    void escape_deveUsarUrlEncoderComMaisParaEspaco() {
        assertEquals("a+b", UrlBuilder.escape("a b"));
        // caractere acentuado vira UTF-8 percent-encoded
        assertEquals("%C3%A1", UrlBuilder.escape("á"));
        assertNull(UrlBuilder.escape(null));
    }

    // ============================
    // buildAbsoluteStrict(base,path)
    // ============================
    @Test
    void buildAbsoluteStrict_deveConstruirQuandoValido() {
        String out = UrlBuilder.buildAbsoluteStrict("https://h.com/base", "v1/ok");
        assertEquals("https://h.com/base/v1/ok", out);
    }

    @Test
    void buildAbsoluteStrict_deveFalharQuandoBaseVaziaOuNula() {
        assertThrows(IllegalArgumentException.class,
            () -> UrlBuilder.buildAbsoluteStrict(null, "x"));
        assertThrows(IllegalArgumentException.class,
            () -> UrlBuilder.buildAbsoluteStrict("  ", "x"));
    }

    @Test
    void buildAbsoluteStrict_deveFalharQuandoPathNulo() {
        assertThrows(IllegalArgumentException.class,
            () -> UrlBuilder.buildAbsoluteStrict("https://h.com", null));
    }

    @Test
    void buildAbsoluteStrict_deveFalharQuandoPathTemEspacoNaoEscapado() {
        assertThrows(IllegalArgumentException.class,
            () -> UrlBuilder.buildAbsoluteStrict("https://h.com", "com espaco"));
    }

    @Test
    void buildAbsoluteStrict_deveFalharQuandoPercentEncodingInvalido() {
        // %G1 é inválido (G não é hex)
        assertThrows(IllegalArgumentException.class,
            () -> UrlBuilder.buildAbsoluteStrict("https://h.com", "x%G1y"));
    }

    @Test
    void buildAbsoluteStrict_deveFalharQuandoSchemeInvalido() {
        assertThrows(IllegalArgumentException.class,
            () -> UrlBuilder.buildAbsoluteStrict("ftp://h.com", "ok"));
        assertThrows(IllegalArgumentException.class,
            () -> UrlBuilder.buildAbsoluteStrict("://bad", "ok"));
    }
}
