package br.com.ramiralvesmelo.util.commons.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

class AuditLogDtoTest {

    @Test
    void noArgsESettersGetters() {
        AuditLogDto dto = new AuditLogDto();
        dto.setId("abc");
        dto.setPayload(Map.of("k", 1));

        assertEquals("abc", dto.getId());
        assertEquals(1, dto.getPayload().get("k"));
    }

    @Test
    void allArgsConstructor() {
        AuditLogDto dto = new AuditLogDto("x", Map.of("a", "b"));
        assertEquals("x", dto.getId());
        assertEquals("b", dto.getPayload().get("a"));
    }

    @Test
    void builder() {
        AuditLogDto dto = AuditLogDto.builder()
                .id("id-1")
                .payload(Map.of("p", 10))
                .build();

        assertEquals("id-1", dto.getId());
        assertEquals(10, dto.getPayload().get("p"));
    }

    @Test
    void equalsHashCodeToString() {
        AuditLogDto a = AuditLogDto.builder()
                .id("same")
                .payload(Map.of("k", "v"))
                .build();

        AuditLogDto b = AuditLogDto.builder()
                .id("same")
                .payload(Map.of("k", "v"))
                .build();

        AuditLogDto c = AuditLogDto.builder()
                .id("other")
                .payload(Map.of("k", "v"))
                .build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);

        String s = a.toString();
        assertTrue(s.contains("same"));
        assertTrue(s.contains("payload"));
    }
}
