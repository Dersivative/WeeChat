package com.weetalk.chat.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private final JwtService jwtService;
	private final AuthCookieService authCookieService;

	public JwtAuthenticationFilter(JwtService jwtService, AuthCookieService authCookieService) {
		this.jwtService = jwtService;
		this.authCookieService = authCookieService;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		if (path == null) {
			return false;
		}
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return true;
		}
		// Use contains() so auth endpoints are skipped even with a servlet context path or proxy prefix
		return path.contains("/api/auth/")
			|| path.equals("/api/children/login")
			|| path.endsWith("/api/children/login")
			|| path.contains("/ws/")
			|| path.contains("/v3/api-docs/")
			|| path.contains("/swagger-ui")
			|| path.endsWith("/swagger-ui.html");
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authorization == null || authorization.isBlank()) {
			String cookieToken = authCookieService.readCookie(request, authCookieService.getAccessCookieName());
			if (cookieToken == null || cookieToken.isBlank()) {
				filterChain.doFilter(request, response);
				return;
			}
			authorization = "Bearer " + cookieToken;
		}

		String token = authorization.trim();
		if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
			token = token.substring(7).trim();
		}

		if (token.isEmpty()) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing bearer token");
			return;
		}

		try {
			AuthUserPrincipal principal = jwtService.parseAccessToken(token);
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				principal,
				token,
				principal.getAuthorities()
			);
			SecurityContextHolder.getContext().setAuthentication(authentication);
			filterChain.doFilter(request, response);
		} catch (BadCredentialsException ex) {
			SecurityContextHolder.clearContext();
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
		}
	}
}
