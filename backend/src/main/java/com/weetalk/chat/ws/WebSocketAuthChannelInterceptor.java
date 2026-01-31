package com.weetalk.chat.ws;

import com.weetalk.chat.auth.security.AuthUserPrincipal;
import com.weetalk.chat.auth.security.JwtService;
import java.util.List;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {
	private static final String SESSION_ACCESS_TOKEN_KEY = "accessToken";
	private final JwtService jwtService;

	public WebSocketAuthChannelInterceptor(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null) {
			return message;
		}

		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			String token = resolveToken(accessor);
			AuthUserPrincipal userDetails = jwtService.parseAccessToken(token);
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				userDetails,
				token,
				userDetails.getAuthorities()
			);
			accessor.setUser(authentication);
		}

		return message;
	}

	private String getFirstNativeHeader(StompHeaderAccessor accessor, String name) {
		List<String> values = accessor.getNativeHeader(name);
		if (values == null || values.isEmpty()) {
			return null;
		}
		return values.get(0);
	}

	private String resolveToken(StompHeaderAccessor accessor) {
		String authorization = getFirstNativeHeader(accessor, "Authorization");
		if (authorization == null) {
			authorization = getFirstNativeHeader(accessor, "authorization");
		}
		if (authorization == null || authorization.isBlank()) {
			Object sessionToken = accessor.getSessionAttributes() == null
				? null
				: accessor.getSessionAttributes().get(SESSION_ACCESS_TOKEN_KEY);
			if (sessionToken instanceof String tokenValue && !tokenValue.isBlank()) {
				return tokenValue;
			}
			throw new BadCredentialsException("Missing Authorization header");
		}

		String token = authorization.trim();
		if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
			token = token.substring(7).trim();
		}
		if (token.isEmpty()) {
			throw new BadCredentialsException("Missing bearer token");
		}
		return token;
	}
}
