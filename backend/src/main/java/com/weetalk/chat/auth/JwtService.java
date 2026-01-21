package com.weetalk.chat.auth;

import com.weetalk.chat.domain.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
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
	private final ObjectMapper objectMapper;
	private final Base64.Encoder base64UrlEncoder;
	private final Base64.Decoder base64UrlDecoder;

	public JwtService(
		@Value("${security.jwt.secret}") String secret,
		@Value("${security.jwt.access-expiration-seconds:300}") long accessExpirationSeconds,
		@Value("${security.jwt.refresh-expiration-seconds:2592000}") long refreshExpirationSeconds,
		ObjectMapper objectMapper
	) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException("JWT secret is not configured");
		}
		this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
		this.accessTokenTtl = Duration.ofSeconds(accessExpirationSeconds);
		this.refreshTokenTtl = Duration.ofSeconds(refreshExpirationSeconds);
		this.objectMapper = objectMapper;
		this.base64UrlEncoder = Base64.getUrlEncoder().withoutPadding();
		this.base64UrlDecoder = Base64.getUrlDecoder();
	}

	public String generateAccessToken(User user) {
		return generateToken(user, TOKEN_TYPE_ACCESS, accessTokenTtl);
	}

	public String generateRefreshToken(User user) {
		return generateToken(user, TOKEN_TYPE_REFRESH, refreshTokenTtl);
	}

	public AuthUserPrincipal parseAccessToken(String token) {
		return parseToken(token, TOKEN_TYPE_ACCESS);
	}

	public AuthUserPrincipal parseRefreshToken(String token) {
		return parseToken(token, TOKEN_TYPE_REFRESH);
	}

	private String generateToken(User user, String tokenType, Duration ttl) {
		Instant now = Instant.now();
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("sub", user.getId().toString());
		payload.put("login", user.getLogin());
		payload.put("iat", now.getEpochSecond());
		payload.put("exp", now.plus(ttl).getEpochSecond());
		payload.put("token_type", tokenType);
		return sign(payload);
	}

	private AuthUserPrincipal parseToken(String token, String expectedType) {
		String[] parts = token.split("\\.");
		if (parts.length != 3) {
			throw new BadCredentialsException("Invalid token format");
		}

		String signingInput = parts[0] + "." + parts[1];
		byte[] signature = decodeBase64(parts[2]);
		byte[] expectedSignature = hmac(signingInput);
		if (!MessageDigest.isEqual(expectedSignature, signature)) {
			throw new BadCredentialsException("Invalid token signature");
		}

		JsonNode payload = readJson(decodeBase64(parts[1]));
		String subject = getTextClaim(payload, "sub");
		String login = getTextClaim(payload, "login");
		String tokenType = getTextClaim(payload, "token_type");
		if (!expectedType.equals(tokenType)) {
			throw new BadCredentialsException("Invalid token type");
		}
		long exp = payload.path("exp").asLong(0);
		if (exp == 0 || Instant.now().isAfter(Instant.ofEpochSecond(exp))) {
			throw new BadCredentialsException("Token expired");
		}

		UUID accountId = UUID.fromString(subject);
		return new AuthUserPrincipal(accountId, login, "");
	}

	private String sign(Map<String, Object> payload) {
		Map<String, Object> header = new LinkedHashMap<>();
		header.put("alg", "HS256");
		header.put("typ", "JWT");

		String headerJson = writeJson(header);
		String payloadJson = writeJson(payload);
		String headerPart = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
		String payloadPart = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
		String signingInput = headerPart + "." + payloadPart;
		String signature = base64UrlEncode(hmac(signingInput));
		return signingInput + "." + signature;
	}

	private byte[] hmac(String data) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(secretKey);
			return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
		} catch (Exception ex) {
			throw new IllegalStateException("Unable to sign token", ex);
		}
	}

	private byte[] decodeBase64(String data) {
		try {
			return base64UrlDecoder.decode(data);
		} catch (IllegalArgumentException ex) {
			throw new BadCredentialsException("Invalid token encoding", ex);
		}
	}

	private String getTextClaim(JsonNode payload, String claim) {
		String value = payload.path(claim).asText(null);
		if (value == null || value.isBlank()) {
			throw new BadCredentialsException("Missing token claim: " + claim);
		}
		return value;
	}

	private String writeJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (Exception ex) {
			throw new IllegalStateException("Unable to serialize token", ex);
		}
	}

	private JsonNode readJson(byte[] bytes) {
		try {
			return objectMapper.readTree(bytes);
		} catch (Exception ex) {
			throw new BadCredentialsException("Invalid token payload", ex);
		}
	}

	private String base64UrlEncode(byte[] bytes) {
		return base64UrlEncoder.encodeToString(bytes);
	}
}
