package br.com.ramiralvesmelo.util.url;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hc.core5.net.URIBuilder;

import br.com.ramiralvesmelo.util.exception.IntegrationException;

public final class UrlUtil {

    private UrlUtil() {
        // utilitário -> não instanciar
    }

    /**
     * Monta uma URL absoluta para download de PDF.
     *
     * @param baseUrl Ex.: http://localhost:8084/app-event
     * @param orderNumber Ex.: ORD-0001
     * @return URI normalizada
     */
    public static String buidlUrl(String baseUrl, String path, String orderNumber) {
        try {
            String pathBase = new URI(baseUrl).getPath();
            URI uri = new URIBuilder(baseUrl)
			                    .setPath(pathBase + path+ sanitize(orderNumber))
			                    .build();
            return uri.toString();
        } catch (URISyntaxException e) {
            throw new IntegrationException("URL inválida: " + baseUrl);
        }
    }

    /**
     * Sanitiza identificadores para evitar caracteres inválidos na URL.
     */
    private static String sanitize(String value) {
        if (value == null) return "";
        return value.replaceAll("[^a-zA-Z0-9._-]", "_"); // somente seguro
    }
}
