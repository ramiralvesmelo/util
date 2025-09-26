package br.com.ramiralvesmelo.util.http.url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.ramiralvesmelo.util.core.exception.UrlException;

class UrlBuilderTest {

    // ===== buildAbsolute (base + path) =====
    @Test
    @DisplayName("buildAbsolute deve juntar base+path e normalizar barras")
    void buildAbsolute_ok() {
        String out = UrlBuilder.buildAbsolute("http://host/app/", "/v1//orders");
        assertThat(out).isEqualTo("http://host/app/v1/orders");
    }

    @Test
    @DisplayName("buildAbsolute deve lançar UrlException quando baseUrl inválida")
    void buildAbsolute_baseInvalida_lancaUrlException() {
        assertThrows(UrlException.class, () ->
            UrlBuilder.buildAbsolute("http://h^ost:8080", "x")
        );
    }

    // ===== buildAbsolute (múltiplos segmentos) =====
    @Test
    @DisplayName("buildAbsolute(base, segments...) junta corretamente e normaliza")
    void buildAbsolute_variosSegmentos_ok() {
        String out = UrlBuilder.buildAbsolute("https://api.exemplo.com/base",
                "v1", "/customers/", "//123");
        assertThat(out).isEqualTo("https://api.exemplo.com/base/v1/customers/123");
    }

    // ===== buildAbsolute com query =====
    @Test
    @DisplayName("buildAbsolute(base, path, query) inclui apenas params com chave/valor não-nulos")
    void buildAbsolute_comQuery_ok() {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("q", "john");
        q.put("page", 2);
        q.put("nullKey", null);
        q.put(null, "x");
        String out = UrlBuilder.buildAbsolute("http://h/svc", "users", q);
        assertThat(out)
            .isEqualTo("http://h/svc/users?q=john&page=2");
    }

    // ===== legado: buidlUrl/buildUrl =====
    @Test
    @DisplayName("buidlUrl preserva '//' no path e sanitiza identificadores")
    void buidlUrl_preservaBarrasDuplasESanitiza() {
        String out = UrlBuilder.buidlUrl("http://h", "/a//b/", "PED#001/A");
        // mantém '//' (legado) e troca caracteres inválidos do orderNumber por '_'
        assertThat(out).isEqualTo("http://h/a//b/PED_001_A");
    }

    @Test
    @DisplayName("buildUrl delega para buidlUrl")
    void buildUrl_alias() {
        String out = UrlBuilder.buildUrl("http://h", "/x/", "Nº 42");
        assertThat(out).isEqualTo("http://h/x/N__42");
    }

    // ===== escape =====
    @Test
    @DisplayName("escape usa URLEncoder UTF-8 e substitui espaço por '+'")
    void escape_ok() {
        assertThat(UrlBuilder.escape("João da Silva & Cia"))
            .isEqualTo("Jo%C3%A3o+da+Silva+%26+Cia");
        assertThat(UrlBuilder.escape(null)).isNull();
    }

    // ===== buildAbsoluteStrict: validações de entrada =====
    @Test
    @DisplayName("buildAbsoluteStrict baseUrl vazio/nulo -> IAE")
    void strict_baseUrlVazioOuNulo() {
        assertThrows(IllegalArgumentException.class, () ->
            UrlBuilder.buildAbsoluteStrict(null, "x"));
        assertThrows(IllegalArgumentException.class, () ->
            UrlBuilder.buildAbsoluteStrict("   ", "x"));
    }

    @Test
    @DisplayName("buildAbsoluteStrict path nulo -> IAE")
    void strict_pathNulo() {
        assertThrows(IllegalArgumentException.class, () ->
            UrlBuilder.buildAbsoluteStrict("http://h", null));
    }

    @Test
    @DisplayName("buildAbsoluteStrict path com espaço cru -> IAE")
    void strict_pathComEspacoCru() {
        assertThrows(IllegalArgumentException.class, () ->
            UrlBuilder.buildAbsoluteStrict("http://h", "a b"));
    }

    @Test
    @DisplayName("buildAbsoluteStrict path com percent-encoding inválido -> IAE")
    void strict_percentEncodingInvalido() {
        assertThrows(IllegalArgumentException.class, () ->
            UrlBuilder.buildAbsoluteStrict("http://h", "a%GZb"));
        assertThrows(IllegalArgumentException.class, () ->
            UrlBuilder.buildAbsoluteStrict("http://h", "a%")); // termina com '%'
    }

    @Test
    @DisplayName("buildAbsoluteStrict scheme inválido -> IAE")
    void strict_schemeInvalido() {
        assertThrows(IllegalArgumentException.class, () ->
            UrlBuilder.buildAbsoluteStrict("ftp://host", "x"));
        assertThrows(IllegalArgumentException.class, () ->
            UrlBuilder.buildAbsoluteStrict("://host", "x"));
    }

    @Test
    @DisplayName("buildAbsoluteStrict detecta '//' redundante (agora antes de normalizar) -> IAE")
    void strict_doubleSlash_noPath() {
        // path contém '//' interno; joinPaths não remove nesse ponto
        assertThrows(IllegalArgumentException.class, () ->
            UrlBuilder.buildAbsoluteStrict("http://h/base", "a//b"));
        // também se vier do basePath + path
        assertThrows(IllegalArgumentException.class, () ->
            UrlBuilder.buildAbsoluteStrict("http://h/base//", "b"));
    }

    @Test
    @DisplayName("buildAbsoluteStrict converte URISyntaxException em IAE")
    void strict_uriSyntaxInvalida_iae() {
        assertThrows(IllegalArgumentException.class, () ->
            UrlBuilder.buildAbsoluteStrict("http://h^ost", "x"));
    }

    @Test
    @DisplayName("buildAbsoluteStrict OK quando entradas válidas")
    void strict_ok() {
        String out = UrlBuilder.buildAbsoluteStrict("https://host/app", "/v1/customers");
        assertThat(out).isEqualTo("https://host/app/v1/customers");
    }
}
