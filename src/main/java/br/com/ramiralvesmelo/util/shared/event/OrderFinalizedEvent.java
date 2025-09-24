package br.com.ramiralvesmelo.util.shared.event;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // retorna {} quando tudo é null
public class OrderFinalizedEvent {

    private Long orderId;
    private String orderNumber;
    private Long customerId;
    private BigDecimal totalAmount;
    private List<ItemDto> items;
    private String occurredAt;
    private String message;
    private String link;
    private Document document;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ItemDto {
        private Long productId;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Document {
        private Long id;
        private String number;
        private String url;
        private String hash;
    }
}
