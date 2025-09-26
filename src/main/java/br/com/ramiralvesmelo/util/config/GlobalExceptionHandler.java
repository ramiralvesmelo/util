package br.com.ramiralvesmelo.util.config;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import br.com.ramiralvesmelo.util.core.exception.BusinessException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class GlobalExceptionHandler {
	
	private static final String INVALID_PARAMETER_MESSAGE = "Parâmetro inválido.";

    private record ErrorResponse(
            String timestamp,
            int status,
            String error,
            String message,
            String path
    ) {
        public ErrorResponse(HttpStatus status, String message, String path) {
            this(LocalDateTime.now().toString(),
                 status.value(),
                 status.getReasonPhrase(),
                 message,
                 path);
        }
    }

    // 400 - parâmetros inválidos
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        HttpStatus st = HttpStatus.BAD_REQUEST;
        log.error("[400] {} {} -> {}: {}", req.getMethod(), req.getRequestURI(),
                ex.getClass().getSimpleName(), ex.getMessage());
        return ResponseEntity.status(st).body(new ErrorResponse(st, ex.getMessage(), req.getRequestURI()));
    }

    // 400 - bean validation em params
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest req) {
        HttpStatus st = HttpStatus.BAD_REQUEST;
        log.error("[400] {} {} -> {}: {}", req.getMethod(), req.getRequestURI(),
                ex.getClass().getSimpleName(), ex.getMessage());
        return ResponseEntity.status(st).body(new ErrorResponse(st, INVALID_PARAMETER_MESSAGE, req.getRequestURI()));
    }

    // 400 - JSON inválido
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex,
                                                           HttpServletRequest req) {
        HttpStatus st = HttpStatus.BAD_REQUEST;
        log.error("[400] {} {} -> {}: {}", req.getMethod(), req.getRequestURI(),
                ex.getClass().getSimpleName(), ex.getMessage());
        return ResponseEntity.status(st).body(new ErrorResponse(st, "JSON inválido ou malformado.", req.getRequestURI()));
    }

    // 400 - @Valid em body
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                      HttpServletRequest req) {
        HttpStatus st = HttpStatus.BAD_REQUEST;
        log.error("[400] {} {} -> {}: {}", req.getMethod(), req.getRequestURI(),
                ex.getClass().getSimpleName(), ex.getMessage());
        return ResponseEntity.status(st).body(new ErrorResponse(st, INVALID_PARAMETER_MESSAGE, req.getRequestURI()));
    }

    // 400 - bind em query/path (mismatch OU BindException)
    @ExceptionHandler({ BindException.class, MethodArgumentTypeMismatchException.class })
    public ResponseEntity<ErrorResponse> handleBindErrors(Exception ex, HttpServletRequest req) {
        HttpStatus st = HttpStatus.BAD_REQUEST;
        log.error("[400] {} {} -> {}: {}", req.getMethod(), req.getRequestURI(),
                ex.getClass().getSimpleName(), ex.getMessage());
        return ResponseEntity.status(st).body(new ErrorResponse(st, INVALID_PARAMETER_MESSAGE, req.getRequestURI()));
    }

    // 400 - parâmetro obrigatório ausente
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
                                                            HttpServletRequest req) {
        HttpStatus st = HttpStatus.BAD_REQUEST;
        log.error("[400] {} {} -> missing param '{}'", req.getMethod(), req.getRequestURI(), ex.getParameterName());
        return ResponseEntity.status(st)
                .body(new ErrorResponse(st, "Parâmetro obrigatório ausente: " + ex.getParameterName(), req.getRequestURI()));
    }

    // 404 - Recurso não encontrado
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex, HttpServletRequest req) {
        HttpStatus st = HttpStatus.NOT_FOUND;
        log.error("[404] {} {} -> {}: {}", req.getMethod(), req.getRequestURI(),
                ex.getClass().getSimpleName(), ex.getMessage());
        return ResponseEntity.status(st).body(new ErrorResponse(st, "Recurso não encontrado.", req.getRequestURI()));
    }

    // 405 / 415
    @ExceptionHandler({ HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class })
    public ResponseEntity<ErrorResponse> handleMethodOrMedia(Exception ex, HttpServletRequest req) {
        HttpStatus st = (ex instanceof HttpRequestMethodNotSupportedException)
                ? HttpStatus.METHOD_NOT_ALLOWED
                : HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        log.error("[{}] {} {} -> {}: {}", st.value(), req.getMethod(), req.getRequestURI(),
                ex.getClass().getSimpleName(), ex.getMessage());
        return ResponseEntity.status(st).body(new ErrorResponse(st, ex.getMessage(), req.getRequestURI()));
    }

    // 500 - optimistic locking (mantém 500 para não quebrar testes)
    @ExceptionHandler({ OptimisticLockingFailureException.class, OptimisticLockException.class })
    public ResponseEntity<ErrorResponse> handleOptimisticHibernate(Exception ex, HttpServletRequest req) {
        HttpStatus st = HttpStatus.INTERNAL_SERVER_ERROR; // altere para CONFLICT se um dia quiser 409
        String msg = "Registro desatualizado. Recarregue os dados e tente novamente.";
        log.error("[{}] {} {} -> {}: {}", st.value(), req.getMethod(), req.getRequestURI(),
                ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return ResponseEntity.status(st).body(new ErrorResponse(st, msg, req.getRequestURI()));
    }

    // 409 - Erro de negócio
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest req) {
        HttpStatus st = HttpStatus.CONFLICT;
        log.error("[409] {} {} -> Business exception", req.getMethod(), req.getRequestURI(), ex);
        return ResponseEntity.status(st).body(new ErrorResponse(st, ex.getMessage(), req.getRequestURI()));
    }

    // 500 - genérico
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        HttpStatus st = HttpStatus.INTERNAL_SERVER_ERROR;
        log.error("[500] {} {} -> Unhandled exception", req.getMethod(), req.getRequestURI(), ex);
        return ResponseEntity.status(st).body(new ErrorResponse(st, ex.getMessage(), req.getRequestURI()));
    }
}
