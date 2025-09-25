package br.com.ramiralvesmelo.util.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final HttpStatus status;

    public BusinessException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    // atalhos úteis
    public static BusinessException conflict(String msg) { 
        return new BusinessException(msg, HttpStatus.CONFLICT); 
    }
    public static BusinessException notFound(String msg) { 
        return new BusinessException(msg, HttpStatus.NOT_FOUND); 
    }
}
