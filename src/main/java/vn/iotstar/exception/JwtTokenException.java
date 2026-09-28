package vn.iotstar.exception;

/**
 * JWT khong hop le (sai dinh dang, khong doc duoc claims...).
 * Dong vai tro nhu io.jsonwebtoken.JwtException ben jjwt.
 */
public class JwtTokenException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public JwtTokenException(String message) {
		super(message);
	}

	public JwtTokenException(String message, Throwable cause) {
		super(message, cause);
	}
}
