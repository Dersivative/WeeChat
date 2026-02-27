package com.weetalk.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.Arrays;

@Configuration
public class CorsConfig {
	@Value("${security.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
	private String corsAllowedOrigins;

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				String[] origins = Arrays.stream(corsAllowedOrigins.split(","))
					.map(String::trim)
					.filter(value -> !value.isEmpty())
					.toArray(String[]::new);

				registry.addMapping("/**")
					.allowedOrigins(origins)
					.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
					.allowedHeaders("*")
					.allowCredentials(true);
			}
		};
	}
}
