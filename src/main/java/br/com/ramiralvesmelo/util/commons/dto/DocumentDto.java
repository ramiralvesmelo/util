package br.com.ramiralvesmelo.util.shared.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO “flat” para Document, sem dependência de JPA/entidade.
 * Use BeanMapper + resolvers para mapear orderId ⇄ order (Entity).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentDto implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Long id;
    private String filename;
    private String contentType;
    private Long sizeBytes;
    private String url;
    private String description;
    private boolean available;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long orderId;
    private String message;
}
