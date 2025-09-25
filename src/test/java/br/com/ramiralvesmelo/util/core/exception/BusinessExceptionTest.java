package br.com.ramiralvesmelo.util.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BusinessExceptionTest {

    @Test
    @DisplayName("Construtor simples define BAD_REQUEST por padrão")
    void defaultStatusIsBadRequest() {
        BusinessException ex = new BusinessException("erro");
        assertEquals("erro", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    @DisplayName("Construtor com status customizado deve refletir o status")
    void customStatusIsApplied() {
        BusinessException ex = new BusinessException("conflito", HttpStatus.CONFLICT);
        assertEquals("conflito", ex.getMessage());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    @DisplayName("Factories conflict/notFound devem retornar os HttpStatus corretos")
    void factoriesWork() {
        BusinessException c = BusinessException.conflict("x");
        BusinessException n = BusinessException.notFound("y");

        assertEquals(HttpStatus.CONFLICT, c.getStatus());
        assertEquals("x", c.getMessage());

        assertEquals(HttpStatus.NOT_FOUND, n.getStatus());
        assertEquals("y", n.getMessage());
    }
}
