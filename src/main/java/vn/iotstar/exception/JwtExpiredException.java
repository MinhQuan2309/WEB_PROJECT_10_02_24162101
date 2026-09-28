package vn.iotstar.exception;

/**
 * JWT da qua thoi gian het han (claim exp).
 */
public class JwtExpiredException extends JwtTokenException {

	private static final long serialVersionUID = 1L;

	public JwtExpiredException(String message) {
		super(message);
	}
}
