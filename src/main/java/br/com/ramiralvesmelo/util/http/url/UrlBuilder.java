package br.com.ramiralvesmelo.util.http.url;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.hc.core5.net.URIBuilder;

import br.com.ramiralvesmelo.util.core.exception.UrlException;

/**
 * Utilitário para construção de URLs seguras e normalizadas.
 *
 * Principais funcionalidades:
 * - buildAbsolute: monta URLs absolutas a partir de uma base (com ou sem path) e paths adicionais,
 *   normalizando barras duplas e preservando esquema/host/query.
 * - buildAbsolute (com query params): adiciona parâmetros de query apenas quando chave e valor não são nulos.
 * - buidlUrl / buildUrl: versão legada que mantém barras duplas no path final e aplica sanitização.
 * - buildAbsoluteStrict: versão rigorosa que valida entradas (http/https, espaços crus, percent-encoding),
 *   lançando IllegalArgumentException em casos suspeitos.
 * - escape: codificação segura para query string (URLEncoder UTF-8).
 */
public final class UrlBuilder {

    private UrlBuilder() {}

    // =========================================================
    // Builders principais
    // =========================================================

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
     * Constrói: basePath + path + sanitize(orderNumber) preservando comportamento legado
     * (não normaliza o path; mantém '//' se houver).
     */
    public static String buidlUrl(String baseUrl, String path, String orderNumber) {
        try {
            URI base = new URI(baseUrl);
            String basePath  = safe(base.getPath());
            String finalPath = safe(basePath) + safe(path) + sanitize(orderNumber);

            return new URIBuilder(base)
                    .setPath(finalPath) // comportamento legado: sem normalizePath
                    .build()
                    .toString();
        } catch (URISyntaxException e) {
            throw new UrlException("URL inválida: " + baseUrl, e);
        }
    }

    /** Alias sem typo (mantém compatibilidade externa). */
    public static String buildUrl(String baseUrl, String path, String orderNumber) {
        return buidlUrl(baseUrl, path, orderNumber);
    }

    // =========================================================
    // Helpers
    // =========================================================

    /** Concatena paths tratando barras à esquerda/direita. */
    private static String joinPaths(String left, String right) {
        String a = safe(left);
        String b = safe(right);
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;

        boolean aEnds   = a.endsWith("/");
        boolean bStarts = b.startsWith("/");

        if (aEnds && bStarts)  return a + b.substring(1); // remove barra duplicada na junção
        if (!aEnds && !bStarts) return a + "/" + b;       // inclui barra faltante
        return a + b;                                     // já está adequado
    }

    /** Colapsa sequências de múltiplas barras no PATH (sem tocar esquema/host). */
    private static String normalizePath(String path) {
        if (path == null || path.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(path.length());
        boolean prevSlash = false;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '/') {
                if (!prevSlash) {
                    sb.append(c);
                    prevSlash = true;
                }
                // múltiplas barras são colapsadas
            } else {
                sb.append(c);
                prevSlash = false;
            }
        }
        return sb.toString();
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    /** Sanitiza identificadores para uso como parte do path (sem regex). */
    private static String sanitize(String value) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'A' && c <= 'Z') ||
                (c >= 'a' && c <= 'z') ||
                (c >= '0' && c <= '9') ||
                c == '.' || c == '_' || c == '-') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    /** Escapa para uso em query/form (URLEncoder usa '+' para espaços). */
    public static String escape(String s) {
        if (s == null) return null;
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** Verifica se há espaço não escapado. (Linear, sem regex) */
    private static boolean containsUnescapedSpace(String s) {
        if (s == null) return false;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ' ') return true;
        }
        return false;
    }

    /** Verifica percent-encoding inválido no path (Linear, sem regex). */
    private static boolean hasInvalidPercentEncoding(String s) {
        if (s == null) return false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '%') {
                if (i + 2 >= s.length()) return true;
                char h1 = s.charAt(i + 1);
                char h2 = s.charAt(i + 2);
                if (!isHex(h1) || !isHex(h2)) return true;
                i += 2;
            }
        }
        return false;
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') ||
               (c >= 'A' && c <= 'F') ||
               (c >= 'a' && c <= 'f');
    }

    /** NOVO: verifica se há '//' redundante em qualquer posição do path (linear, sem regex). */
    private static boolean hasDoubleSlash(String s) {
        if (s == null || s.length() < 2) return false;
        char prev = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '/' && prev == '/') return true;
            prev = c;
        }
        return false;
    }

    /** Versão estrita: valida entradas e lança IllegalArgumentException para casos inválidos. */
    public static String buildAbsoluteStrict(String baseUrl, String path) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl vazio/nulo");
        }
        if (path == null) {
            throw new IllegalArgumentException("path nulo");
        }
        if (containsUnescapedSpace(path) || hasInvalidPercentEncoding(path)) {
            throw new IllegalArgumentException("path inválido (espaços não escapados ou percent-encoding inválido)");
        }

        try {
            URI base = new URI(baseUrl);
            String scheme = base.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException("scheme inválido: " + scheme);
            }

            // >>> Mudança: validar '//' antes de normalizar
            String joined = joinPaths(safe(base.getPath()), path);
            if (hasDoubleSlash(joined)) {
                throw new IllegalArgumentException("path contém '//' redundante");
            }

            String normPath = normalizePath(joined);
            return new URIBuilder(base)
                    .setPath(normPath)
                    .build()
                    .toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URL base inválida: " + baseUrl, e);
        }
    }
}
