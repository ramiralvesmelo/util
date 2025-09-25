package br.com.ramiralvesmelo.util.shared.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

//TODO: CORRIGIR!!!
@Disabled("Desativado temporariamente até corrigir implementação")
class DtoSerializationTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setup() {
        mapper = new ObjectMapper();
    }

    @Test
    void testOrderItemDtoSerialization() throws Exception {
        OrderItemDto dto = OrderItemDto.builder()
                //.id(1L)
                //.productId(101L)
                .productName("Produto A")
                //.quantity(2)
                //.unitPrice(new BigDecimal("10.50"))
                //.subtotal(new BigDecimal("21.00"))
                .build();

        String json = mapper.writeValueAsString(dto);
        assertNotNull(json);

        OrderItemDto back = mapper.readValue(json, OrderItemDto.class);
        assertEquals(dto.getProductName(), back.getProductName());
        //assertEquals(dto.getSubtotal(), back.getSubtotal());
    }

    @Test
    void testDocumentDtoSerialization() throws Exception {
        DocumentDto dto = DocumentDto.builder()
                .id(10L)
                .filename("arquivo.pdf")
                .contentType("application/pdf")
                .sizeBytes(2048L)
                .url("http://localhost/files/arquivo.pdf")
                .available(true)
                .createdAt(LocalDateTime.now())
                .orderId(5L)
                .message("Teste de documento")
                .build();

        String json = mapper.writeValueAsString(dto);
        assertNotNull(json);

        DocumentDto back = mapper.readValue(json, DocumentDto.class);
        assertEquals(dto.getFilename(), back.getFilename());
        assertEquals(dto.getOrderId(), back.getOrderId());
    }

    @Test
    void testOrderDtoSerialization() throws Exception {
        OrderItemDto item = OrderItemDto.builder()
                //.id(1L)
               // .productId(202L)
                .productName("Produto B")
               // .quantity(1)
                //.unitPrice(new BigDecimal("50.00"))
                //.subtotal(new BigDecimal("50.00"))
                .build();

        DocumentDto doc = DocumentDto.builder()
                .id(99L)
                .filename("nota-fiscal.xml")
                .contentType("application/xml")
                .orderId(123L)
                .message("NF-e vinculada")
                .build();

        OrderDto dto = OrderDto.builder()
                .id(123L)
                .orderNumber("ORD-2025-0001")
                .orderDate(LocalDateTime.now())
                .totalAmount(new BigDecimal("50.00"))
                .customerId(456L)
                //.document(doc)
                //.items(List.of(item))
                .message("Pedido de teste")
                .build();

        String json = mapper.writeValueAsString(dto);
        assertNotNull(json);

        OrderDto back = mapper.readValue(json, OrderDto.class);
        assertEquals(dto.getOrderNumber(), back.getOrderNumber());
        //assertEquals(dto.getItems().size(), back.getItems().size());
        //(dto.getDocument().getFilename(), back.getDocument().getFilename());
    }
}
