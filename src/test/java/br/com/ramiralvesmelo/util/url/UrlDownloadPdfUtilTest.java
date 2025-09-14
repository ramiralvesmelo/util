package br.com.ramiralvesmelo.util.url;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UrlDownloadPdfUtilTest {

    @Test
    @DisplayName("Deve montar URL preservando path base e adicionando /download/pdf/{arquivo}.pdf")
    void buildPdfLink_preservaPathEAdicionaSegmentos() {
        String host = "http://localhost:8080/app-event";
        String file = "ORD-123";

        String out = UrlDownloadPdfUtil.buildPdfLink(host, file);

        assertTrue(out.startsWith("http://localhost:8080/app-event"), "Prefixo deve preservar o path base");
        assertTrue(out.endsWith("/download/pdf/ORD-123.pdf"), "Sufixo deve ter segmentos e .pdf");
    }

    @Test
    @DisplayName("Deve remover barras finais do path base para evitar //download")
    void buildPdfLink_removeBarrasFinaisDoPath() {
        String host = "http://localhost:8080/app-event////";
        String file = "ORD-123";

        String out = UrlDownloadPdfUtil.buildPdfLink(host, file);

        // Não pode conter "//download" depois do host
        assertFalse(out.contains("//download"), "Não deve haver '//' antes de 'download'");
        assertTrue(out.endsWith("/download/pdf/ORD-123.pdf"));
    }

    @Test
    @DisplayName("Deve aceitar fileName com .pdf (qualquer caixa) e não duplicar extensão")
    void buildPdfLink_respeitaExtensaoPdfCaseInsensitive() {
        String host = "http://localhost:8080/app-event/";
        String out1 = UrlDownloadPdfUtil.buildPdfLink(host, "nota.PDF");
        String out2 = UrlDownloadPdfUtil.buildPdfLink(host, "nota.pdf");

        assertTrue(out1.endsWith("/download/pdf/nota.PDF"));
        assertTrue(out2.endsWith("/download/pdf/nota.pdf"));
    }

    @Test
    @DisplayName("Deve falhar com appEventHost null ou vazio")
    void buildPdfLink_hostNullOuVazio() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> UrlDownloadPdfUtil.buildPdfLink("   ", "ORD-1"));
        assertTrue(ex1.getMessage().contains("appEventHost"));

        assertThrows(NullPointerException.class,
                () -> UrlDownloadPdfUtil.buildPdfLink(null, "ORD-1"));
    }

    @Test
    @DisplayName("Deve falhar com fileName null ou vazio")
    void buildPdfLink_fileNullOuVazio() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> UrlDownloadPdfUtil.buildPdfLink("http://localhost:8080/app", "   "));
        assertTrue(ex1.getMessage().contains("fileName"));

        assertThrows(NullPointerException.class,
                () -> UrlDownloadPdfUtil.buildPdfLink("http://localhost:8080/app", null));
    }

    @Test
    @DisplayName("Caminho de exceção: URIBuilder lança URISyntaxException no helper privado e método retorna a URL original")
    void removeTrailingSlashFromPath_caminhoExcecaoRetornaOriginal() throws Exception {
        // Acessa o método privado via reflection para cobrir o bloco catch
        Method m = UrlDownloadPdfUtil.class.getDeclaredMethod("removeTrailingSlashFromPath", String.class);
        m.setAccessible(true);

        String invalid = "http://:porta-invalida"; // inválida para URIBuilder
        String result;
        try {
            result = (String) m.invoke(null, invalid); // método é static
        } catch (InvocationTargetException ite) {
            // Em caso de erro inesperado do invoke, rethrow causa real para depurar
            throw (ite.getCause() instanceof Exception) ? (Exception) ite.getCause() : ite;
        }

        assertEquals(invalid, result, "Quando há URISyntaxException, deve retornar a URL original");
    }

    @Test
    @DisplayName("Não deve alterar host sem path; apenas anexa segmentos corretamente")
    void buildPdfLink_hostSemPath() {
        String host = "http://example.com";
        String out = UrlDownloadPdfUtil.buildPdfLink(host, "A1");

        assertTrue(out.startsWith("http://example.com"), "Host simples deve ser preservado");
        assertTrue(out.endsWith("/download/pdf/A1.pdf"));
    }
}
