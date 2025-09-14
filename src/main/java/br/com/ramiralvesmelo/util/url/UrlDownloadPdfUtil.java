package br.com.ramiralvesmelo.util.url;

import static java.util.Objects.requireNonNull;

import java.net.URISyntaxException;
import java.util.List;

import org.apache.hc.core5.net.URIBuilder;

import lombok.extern.log4j.Log4j2;

@Log4j2
public final class UrlDownloadPdfUtil {
 
    private UrlDownloadPdfUtil() {}

    /**
     * Monta a URL de download de PDF: {host}/download/pdf/{fileName.pdf}
     * - Preserva o path base de {@code appEventHost}
     * - Sanitiza o {@code fileName}
     * - Garante extensão .pdf
     */
    public static String buildPdfLink(String appEventHost, String fileName) {
        requireNonNull(appEventHost, "appEventHost não pode ser null");
        requireNonNull(fileName, "fileName não pode ser null");

        String hostInput = appEventHost.trim();
        String fileInput = fileName.trim();

        if (hostInput.isEmpty()) {
            throw new IllegalArgumentException("Parâmetro appEventHost é obrigatório e não pode ser vazio.");
        }
        if (fileInput.isEmpty()) {
            throw new IllegalArgumentException("Parâmetro fileName é obrigatório e não pode ser vazio.");
        }

        // 1) Sanitiza host (sem remover barras finais ainda)
        String host = UrlSanitizer.sanitize(hostInput);

        // 2) Remove barras finais do PATH (evita //download), antes da validação
        host = removeTrailingSlashFromPath(host);

        // 3) Agora sim valida http/https
        if (!UrlSanitizer.isValid(host)) {
            throw new IllegalArgumentException("URL base inválida: " + hostInput);
        }

        // Sanitiza o nome e garante .pdf (case-insensitive)
        String safe = UrlSanitizer.sanitize(fileInput);
        if (!safe.toLowerCase().endsWith(".pdf")) {
            safe = safe + ".pdf";
        }

        // Constrói preservando path base do host
        String url = UrlSanitizer.buildUrl(host, List.of("download", "pdf", safe), null);
        log.debug("URL de download gerada: {}", url);
        return url;
    }

    /**
     * Remove apenas as barras finais do PATH da URL (não altera esquema/host/porta/query/fragment).
     * Ex.: http://srv/base/ -> http://srv/base
     *      http://srv/ -> http://srv
     */
    private static String removeTrailingSlashFromPath(String url) {
        try {
            URIBuilder b = new URIBuilder(url);
            String path = b.getPath();
            if (path != null && !path.isEmpty()) {
                int end = path.length();
                while (end > 0 && path.charAt(end - 1) == '/') {
                    end--;
                }
                String cleaned = (end == path.length()) ? path : path.substring(0, end);
                b.setPath(cleaned);
            }
            return b.build().toString();
        } catch (URISyntaxException e) {
            // URL já foi sanitizada; em caso extremo, devolve original
            return url;
        }
    }
}
