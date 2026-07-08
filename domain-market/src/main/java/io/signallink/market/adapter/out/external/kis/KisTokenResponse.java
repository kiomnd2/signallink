package io.signallink.market.adapter.out.external.kis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** POST /oauth2/tokenP 응답. expires_in은 초 단위(보통 86400=24h). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KisTokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") long expiresIn
) {}
