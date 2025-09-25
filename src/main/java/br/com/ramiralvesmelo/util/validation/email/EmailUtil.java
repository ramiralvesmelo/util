package br.com.ramiralvesmelo.util.email;

import java.net.IDN;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class EmailUtil {

    private static final Pattern LOCAL_ALLOWED = Pattern.compile("^[A-Za-z0-9._%+-]+$");
    private static final Pattern LABEL_PATTERN = Pattern.compile("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$");
    private static final Pattern TLD_PATTERN   = Pattern.compile("^[A-Za-z]{2,}$");

    private EmailUtil() {}

    public static boolean validateCustomerEmail(String email) {
        return validateCustomerEmail(email, false);
    }

    public static boolean validateCustomerEmail(String email, boolean allowIdn) {
        log.debug("Validando e-mail: {}", email);

        if (email == null) {
            log.warn("E-mail nulo não é válido.");
            return false;
        }

        email = email.trim();
        if (email.isEmpty()) {
            log.warn("E-mail vazio não é válido.");
            return false;
        }

        final int at = email.indexOf('@');
        if (at <= 0 || at != email.lastIndexOf('@') || at == email.length() - 1) {
            log.warn("E-mail inválido, problema com '@': {}", email);
            return false;
        }

        final String local = email.substring(0, at);
        String domain = email.substring(at + 1);

        if (local.length() > 64 || domain.length() < 3 || domain.length() > 253) {
            log.warn("E-mail inválido, tamanho incorreto: {}", email);
            return false;
        }

        if (local.contains("..") || domain.contains("..")) {
            log.warn("E-mail inválido, contém pontos consecutivos: {}", email);
            return false;
        }

        if (!LOCAL_ALLOWED.matcher(local).matches()) {
            log.warn("Local-part inválida: {}", local);
            return false;
        }

        if (!isAsciiLetterOrDigit(local.charAt(0)) || !isAsciiLetterOrDigit(local.charAt(local.length() - 1))) {
            log.warn("Local-part começa ou termina com caractere inválido: {}", local);
            return false;
        }

        if (allowIdn) {
            try {
                domain = IDN.toASCII(domain);
            } catch (IllegalArgumentException ex) {
                log.trace("Erro ao converter domínio IDN: {}", domain, ex);
                return false;
            }
        }

        final String[] labels = domain.split("\\.");
        if (labels.length < 2) {
            log.warn("Domínio inválido (sem labels suficientes): {}", domain);
            return false;
        }

        for (int i = 0; i < labels.length; i++) {
            final String lbl = labels[i];
            if (lbl.isEmpty() || lbl.length() > 63) {
                log.warn("Label inválida: {}", lbl);
                return false;
            }
            if (!LABEL_PATTERN.matcher(lbl).matches()) {
                log.warn("Label contém caracteres inválidos: {}", lbl);
                return false;
            }
            if (i == labels.length - 1 && !TLD_PATTERN.matcher(lbl).matches()) {
                log.warn("TLD inválido: {}", lbl);
                return false;
            }
        }

        log.debug("E-mail válido: {}", email);
        return true;
    }

    protected static boolean isAsciiLetterOrDigit(char c) {
        return (c >= 'A' && c <= 'Z') ||
               (c >= 'a' && c <= 'z') ||
               (c >= '0' && c <= '9');
    }
    
  
}