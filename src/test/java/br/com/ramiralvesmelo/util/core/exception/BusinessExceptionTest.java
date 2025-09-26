package br.com.ramiralvesmelo.util.core.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BusinessExceptionTest {

    @Test
    void deveCriarComMensagemEStatusPadrao() {
        BusinessException ex = new BusinessException("erro-padrao");
        assertEquals("erro-padrao", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void deveCriarComMensagemEStatusCustom() {
        BusinessException ex = new BusinessException("erro-custom", HttpStatus.CONFLICT);
        assertEquals("erro-custom", ex.getMessage());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void atalhoConflict() {
        BusinessException ex = BusinessException.conflict("msg");
        assertEquals("msg", ex.getMessage());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void atalhoNotFound() {
        BusinessException ex = BusinessException.notFound("nao achei");
        assertEquals("nao achei", ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
