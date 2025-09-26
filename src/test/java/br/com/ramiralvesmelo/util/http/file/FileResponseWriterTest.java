package br.com.ramiralvesmelo.util.http.file;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;

import org.junit.jupiter.api.Test;

import br.com.ramiralvesmelo.util.core.exception.IntegrationException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;

class FileResponseWriterTest {

    // Implementação simples de ServletOutputStream que grava em memória
    static class TestServletOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();

        @Override
        public boolean isReady() { return true; }

        @Override
        public void setWriteListener(WriteListener writeListener) { /* no-op */ }

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
        }

        byte[] toByteArray() {
            return delegate.toByteArray();
        }
    }

    @Test
    void construtorPrivado_deveSerAcessivelPorReflexao() throws Exception {
        Constructor<FileResponseWriter> ctor = FileResponseWriter.class.getDeclaredConstructor();
        assertFalse(ctor.canAccess(null)); // é private
        ctor.setAccessible(true);
        Object instance = ctor.newInstance();
        assertNotNull(instance);
    }

    @Test
    void deveLancarIntegrationExceptionQuandoResponseNull() {
        // O método captura NPE e relança IntegrationException
        assertThrows(IntegrationException.class,
                () -> FileResponseWriter.writeBytesToResponse(null, new byte[]{1}, "file.txt", "text/plain"));
    }

    @Test
    void deveRetornarNoContentQuandoArquivoVazio() throws Exception {
        HttpServletResponse resp = mock(HttpServletResponse.class);

        FileResponseWriter.writeBytesToResponse(resp, new byte[0], "file.txt", "text/plain");

        verify(resp).sendError(HttpServletResponse.SC_NO_CONTENT, "Arquivo vazio");
        verifyNoMoreInteractions(resp);
    }

    @Test
    void deveRetornarBadRequestQuandoNomeInvalido() throws Exception {
        HttpServletResponse resp = mock(HttpServletResponse.class);

        FileResponseWriter.writeBytesToResponse(resp, "abc".getBytes(), "  ", "text/plain");

        verify(resp).sendError(HttpServletResponse.SC_BAD_REQUEST, "Nome do arquivo inválido");
    }

    @Test
    void deveEscreverArquivoComSucesso() throws Exception {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        TestServletOutputStream out = new TestServletOutputStream();

        when(resp.getOutputStream()).thenReturn(out);

        byte[] data = "ola".getBytes();
        FileResponseWriter.writeBytesToResponse(resp, data, "teste.txt", "text/plain");

        verify(resp).setContentType("text/plain");
        verify(resp).setHeader(eq("Content-Disposition"), contains("teste.txt"));
        verify(resp).setHeader(eq("Content-Disposition"), contains("filename*=")); // variante UTF-8 presente
        verify(resp).setContentLengthLong(data.length);

        // confere bytes foram escritos
        assertArrayEquals(data, out.toByteArray());
    }

    @Test
    void deveUsarContentTypePadraoQuandoNulo() throws Exception {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        TestServletOutputStream out = new TestServletOutputStream();

        when(resp.getOutputStream()).thenReturn(out);

        byte[] data = "xyz".getBytes();
        FileResponseWriter.writeBytesToResponse(resp, data, "arquivo.txt", null);

        verify(resp).setContentType("application/octet-stream");
        verify(resp).setHeader(eq("Content-Disposition"), contains("arquivo.txt"));
        assertArrayEquals(data, out.toByteArray());
    }

    @Test
    void deveLancarIntegrationExceptionQuandoIOException() throws Exception {
        HttpServletResponse resp = mock(HttpServletResponse.class);

        when(resp.getOutputStream()).thenThrow(new IOException("falha"));

        assertThrows(IntegrationException.class, () ->
                FileResponseWriter.writeBytesToResponse(resp, "abc".getBytes(), "f.txt", "text/plain"));
    }

    @Test
    void deveLancarIntegrationExceptionQuandoErroGenerico() throws Exception {
        HttpServletResponse resp = mock(HttpServletResponse.class);

        when(resp.getOutputStream()).thenThrow(new RuntimeException("boom"));

        assertThrows(IntegrationException.class, () ->
                FileResponseWriter.writeBytesToResponse(resp, "abc".getBytes(), "f.txt", "text/plain"));
    }
}
