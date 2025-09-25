package br.com.ramiralvesmelo.util.audit.logging;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.ramiralvesmelo.util.commons.dto.AuditLogDto;
import br.com.ramiralvesmelo.util.commons.interfaces.AuditLogService;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class AuditLogUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Converte o objeto em Map<String,Object>, embala em AuditLogEvent e envia.
     */
    public static void sendAsAuditLog(AuditLogService service, Object sourceObj) {
        if (service == null || sourceObj == null) {
            throw new IllegalArgumentException("AuditLog não enviado: service ou sourceObj nulos");
        }

        try {
            Map<String, Object> payload = MAPPER.convertValue(
                    sourceObj, new TypeReference<Map<String, Object>>() {}
            );

            AuditLogDto event = new AuditLogDto();
            event.setPayload(payload);
            service.send(event);
            log.info("AuditLog enviado. type={}", sourceObj.getClass().getSimpleName());

        } catch (Exception e) {
            log.error("Erro ao enviar AuditLog. type={}", sourceObj.getClass().getName(), e);
        }
    }

}
