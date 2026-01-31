package com.weetalk.chat.ws;

import com.weetalk.chat.auth.security.AuthCookieService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class WebSocketCookieHandshakeInterceptor implements HandshakeInterceptor {
	private static final String SESSION_ACCESS_TOKEN_KEY = "accessToken";
	private final AuthCookieService authCookieService;

	public WebSocketCookieHandshakeInterceptor(AuthCookieService authCookieService) {
		this.authCookieService = authCookieService;
	}

	@Override
	public boolean beforeHandshake(
		ServerHttpRequest request,
		ServerHttpResponse response,
		WebSocketHandler wsHandler,
		Map<String, Object> attributes
	) {
		if (request instanceof ServletServerHttpRequest servletRequest) {
			HttpServletRequest httpRequest = servletRequest.getServletRequest();
			String token = authCookieService.readCookie(httpRequest, authCookieService.getAccessCookieName());
			if (token != null && !token.isBlank()) {
				attributes.put(SESSION_ACCESS_TOKEN_KEY, token);
			}
		}
		return true;
	}

	@Override
	public void afterHandshake(
		ServerHttpRequest request,
		ServerHttpResponse response,
		WebSocketHandler wsHandler,
		Exception exception
	) {
	}
}
