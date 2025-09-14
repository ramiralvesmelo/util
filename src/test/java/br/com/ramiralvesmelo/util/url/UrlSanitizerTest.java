package br.com.ramiralvesmelo.util.url;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.junit.jupiter.api.Test;

class UrlSanitizerTest {

    // --- isValid ---

    @Test
    void isValid_deveAceitarHttpHttpsELocalhost() {
        assertTrue(UrlSanitizer.isValid("http://example.com"));
        assertTrue(UrlSanitizer.isValid("https://example.com"));
        assertTrue(UrlSanitizer.isValid("http://localhost"));
        assertTrue(UrlSanitizer.isValid("http://localhost:8080/app"));
        assertTrue(UrlSanitizer.isValid("https://127.0.0.1:8443/health"));
    }

    @Test
    void isValid_deveRejeitarInvalidos() {
        assertFalse(UrlSanitizer.isValid(null));
        assertFalse(UrlSanitizer.isValid("ftp://example.com")); // apenas http/https
        assertFalse(UrlSanitizer.isValid("ht!tp://bad"));
        assertFalse(UrlSanitizer.isValid("://sem-esquema"));
    }

    // --- sanitize ---

    @Test
    void sanitize_deveSanitizarTextoSimplesSemTratarComoUrl() {
        String in = "  li\"nha\rquebra\n  ";
        String out = UrlSanitizer.sanitize(in);
        assertEquals("li'nha quebra", out); // troca " por ' + remove CR/LF + trim
    }

    @Test
    void sanitize_deveSanitizarENormalizarQuandoForUrlValida() {
        String in = "  http://localhost:8080/app\r\n";
        String out = UrlSanitizer.sanitize(in);
        assertTrue(out.startsWith("http://localhost:8080/app"));
        assertFalse(out.contains("\r"));
        assertFalse(out.contains("\n"));
        assertEquals(out, UrlSanitizer.sanitize(out)); // idempotente
    }

    @Test
    void sanitize_naoAceitaNull() {
        assertThrows(NullPointerException.class, () -> UrlSanitizer.sanitize(null));
    }

    // --- buildUrl ---

    @Test
    void buildUrl_devePreservarPathDaBaseEAdicionarSegmentsESanitizarQuery() {
        String base = "http://host/app";
        List<String> segments = Arrays.asList(" v1 ", " pedi\"dos  \r\n", "itens");
        Map<String, String> query = new LinkedHashMap<>();
        query.put(" q ", " numero 123\r\n");
        query.put("pag\"e", " 1 ");

        String url = UrlSanitizer.buildUrl(base, segments, query);

        // Verifica PATH decodificado (independente se veio ' ou %27)
        URI uri = URI.create(url);
        String decodedPath = URLDecoder.decode(uri.getRawPath(), StandardCharsets.UTF_8);
        assertEquals("/app/v1/pedi'dos/itens", decodedPath);

        // Verifica QUERY decodificada (espaço pode ser '+' ou %20; URLDecoder trata ambos)
        Map<String, String> qp = decodeQueryParams(uri.getRawQuery());
        assertEquals("numero 123", qp.get("q"));
        assertEquals("1", qp.get("pag'e"));
    }

    @Test
    void buildUrl_deveFuncionarComListasVaziasOuNull() {
        String base = "https://x/y";
        String u1 = UrlSanitizer.buildUrl(base, null, null); // ambos null
        assertEquals("https://x/y", u1);

        String u2 = UrlSanitizer.buildUrl(base, Collections.emptyList(), null); // path vazio, query null
        assertEquals("https://x/y", u2);

        String u3 = UrlSanitizer.buildUrl(base, null, Collections.emptyMap()); // path null, query vazia
        assertEquals("https://x/y", u3);
    }

    @Test
    void buildUrl_deveLancarIllegalArgumentExceptionSeBaseInvalida() {
        String baseInvalida = "http//:bad host";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> UrlSanitizer.buildUrl(baseInvalida, List.of("a"), Map.of("k","v")));
        assertTrue(ex.getMessage().contains("URL base inválida"));
    }

    @Test
    void buildUrl_naoAceitaBaseNull() {
        assertThrows(NullPointerException.class,
            () -> UrlSanitizer.buildUrl(null, List.of("a"), Map.of("k","v")));
    }

    // --- helpers ---

    private static Map<String, String> decodeQueryParams(String rawQuery) {
        Map<String, String> map = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) return map;
        for (String pair : rawQuery.split("&")) {
            int idx = pair.indexOf('=');
            String k = idx >= 0 ? pair.substring(0, idx) : pair;
            String v = idx >= 0 ? pair.substring(idx + 1) : "";
            k = URLDecoder.decode(k, StandardCharsets.UTF_8);
            v = URLDecoder.decode(v, StandardCharsets.UTF_8);
            map.put(k, v);
        }
        return map;
    }
}
