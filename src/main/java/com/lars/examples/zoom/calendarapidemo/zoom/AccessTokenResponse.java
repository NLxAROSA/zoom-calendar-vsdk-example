package com.lars.examples.zoom.calendarapidemo.zoom;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AccessTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("scope") String scope) {
}
