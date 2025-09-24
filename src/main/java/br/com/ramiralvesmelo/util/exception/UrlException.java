package br.com.ramiralvesmelo.util.exception;

public class UrlException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public UrlException(String message) { super(message); }
    public UrlException(String message, Throwable cause) { super(message, cause); }
}
