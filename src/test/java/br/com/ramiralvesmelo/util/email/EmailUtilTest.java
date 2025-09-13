package br.com.ramiralvesmelo.util.email;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmailUtilTest {

    private static String repeat(char ch, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(ch);
        return sb.toString();
    }

    @Test
    void deveAceitarEmailsValidosAscii() {
        assertTrue(EmailUtil.validateCustomerEmail("a@b.co"));
        assertTrue(EmailUtil.validateCustomerEmail("foo.bar+tag@sub.example.com"));
        assertTrue(EmailUtil.validateCustomerEmail("USER_123-xyz@example.COM"));
        assertTrue(EmailUtil.validateCustomerEmail("john.doe@example.travel"));
        assertTrue(EmailUtil.validateCustomerEmail("a1.b2-c3@example-domain.org"));
    }

    @Test
    void deveRejeitarNullOuVazio() {
        assertFalse(EmailUtil.validateCustomerEmail(null));
        assertFalse(EmailUtil.validateCustomerEmail(""));
        assertFalse(EmailUtil.validateCustomerEmail("   "));
    }

    @Test
    void deveRejeitarProblemasComArroba() {
        assertFalse(EmailUtil.validateCustomerEmail("noatsymbol.com"));
        assertFalse(EmailUtil.validateCustomerEmail("@domain.com"));
        assertFalse(EmailUtil.validateCustomerEmail("local@"));
        assertFalse(EmailUtil.validateCustomerEmail("a@b@c.com"));
    }

    @Test
    void deveRejeitarTamanhosInvalidos() {
        String local = repeat('a', 65);
        assertFalse(EmailUtil.validateCustomerEmail(local + "@example.com"));

        String longLabel = repeat('a', 63);
        String base = longLabel + "." + longLabel + "." + longLabel + "." + longLabel;
        assertTrue(base.length() > 253);
        assertFalse(EmailUtil.validateCustomerEmail("user@" + base));

        assertFalse(EmailUtil.validateCustomerEmail("x@a.b"));
    }

    @Test
    void deveRejeitarPontosConsecutivos() {
        assertFalse(EmailUtil.validateCustomerEmail("a..b@example.com"));
        assertFalse(EmailUtil.validateCustomerEmail("a@exa..mple.com"));
    }

    @Test
    void deveRejeitarLocalPartInvalida() {
        assertFalse(EmailUtil.validateCustomerEmail(".abc@example.com"));
        assertFalse(EmailUtil.validateCustomerEmail("abc.@example.com"));
        assertFalse(EmailUtil.validateCustomerEmail("abç@example.com"));
        assertFalse(EmailUtil.validateCustomerEmail("a!b@example.com"));
    }

    @Test
    void deveRejeitarDominioInvalido() {
        assertFalse(EmailUtil.validateCustomerEmail("user@-example.com"));
        assertFalse(EmailUtil.validateCustomerEmail("user@example-.com"));
        assertFalse(EmailUtil.validateCustomerEmail("user@ex_ample.com"));
        assertFalse(EmailUtil.validateCustomerEmail("user@.example.com"));
        assertFalse(EmailUtil.validateCustomerEmail("user@example..com"));
        assertFalse(EmailUtil.validateCustomerEmail("user@example.123"));
        assertFalse(EmailUtil.validateCustomerEmail("user@example.c"));
        assertFalse(EmailUtil.validateCustomerEmail("user@example"));
        assertFalse(EmailUtil.validateCustomerEmail("user@" + repeat('a', 64) + ".com"));
    }

    @Test
    void deveTestarIdn() {
        assertFalse(EmailUtil.validateCustomerEmail("usuario@müller.de", false));
        assertTrue(EmailUtil.validateCustomerEmail("usuario@müller.de", true));
        assertFalse(EmailUtil.validateCustomerEmail("user@\u0007bad\u0002domain.com", true));
    }

    @Test
    void deveAceitarMaiusculasEBordasValidas() {
        assertTrue(EmailUtil.validateCustomerEmail("A@example.com"));
        assertTrue(EmailUtil.validateCustomerEmail("a@example.COM"));
        assertTrue(EmailUtil.validateCustomerEmail("Z9@example-domain.com"));
    }
    
    @Test
    void deveCobrirCatchIdn_comSurrogateAltoIsolado() {
        // \uD800 = high surrogate isolado -> IDN.toASCII lança IllegalArgumentException
        assertFalse(EmailUtil.validateCustomerEmail("user@\uD800.com", true));
    }

    @Test
    void deveCobrirCatchIdn_comSurrogateBaixoIsolado() {
        // \uDC00 = low surrogate isolado -> também dispara IllegalArgumentException
        assertFalse(EmailUtil.validateCustomerEmail("user@\uDC00.com", true));
    }
    
    @Test
    void deveCobrirAsciiLettersDigits() {
        // 'A' -> letra maiúscula (c >= 'A' && c <= 'Z')
        assertTrue(EmailUtil.validateCustomerEmail("A@example.com"));

        // 'z' -> letra minúscula (c >= 'a' && c <= 'z')
        assertTrue(EmailUtil.validateCustomerEmail("z@example.com"));

        // '0' -> dígito numérico (c >= '0' && c <= '9')
        assertTrue(EmailUtil.validateCustomerEmail("user0@example.com"));

        // caractere inválido '@' no local-part -> cai no else do método
        assertFalse(EmailUtil.validateCustomerEmail("user!@example.com"));
    }
    
    
}
