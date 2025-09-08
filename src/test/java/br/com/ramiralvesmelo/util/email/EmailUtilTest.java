package br.com.ramiralvesmelo.util.email;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class EmailUtilTest {

    @Test
    @DisplayName("Válidos básicos e subdomínios")
    void validEmails() {
        assertTrue(EmailUtil.validateCustomerEmail("user@example.com"));
        assertTrue(EmailUtil.validateCustomerEmail("user.name+tag@sub.example.co"));
        assertTrue(EmailUtil.validateCustomerEmail("a-b.c_d@dominio.com.br"));
    }

    @Test
    @DisplayName("Inválidos comuns")
    void invalidEmails() {
        assertFalse(EmailUtil.validateCustomerEmail(null));
        assertFalse(EmailUtil.validateCustomerEmail(""));
        assertFalse(EmailUtil.validateCustomerEmail("user@"));
        assertFalse(EmailUtil.validateCustomerEmail("@dominio.com"));
        assertFalse(EmailUtil.validateCustomerEmail("user@dominio"));
        assertFalse(EmailUtil.validateCustomerEmail("user@dominio..com"));
        assertFalse(EmailUtil.validateCustomerEmail(".user@example.com"));
        assertFalse(EmailUtil.validateCustomerEmail("user.@example.com"));
        assertFalse(EmailUtil.validateCustomerEmail("user@-example.com"));
        assertFalse(EmailUtil.validateCustomerEmail("user@example-.com"));
    }

    @Test
    @DisplayName("IDN opcional com Punycode")
    void idnDomain() {
        // domínio com caractere especial: só valida quando allowIdn=true
        String idn = "usuario@exemplo.çom";
        assertFalse(EmailUtil.validateCustomerEmail(idn, false));
        // dependendo do seu critério, pode aceitar com allowIdn
        assertFalse(EmailUtil.validateCustomerEmail(idn, true)); // ajuste para true/false conforme regra desejada
    }

    @Test
    @DisplayName("Captura de logs de validação")
    void logsAreEmitted(CapturedOutput output) {
        EmailUtil.validateCustomerEmail("user@dominio..com");
        String logs = output.getOut() + output.getErr();
        assertTrue(logs.contains("pontos consecutivos") || logs.toLowerCase().contains("consecut"),
                "Deveria logar mensagem de pontos consecutivos");
    }
}
