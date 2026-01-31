package com.weetalk.chat.auth.security;

import com.weetalk.chat.accounts.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final String TOKEN_TYPE_ACCESS = "access";
	private static final String TOKEN_TYPE_REFRESH = "refresh";

	private final SecretKey secretKey;
	private final Duration accessTokenTtl;
	private final Duration refreshTokenTtl;

	public JwtService(
		@Value("${security.jwt.secret}") String secret,
		@Value("${security.jwt.access-expiration-seconds:300}") long accessExpirationSeconds,
		@Value("${security.jwt.refresh-expiration-seconds:2592000}") long refreshExpirationSeconds
	) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException("JWT secret is not configured");
		}
		try {
			this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		} catch (Exception ex) {
			throw new IllegalStateException("JWT secret is too weak for " + HMAC_ALGORITHM, ex);
		}
		this.accessTokenTtl = Duration.ofSeconds(accessExpirationSeconds);
		this.refreshTokenTtl = Duration.ofSeconds(refreshExpirationSeconds);
	}

	public String generateAccessToken(User user) {
		return generateToken(user.getId(), user.getLogin(), TOKEN_TYPE_ACCESS, accessTokenTtl);
	}

	public String generateRefreshToken(User user) {
		return generateToken(user.getId(), user.getLogin(), TOKEN_TYPE_REFRESH, refreshTokenTtl);
	}

	public String generateAccessToken(UUID accountId, String login) {
		return generateToken(accountId, login, TOKEN_TYPE_ACCESS, accessTokenTtl);
	}

	public String generateRefreshToken(UUID accountId, String login) {
		return generateToken(accountId, login, TOKEN_TYPE_REFRESH, refreshTokenTtl);
	}

	public AuthUserPrincipal parseAccessToken(String token) {
		return parseToken(token, TOKEN_TYPE_ACCESS);
	}

	public AuthUserPrincipal parseRefreshToken(String token) {
		return parseToken(token, TOKEN_TYPE_REFRESH);
	}

	private String generateToken(UUID accountId, String login, String tokenType, Duration ttl) {
		Instant now = Instant.now();
		return Jwts.builder()
			.setSubject(accountId.toString())
			.claim("login", login)
			.claim("token_type", tokenType)
			.setIssuedAt(Date.from(now))
			.setExpiration(Date.from(now.plus(ttl)))
			.signWith(secretKey, SignatureAlgorithm.HS256)
			.compact();
	}

	private AuthUserPrincipal parseToken(String token, String expectedType) {
		try {
			Claims claims = Jwts.parserBuilder()
				.setSigningKey(secretKey)
				.build()
				.parseClaimsJws(token)
				.getBody();
			String subject = claims.getSubject();
			String login = claims.get("login", String.class);
			String tokenType = claims.get("token_type", String.class);
			if (subject == null || subject.isBlank()) {
				throw new BadCredentialsException("Missing token claim: sub");
			}
			if (login == null || login.isBlank()) {
				throw new BadCredentialsException("Missing token claim: login");
			}
			if (!expectedType.equals(tokenType)) {
				throw new BadCredentialsException("Invalid token type");
			}
			return new AuthUserPrincipal(UUID.fromString(subject), login, "");
		} catch (ExpiredJwtException ex) {
			throw new BadCredentialsException("Token expired", ex);
		} catch (JwtException | IllegalArgumentException ex) {
			throw new BadCredentialsException("Invalid token", ex);
		}
	}
}
