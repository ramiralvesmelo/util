package br.com.ramiralvesmelo.util.commons.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class DocumentDtoTest {

    @Test
    void noArgsESettersGetters() {
        DocumentDto dto = new DocumentDto();
        dto.setId(1L);
        dto.setFilename("file.pdf");
        dto.setContentType("application/pdf");
        dto.setSizeBytes(123L);
        dto.setUrl("http://x");
        dto.setDescription("desc");
        dto.setAvailable(true);
        dto.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
        dto.setUpdatedAt(LocalDateTime.of(2024, 1, 2, 11, 0));
        dto.setOrderId(99L);
        dto.setMessage("ok");

        assertEquals(1L, dto.getId());
        assertEquals("file.pdf", dto.getFilename());
        assertEquals("application/pdf", dto.getContentType());
        assertEquals(123L, dto.getSizeBytes());
        assertEquals("http://x", dto.getUrl());
        assertEquals("desc", dto.getDescription());
        assertTrue(dto.isAvailable());
        assertEquals(LocalDateTime.of(2024, 1, 1, 10, 0), dto.getCreatedAt());
        assertEquals(LocalDateTime.of(2024, 1, 2, 11, 0), dto.getUpdatedAt());
        assertEquals(99L, dto.getOrderId());
        assertEquals("ok", dto.getMessage());
    }

    @Test
    void allArgsConstructor() {
        DocumentDto dto = new DocumentDto(
                2L, "a.txt", "text/plain", 10L, "http://y", "d", false,
                LocalDateTime.of(2023, 5, 1, 1, 1),
                LocalDateTime.of(2023, 5, 2, 2, 2),
                7L, "m"
        );

        assertEquals(2L, dto.getId());
        assertEquals("a.txt", dto.getFilename());
        assertEquals("text/plain", dto.getContentType());
        assertEquals(10L, dto.getSizeBytes());
        assertEquals("http://y", dto.getUrl());
        assertEquals("d", dto.getDescription());
        assertFalse(dto.isAvailable());
        assertEquals(LocalDateTime.of(2023, 5, 1, 1, 1), dto.getCreatedAt());
        assertEquals(LocalDateTime.of(2023, 5, 2, 2, 2), dto.getUpdatedAt());
        assertEquals(7L, dto.getOrderId());
        assertEquals("m", dto.getMessage());
    }

    @Test
    void builder() {
        DocumentDto dto = DocumentDto.builder()
                .id(3L)
                .filename("z.csv")
                .contentType("text/csv")
                .sizeBytes(44L)
                .url("http://z")
                .description("csv")
                .available(true)
                .orderId(55L)
                .message("ok")
                .build();

        assertEquals(3L, dto.getId());
        assertEquals("z.csv", dto.getFilename());
        assertEquals("text/csv", dto.getContentType());
        assertEquals(44L, dto.getSizeBytes());
        assertEquals("http://z", dto.getUrl());
        assertEquals("csv", dto.getDescription());
        assertTrue(dto.isAvailable());
        assertEquals(55L, dto.getOrderId());
        assertEquals("ok", dto.getMessage());
    }

    @Test
    void equalsHashCodeToString() {
        DocumentDto a = DocumentDto.builder().id(1L).filename("f").build();
        DocumentDto b = DocumentDto.builder().id(1L).filename("f").build();
        DocumentDto c = DocumentDto.builder().id(2L).filename("f").build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);

        String s = a.toString();
        assertTrue(s.contains("id=1"));
        assertTrue(s.contains("filename"));
    }
}
