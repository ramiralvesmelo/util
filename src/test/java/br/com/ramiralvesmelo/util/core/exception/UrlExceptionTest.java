package br.com.ramiralvesmelo.util.core.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class UrlExceptionTest {

    @Test
    void deveCriarComMensagem() {
        UrlException ex = new UrlException("erro url");
        assertEquals("erro url", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void deveCriarComMensagemECausa() {
        Throwable cause = new NullPointerException("causa");
        UrlException ex = new UrlException("falha url", cause);
        assertEquals("falha url", ex.getMessage());
        assertSame(cause, ex.getCause());
    }
}
