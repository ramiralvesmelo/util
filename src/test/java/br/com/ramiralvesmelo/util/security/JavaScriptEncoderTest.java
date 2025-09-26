package br.com.ramiralvesmelo.util.security;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.Test;

class JavaScriptEncoderTest {

    @Test
    void construtorPrivado_deveSerInacessivelMasInstanciavelPorReflexao() throws Exception {
        Constructor<JavaScriptEncoder> ctor = JavaScriptEncoder.class.getDeclaredConstructor();
        assertFalse(ctor.canAccess(null));
        ctor.setAccessible(true);
        Object instance = ctor.newInstance();
        assertNotNull(instance);
    }

    @Test
    void escape_deveRetornarNullLiteralQuandoEntradaForNull() {
        assertEquals("null", JavaScriptEncoder.escape(null));
    }

    @Test
    void escape_deveEscaparAspasEBarra() {
        String original = "texto \"com aspas\" e barra \\";
        String encoded = JavaScriptEncoder.escape(original);

        assertTrue(encoded.startsWith("\"") && encoded.endsWith("\""));
        assertTrue(encoded.contains("\\\"com aspas\\\""));
        assertTrue(encoded.contains("barra \\\\"));
    }

    @Test
    void escape_deveEscaparQuebrasDeLinhaETab() {
        String original = "linha1\nlinha2\rlinha3\tfim";
        String encoded = JavaScriptEncoder.escape(original);

        // quebra de linha vira \n
        assertTrue(encoded.contains("linha1\\nlinha2linha3\\tfim"));
        // \r é removido
        assertFalse(encoded.contains("\r"));
    }

    @Test
    void escape_deveManterTextoSimplesEntreAspas() {
        String original = "abc123";
        String encoded = JavaScriptEncoder.escape(original);
        assertEquals("\"abc123\"", encoded);
    }
}
