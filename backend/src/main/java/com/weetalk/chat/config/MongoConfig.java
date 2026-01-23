package com.weetalk.chat.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.UuidRepresentation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class MongoConfig {
	@Bean
	@ConditionalOnMissingBean(MongoClient.class)
	public MongoClient mongoClient(Environment environment) {
		String uri = environment.getProperty("spring.data.mongodb.uri");
		if (uri == null || uri.isBlank()) {
			uri = environment.getProperty("spring.mongodb.uri");
		}
		String configuredDatabase = environment.getProperty("spring.data.mongodb.database",
			environment.getProperty("spring.mongodb.database"));
		if (uri == null || uri.isBlank()) {
			String host = environment.getProperty("spring.data.mongodb.host",
				environment.getProperty("spring.mongodb.host", "localhost"));
			String portValue = environment.getProperty("spring.data.mongodb.port",
				environment.getProperty("spring.mongodb.port", "27017"));
			int port = Integer.parseInt(portValue);
			String database = environment.getProperty("spring.data.mongodb.database",
				environment.getProperty("spring.mongodb.database", "test"));
			String username = environment.getProperty("spring.data.mongodb.username",
				environment.getProperty("spring.mongodb.username"));
			String password = environment.getProperty("spring.data.mongodb.password",
				environment.getProperty("spring.mongodb.password"));
			String authDatabase = environment.getProperty("spring.data.mongodb.authentication-database",
				environment.getProperty("spring.mongodb.authentication-database", "admin"));
			if (username != null && !username.isBlank()) {
				uri = String.format(
					"mongodb://%s:%s@%s:%d/%s?authSource=%s",
					username,
					password == null ? "" : password,
					host,
					port,
					database,
					authDatabase
				);
			} else {
				uri = String.format("mongodb://%s:%d/%s", host, port, database);
			}
		}

		uri = normalizeDatabase(uri, configuredDatabase);

		MongoClientSettings settings = MongoClientSettings.builder()
			.applyConnectionString(new ConnectionString(uri))
			.uuidRepresentation(UuidRepresentation.STANDARD)
			.build();

		return MongoClients.create(settings);
	}

	private String normalizeDatabase(String uri, String database) {
		if (uri == null || uri.isBlank() || database == null || database.isBlank()) {
			return uri;
		}
		int schemeIndex = uri.indexOf("://");
		if (schemeIndex < 0) {
			return uri;
		}
		int queryIndex = uri.indexOf("?", schemeIndex + 3);
		int pathIndex = uri.indexOf("/", schemeIndex + 3);
		int pathEnd = queryIndex >= 0 ? queryIndex : uri.length();
		boolean hasDatabase = pathIndex >= 0 && pathIndex < pathEnd;
		if (!hasDatabase) {
			return uri.substring(0, pathEnd) + "/" + database + uri.substring(pathEnd);
		}
		String currentDatabase = uri.substring(pathIndex + 1, pathEnd);
		if (currentDatabase.isBlank() || "admin".equals(currentDatabase)) {
			return uri.substring(0, pathIndex + 1) + database + uri.substring(pathEnd);
		}
		return uri;
	}
}
