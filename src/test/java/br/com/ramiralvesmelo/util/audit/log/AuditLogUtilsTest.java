package br.com.ramiralvesmelo.util.audit.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.ramiralvesmelo.util.message.event.AuditLogEvent;

/**
 * Testes unitários para AuditLogUtils.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogUtilsTest {

    @Mock
    private AuditLogService service;

    @Captor
    private ArgumentCaptor<AuditLogEvent> eventCaptor;
  
    static class OrderDto {
        Long id;
        String customer;
        BigDecimal total;

        OrderDto(Long id, String customer, BigDecimal total) {
            this.id = id;
            this.customer = customer;
            this.total = total;
        }
    }

    @Test
    @DisplayName("Deve converter objeto em payload e enviar via service.send")  
    void shouldConvertAndSend_withMapSource() {
        Map<String, Object> source = Map.of(
            "id", 10L,
            "customer", "Alice",
            "total", new BigDecimal("123.45")
        );

        AuditLogUtils.sendAsAuditLog(service, source);

        var captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(service).send(captor.capture());

        AuditLogEvent sent = captor.getValue();
        assertNotNull(sent);
        assertEquals(10, ((Number) sent.getPayload().get("id")).intValue());
        assertEquals("Alice", sent.getPayload().get("customer"));
        assertEquals("123.45", String.valueOf(sent.getPayload().get("total")));
    }


    @Test
    @DisplayName("Deve lançar IllegalArgumentException se service for nulo")
    void shouldThrowWhenServiceIsNull() {
        OrderDto source = new OrderDto(1L, "Bob", new BigDecimal("1.00"));
        assertThrows(IllegalArgumentException.class,
            () -> AuditLogUtils.sendAsAuditLog(null, source));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException se sourceObj for nulo")
    void shouldThrowWhenSourceIsNull() {
        assertThrows(IllegalArgumentException.class,
            () -> AuditLogUtils.sendAsAuditLog(service, null));
    }
    
    
    @Test
    @DisplayName("Deve logar erro quando service.send lançar exceção")
    void shouldLogErrorWhenServiceThrows() {
        Map<String, Object> source = Map.of("id", 99L);

        // Força o mock a lançar RuntimeException quando service.send for chamado
        RuntimeException boom = new RuntimeException("falha no envio");
        doThrow(boom).when(service).send(any(AuditLogEvent.class));

        // Executa o método (não deve propagar a exceção)
        AuditLogUtils.sendAsAuditLog(service, source);

        // Verifica que tentou enviar uma vez
        verify(service).send(any(AuditLogEvent.class));
    }
    
}
