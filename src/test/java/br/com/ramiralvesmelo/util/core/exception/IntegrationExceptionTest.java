package br.com.ramiralvesmelo.util.core.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class IntegrationExceptionTest {

    @Test
    void deveCriarComMensagemEStatusPadrao() {
        IntegrationException ex = new IntegrationException("erro-integ");
        assertEquals("erro-integ", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void deveCriarComMensagemEStatusCustom() {
        IntegrationException ex = new IntegrationException("erro-x", HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals("erro-x", ex.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
    }

    @Test
    void atalhoConflict() {
        IntegrationException ex = IntegrationException.conflict("msg-conflito");
        assertEquals("msg-conflito", ex.getMessage());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void atalhoNotFound() {
        IntegrationException ex = IntegrationException.notFound("msg-notfound");
        assertEquals("msg-notfound", ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
