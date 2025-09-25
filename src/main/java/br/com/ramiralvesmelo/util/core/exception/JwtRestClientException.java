package br.com.ramiralvesmelo.util.core.exception;

public class JwtRestClientException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public JwtRestClientException(String message) { super(message); }
    public JwtRestClientException(String message, Throwable cause) { super(message, cause); }
}
