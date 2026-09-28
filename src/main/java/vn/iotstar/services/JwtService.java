package vn.iotstar.services;

import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import vn.iotstar.exception.JwtExpiredException;
import vn.iotstar.exception.JwtSignatureException;
import vn.iotstar.exception.JwtTokenException;

/**
 * Tao va xac thuc JWT bang thu vien Nimbus JOSE + JWT (thay cho jjwt).
 *
 * jjwt                          -> Nimbus
 * Jwts.builder()                -> JWTClaimsSet.Builder + SignedJWT
 * signWith(key, Jwts.SIG.HS256) -> signedJWT.sign(new MACSigner(key))
 * Jwts.parser().verifyWith(key) -> SignedJWT.parse(token) + verify(new MACVerifier(key))
 * Claims                        -> JWTClaimsSet
 */
@Service
public class JwtService {
	@Value("${security.jwt.secret-key}")
	private String secretKey;

	@Value("${security.jwt.expiration-time}")
	private long jwtExpiration;

	public String extractUsername(String token) {
		return extractClaim(token, JWTClaimsSet::getSubject);
	}

	public <T> T extractClaim(String token, Function<JWTClaimsSet, T> claimsResolver) {
		final JWTClaimsSet claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	public String generateToken(UserDetails userDetails) {
		return generateToken(new HashMap<>(), userDetails);
	}

	public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
		return buildToken(extraClaims, userDetails, jwtExpiration);
	}

	public long getExpirationTime() {
		return jwtExpiration;
	}

	private String buildToken(
			Map<String, Object> extraClaims,
			UserDetails userDetails,
			long expiration
	) {
		// 1. Tao claims (payload)
		JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder();
		extraClaims.forEach(claimsBuilder::claim);

		JWTClaimsSet claimsSet = claimsBuilder
				.subject(userDetails.getUsername())
				.issueTime(new Date(System.currentTimeMillis()))
				.expirationTime(new Date(System.currentTimeMillis() + expiration))
				.build();

		// 2. Tao header: {"alg":"HS256","typ":"JWT"}
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
				.type(JOSEObjectType.JWT)
				.build();

		// 3. Tao SignedJWT va ky bang secret key (HMAC)
		SignedJWT signedJWT = new SignedJWT(header, claimsSet);
		try {
			signedJWT.sign(new MACSigner(getSignInKey()));
		} catch (JOSEException e) {
			throw new IllegalStateException("Khong the ky JWT", e);
		}

		// 4. Serialize ra chuoi header.payload.signature
		return signedJWT.serialize();
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		final String username = extractUsername(token);
		return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
	}

	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	private Date extractExpiration(String token) {
		return extractClaim(token, JWTClaimsSet::getExpirationTime);
	}

	private JWTClaimsSet extractAllClaims(String token) {
		final SignedJWT signedJWT;
		try {
			signedJWT = SignedJWT.parse(token);
		} catch (ParseException e) {
			throw new JwtTokenException("Invalid compact JWT string: " + e.getMessage(), e);
		}

		try {
			// chi chap nhan dung thuat toan HS256 (chan tan cong doi alg)
			if (!JWSAlgorithm.HS256.equals(signedJWT.getHeader().getAlgorithm())
					|| !signedJWT.verify(new MACVerifier(getSignInKey()))) {
				throw new JwtSignatureException(
						"JWT signature does not match locally computed signature. JWT validity cannot be asserted and should not be trusted.");
			}

			JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

			// jjwt tu nem ExpiredJwtException khi parse, Nimbus thi phai tu kiem tra exp
			Date expiration = claims.getExpirationTime();
			if (expiration == null || expiration.before(new Date())) {
				throw new JwtExpiredException("JWT expired at " + expiration + ". Current time: " + new Date());
			}
			return claims;
		} catch (JOSEException | ParseException e) {
			throw new JwtTokenException(e.getMessage(), e);
		}
	}

	private SecretKey getSignInKey() {
		byte[] keyBytes = Base64.getDecoder().decode(secretKey);
		return new SecretKeySpec(keyBytes, "HmacSHA256");
	}
}
