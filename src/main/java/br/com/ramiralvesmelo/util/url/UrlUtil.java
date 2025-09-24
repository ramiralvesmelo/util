package br.com.ramiralvesmelo.util.url;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.hc.core5.net.URIBuilder;

import br.com.ramiralvesmelo.util.exception.UrlException;

public final class UrlUtil {

    private UrlUtil() {}

    /** Monta URL absoluta juntando a base (que pode conter path) com um path relativo. */
    public static String buildAbsolute(String baseUrl, String path) {
        try {
            URI base = new URI(baseUrl);
            String basePath = safe(base.getPath());
            String joined   = joinPaths(basePath, path);
            String normPath = normalizePath(joined);

            return new URIBuilder(base)
                    .setPath(normPath)
                    .build()
                    .toString();
        } catch (URISyntaxException e) {
            throw new UrlException("URL inválida: " + baseUrl, e);
        }
    }

    /** Monta URL absoluta juntando múltiplos segmentos de path. */
    public static String buildAbsolute(String baseUrl, String... segments) {
        try {
            URI base = new URI(baseUrl);
            String current = safe(base.getPath());
            for (String seg : segments) {
                current = joinPaths(current, seg);
            }
            String normPath = normalizePath(current);

            return new URIBuilder(base)
                    .setPath(normPath)
                    .build()
                    .toString();
        } catch (URISyntaxException e) {
            throw new UrlException("URL inválida: " + baseUrl, e);
        }
    }

    /** Monta URL absoluta e adiciona query params. */
    public static String buildAbsolute(String baseUrl, String path, Map<String, ?> queryParams) {
        try {
            URI base = new URI(baseUrl);
            String basePath = safe(base.getPath());
            String joined   = joinPaths(basePath, path);
            String normPath = normalizePath(joined);

            URIBuilder ub = new URIBuilder(base).setPath(normPath);
            if (queryParams != null) {
                for (Map.Entry<String, ?> e : queryParams.entrySet()) {
                    if (e.getKey() != null && e.getValue() != null) {
                        ub.addParameter(e.getKey(), String.valueOf(e.getValue()));
                    }
                }
            }
            return ub.build().toString();
        } catch (URISyntaxException e) {
            throw new UrlException("URL inválida: " + baseUrl, e);
        }
    }

    /**
     * Constrói: basePath + path + sanitize(orderNumber) normalizando as barras.
     * (mantido por compatibilidade com chamadas existentes)
     */
    public static String buidlUrl(String baseUrl, String path, String orderNumber) {
        try {
            String basePath = new URI(baseUrl).getPath(); // preserva a barra final se houver
            // concatena sem normalizar para manter comportamento legado (pode resultar em "//")
            String finalPath = safe(basePath) + safe(path) + sanitize(orderNumber);
            return new org.apache.hc.core5.net.URIBuilder(baseUrl)
                    .setPath(finalPath)
                    .build()
                    .toString();
        } catch (URISyntaxException e) {
            // teste espera UrlException
            throw new br.com.ramiralvesmelo.util.exception.UrlException("URL inválida: " + baseUrl, e);
        }
    }

    /** Alias sem typo (opcional). */
    public static String buildUrl(String baseUrl, String path, String orderNumber) {
        return buidlUrl(baseUrl, path, orderNumber);
    }

    // ----------------- helpers -----------------

    private static String joinPaths(String left, String right) {
        String a = safe(left);
        String b = safe(right);
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;
        boolean aEnds = a.endsWith("/");
        boolean bStarts = b.startsWith("/");
        if (aEnds && bStarts)  return a + b.substring(1); // remove barra duplicada na junção
        if (!aEnds && !bStarts) return a + "/" + b;       // adiciona barra quando falta
        return a + b;
    }

    /** Colapsa sequências de múltiplas barras no PATH (sem tocar o esquema). */
    private static String normalizePath(String path) {
        if (path == null || path.isEmpty()) return "";
        // substitui "////" -> "/" apenas no path (URIBuilder cuidará do restante)
        return path.replaceAll("/{2,}", "/");
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /** Sanitiza identificadores para evitar caracteres inválidos na URL. */
    private static String sanitize(String value) {
        if (value == null) return "";
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
