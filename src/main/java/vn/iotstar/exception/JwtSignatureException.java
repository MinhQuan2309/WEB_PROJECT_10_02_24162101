package vn.iotstar.exception;

/**
 * Chu ky JWT khong khop voi secret key cua server.
 */
public class JwtSignatureException extends JwtTokenException {

	private static final long serialVersionUID = 1L;

	public JwtSignatureException(String message) {
		super(message);
	}
}
