package br.com.ramiralvesmelo.util.core.exception;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PdfExceptionTest {

    @Test
    void deveCriarComMensagem() {
        PdfException ex = new PdfException("erro pdf");
        assertEquals("erro pdf", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void deveCriarComMensagemECausa() {
        Throwable cause = new RuntimeException("causa");
        PdfException ex = new PdfException("falha pdf", cause);
        assertEquals("falha pdf", ex.getMessage());
        assertSame(cause, ex.getCause());
    }
}
