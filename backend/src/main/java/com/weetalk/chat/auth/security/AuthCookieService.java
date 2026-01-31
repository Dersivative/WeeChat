package com.weetalk.chat.auth.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class AuthCookieService {
	private final String accessCookieName;
	private final String refreshCookieName;
	private final boolean cookieSecure;
	private final String cookieSameSite;
	private final String cookieDomain;

	public AuthCookieService(
		@Value("${security.jwt.cookie.access-name:weechat_access}") String accessCookieName,
		@Value("${security.jwt.cookie.refresh-name:weechat_refresh}") String refreshCookieName,
		@Value("${security.jwt.cookie.secure:false}") boolean cookieSecure,
		@Value("${security.jwt.cookie.same-site:Lax}") String cookieSameSite,
		@Value("${security.jwt.cookie.domain:}") String cookieDomain
	) {
		this.accessCookieName = accessCookieName;
		this.refreshCookieName = refreshCookieName;
		this.cookieSecure = cookieSecure;
		this.cookieSameSite = cookieSameSite;
		this.cookieDomain = cookieDomain;
	}

	public String getAccessCookieName() {
		return accessCookieName;
	}

	public String getRefreshCookieName() {
		return refreshCookieName;
	}

	public ResponseCookie buildAccessCookie(String token, long maxAgeSeconds) {
		return buildCookie(accessCookieName, token, Duration.ofSeconds(maxAgeSeconds));
	}

	public ResponseCookie buildRefreshCookie(String token, long maxAgeSeconds) {
		return buildCookie(refreshCookieName, token, Duration.ofSeconds(maxAgeSeconds));
	}

	public ResponseCookie clearAccessCookie() {
		return clearCookie(accessCookieName);
	}

	public ResponseCookie clearRefreshCookie() {
		return clearCookie(refreshCookieName);
	}

	public String readCookie(HttpServletRequest request, String name) {
		if (request == null || request.getCookies() == null) {
			return null;
		}
		for (Cookie cookie : request.getCookies()) {
			if (cookie != null && name.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	private ResponseCookie buildCookie(String name, String value, Duration maxAge) {
		ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
			.path("/")
			.httpOnly(true)
			.secure(cookieSecure)
			.sameSite(cookieSameSite)
			.maxAge(maxAge);
		if (cookieDomain != null && !cookieDomain.isBlank()) {
			builder.domain(cookieDomain);
		}
		return builder.build();
	}

	private ResponseCookie clearCookie(String name) {
		ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, "")
			.path("/")
			.httpOnly(true)
			.secure(cookieSecure)
			.sameSite(cookieSameSite)
			.maxAge(Duration.ZERO);
		if (cookieDomain != null && !cookieDomain.isBlank()) {
			builder.domain(cookieDomain);
		}
		return builder.build();
	}
}
