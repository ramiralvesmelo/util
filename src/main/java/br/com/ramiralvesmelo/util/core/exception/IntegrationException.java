package br.com.ramiralvesmelo.util.core.exception;

import org.springframework.http.HttpStatus;

public class IntegrationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final HttpStatus status;

    public IntegrationException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public IntegrationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    // atalhos úteis
    public static IntegrationException conflict(String msg) { 
        return new IntegrationException(msg, HttpStatus.CONFLICT); 
    }
    public static IntegrationException notFound(String msg) { 
        return new IntegrationException(msg, HttpStatus.NOT_FOUND); 
    }
}
