package br.com.ramiralvesmelo.util.http.file;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import br.com.ramiralvesmelo.util.core.exception.IntegrationException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class FileResponseWriter {

    private FileResponseWriter() {
        // utilitário -> não deve ser instanciado
    }

    /**
     * Escreve um array de bytes no HttpServletResponse para download.
     *
     * @param response     HttpServletResponse onde o arquivo será escrito (não pode ser null)
     * @param data         conteúdo do arquivo em bytes (não pode ser null/vazio)
     * @param downloadName nome sugerido para o arquivo no navegador (não pode ser null/vazio)
     * @param contentType  MIME type (pode ser null, será "application/octet-stream" por padrão)
     */
    public static void writeBytesToResponse(HttpServletResponse response,
                                            byte[] data,
                                            String downloadName,
                                            String contentType) {
        try {
            // ===== Validações =====
            Objects.requireNonNull(response, "HttpServletResponse não pode ser null");

            if (data == null || data.length == 0) {
                log.warn("Tentativa de envio de arquivo vazio: '{}'", downloadName);
                response.sendError(HttpServletResponse.SC_NO_CONTENT, "Arquivo vazio");
                return;
            }

            if (downloadName == null || downloadName.isBlank()) {
                log.error("Nome de download inválido (null ou vazio)");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Nome do arquivo inválido");
                return;
            }

            // Sanitiza nome para evitar CRLF injection (colapsa múltiplos CR/LF em um espaço)
            String safeName = downloadName.replaceAll("[\\r\\n]+", " ").replace("\"", "'");
            String encodedName = URLEncoder.encode(safeName, StandardCharsets.UTF_8);

            // ===== Configuração da resposta =====
            response.setContentType(contentType != null ? contentType : "application/octet-stream");

            // Compatibilidade: filename simples + filename* (UTF-8)
            String cdHeader = String.format("attachment; filename=\"%s\"; filename*=UTF-8''%s",
                                            safeName, encodedName);
            response.setHeader("Content-Disposition", cdHeader);

            // Sempre usa a variante "long" (cobre qualquer tamanho)
            response.setContentLengthLong(data.length);

            // ===== Escrita =====
            try (OutputStream out = response.getOutputStream()) {
                out.write(data);
                out.flush();
            }

            log.info("Arquivo '{}' enviado com sucesso ({} bytes, tipo={})",
                    safeName, data.length, response.getContentType());

        } catch (IOException e) {
            log.error("Erro de I/O ao escrever o arquivo '{}' na resposta HTTP", downloadName, e);
            throw new IntegrationException("Falha ao enviar arquivo: " + downloadName);
        } catch (Exception e) {
            log.error("Erro inesperado ao preparar a resposta para '{}'", downloadName, e);
            throw new IntegrationException("Erro inesperado ao enviar arquivo: " + downloadName);
        }
    }
}
