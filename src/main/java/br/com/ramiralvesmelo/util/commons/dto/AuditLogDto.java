package br.com.ramiralvesmelo.util.shared.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDto {

    private String id;
    private Map<String, Object> payload;
}
