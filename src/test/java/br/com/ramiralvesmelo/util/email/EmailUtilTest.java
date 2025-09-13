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

    // ---------- AUXILIARES ----------
    private static String label(int n) {
        return repeat('a', n);
    }

    // Monta domínio com 3 labels de 63 e 1 de 61 (total = 63*3 + 61 + 3 pontos = 253)
    private static String domainLen253() {
        return label(63) + "." + label(63) + "." + label(63) + "." + label(61);
    }

    // ---------- NOVOS TESTES ----------

    @Test
    void deveRejeitarDominioCurtoDemais() {
        // domain.length() = 1
        assertFalse(EmailUtil.validateCustomerEmail("x@a"));
        // domain.length() = 2
        assertFalse(EmailUtil.validateCustomerEmail("x@ab"));
    }

    @Test
    void deveAceitarLimitesExatos() {
        // local exatamente 64
        String local64 = repeat('a', 64);
        assertTrue(EmailUtil.validateCustomerEmail(local64 + "@example.com"));

        // domínio exatamente 253
        String dom253 = domainLen253();
        assertTrue(dom253.length() == 253);
        assertTrue(EmailUtil.validateCustomerEmail("user@" + dom253));
    }

    @Test
    void deveRejeitarLabelDeDominioCom64Chars() {
        // um label com 64 (inválido), ainda que TLD e total estejam ok
        String dom = label(64) + ".com";
        assertFalse(EmailUtil.validateCustomerEmail("user@" + dom));
    }

    @Test
    void deveRejeitarCaracteresNaoAsciiLetterDigitNoDominio() {
        // força caminho onde o char não é [A-Za-z0-9] nem '-'
        assertFalse(EmailUtil.validateCustomerEmail("user@exa`mple.com")); // crase
        assertFalse(EmailUtil.validateCustomerEmail("user@exam[ple.com")); // '['
        assertFalse(EmailUtil.validateCustomerEmail("user@exam]ple.com")); // ']'
        assertFalse(EmailUtil.validateCustomerEmail("user@exam/ple.com")); // '/'
        assertFalse(EmailUtil.validateCustomerEmail("user@exam:ple.com")); // ':'
    }

    @Test
    void deveCobrirIdnPunycodeValidoEEmojiInvalidoNoLocal() {
        // domínio IDN válido via punycode (equivale a müller.de)
        assertTrue(EmailUtil.validateCustomerEmail("usuario@xn--mller-kva.de", true));

        // emoji no local-part (não ASCII permitido pela sua regra)
        assertFalse(EmailUtil.validateCustomerEmail("us\uD83D\uDE00er@example.com"));
    }

    
}
