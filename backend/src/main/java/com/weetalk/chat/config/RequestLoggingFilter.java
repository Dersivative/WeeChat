package com.weetalk.chat.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String method = request.getMethod();
		String uri = request.getRequestURI();
		if (request.getQueryString() != null) {
			uri = uri + "?" + request.getQueryString();
		}
		log.info(">>> {} {}", method, uri);
		try {
			filterChain.doFilter(request, response);
		} finally {
			log.info("<<< {} {} -> {}", method, request.getRequestURI(), response.getStatus());
		}
	}
}
