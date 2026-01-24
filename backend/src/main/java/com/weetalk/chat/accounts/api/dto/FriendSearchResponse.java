package com.weetalk.chat.accounts.api.dto;

import java.util.List;

public class FriendSearchResponse {
	private final List<FriendSearchResultResponse> results;

	public FriendSearchResponse(List<FriendSearchResultResponse> results) {
		this.results = results;
	}

	public List<FriendSearchResultResponse> getResults() {
		return results;
	}
}
