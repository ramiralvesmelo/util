package br.com.ramiralvesmelo.util.core.exception;

public class PdfException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public PdfException(String message) { super(message); }
    public PdfException(String message, Throwable cause) { super(message, cause); }
}
