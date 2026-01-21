package com.weetalk.chat.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.UuidRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoConfig {
	@Bean
	public MongoClient mongoClient(
		@Value("${spring.mongodb.host}") String host,
		@Value("${spring.mongodb.port}") int port,
		@Value("${spring.mongodb.username}") String username,
		@Value("${spring.mongodb.password}") String password,
		@Value("${spring.mongodb.database}") String database,
		@Value("${spring.mongodb.authentication-database}") String authDatabase
	) {
		String uri = String.format(
			"mongodb://%s:%s@%s:%d/%s?authSource=%s",
			username,
			password,
			host,
			port,
			database,
			authDatabase
		);

		MongoClientSettings settings = MongoClientSettings.builder()
			.applyConnectionString(new ConnectionString(uri))
			.uuidRepresentation(UuidRepresentation.STANDARD)
			.build();

		return MongoClients.create(settings);
	}
}
