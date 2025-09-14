package br.com.ramiralvesmelo.util.url;

import static java.util.Objects.requireNonNull;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.hc.core5.net.URIBuilder;

public final class UrlSanitizer {

    private static final UrlValidator URL_VALIDATOR =
            new UrlValidator(new String[]{"http", "https"}, UrlValidator.ALLOW_LOCAL_URLS);

    private UrlSanitizer() {}

    /** Valida rapidamente a URL (http/https; localhost permitido). */
    public static boolean isValid(String url) {
        return url != null && URL_VALIDATOR.isValid(url);
    }

    /**
     * Sanitiza string removendo CR/LF, aspas e espaços extras
     * e normaliza caso seja uma URL completa.
     */
    public static String sanitize(String value) {
        requireNonNull(value, "valor não pode ser null");
        String cleaned = value.replace("\r", " ")
                              .replace("\n", " ")
                              .replace("\"", "'")
                              .trim();
        // se for uma URL válida, normaliza via URIBuilder
        if (isValid(cleaned)) {
            try {
                return new URIBuilder(cleaned).build().toString();
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("URL inválida: " + cleaned, e);
            }
        }
        return cleaned;
    }

    /**
     * Monta URL: base + pathSegments + queryParams.
     * - Preserva o path existente da base.
     * - Path e query são sanitizados.
     */
    public static String buildUrl(String baseUrl,
                                  List<String> pathSegments,
                                  Map<String, String> queryParams) {
        requireNonNull(baseUrl, "baseUrl não pode ser null");
        try {
            URIBuilder b = new URIBuilder(sanitize(baseUrl));

            if (pathSegments != null && !pathSegments.isEmpty()) {
                List<String> merged = new ArrayList<>(b.getPathSegments());
                for (String seg : pathSegments) {
                    merged.add(sanitize(seg));
                }
                b.setPathSegments(merged);
            }

            if (queryParams != null && !queryParams.isEmpty()) {
                for (Map.Entry<String, String> e : queryParams.entrySet()) {
                    b.addParameter(sanitize(e.getKey()), sanitize(e.getValue()));
                }
            }

            return b.build().toString();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("URL base inválida: " + baseUrl, ex);
        }
    }
}
