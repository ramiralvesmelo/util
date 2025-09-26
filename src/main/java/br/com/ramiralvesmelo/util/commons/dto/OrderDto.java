package br.com.ramiralvesmelo.util.commons.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import br.com.ramiralvesmelo.util.commons.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNumber;    
    private LocalDateTime orderDate;
    private BigDecimal totalAmount;
    private Long customerId;
    private DocumentDto document;
    private String message;    
    private Status status;

    @Builder.Default
    private List<OrderItemDto> items = new ArrayList<>();
}
