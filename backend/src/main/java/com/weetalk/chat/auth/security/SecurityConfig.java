package com.weetalk.chat.auth.security;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import com.weetalk.chat.config.RequestLoggingFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final RequestLoggingFilter requestLoggingFilter;
	@Value("${security.cors.allowed-origins:}")
	private String corsAllowedOrigins;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, RequestLoggingFilter requestLoggingFilter) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.requestLoggingFilter = requestLoggingFilter;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			.csrf(csrf -> csrf.disable())
			.cors(Customizer.withDefaults())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.requestMatchers("/", "/index.html", "/vite.svg", "/assets/**").permitAll()
				.requestMatchers("/api/auth/**").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/children/login").permitAll()
				.requestMatchers("/ws/**").permitAll()
				.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
				.requestMatchers(request -> "GET".equals(request.getMethod())
					&& request.getRequestURI() != null
					&& !request.getRequestURI().startsWith("/api"))
					.permitAll()
				.anyRequest().authenticated()
			)
			.addFilterBefore(requestLoggingFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration cfg = new CorsConfiguration();
		cfg.setAllowedOrigins(resolveAllowedOrigins());
		cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
		cfg.setAllowedHeaders(List.of("Authorization","Content-Type"));
		cfg.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
		src.registerCorsConfiguration("/**", cfg);
		return src;
	}

	private List<String> resolveAllowedOrigins() {
		if (corsAllowedOrigins == null || corsAllowedOrigins.isBlank()) {
			return List.of("http://localhost:5173", "http://localhost:3000");
		}
		return Arrays.stream(corsAllowedOrigins.split(","))
			.map(String::trim)
			.filter(value -> !value.isEmpty())
			.collect(Collectors.toList());
	}


	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}
}
