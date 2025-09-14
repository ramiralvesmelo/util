package br.com.ramiralvesmelo.util.url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import br.com.ramiralvesmelo.util.exception.IntegrationException;

class UrlUtilTest {

    private static final String BASE = "http://localhost:8084/app-event";
    private static final String PATH = "/download/pdf/";

    @Test
    @DisplayName("Deve montar URL completa a partir de base + path + orderNumber")
    void buidlUrl_ok() {
        String result = UrlUtil.buidlUrl(BASE, PATH, "ORD-0001.pdf");
        assertThat(result)
            .isEqualTo("http://localhost:8084/app-event/download/pdf/ORD-0001.pdf");
    }

    @ParameterizedTest(name = "Sanitiza orderNumber \"{0}\" -> \"{1}\"")
    @MethodSource("orderNumbers")
    @DisplayName("Sanitização: caracteres fora do whitelist viram '_' e demais são preservados")
    void buidlUrl_sanitize(String input, String expectedFile) {
        String result = UrlUtil.buidlUrl(BASE, PATH, input);
        assertThat(result)
            .isEqualTo("http://localhost:8084/app-event/download/pdf/" + expectedFile);
    }

    static Stream<Arguments> orderNumbers() {
        return Stream.of(
            Arguments.of("ORD 0001.pdf", "ORD_0001.pdf"),  // espaço -> _
            Arguments.of("A/B?C.pdf", "A_B_C.pdf"),        // / e ? -> _
            Arguments.of("ç-ã_β.pdf", "_-___.pdf"),        // unicode fora do whitelist -> múltiplos _
            Arguments.of("ORD-1.2.3.pdf", "ORD-1.2.3.pdf") // preserva . _ - e dígitos/letras
        );
    }

    @Test
    @DisplayName("Com orderNumber null, deve terminar no path sem arquivo (string vazia pós-sanitização)")
    void buidlUrl_orderNull() {
        String result = UrlUtil.buidlUrl(BASE, PATH, null);
        assertThat(result)
            .isEqualTo("http://localhost:8084/app-event/download/pdf/");
    }

    @Test
    @DisplayName("Deve lançar IntegrationException quando baseUrl é inválida (URISyntaxException)")
    void buidlUrl_baseInvalida() {
        String invalidBase = "http://localhost:8084 app-event"; // espaço no host
        assertThatThrownBy(() -> UrlUtil.buidlUrl(invalidBase, PATH, "X.pdf"))
            .isInstanceOf(IntegrationException.class)
            .hasMessageContaining("URL inválida");
    }

    @Test
    @DisplayName("Base com barra final preserva comportamento atual (gera //download no resultado)")
    void buidlUrl_baseComBarraFinal() {
        String baseComBarra = "http://localhost:8084/app-event/";
        String result = UrlUtil.buidlUrl(baseComBarra, PATH, "ORD-9.pdf");
        // Observação: implementação atual concatena "/app-event/" + "/download..." => "//download..."
        assertThat(result)
            .isEqualTo("http://localhost:8084/app-event//download/pdf/ORD-9.pdf");
    }

    @Test
    @DisplayName("Cobertura do construtor privado via reflexão")
    void constructor_privateCoverage() throws Exception {
        Constructor<UrlUtil> c = UrlUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        UrlUtil instance = c.newInstance();
        assertThat(instance).isNotNull();
    }
}
