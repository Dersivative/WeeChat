package com.weetalk.chat.moderation.infrastructure;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class PlGuardClientConfig {
	@Bean
	public RestClient plGuardRestClient(
		@Value("${moderation.pl-guard.base-url}") String baseUrl,
		@Value("${moderation.pl-guard.timeout-ms}") long timeoutMs
	) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
		requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));
		return RestClient.builder()
			.baseUrl(baseUrl)
			.requestFactory(requestFactory)
			.build();
	}
}
