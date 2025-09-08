package br.com.ramiralvesmelo.util.email;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("IDN (domínios internacionalizados)")
public class EmailUtilTest {
    @Test
    @DisplayName("Aceita IDN quando allowIdn=true (bücher.de)")
    void idn_allowed() {
        assertTrue(EmailUtil.validateCustomerEmail("user@bücher.de", true));
    }

    @Test
    @DisplayName("Rejeita IDN quando allowIdn=false")
    void idn_not_allowed() {
        assertFalse(EmailUtil.validateCustomerEmail("user@bücher.de", false));
    }

    @Test
    @DisplayName("IDN com label inválida deve falhar (pontos duplos)")
    void idn_invalid_label() {
        assertFalse(EmailUtil.validateCustomerEmail("user@ex..emplo.com", true));
    }
}
