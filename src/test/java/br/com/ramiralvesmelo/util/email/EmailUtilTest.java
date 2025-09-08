package br.com.ramiralvesmelo.util.email;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmailUtilTest {

    @Test
    @DisplayName("Deve validar e-mails válidos")
    void shouldValidateValidEmails() {
        String[] valid = {
            "user@example.com",
            "user.name+tag@sub.example.co",
            "USER_123@test.io",
            "a-b.c_d@dominio.com.br"
        };
        for (String e : valid) {
            assertTrue(EmailUtil.validateCustomerEmail(e), "Esperava válido: " + e);
        }
    }

    @Test
    @DisplayName("Deve invalidar e-mails nulos, vazios ou malformados")
    void shouldInvalidateInvalidEmails() {
        String[] invalid = {
            null,
            "",
            "   ",
            "sem-arroba.com",
            "user@",
            "@dominio.com",
            "user@dominio",
            "user@dominio..com",
            "user@@dominio.com",
            "user@domínio.com" // acento não permitido
        };
        for (String e : invalid) {
            assertFalse(EmailUtil.validateCustomerEmail(e), "Esperava inválido: " + e);
        }
    }
}
