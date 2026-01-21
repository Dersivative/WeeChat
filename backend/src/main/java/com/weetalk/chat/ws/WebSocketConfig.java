package com.weetalk.chat.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	private final WebSocketAuthChannelInterceptor authChannelInterceptor;

	public WebSocketConfig(WebSocketAuthChannelInterceptor authChannelInterceptor) {
		this.authChannelInterceptor = authChannelInterceptor;
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws")
			.setAllowedOriginPatterns("http://localhost:5173");
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
}
