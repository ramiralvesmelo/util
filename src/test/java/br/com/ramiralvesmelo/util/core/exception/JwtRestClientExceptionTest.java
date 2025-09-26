package br.com.ramiralvesmelo.util.core.exception;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class JwtRestClientExceptionTest {

    @Test
    void deveCriarComMensagem() {
        JwtRestClientException ex = new JwtRestClientException("erro jwt");
        assertEquals("erro jwt", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void deveCriarComMensagemECausa() {
        Throwable cause = new IllegalStateException("causa");
        JwtRestClientException ex = new JwtRestClientException("falha jwt", cause);
        assertEquals("falha jwt", ex.getMessage());
        assertSame(cause, ex.getCause());
    }
}
