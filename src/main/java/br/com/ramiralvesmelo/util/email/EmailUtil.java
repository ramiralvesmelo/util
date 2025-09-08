package br.com.ramiralvesmelo.util.email;

import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

/**
 * Classe utilitária para gerar números de pedido baseados em ULID.
 */
@Slf4j
public final class EmailUtil {

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^(?!.*\\.\\.)[A-Za-z0-9](?:[A-Za-z0-9._%+-]{0,63}[A-Za-z0-9])?@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,}$");

	
	private EmailUtil() {
	}

	public static boolean validateCustomerEmail(String email) {
		log.debug("Validando e-mail: {}", email);
		try {
			if (email == null || email.isEmpty()) {
				log.debug("E-mail: {} incorreto!", email);
				return false;
			}
			log.debug("E-mail: {} valido!", email);
			return EMAIL_PATTERN.matcher(email).matches();
		} catch (Exception e) {
			log.error("Erro ao validar e-mail: {}", email, e);
			throw e;
		}
	}
}
