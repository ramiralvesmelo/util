package br.com.ramiralvesmelo.util.order;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.Test;

class OrderNumberUtilTest {

    @Test
    void construtorPrivado_deveSerInacessivelMasInstanciavelPorReflexao() throws Exception {
        Constructor<OrderNumberUtil> ctor = OrderNumberUtil.class.getDeclaredConstructor();
        assertFalse(ctor.canAccess(null)); // privado
        ctor.setAccessible(true);
        OrderNumberUtil instance = ctor.newInstance();
        assertNotNull(instance); // garante que instanciou
    }

    @Test
    void generate_deveGerarNumeroComPrefixoOrdEFormatoUlid() {
        String order = OrderNumberUtil.generate();

        assertNotNull(order);
        assertTrue(order.startsWith("ORD-"), "Deve começar com ORD-");
        String ulid = order.substring(4);

        // ULID deve ter 26 caracteres alfanuméricos maiúsculos
        assertEquals(26, ulid.length());
        assertTrue(ulid.matches("^[0-9A-Z]{26}$"),
                "ULID deve conter apenas letras maiúsculas e números: " + ulid);
    }

    @Test
    void generate_deveGerarValoresUnicos() {
        String a = OrderNumberUtil.generate();
        String b = OrderNumberUtil.generate();
        assertNotEquals(a, b);
    }
}
