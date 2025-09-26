package br.com.ramiralvesmelo.util.audit.publisher;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.ramiralvesmelo.util.commons.dto.AuditLogDto;
import br.com.ramiralvesmelo.util.commons.interfaces.AuditLogStoreService;

@ExtendWith(MockitoExtension.class)
class AuditLogPublisherTest {

    @Mock
    private AuditLogStoreService service;

    // ======= Validações de parâmetros =======

    @Test
    void publishAuditLog_deveLancarQuandoServiceNulo() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> AuditLogPublisher.publishAuditLog(null, new Object()));
        assertTrue(ex.getMessage().contains("service ou sourceObj nulos"));
        verifyNoInteractions(service);
    }

    @Test
    void publishAuditLog_deveLancarQuandoSourceNulo() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> AuditLogPublisher.publishAuditLog(service, null));
        assertTrue(ex.getMessage().contains("service ou sourceObj nulos"));
        verifyNoMoreInteractions(service);
    }

    // ======= Fluxo de sucesso =======

    @Test
    void publishAuditLog_deveConverterMapEEnviar() {
        // given: um Map simples como source (evita criar DTO só para teste)
        Map<String, Object> source = new HashMap<>();
        source.put("id", 123);
        source.put("nome", "teste");

        ArgumentCaptor<AuditLogDto> captor = ArgumentCaptor.forClass(AuditLogDto.class);
        doNothing().when(service).send(any(AuditLogDto.class));

        // when
        AuditLogPublisher.publishAuditLog(service, source);

        // then
        verify(service, times(1)).send(captor.capture());
        AuditLogDto enviado = captor.getValue();
        assertNotNull(enviado, "AuditLogDto não deve ser nulo");
        assertNotNull(enviado.getPayload(), "Payload não deve ser nulo");
        assertEquals(2, enviado.getPayload().size(), "Payload deve conter os mesmos itens do source");
        assertEquals(123, enviado.getPayload().get("id"));
        assertEquals("teste", enviado.getPayload().get("nome"));

        verifyNoMoreInteractions(service);
    }

    // ======= Fluxo de erro (exceção durante envio) =======

    @Test
    void publishAuditLog_quandoSendLancarExcecao_deveApenasLogarSemPropagar() {
        Map<String, Object> source = Map.of("k", "v");

        // Força uma RuntimeException dentro do bloco try (após conversão)
        doThrow(new RuntimeException("falha ao enviar"))
            .when(service).send(any(AuditLogDto.class));

        assertDoesNotThrow(() -> AuditLogPublisher.publishAuditLog(service, source));

        // Mesmo tendo ocorrido falha, método foi chamado
        verify(service, times(1)).send(any(AuditLogDto.class));
        verifyNoMoreInteractions(service);
    }

    // ======= Cobertura do construtor privado do @UtilityClass =======

    @Test
    void constructor_privadoDeUtilityClass_deveLancarUnsupportedOperationException() throws Exception {
        Constructor<?> ctor = AuditLogPublisher.class.getDeclaredConstructor();
        assertTrue(ctor.canAccess(null) == false);
        ctor.setAccessible(true);
        try {
            ctor.newInstance();
            fail("Esperava UnsupportedOperationException ao instanciar UtilityClass");
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            assertTrue(cause instanceof UnsupportedOperationException,
                "Causa esperada: UnsupportedOperationException");
        }
    }
}
