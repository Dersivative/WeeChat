package com.weetalk.chat.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	private final WebSocketAuthChannelInterceptor authChannelInterceptor;
	private final WebSocketCookieHandshakeInterceptor cookieHandshakeInterceptor;
	@Value("${security.cors.allowed-origins:}")
	private String corsAllowedOrigins;

	public WebSocketConfig(
		WebSocketAuthChannelInterceptor authChannelInterceptor,
		WebSocketCookieHandshakeInterceptor cookieHandshakeInterceptor
	) {
		this.authChannelInterceptor = authChannelInterceptor;
		this.cookieHandshakeInterceptor = cookieHandshakeInterceptor;
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws")
			.setAllowedOriginPatterns(resolveAllowedOriginPatterns())
			.addInterceptors(cookieHandshakeInterceptor);
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.setApplicationDestinationPrefixes("/app");
		registry.enableSimpleBroker("/topic", "/queue");
		registry.setUserDestinationPrefix("/user");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(authChannelInterceptor);
	}

	private String[] resolveAllowedOriginPatterns() {
		if (corsAllowedOrigins == null || corsAllowedOrigins.isBlank()) {
			return new String[0];
		}
		return Arrays.stream(corsAllowedOrigins.split(","))
			.map(String::trim)
			.filter(value -> !value.isEmpty())
			.toArray(String[]::new);
	}
}
