package br.com.ramiralvesmelo.util.email;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EmailUtil.validateCustomerEmail")
class EmailUtilTest {

    // ===========================
    // Casos de sucesso (válidos)
    // ===========================
    @Nested
    @DisplayName("Válidos")
    class Validos {

        @Test
        @DisplayName("ASCII comuns e variações de case")
        void deveAceitarEmailsValidosAscii() {
            assertTrue(EmailUtil.validateCustomerEmail("a@b.co"));
            assertTrue(EmailUtil.validateCustomerEmail("foo.bar+tag@sub.example.com"));
            assertTrue(EmailUtil.validateCustomerEmail("USER_123-xyz@example.COM"));
            assertTrue(EmailUtil.validateCustomerEmail("john.doe@example.travel"));
            assertTrue(EmailUtil.validateCustomerEmail("a1.b2-c3@example-domain.org"));
        }

        @Test
        @DisplayName("Bordas válidas: maiúsculas, dígitos, hífen no domínio")
        void deveAceitarMaiusculasEBordasValidas() {
            assertTrue(EmailUtil.validateCustomerEmail("A@example.com"));
            assertTrue(EmailUtil.validateCustomerEmail("a@example.COM"));
            assertTrue(EmailUtil.validateCustomerEmail("Z9@example-domain.com"));
            assertTrue(EmailUtil.validateCustomerEmail("user0@example.com"));
        }

        @Test
        @DisplayName("Limites exatos aceitos (local=64; dominio=253)")
        void deveAceitarLimitesExatos() {
            String local64 = repeat('a', 64);
            assertTrue(EmailUtil.validateCustomerEmail(local64 + "@example.com"));

            String dom253 = domainLen253();
            assertTrue(dom253.length() == 253);
            assertTrue(EmailUtil.validateCustomerEmail("user@" + dom253));
        }

        @Test
        @DisplayName("IDN válido via punycode (equivale a müller.de)")
        void deveAceitarIdnPunycodeValido() {
            assertTrue(EmailUtil.validateCustomerEmail("usuario@xn--mller-kva.de", true));
        }
    }

    // ===========================
    // Entradas vazias / formato
    // ===========================
    @Nested
    @DisplayName("Nulos e formato básico")
    class NulosEFormato {

        @Test
        @DisplayName("Null, vazio e whitespace")
        void deveRejeitarNullOuVazio() {
            assertFalse(EmailUtil.validateCustomerEmail(null));
            assertFalse(EmailUtil.validateCustomerEmail(""));
            assertFalse(EmailUtil.validateCustomerEmail("   "));
        }

        @Test
        @DisplayName("Problemas com @")
        void deveRejeitarProblemasComArroba() {
            assertFalse(EmailUtil.validateCustomerEmail("noatsymbol.com"));
            assertFalse(EmailUtil.validateCustomerEmail("@domain.com"));
            assertFalse(EmailUtil.validateCustomerEmail("local@"));
            assertFalse(EmailUtil.validateCustomerEmail("a@b@c.com"));
        }

        @Test
        @DisplayName("Pontos consecutivos no local-part e no domínio")
        void deveRejeitarPontosConsecutivos() {
            assertFalse(EmailUtil.validateCustomerEmail("a..b@example.com"));
            assertFalse(EmailUtil.validateCustomerEmail("a@exa..mple.com"));
        }
    }

    // ===========================
    // Local-part inválida
    // ===========================
    @Nested
    @DisplayName("Local-part inválida")
    class LocalInvalida {

        @Test
        @DisplayName("Ponto nas bordas / caractere não permitido / símbolo")
        void deveRejeitarLocalPartInvalida() {
            assertFalse(EmailUtil.validateCustomerEmail(".abc@example.com"));
            assertFalse(EmailUtil.validateCustomerEmail("abc.@example.com"));
            assertFalse(EmailUtil.validateCustomerEmail("abç@example.com"));
            assertFalse(EmailUtil.validateCustomerEmail("a!b@example.com"));
        }

        @Test
        @DisplayName("Emoji no local-part (não ASCII)") 
        void deveRejeitarEmojiNoLocal() {
            assertFalse(EmailUtil.validateCustomerEmail("us\uD83D\uDE00er@example.com"));
        }

        @Test
        @DisplayName("Caracter inválido no local-part que aciona caminho 'else'")
        void deveCobrirAsciiLettersDigits_noLocalPartComSimbolo() {
            assertFalse(EmailUtil.validateCustomerEmail("user!@example.com"));
        }
    }

    // ===========================
    // Domínio inválido / limites
    // ===========================
    @Nested
    @DisplayName("Domínio inválido e limites")
    class DominioInvalidoELimites {

        @Test
        @DisplayName("Tamanhos inválidos (local>64, dominio>253, TLD curto)")
        void deveRejeitarTamanhosInvalidos() {
            String local65 = repeat('a', 65);
            assertFalse(EmailUtil.validateCustomerEmail(local65 + "@example.com"));

            String longLabel = repeat('a', 63);
            String base = longLabel + "." + longLabel + "." + longLabel + "." + longLabel;
            assertTrue(base.length() > 253);
            assertFalse(EmailUtil.validateCustomerEmail("user@" + base));

            assertFalse(EmailUtil.validateCustomerEmail("x@a.b")); // TLD 1 char
        }

        @Test
        @DisplayName("Regras de rótulos e TLD numérico")
        void deveRejeitarDominioInvalido() {
            assertFalse(EmailUtil.validateCustomerEmail("user@-example.com"));
            assertFalse(EmailUtil.validateCustomerEmail("user@example-.com"));
            assertFalse(EmailUtil.validateCustomerEmail("user@ex_ample.com"));
            assertFalse(EmailUtil.validateCustomerEmail("user@.example.com"));
            assertFalse(EmailUtil.validateCustomerEmail("user@example..com"));
            assertFalse(EmailUtil.validateCustomerEmail("user@example.123"));
            assertFalse(EmailUtil.validateCustomerEmail("user@example.c"));
            assertFalse(EmailUtil.validateCustomerEmail("user@example"));
            assertFalse(EmailUtil.validateCustomerEmail("user@" + repeat('a', 64) + ".com")); // label=64
        }

        @Test
        @DisplayName("Domínio curto demais (len < 3)")
        void deveRejeitarDominioCurtoDemais() {
            assertFalse(EmailUtil.validateCustomerEmail("x@a"));
            assertFalse(EmailUtil.validateCustomerEmail("x@ab"));
        }

        @Test
        @DisplayName("Label do domínio com 64 chars (inválido)")
        void deveRejeitarLabelDeDominioCom64Chars() {
            String dom = label(64) + ".com";
            assertFalse(EmailUtil.validateCustomerEmail("user@" + dom));
        }

        @Test
        @DisplayName("Caracteres não [A-Za-z0-9-] no domínio")
        void deveRejeitarCaracteresNaoAsciiLetterDigitNoDominio() {
            assertFalse(EmailUtil.validateCustomerEmail("user@exa`mple.com"));
            assertFalse(EmailUtil.validateCustomerEmail("user@exam[ple.com"));
            assertFalse(EmailUtil.validateCustomerEmail("user@exam]ple.com"));
            assertFalse(EmailUtil.validateCustomerEmail("user@exam/ple.com"));
            assertFalse(EmailUtil.validateCustomerEmail("user@exam:ple.com"));
        }
    }

    // ===========================
    // IDN / Surrogates
    // ===========================
    @Nested
    @DisplayName("IDN e surrogates")
    class IdnESurrogates {

        @Test
        @DisplayName("IDN desligado vs ligado e controle no domínio")
        void deveTestarIdn() {
            assertFalse(EmailUtil.validateCustomerEmail("usuario@müller.de", false));
            assertTrue(EmailUtil.validateCustomerEmail("usuario@müller.de", true));
            assertFalse(EmailUtil.validateCustomerEmail("user@\u0007bad\u0002domain.com", true));
        }

        @Test
        @DisplayName("High surrogate isolado no domínio")
        void deveCobrirCatchIdn_comSurrogateAltoIsolado() {
            assertFalse(EmailUtil.validateCustomerEmail("user@\uD800.com", true));
        }

        @Test
        @DisplayName("Low surrogate isolado no domínio")
        void deveCobrirCatchIdn_comSurrogateBaixoIsolado() {
            assertFalse(EmailUtil.validateCustomerEmail("user@\uDC00.com", true));
        }
    }

    // ===========================
    // Auxiliares
    // ===========================
    private static String repeat(char ch, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(ch);
        return sb.toString();
    }

    private static String label(int n) {
        return repeat('a', n);
    }

    // 63+63+63+61 + 3 pontos = 253
    private static String domainLen253() {
        return label(63) + "." + label(63) + "." + label(63) + "." + label(61);
    }
}
