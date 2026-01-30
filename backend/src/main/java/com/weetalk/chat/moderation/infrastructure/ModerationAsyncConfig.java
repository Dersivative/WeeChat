package com.weetalk.chat.moderation.infrastructure;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class ModerationAsyncConfig {
	@Bean(name = "moderationTaskExecutor")
	public Executor moderationTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(6);
		executor.setQueueCapacity(200);
		executor.setThreadNamePrefix("moderation-");
		executor.initialize();
		return executor;
	}
}
