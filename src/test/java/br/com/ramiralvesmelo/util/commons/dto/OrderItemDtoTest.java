package br.com.ramiralvesmelo.util.commons.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class OrderItemDtoTest {

    @Test
    void noArgsESettersGetters() {
        OrderItemDto dto = new OrderItemDto();
        dto.setId(11L);
        dto.setProductId(5L);
        dto.setProductName("Notebook");
        dto.setQuantity(2);
        dto.setUnitPrice(new BigDecimal("3500.00"));
        dto.setSubtotal(new BigDecimal("7000.00"));

        assertEquals(11L, dto.getId());
        assertEquals(5L, dto.getProductId());
        assertEquals("Notebook", dto.getProductName());
        assertEquals(2, dto.getQuantity());
        assertEquals(new BigDecimal("3500.00"), dto.getUnitPrice());
        assertEquals(new BigDecimal("7000.00"), dto.getSubtotal());
    }

    @Test
    void allArgsConstructor() {
        OrderItemDto dto = new OrderItemDto(
                1L, 2L, "Mouse", 3, new BigDecimal("10.00"), new BigDecimal("30.00"));
        assertEquals(1L, dto.getId());
        assertEquals(2L, dto.getProductId());
        assertEquals("Mouse", dto.getProductName());
        assertEquals(3, dto.getQuantity());
        assertEquals(new BigDecimal("10.00"), dto.getUnitPrice());
        assertEquals(new BigDecimal("30.00"), dto.getSubtotal());
    }

    @Test
    void builderEToBuilder() {
        OrderItemDto dto = OrderItemDto.builder()
                .id(99L)
                .productId(10L)
                .productName("Teclado")
                .quantity(1)
                .unitPrice(new BigDecimal("100.00"))
                .subtotal(new BigDecimal("100.00"))
                .build();

        assertEquals(99L, dto.getId());
        assertEquals(10L, dto.getProductId());

        // toBuilder()
        OrderItemDto alterado = dto.toBuilder()
                .quantity(2)
                .subtotal(new BigDecimal("200.00"))
                .build();

        assertEquals(2, alterado.getQuantity());
        assertEquals(new BigDecimal("200.00"), alterado.getSubtotal());
        assertEquals(dto.getProductId(), alterado.getProductId());
    }

    @Test
    void equalsHashCodeToString() {
        OrderItemDto a = OrderItemDto.builder().id(1L).productId(2L).build();
        OrderItemDto b = OrderItemDto.builder().id(1L).productId(2L).build();
        OrderItemDto c = OrderItemDto.builder().id(3L).productId(2L).build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);

        String s = a.toString();
        assertTrue(s.contains("id=1"));
        assertTrue(s.contains("productId=2"));
    }
}
