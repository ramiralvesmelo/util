package br.com.ramiralvesmelo.util.http.url;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.hc.core5.net.URIBuilder;

import br.com.ramiralvesmelo.util.core.exception.UrlException;


/**
 * Tratado ReDoS (Regular Expression Denial of Service) por Backtracking 
 * ()
 */

/**
 * Utilitário para construção de URLs seguras e normalizadas.
 *
 * <p>Principais funcionalidades:
 * <ul>
 *   <li><b>buildAbsolute:</b> monta URLs absolutas a partir de uma base (com ou sem path)
 *       e paths adicionais, normalizando barras duplas e preservando esquema/host/query.</li>
 *   <li><b>buildAbsolute (com query params):</b> adiciona parâmetros de query apenas quando
 *       chave e valor não são nulos.</li>
 *   <li><b>buidlUrl / buildUrl:</b> versão legada que mantém barras duplas no path final
 *       e aplica sanitização em identificadores (ex.: número do pedido).</li>
 *   <li><b>buildAbsoluteStrict:</b> versão mais rigorosa que valida entradas
 *       (baseUrl não nulo, esquema apenas http/https, path sem espaços crus ou encoding inválido),
 *       lançando {@link IllegalArgumentException} em casos suspeitos.</li>
 *   <li><b>escape:</b> codificação segura para query string (usa URLEncoder com UTF-8).</li>
 * </ul>
 *
 * <p>Proteções contra ataques:
 * <ul>
 *   <li>Regex simples e não ambíguos para evitar ReDoS. quando uma expressão regular demora MUITO tempo para processar um texto malicioso</li>
 *   <li>Sanitização de identificadores para uso em paths.</li>
 *   <li>Validação rigorosa em {@code buildAbsoluteStrict} para prevenir
 *       URLs malformadas ou manipuladas.</li>
 * </ul>
 *
 * <p>Uso recomendado em todas as partes da aplicação onde seja necessário montar URLs
 * dinamicamente a partir de base + paths/segmentos/query params de forma segura e consistente.
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
            String normPath = normalizePath(joined); // agora sem regex

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
            String normPath = normalizePath(current); // agora sem regex

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
            String normPath = normalizePath(joined); // agora sem regex

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
            URI base = new URI(baseUrl);
            // ⚠️ comportamento legado: concatenar sem normalizar, preservando '//' se houver
            String basePath  = safe(base.getPath());     // preserva a barra final, se existir
            String finalPath = safe(basePath) + safe(path) + sanitize(orderNumber); // sanitize sem regex

            return new URIBuilder(base)
                    .setPath(finalPath) // não chama normalizePath aqui!
                    .build()
                    .toString();
        } catch (URISyntaxException e) {
            throw new br.com.ramiralvesmelo.util.core.exception.UrlException("URL inválida: " + baseUrl, e);
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

        if (aEnds && bStarts)  return a + b.substring(1); // remove barra duplicada
        if (!aEnds && !bStarts) return a + "/" + b;       // inclui barra faltante
        return a + b;                                      // já está adequado
    }

    /** Colapsa sequências de múltiplas barras no PATH (sem tocar esquema/host). */
    private static String normalizePath(String path) {
        if (path == null || path.isEmpty()) return "";
        // Implementação linear (sem regex) para evitar backtracking
        StringBuilder sb = new StringBuilder(path.length());
        boolean prevSlash = false;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '/') {
                if (!prevSlash) {
                    sb.append(c);
                    prevSlash = true;
                }
                // se já era '/', ignora (colapsa)
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
            // Permitidos: letras, números, ponto, sublinhado e hífen
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
                // precisa ter mais 2 chars
                if (i + 2 >= s.length()) return true;
                char h1 = s.charAt(i + 1);
                char h2 = s.charAt(i + 2);
                if (!isHex(h1) || !isHex(h2)) return true;
                i += 2; // pula os dois hex já validados
            }
        }
        return false;
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') ||
               (c >= 'A' && c <= 'F') ||
               (c >= 'a' && c <= 'f');
    }

    /** Versão estrita: valida entradas e lança IllegalArgumentException para casos inválidos. */
    public static String buildAbsoluteStrict(String baseUrl, String path) {
        // validações rápidas que provocam erros reveladores (sem regex)
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl vazio/nulo");
        }
        if (path == null) {
            throw new IllegalArgumentException("path nulo");
        }
        // espaços não escapados e percent-encoding inválido
        if (containsUnescapedSpace(path) || hasInvalidPercentEncoding(path)) {
            throw new IllegalArgumentException("path inválido (espaços não escapados ou percent-encoding inválido)");
        }

        try {
            URI base = new URI(baseUrl);
            String scheme = base.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException("scheme inválido: " + scheme);
            }

            // Monta como a versão normal, mas recusa '//' redundante no PATH final
            String joined   = joinPaths(safe(base.getPath()), path);
            String normPath = normalizePath(joined);
            if (normPath.contains("//")) {
                throw new IllegalArgumentException("path normalizado contém '//' redundante");
            }

            return new URIBuilder(base)
                    .setPath(normPath)
                    .build()
                    .toString();
        } catch (URISyntaxException e) {
            // converte para IAE para ser classificado como erro pelo Randoop (ERROR)
            throw new IllegalArgumentException("URL base inválida: " + baseUrl, e);
        }
    }
}
