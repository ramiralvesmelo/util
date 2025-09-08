package br.com.ramiralvesmelo.util.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class EmailUtilIDNTest {

	// helpers
	private static String repeat(char c, int times) {
		return String.valueOf(c).repeat(times);
	}

	private static String dotJoin(String... parts) {
		return String.join(".", parts);
	}

	// ----------------- VÁLIDOS (ASCII) -----------------
	static Stream<String> validAsciiEmails() {
		return Stream.of("a@a.br", "john.doe@example.com", "user+tag@sub.example.co", "name_surname-1@a1.b2.cde", // <-
																													// antes
																													// era
																													// ...c3
				repeat('a', 64) + "@example.com",
				"x@" + dotJoin("a".repeat(63), "b".repeat(63), "c".repeat(61), "com"));
	}

	@ParameterizedTest
	@MethodSource("validAsciiEmails")
	@DisplayName("Válidos (ASCII)")
	void should_accept_valid_ascii(String email) {
		assertTrue(EmailUtil.validateCustomerEmail(email));
	}

	// ----------------- INVÁLIDOS BÁSICOS -----------------
	@ParameterizedTest
	@CsvSource({ ", false", // null
			"'   ', false", // vazio
			"'no-at-domain', false", // sem '@'
			"'@domain.com', false", // sem local
			"'user@', false", // sem domínio
			"'a@@b.com', false", // 2 @
			"'a..b@c.com', false", // pontos duplos no local
			"'ab@c..com', false", // pontos duplos no domínio
			"'.abc@x.com', false", // local começa com inválido
			"'abc.@x.com', false", // local termina com inválido
			"'a@b', false", // domínio com 1 label
			"'a@-abc.com', false", // label começa com '-'
			"'a@abc-.com', false", // label termina com '-'
			"'a@abc.c', false", // TLD 1 char
			"'a@abc.123', false", // TLD numérica
			"'a@' + 'a'*254 + '.com', false" // > 253 (não dá pra montar aqui)
	})
	@DisplayName("Inválidos básicos")
	void should_reject_basic_invalids(String email, boolean expected) {
		assertEquals(expected, EmailUtil.validateCustomerEmail(email));
	}

	@Test
	@DisplayName("Local > 64 deve rejeitar")
	void local_too_long() {
		String local = repeat('x', 65);
		String email = local + "@example.com";
		assertFalse(EmailUtil.validateCustomerEmail(email));
	}

	@Test
	@DisplayName("Label > 63 deve rejeitar")
	void label_too_long() {
		String tooLong = repeat('a', 64);
		String email = "x@" + tooLong + ".com";
		assertFalse(EmailUtil.validateCustomerEmail(email));
	}

}
