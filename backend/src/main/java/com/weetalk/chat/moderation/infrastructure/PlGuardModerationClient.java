package com.weetalk.chat.moderation.infrastructure;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PlGuardModerationClient {
	private final RestClient restClient;

	public PlGuardModerationClient(RestClient plGuardRestClient) {
		this.restClient = plGuardRestClient;
	}

	public PlGuardResult moderate(String text) {
		PlGuardResponse response = restClient.post()
			.uri("/moderate")
			.body(new PlGuardRequest(text))
			.retrieve()
			.body(PlGuardResponse.class);
		if (response == null || response.top() == null) {
			throw new IllegalStateException("PL-Guard returned empty response");
		}
		return new PlGuardResult(response.top().label(), response.top().score(), response.labels());
	}

	public record PlGuardRequest(String text) {
	}

	public record PlGuardResponse(String text, PlGuardLabel top, List<PlGuardLabel> labels) {
	}

	public record PlGuardLabel(String label, double score) {
	}

	public record PlGuardResult(String label, double score, List<PlGuardLabel> labels) {
	}
}
