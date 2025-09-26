package br.com.ramiralvesmelo.util.validation.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmailUtilTest {

    // =========================
    // Casos válidos (baseline)
    // =========================
    @Test
    @DisplayName("Válido: local + domínio simples com TLD alfabético ≥ 2")
    void valido_basico() {
        assertThat(EmailUtil.validateCustomerEmail("john.doe+tag@example-domain.com")).isTrue();
    }

    @Test
    @DisplayName("Válido com IDN: domínio internacional convertido para ASCII (Punycode)")
    void valido_idn_convertido() {
        // "täst.de" -> "xn--tst-qla.de"
        assertThat(EmailUtil.validateCustomerEmail("user@täst.de", true)).isTrue();
    }

    // =========================
    // Nulos, vazios e '@'
    // =========================
    @Test
    @DisplayName("Inválido: e-mail nulo")
    void invalido_nulo() {
        assertThat(EmailUtil.validateCustomerEmail(null)).isFalse();
    }

    @Test
    @DisplayName("Inválido: e-mail vazio ou só espaços")
    void invalido_vazio() {
        assertThat(EmailUtil.validateCustomerEmail("   ")).isFalse();
    }

    @Test
    @DisplayName("Inválido: sem '@', múltiplos '@' ou '@' no final")
    void invalido_arroba() {
        assertThat(EmailUtil.validateCustomerEmail("no-at.domain")).isFalse();
        assertThat(EmailUtil.validateCustomerEmail("a@@b.com")).isFalse();
        assertThat(EmailUtil.validateCustomerEmail("a@")).isFalse();
    }

    // =========================
    // Tamanhos local/domain
    // =========================
    @Test
    @DisplayName("Inválido: local-part > 64 chars")
    void invalido_localMuitoLongo() {
        String local = "a".repeat(65);
        assertThat(EmailUtil.validateCustomerEmail(local + "@example.com")).isFalse();
    }

    @Test
    @DisplayName("Inválido: domínio < 3 chars ou > 253 chars")
    void invalido_domainLen() {
        assertThat(EmailUtil.validateCustomerEmail("a@a")).isFalse(); // 1-char
        // monta domínio > 253: 4 labels com 63 + 3 pontos = 255
        String label63 = "a".repeat(63);
        String longDomain = label63 + "." + label63 + "." + label63 + "." + label63;
        assertThat(EmailUtil.validateCustomerEmail("x@" + longDomain)).isFalse();
    }

    // =========================
    // Pontos consecutivos
    // =========================
    @Test
    @DisplayName("Inválido: '..' no local ou no domínio")
    void invalido_pontosConsecutivos() {
        assertThat(EmailUtil.validateCustomerEmail("a..b@example.com")).isFalse();
        assertThat(EmailUtil.validateCustomerEmail("a@ex..ample.com")).isFalse();
    }

    // =========================
    // LOCAL_ALLOWED e início/fim inválidos no local
    // =========================
    @Test
    @DisplayName("Inválido: local-part com caracteres fora de LOCAL_ALLOWED")
    void invalido_localChars() {
        assertThat(EmailUtil.validateCustomerEmail("a b@example.com")).isFalse();  // espaço
        assertThat(EmailUtil.validateCustomerEmail("a,bc@example.com")).isFalse(); // vírgula
    }

    @Test
    @DisplayName("Inválido: local-part começa/termina com caractere não alfanumérico")
    void invalido_localInicioFim() {
        assertThat(EmailUtil.validateCustomerEmail(".abc@example.com")).isFalse();
        assertThat(EmailUtil.validateCustomerEmail("abc.@example.com")).isFalse();
        assertThat(EmailUtil.validateCustomerEmail("-abc@example.com")).isFalse(); // começa com '-'
        assertThat(EmailUtil.validateCustomerEmail("abc-@example.com")).isFalse(); // termina com '-'
    }

    // =========================
    // IDN: exceção e conversão
    // =========================
    @Test
    @DisplayName("Inválido com IDN: domínio com code point inválido causa IllegalArgumentException no IDN.toASCII")
    void invalido_idn_excecao() {
        // usa um surrogate inválido sozinho para forçar IllegalArgumentException em IDN.toASCII
        String invalidDomain = "bad." + "\uD800" + "com"; // high-surrogate não pareado
        assertThat(EmailUtil.validateCustomerEmail("user@" + invalidDomain, true)).isFalse();
    }

    // =========================
    // Labels e TLD
    // =========================
    @Test
    @DisplayName("Inválido: menos de 2 labels no domínio")
    void invalido_labelsInsuficientes() {
        assertThat(EmailUtil.validateCustomerEmail("x@localhost")).isFalse();
    }

    @Test
    @DisplayName("Inválido: label vazia ou > 63 chars")
    void invalido_labelVaziaOuLonga() {
        assertThat(EmailUtil.validateCustomerEmail("x@a..com")).isFalse(); // vazia entre pontos
        String longLabel = "a".repeat(64);
        assertThat(EmailUtil.validateCustomerEmail("x@" + longLabel + ".com")).isFalse();
    }

    @Test
    @DisplayName("Inválido: label com caractere inválido (_)")
    void invalido_labelChars() {
        assertThat(EmailUtil.validateCustomerEmail("x@_bad.com")).isFalse();
    }

    @Test
    @DisplayName("Inválido: label começa/termina com '-' (não casa LABEL_PATTERN)")
    void invalido_labelHifenBorda() {
        assertThat(EmailUtil.validateCustomerEmail("x@-bad.com")).isFalse();
        assertThat(EmailUtil.validateCustomerEmail("x@bad-.com")).isFalse();
    }

    @Test
    @DisplayName("Inválido: TLD não-alfabética ou com tamanho < 2")
    void invalido_tld() {
        assertThat(EmailUtil.validateCustomerEmail("x@example.c")).isFalse();     // 1 letra
        assertThat(EmailUtil.validateCustomerEmail("x@example.c1")).isFalse();    // contém dígito
        assertThat(EmailUtil.validateCustomerEmail("x@example.123")).isFalse();   // só dígitos
    }

    // =========================
    // Método protegido (cobertura)
    // =========================
    @Test
    @DisplayName("isAsciiLetterOrDigit: true e false")
    void isAsciiLetterOrDigit_paths() {
        // true
        assertThat(EmailUtil.isAsciiLetterOrDigit('A')).isTrue();
        assertThat(EmailUtil.isAsciiLetterOrDigit('z')).isTrue();
        assertThat(EmailUtil.isAsciiLetterOrDigit('0')).isTrue();
        // false
        assertThat(EmailUtil.isAsciiLetterOrDigit('-')).isFalse();
        assertThat(EmailUtil.isAsciiLetterOrDigit('.')).isFalse();
        assertThat(EmailUtil.isAsciiLetterOrDigit('_')).isFalse();
    }

    // =========================
    // Extra: muitos casos válidos aleatórios p/ robustez (opcional)
    // =========================
    @Test
    @DisplayName("Válidos diversos: variações de local-part e labels (sanidade)")
    void validos_varios() {
        String[] locals = {"a", "a.b", "a_b", "a-b", "a+b", "A1._%+-z"};
        String[] domains = {"ex.com", "sub.example.com", "exa-mple.co"};
        for (String l : locals) {
            for (String d : domains) {
                String email = l + "@" + d;
                assertThat(EmailUtil.validateCustomerEmail(email))
                    .as("deveria ser válido: %s", email)
                    .isTrue();
            }
        }
    }

    // =========================
    // Domínio gigantesco, mas válido (no limite interno das labels)
    // (sanidade de performance e caminhos sem logging)
    // =========================
    @Test
    @DisplayName("Válido: domínio com várias labels no limite [63], respeitando comprimento total")
    void valido_domainLimiteInterno() {
        // 3 labels de 63 + ".com" → total < 253
        String label63 = IntStream.range(0, 63).mapToObj(i -> "a").collect(Collectors.joining());
        String domain = label63 + "." + label63 + "." + label63 + ".com";
        assertThat(domain.length()).isLessThanOrEqualTo(253);
        assertThat(EmailUtil.validateCustomerEmail("a@" + domain)).isTrue();
    }
}
