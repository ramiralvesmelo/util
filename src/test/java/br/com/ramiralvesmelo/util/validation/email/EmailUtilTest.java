package br.com.ramiralvesmelo.util.validation.email;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmailUtilTest {

    // ========= casos válidos =========

    @Test
    void deveValidarEmailSimplesValido() {
        String email = "john.doe+tag@example-domain.com";
        assertTrue(EmailUtil.validateCustomerEmail(email));
    }

    @Test
    void deveValidarEmailComIdnQuandoAllowIdnTrue() {
        // domínio com caractere não-ASCII -> IDN (ex.: müller.de)
        String emailIdn = "user@müller.de";
        assertTrue(EmailUtil.validateCustomerEmail(emailIdn, true));
    }

    // ========= casos inválidos gerais =========

    @Test
    void deveFalharQuandoEmailNuloOuVazio() {
        assertFalse(EmailUtil.validateCustomerEmail(null));
        assertFalse(EmailUtil.validateCustomerEmail("   "));
    }

    @Test
    void deveFalharQuandoArrobaInvalida() {
        // sem @
        assertFalse(EmailUtil.validateCustomerEmail("john.doe.example.com"));
        // mais de um @
        assertFalse(EmailUtil.validateCustomerEmail("a@b@c.com"));
        // @ no final
        assertFalse(EmailUtil.validateCustomerEmail("a@"));
    }

    @Test
    void deveFalharQuandoTamanhoLocalOuDominioIncorreto() {
        // local > 64
        String local65 = "a".repeat(65);
        assertFalse(EmailUtil.validateCustomerEmail(local65 + "@example.com"));

        // dominio < 3
        assertFalse(EmailUtil.validateCustomerEmail("a@x"));

        // dominio > 253
        String longLabel = "a".repeat(63);
        String bigDomain = longLabel + "." + longLabel + "." + longLabel + "." + longLabel + "." + longLabel;
        assertTrue(bigDomain.length() > 253);
        assertFalse(EmailUtil.validateCustomerEmail("a@" + bigDomain));
    }

    @Test
    void deveFalharQuandoHaPontosConsecutivosEmLocalOuDominio() {
        assertFalse(EmailUtil.validateCustomerEmail("ab..cd@example.com"));
        assertFalse(EmailUtil.validateCustomerEmail("abcd@ex..ample.com"));
    }

    @Test
    void deveFalharQuandoLocalPartTemCaracterInvalido() {
        assertFalse(EmailUtil.validateCustomerEmail("ab!cd@example.com")); // '!' não permitido no regex LOCAL_ALLOWED
    }

    @Test
    void deveFalharQuandoLocalPartComecaOuTerminaComCharNaoAlfaNum() {
        assertFalse(EmailUtil.validateCustomerEmail(".abc@example.com")); // começa com '.'
        assertFalse(EmailUtil.validateCustomerEmail("abc.@example.com")); // termina com '.'
        assertFalse(EmailUtil.validateCustomerEmail("_abc@example.com")); // começa com '_' (underscore não é letra/dígito)
        assertFalse(EmailUtil.validateCustomerEmail("abc-@example.com")); // termina com '-' (hífen não é letra/dígito)
    }

    // ========= domínio / labels / TLD =========

    @Test
    void deveFalharQuandoDominioTemMenosDeDuasLabels() {
        assertFalse(EmailUtil.validateCustomerEmail("user@localhost"));
    }

    @Test
    void deveFalharQuandoLabelVaziaOuMuitoLonga() {
        assertFalse(EmailUtil.validateCustomerEmail("user@ex..com")); // label vazia
        String label64 = "a".repeat(64);
        assertFalse(EmailUtil.validateCustomerEmail("user@" + label64 + ".com")); // >63
    }

    @Test
    void deveFalharQuandoLabelTemCaracterInvalido() {
        assertFalse(EmailUtil.validateCustomerEmail("user@exa$mple.com"));
        assertFalse(EmailUtil.validateCustomerEmail("user@-startdash.com"));  // começa com '-'
        assertFalse(EmailUtil.validateCustomerEmail("user@enddash-.com"));    // termina com '-'
    }

    @Test
    void deveFalharQuandoTldInvalido() {
        assertFalse(EmailUtil.validateCustomerEmail("user@example.c"));  // < 2 letras
        assertFalse(EmailUtil.validateCustomerEmail("user@example.c0m")); // contém número
    }

    @Test
    void deveFalharIdnQuandoAllowIdnFalse() {
        // domínio com caractere não-ASCII e allowIdn = false -> LABEL_PATTERN falha
        assertFalse(EmailUtil.validateCustomerEmail("user@müller.de", false));
    }

    @Test
    void deveFalharQuandoConversaoIdnLancarExcecao() {
        // domínio com caractere de controle força IllegalArgumentException no IDN.toASCII
        // (não é garantido em todas as JDKs, mas caracteres de controle são inválidos)
        String dominioRuim = "exa\u0001mple.com";
        assertFalse(EmailUtil.validateCustomerEmail("user@" + dominioRuim, true));
    }

    // ========= helper protegido =========

    @Test
    void isAsciiLetterOrDigit_deveRetornarCorreto() {
        assertTrue(EmailUtil.isAsciiLetterOrDigit('A'));
        assertTrue(EmailUtil.isAsciiLetterOrDigit('z'));
        assertTrue(EmailUtil.isAsciiLetterOrDigit('0'));
        assertFalse(EmailUtil.isAsciiLetterOrDigit('-'));
        assertFalse(EmailUtil.isAsciiLetterOrDigit('_'));
        assertFalse(EmailUtil.isAsciiLetterOrDigit('.'));
    }

    // ========= delegação do método sem allowIdn =========

    @Test
    void validateCustomerEmail_semAllowIdnDeveDelegarParaFalse() {
        // aqui só garantimos que o comportamento default (false) mantém inválido um IDN
        assertFalse(EmailUtil.validateCustomerEmail("u@tésté.com"));
        // e válido um ASCII puro
        assertTrue(EmailUtil.validateCustomerEmail("u@teste.com"));
    }
}
	