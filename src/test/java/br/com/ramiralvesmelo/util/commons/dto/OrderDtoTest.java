package br.com.ramiralvesmelo.util.commons.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import br.com.ramiralvesmelo.util.commons.enums.Status;

class OrderDtoTest {

    @Test
    void noArgsESettersGettersEListaDefault() {
        OrderDto dto = new OrderDto();
        assertNotNull(dto.getItems(), "Lista items deve ser inicializada por @Builder.Default");
        assertTrue(dto.getItems().isEmpty());

        dto.setId(1L);
        dto.setOrderNumber("ORD-1");
        dto.setOrderDate(LocalDateTime.of(2024, 6, 1, 12, 0));
        dto.setTotalAmount(new BigDecimal("99.90"));
        dto.setCustomerId(7L);
        dto.setDocument(DocumentDto.builder().id(10L).filename("x").build());
        dto.setMessage("ok");
        dto.setStatus(Status.CANCELADO);

        assertEquals(1L, dto.getId());
        assertEquals("ORD-1", dto.getOrderNumber());
        assertEquals(LocalDateTime.of(2024, 6, 1, 12, 0), dto.getOrderDate());
        assertEquals(new BigDecimal("99.90"), dto.getTotalAmount());
        assertEquals(7L, dto.getCustomerId());
        assertEquals(10L, dto.getDocument().getId());
        assertEquals("ok", dto.getMessage());
        assertEquals(Status.CANCELADO, dto.getStatus());
    }

    @Test
    void builderEToBuilderMantemLista() {
        OrderDto dto = OrderDto.builder()
                .id(2L)
                .orderNumber("ORD-2")
                .totalAmount(new BigDecimal("10.00"))
                .build();

        assertNotNull(dto.getItems());
        assertTrue(dto.getItems().isEmpty());

        // adiciona item
        dto.getItems().add(OrderItemDto.builder().id(1L).productName("A").build());
        assertEquals(1, dto.getItems().size());

        // toBuilder deve copiar o estado atual
        OrderDto copy = dto.toBuilder()
                .message("cópia")
                .build();

        assertEquals("ORD-2", copy.getOrderNumber());
        assertEquals(1, copy.getItems().size());
        assertEquals("A", copy.getItems().get(0).getProductName());
        assertEquals("cópia", copy.getMessage());
    }

    @Test
    void equalsHashCodeToString() {
        OrderDto a = OrderDto.builder().id(10L).orderNumber("N1").build();
        OrderDto b = OrderDto.builder().id(10L).orderNumber("N1").build();
        OrderDto c = OrderDto.builder().id(11L).orderNumber("N1").build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);

        String s = a.toString();
        assertTrue(s.contains("id=10"));
        assertTrue(s.contains("orderNumber=N1"));
        assertTrue(s.contains("items=")); // lista presente no toString gerado pelo Lombok
    }
}
