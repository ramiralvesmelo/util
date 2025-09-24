package br.com.springboot.appdemo.message.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.ramiralvesmelo.util.shared.event.OrderFinalizedEvent;

class OrderFinalizedEventTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Deve construir OrderFinalizedEvent com itens via builder")
    void shouldBuildEventWithItems() {
        OrderFinalizedEvent.ItemDto item1 = OrderFinalizedEvent.ItemDto.builder()
                .productId(10L)
                .quantity(2)
                .unitPrice(new BigDecimal("19.90"))
                .subtotal(new BigDecimal("39.80"))
                .build();

        OrderFinalizedEvent.ItemDto item2 = OrderFinalizedEvent.ItemDto.builder()
                .productId(11L)
                .quantity(1)
                .unitPrice(new BigDecimal("99.00"))
                .subtotal(new BigDecimal("99.00"))
                .build();

        OrderFinalizedEvent evt = OrderFinalizedEvent.builder()
                .orderId(1L)
                .orderNumber("ORD-2025-0001")
                .customerId(100L)
                .totalAmount(new BigDecimal("138.80"))
                .items(List.of(item1, item2))
                .occurredAt("2025-09-02T18:15:00-03:00")
                .build();

        assertEquals(1L, evt.getOrderId());
        assertEquals("ORD-2025-0001", evt.getOrderNumber());
        assertEquals(100L, evt.getCustomerId());
        assertEquals(new BigDecimal("138.80"), evt.getTotalAmount());
        assertNotNull(evt.getItems());
        assertEquals(2, evt.getItems().size());

        // Valida formato básico ISO-8601 com offset
        assertTrue(Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T.*[+-]\\d{2}:\\d{2}$").matcher(evt.getOccurredAt()).find());
    }

    @Test
    @DisplayName("Deve serializar para {} quando todos os campos forem null (JsonInclude.NON_NULL)")
    void shouldSerializeEmptyJsonWhenAllNull() throws JsonProcessingException {
        OrderFinalizedEvent evt = new OrderFinalizedEvent();
        String json = mapper.writeValueAsString(evt);
        assertEquals("{}", json);
    }

    @Test
    @DisplayName("Deve desserializar JSON mínimo para OrderFinalizedEvent")
    void shouldDeserializeFromJson() throws Exception {
        String json = """
            {
              "orderId": 5,
              "orderNumber": "ORD-5",
              "customerId": 200,
              "totalAmount": 123.45,
              "occurredAt": "2025-09-02T18:30:00-03:00",
              "items": [
                {"productId": 7, "quantity": 3, "unitPrice": 10.00, "subtotal": 30.00}
              ]
            }
            """;

        OrderFinalizedEvent evt = mapper.readValue(json, OrderFinalizedEvent.class);

        assertEquals(5L, evt.getOrderId());
        assertEquals("ORD-5", evt.getOrderNumber());
        assertEquals(200L, evt.getCustomerId());
        assertEquals(new BigDecimal("123.45"), evt.getTotalAmount());
        assertEquals("2025-09-02T18:30:00-03:00", evt.getOccurredAt());

        assertNotNull(evt.getItems());
        assertEquals(1, evt.getItems().size());

        OrderFinalizedEvent.ItemDto item = evt.getItems().get(0);
        assertEquals(7L, item.getProductId());
        assertEquals(3, item.getQuantity());
        assertEquals(new BigDecimal("10.00"), item.getUnitPrice());
        assertEquals(new BigDecimal("30.00"), item.getSubtotal());
    }
}
