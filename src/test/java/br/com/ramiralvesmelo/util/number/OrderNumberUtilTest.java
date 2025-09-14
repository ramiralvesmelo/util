package br.com.ramiralvesmelo.util.number;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderNumberUtilTest {

    // ULID (Crockford Base32) sem I, L, O, U — 26 caracteres
    private static final Pattern ULID_PATTERN =
        Pattern.compile("^ORD-[0-9A-HJKMNP-TV-Z]{26}$");

    @Test
    @DisplayName("Formato deve ser ORD- + ULID (26 chars Base32 Crockford)")
    void formatIsCorrect() {
        String num = OrderNumberUtil.generate();
        assertTrue(ULID_PATTERN.matcher(num).matches(),
            "Formato inválido: " + num);
        assertTrue(num.startsWith("ORD-"));
        assertEquals(30, num.length()); // "ORD-" (4) + 26 = 30
    }

    @Test
    @DisplayName("Geração deve ser única em um conjunto razoável")
    void shouldBeUnique() {
        int n = 1000;
        Set<String> set = new HashSet<>(n);
        for (int i = 0; i < n; i++) {
            set.add(OrderNumberUtil.generate());
        }
        assertEquals(n, set.size(), "Houve colisões inesperadas.");
    }
}
