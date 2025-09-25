package br.com.ramiralvesmelo.util.core.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.ramiralvesmelo.util.core.exception.StorageException;

class StorageExceptionTest {

    @Test
    @DisplayName("Deve estender RuntimeException")
    void deveEstenderRuntimeException() {
        assertTrue(RuntimeException.class.isAssignableFrom(StorageException.class));
    }

    @Test
    @DisplayName("Construtor(message) deve armazenar a mensagem e não ter causa")
    void construtorSoMensagem() {
        String msg = "Falha no storage";
        StorageException ex = new StorageException(msg);

        assertEquals(msg, ex.getMessage());
        assertNull(ex.getCause());
        assertTrue(ex.toString().contains(msg));
        assertNotNull(ex.getStackTrace());
    }

    @Test
    @DisplayName("Construtor(message, cause) deve armazenar a mensagem e a causa")
    void construtorMensagemECausa() {
        String msg = "Erro ao enviar arquivo";
        Throwable cause = new IllegalStateException("estado inválido");

        StorageException ex = new StorageException(msg, cause);

        assertEquals(msg, ex.getMessage());
        assertSame(cause, ex.getCause());
        assertTrue(ex.toString().contains("StorageException"));
        assertTrue(ex.toString().contains(msg));
    }

    @Test
    @DisplayName("serialVersionUID deve existir e ser 1L")
    void serialVersionUID() throws Exception {
        Field f = StorageException.class.getDeclaredField("serialVersionUID");
        f.setAccessible(true);
        assertEquals(1L, f.getLong(null));
    }

    @Test
    @DisplayName("Lançamento e captura funcionam como esperado")
    void lancarECapturar() {
        String msg = "Erro genérico";
        try {
            throw new StorageException(msg);
        } catch (StorageException ex) {
            assertEquals(msg, ex.getMessage());
        }
    }
}
