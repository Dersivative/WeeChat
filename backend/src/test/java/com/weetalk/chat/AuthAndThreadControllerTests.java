package com.weetalk.chat;

import com.weetalk.chat.accounts.domain.User;
import com.weetalk.chat.accounts.infrastructure.UserRepository;
import com.weetalk.chat.media.MediaUrlResolver;
import com.weetalk.chat.threads.infrastructure.mongo.ThreadDoc;
import com.weetalk.chat.threads.infrastructure.mongo.ThreadMember;
import com.weetalk.chat.threads.infrastructure.mongo.ThreadRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthAndThreadControllerTests {
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ThreadRepository threadRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private MediaUrlResolver mediaUrlResolver;
	private RestTemplate restTemplate;
	@LocalServerPort
	private int port;

	private User alice;
	private User bob;

	@BeforeEach
	void setUp() {
		restTemplate = new RestTemplate();
		threadRepository.deleteAll();
		userRepository.deleteAll();

		alice = new User();
		alice.setLogin("alice");
		alice.setDisplayName("Alice");
		alice.setPasswordHash(passwordEncoder.encode("secret"));
		alice.setTwoFactorEnabled(false);
		alice.setAvatarFileName("alice.png");
		alice = userRepository.save(alice);

		bob = new User();
		bob.setLogin("bob");
		bob.setDisplayName("Bob");
		bob.setPasswordHash(passwordEncoder.encode("password"));
		bob.setTwoFactorEnabled(false);
		bob = userRepository.save(bob);

		ThreadDoc aliceThread = new ThreadDoc();
		aliceThread.setCreatedAt(Instant.parse("2026-01-20T10:00:00Z"));
		aliceThread.setLastMessageAt(Instant.parse("2026-01-20T12:00:00Z"));
		aliceThread.setMembers(List.of(memberFor(alice.getId())));
		threadRepository.save(aliceThread);

		ThreadDoc bobThread = new ThreadDoc();
		bobThread.setCreatedAt(Instant.parse("2026-01-20T09:00:00Z"));
		bobThread.setLastMessageAt(Instant.parse("2026-01-20T11:00:00Z"));
		bobThread.setMembers(List.of(memberFor(bob.getId())));
		threadRepository.save(bobThread);
	}

	@Test
	void loginReturnsProfileData() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, String>> request = new HttpEntity<>(
			Map.of("login", "alice", "password", "secret"),
			headers
		);

		ResponseEntity<String> response = restTemplate.postForEntity(url("/api/auth/login"), request, String.class);

		Assertions.assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		String expectedAvatarUrl = mediaUrlResolver.resolveAvatarUrl("alice.png");
		Assertions.assertThat(response.getBody())
			.contains(alice.getId().toString())
			.contains("\"login\":\"alice\"")
			.contains("\"twoFactorEnabled\":false")
			.contains(expectedAvatarUrl);
	}

	@Test
	void loginRejectsInvalidPassword() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, String>> request = new HttpEntity<>(
			Map.of("login", "alice", "password", "wrong"),
			headers
		);

		try {
			restTemplate.postForEntity(url("/api/auth/login"), request, String.class);
			Assertions.fail("Expected unauthorized response");
		} catch (HttpClientErrorException ex) {
			Assertions.assertThat(ex.getStatusCode().value()).isEqualTo(401);
		}
	}

	@Test
	void threadsRequireAuthentication() {
		try {
			restTemplate.getForEntity(url("/api/threads"), String.class);
			Assertions.fail("Expected unauthorized response");
		} catch (HttpClientErrorException ex) {
			Assertions.assertThat(ex.getStatusCode().value()).isEqualTo(401);
		}
	}

	@Test
	void listThreadsReturnsOnlyMembersThreads() {
		HttpHeaders headers = bearerHeaders(loginAndGetAccessToken("alice", "secret"));
		ResponseEntity<String> response = restTemplate.exchange(
			url("/api/threads"),
			HttpMethod.GET,
			new HttpEntity<>(headers),
			String.class
		);
		Assertions.assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		Assertions.assertThat(response.getBody())
			.contains("\"threads\"")
			.contains("Alice")
			.doesNotContain("Bob");
	}

	private ThreadMember memberFor(UUID accountId) {
		ThreadMember member = new ThreadMember();
		member.setAccountId(accountId);
		member.setJoinedAt(Instant.parse("2026-01-20T08:00:00Z"));
		return member;
	}

	private String url(String path) {
		return "http://localhost:" + port + path;
	}

	private String loginAndGetAccessToken(String login, String password) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, String>> request = new HttpEntity<>(
			Map.of("login", login, "password", password),
			headers
		);
		ResponseEntity<String> response = restTemplate.postForEntity(url("/api/auth/login"), request, String.class);
		Assertions.assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		try {
			JsonNode json = objectMapper.readTree(response.getBody());
			return json.path("accessToken").asText();
		} catch (Exception ex) {
			throw new IllegalStateException("Missing access token in response", ex);
		}
	}

	private HttpHeaders bearerHeaders(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
		return headers;
	}
}
