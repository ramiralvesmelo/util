package br.com.ramiralvesmelo.util.sanitize;

/**
 * Utilitário para sanitizar strings antes de embutir em JavaScript.
 * Garante que não haja quebras ou caracteres perigosos.
 */
public final class JsSanitizer {

    private JsSanitizer() {
        // Classe utilitária → não instanciável
    }

    /**
     * Converte um valor Java em string literal JS segura.
     *
     * @param value Texto a ser sanitizado (pode ser null)
     * @return String entre aspas duplas pronta para uso em JS
     */
    public static String clean(String value) {
        if (value == null) return "null";

        String sanitized = value
                .replace("\\", "\\\\")   // escapa barras
                .replace("\"", "\\\"")   // escapa aspas duplas
                .replace("\n", "\\n")    // normaliza quebra de linha
                .replace("\r", "")       // remove CR
                .replace("\t", "\\t");   // tabulação

        return "\"" + sanitized + "\"";
    }
}