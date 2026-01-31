package com.weetalk.chat.children.application;

import com.weetalk.chat.children.api.dto.ChildLoginResponse;

public record ChildLoginResult(ChildLoginResponse response, String accessToken, String refreshToken) {
}
