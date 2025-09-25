package br.com.ramiralvesmelo.util.core.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import br.com.ramiralvesmelo.util.core.exception.IntegrationException;

class IntegrationExceptionTest {

    @Test
    @DisplayName("Construtor padrão deve setar status BAD_REQUEST")
    void defaultConstructor_setsBadRequest() {
        IntegrationException ex = new IntegrationException("erro padrão");
        assertEquals("erro padrão", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    @DisplayName("Construtor com status customizado deve manter mensagem e status")
    void constructor_withCustomStatus() {
        IntegrationException ex = new IntegrationException("conflito", HttpStatus.CONFLICT);
        assertEquals("conflito", ex.getMessage());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    @DisplayName("Factory notFound deve retornar status 404")
    void factory_notFound() {
        IntegrationException ex = IntegrationException.notFound("não encontrado");
        assertEquals("não encontrado", ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    @DisplayName("Factory conflict deve retornar status 409")
    void factory_conflict() {
        IntegrationException ex = IntegrationException.conflict("conflito");
        assertEquals("conflito", ex.getMessage());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    @DisplayName("Lançamento da exceção preserva mensagem e status")
    void throwing_preservesMessageAndStatus() {
        IntegrationException ex = assertThrows(
            IntegrationException.class,
            () -> { throw IntegrationException.conflict("falha integração"); }
        );
        assertEquals("falha integração", ex.getMessage());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }
}
