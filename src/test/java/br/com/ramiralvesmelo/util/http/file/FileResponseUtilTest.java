package br.com.ramiralvesmelo.util.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.ramiralvesmelo.util.exception.IntegrationException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;

class FileResponseUtilTest {

    // Helper para simular ServletOutputStream escrevendo em memória
    private static ServletOutputStream streamTo(ByteArrayOutputStream baos) {
        return new ServletOutputStream() {
            @Override public boolean isReady() { return true; }
            @Override public void setWriteListener(WriteListener writeListener) {}
            @Override public void write(int b) throws IOException { baos.write(b); }
        };
    }

    @Test
    @DisplayName("Caminho feliz: escreve bytes, headers e contentType explícito")
    void write_ok_withExplicitContentType() throws Exception {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        when(resp.getOutputStream()).thenReturn(streamTo(baos));

        byte[] data = "conteudo".getBytes(StandardCharsets.UTF_8);

        FileResponseUtil.writeBytesToResponse(resp, data, "relatorio.pdf", "application/pdf");

        // Content-Type e Content-Disposition corretos
        verify(resp).setContentType("application/pdf");
        verify(resp).setHeader(eq("Content-Disposition"),
                argThat(h -> h.startsWith("attachment;")
                        && h.contains("filename=\"relatorio.pdf\"")
                        && h.contains("filename*=UTF-8''relatorio.pdf")));

        // Deve usar sempre setContentLengthLong
        verify(resp).setContentLengthLong(data.length);
        verify(resp, never()).setContentLength(anyInt());

        // Conteúdo escrito
        assertThat(baos.toByteArray()).isEqualTo(data);
    }

    @Test
    @DisplayName("Quando contentType é null, deve usar application/octet-stream")
    void write_ok_defaultContentType() throws Exception {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        when(resp.getOutputStream()).thenReturn(streamTo(baos));

        byte[] data = "abc".getBytes(StandardCharsets.UTF_8);

        FileResponseUtil.writeBytesToResponse(resp, data, "file.txt", null);

        verify(resp).setContentType("application/octet-stream");
        verify(resp).setContentLengthLong(data.length);
        verify(resp, never()).setContentLength(anyInt());
        assertThat(baos.toByteArray()).isEqualTo(data);
    }

    @Test
    @DisplayName("Sanitiza CR/LF e aspas no nome e codifica filename* em UTF-8 (colapsando múltiplos espaços)")
    void write_ok_sanitizesNameAndEncodes() throws Exception {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        when(resp.getOutputStream()).thenReturn(streamTo(baos));

        String rawName = "repor\"t\r\n\n.pdf"; // com CR+LF+LF
        byte[] data = "x".getBytes(StandardCharsets.UTF_8);

        // Reproduz a lógica da classe (com [\r\n]+ → " ")
        String safeName = rawName.replaceAll("[\\r\\n]+", " ").replace("\"", "'");
        String encodedName = java.net.URLEncoder.encode(safeName, StandardCharsets.UTF_8);

        FileResponseUtil.writeBytesToResponse(resp, data, rawName, "text/plain");

        verify(resp).setHeader(eq("Content-Disposition"),
                argThat(h -> h.startsWith("attachment;")
                        && h.contains("filename=\"" + safeName + "\"")
                        && h.contains("filename*=UTF-8''" + encodedName)));
        verify(resp).setContentLengthLong(data.length);
        verify(resp, never()).setContentLength(anyInt());
    }

    @Test
    @DisplayName("Data vazia → 204 NO_CONTENT e não escreve no output")
    void write_emptyData_returns204() throws Exception {
        HttpServletResponse resp = mock(HttpServletResponse.class);

        FileResponseUtil.writeBytesToResponse(resp, new byte[0], "x.pdf", "application/pdf");

        verify(resp).sendError(HttpServletResponse.SC_NO_CONTENT, "Arquivo vazio");
        verify(resp, never()).getOutputStream();
        verify(resp, never()).setContentLengthLong(anyLong());
        verify(resp, never()).setContentType(anyString());
    }

    @Test
    @DisplayName("Nome inválido → 400 BAD_REQUEST e não escreve no output")
    void write_invalidName_returns400() throws Exception {
        HttpServletResponse resp = mock(HttpServletResponse.class);

        FileResponseUtil.writeBytesToResponse(resp, "abc".getBytes(StandardCharsets.UTF_8), "   ", "application/pdf");

        verify(resp).sendError(HttpServletResponse.SC_BAD_REQUEST, "Nome do arquivo inválido");
        verify(resp, never()).getOutputStream();
        verify(resp, never()).setContentLengthLong(anyLong());
        verify(resp, never()).setContentType(anyString());
    }


    @Test
    @DisplayName("Exceção inesperada (ex.: setHeader) → IntegrationException")
    void write_unexpectedException_wrappedAsIntegrationException() throws Exception {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        when(resp.getOutputStream()).thenReturn(streamTo(baos));

        // Força exceção inesperada durante a montagem de header
        doThrow(new IllegalStateException("boom"))
            .when(resp).setHeader(eq("Content-Disposition"), anyString());

        assertThatThrownBy(() ->
                FileResponseUtil.writeBytesToResponse(resp, "abc".getBytes(StandardCharsets.UTF_8), "x.pdf", "application/pdf"))
            .isInstanceOf(IntegrationException.class)
            .hasMessageContaining("Erro inesperado ao enviar arquivo: x.pdf");

        // Como quebrou antes de escrever, não deve ter escrito nada
        assertThat(baos.toByteArray()).isEmpty();
    }
}
