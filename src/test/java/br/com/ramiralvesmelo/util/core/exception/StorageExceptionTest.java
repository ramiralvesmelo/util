package br.com.ramiralvesmelo.util.core.exception;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class StorageExceptionTest {

    @Test
    void deveCriarComMensagem() {
        StorageException ex = new StorageException("erro storage");
        assertEquals("erro storage", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void deveCriarComMensagemECausa() {
        Throwable cause = new IllegalArgumentException("causa");
        StorageException ex = new StorageException("falha storage", cause);
        assertEquals("falha storage", ex.getMessage());
        assertSame(cause, ex.getCause());
    }
}
