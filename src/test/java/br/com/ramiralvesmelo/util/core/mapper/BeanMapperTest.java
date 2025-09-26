package br.com.ramiralvesmelo.util.core.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.ramiralvesmelo.util.commons.dto.OrderItemDto;

class BeanMapperTest {

    @Test
    void deveMapearEntreObjetosUsandoDTOExistente() {
        // usa um DTO já presente no projeto (sem criar classes auxiliares)
        OrderItemDto origem = OrderItemDto.builder()
                .id(1L)
                .productId(2L)
                .productName("Teclado")
                .quantity(3)
                .unitPrice(new BigDecimal("50.00"))
                .subtotal(new BigDecimal("150.00"))
                .build();

        // mapeia para a MESMA classe como destino (ModelMapper suporta isso)
        OrderItemDto destino = BeanMapper.map(origem, OrderItemDto.class);

        assertNotNull(destino);
        assertEquals(origem.getId(), destino.getId());
        assertEquals(origem.getProductId(), destino.getProductId());
        assertEquals(origem.getProductName(), destino.getProductName());
        assertEquals(origem.getQuantity(), destino.getQuantity());
        assertEquals(origem.getUnitPrice(), destino.getUnitPrice());
        assertEquals(origem.getSubtotal(), destino.getSubtotal());
    }

    @Test
    void deveCobrirConstrutorPrivadoSemEsperarExcecao() throws Exception {
        Constructor<BeanMapper> ctor = BeanMapper.class.getDeclaredConstructor();
        assertFalse(ctor.canAccess(null)); // é private
        ctor.setAccessible(true);
        Object instancia = ctor.newInstance(); // não deve lançar
        assertNotNull(instancia);
        assertEquals(BeanMapper.class, instancia.getClass());
    }
}
