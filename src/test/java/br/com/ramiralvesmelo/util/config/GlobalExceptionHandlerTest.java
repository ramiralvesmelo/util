package br.com.ramiralvesmelo.util.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import br.com.ramiralvesmelo.util.core.exception.BusinessException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest req;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn("GET");
        when(req.getRequestURI()).thenReturn("/api/teste");
    }

    private static void assertBodyContem(ResponseEntity<?> resp, HttpStatus esperado, String... trechos) {
        assertEquals(esperado, resp.getStatusCode());
        assertNotNull(resp.getBody());
        String s = resp.getBody().toString(); // ErrorResponse é private; usa toString
        for (String t : trechos) {
            assertTrue(s.contains(t), "Body deveria conter: " + t + " em: " + s);
        }
    }

    @Test
    void illegalArgument_deveRetornar400() {
        var ex = new IllegalArgumentException("bad args");
        var resp = handler.handleIllegalArgument(ex, req);
        assertBodyContem(resp, HttpStatus.BAD_REQUEST, "Bad Request", "bad args", "/api/teste");
    }

    @Test
    void constraintViolation_deveRetornar400() {
        var ex = new ConstraintViolationException("violacao", null);
        var resp = handler.handleConstraintViolation(ex, req);
        assertBodyContem(resp, HttpStatus.BAD_REQUEST, "Parâmetro inválido.");
    }

    @Test
    void notReadable_deveRetornar400() {
        var ex = new HttpMessageNotReadableException("json ruim");
        var resp = handler.handleNotReadable(ex, req);
        assertBodyContem(resp, HttpStatus.BAD_REQUEST, "JSON inválido ou malformado.");
    }

    @Test
    void methodArgumentNotValid_deveRetornar400() {
        var ex = mock(MethodArgumentNotValidException.class);
        when(ex.getMessage()).thenReturn("invalid body");
        var resp = handler.handleMethodArgumentNotValid(ex, req);
        assertBodyContem(resp, HttpStatus.BAD_REQUEST, "Parâmetro inválido.");
    }

    @Test
    void bindErrors_deveRetornar400_comBindException() {
        var ex = new BindException(new Object(), "obj");
        var resp = handler.handleBindErrors(ex, req);
        assertBodyContem(resp, HttpStatus.BAD_REQUEST, "Parâmetro inválido.");
    }

    @Test
    void missingServletRequestParam_deveRetornar400() throws Exception {
        var ex = new MissingServletRequestParameterException("p", "String");
        var resp = handler.handleMissingParam(ex, req);
        assertBodyContem(resp, HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: p");
    }

    @Test
    void notFound_deveRetornar404() {
        var ex = new NoSuchElementException("x");
        var resp = handler.handleNotFound(ex, req);
        assertBodyContem(resp, HttpStatus.NOT_FOUND, "Recurso não encontrado.");
    }

    @Test
    void methodNotAllowed_deveRetornar405() {
        var ex = new HttpRequestMethodNotSupportedException("PATCH");
        var resp = handler.handleMethodOrMedia(ex, req);
        assertBodyContem(resp, HttpStatus.METHOD_NOT_ALLOWED, "PATCH");
    }

    @Test
    void mediaTypeNotSupported_deveRetornar415() {
        var ex = new HttpMediaTypeNotSupportedException("sem suporte");
        var resp = handler.handleMethodOrMedia(ex, req);
        assertBodyContem(resp, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "sem suporte");
    }

    @Test
    void optimisticLockSpring_deveRetornar500() {
        var ex = new OptimisticLockingFailureException("lock");
        var resp = handler.handleOptimisticHibernate(ex, req);
        assertBodyContem(resp, HttpStatus.INTERNAL_SERVER_ERROR, "Registro desatualizado");
    }

    @Test
    void optimisticLockJakarta_deveRetornar500() {
        var ex = new OptimisticLockException("lock-j");
        var resp = handler.handleOptimisticHibernate(ex, req);
        assertBodyContem(resp, HttpStatus.INTERNAL_SERVER_ERROR, "Registro desatualizado");
    }

    @Test
    void businessException_deveRetornar409() {
        var ex = new BusinessException("regra de negocio");
        var resp = handler.handleBusiness(ex, req);
        assertBodyContem(resp, HttpStatus.CONFLICT, "regra de negocio");
    }

    @Test
    void genericException_deveRetornar500() {
        var ex = new Exception("boom");
        var resp = handler.handleGeneric(ex, req);
        assertBodyContem(resp, HttpStatus.INTERNAL_SERVER_ERROR, "boom");
    }
}
