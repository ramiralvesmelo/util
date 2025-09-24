package br.com.ramiralvesmelo.util.shared.dto;

import java.time.LocalDateTime;

public record DocumentDto(
    Long id,
    String filename,
    String contentType,
    Long sizeBytes,
    String url,
    String description,
    boolean available,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long orderId,
    String orderNumber
) {

}
