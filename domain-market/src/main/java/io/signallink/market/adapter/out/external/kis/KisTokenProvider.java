package io.signallink.market.adapter.out.external.kis;

import io.signallink.common.TimeUtils;
import java.time.OffsetDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * KIS 액세스 토큰 발급·캐시.
 *
 * <p>{@code POST /oauth2/tokenP}는 재발급 제한이 있어(M0 확인), 발급 토큰을 DB(market.kis_token, 단일 행)에
 * 영속하고 만료 여유({@value #REFRESH_MARGIN_MIN}분) 전이면 재사용한다. 모든 KIS 호출은 이 토큰을 Bearer로 쓴다.
 */
@Component
public class KisTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(KisTokenProvider.class);
    private static final int TOKEN_ID = 1;
    private static final long REFRESH_MARGIN_MIN = 10;

    private final KisTokenJpaRepository repository;
    private final RestClient client;
    private final String appKey;
    private final String appSecret;

    public KisTokenProvider(
            KisTokenJpaRepository repository,
            @Value("${signallink.kis.base-url:https://openapi.koreainvestment.com:9443}") String baseUrl,
            @Value("${signallink.kis.app-key:}") String appKey,
            @Value("${signallink.kis.app-secret:}") String appSecret) {
        this.repository = repository;
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.appKey = appKey;
        this.appSecret = appSecret;
    }

    /** 유효한 액세스 토큰을 반환한다 — 캐시가 살아있으면 재사용, 아니면 신규 발급 후 저장. */
    public synchronized String bearerToken() {
        OffsetDateTime now = TimeUtils.nowKst();
        KisToken cached = repository.findById(TOKEN_ID).orElse(null);
        if (isUsable(cached, now)) {
            return cached.getAccessToken();
        }
        KisToken issued = repository.save(issue(now));
        log.info("KIS 토큰 신규 발급 (만료 {})", issued.getExpiresAt());
        return issued.getAccessToken();
    }

    /** 만료까지 여유({@value #REFRESH_MARGIN_MIN}분)가 남아 있으면 재사용 가능. */
    static boolean isUsable(KisToken token, OffsetDateTime now) {
        return token != null && token.getExpiresAt().isAfter(now.plusMinutes(REFRESH_MARGIN_MIN));
    }

    private KisToken issue(OffsetDateTime now) {
        KisTokenResponse res = client.post()
            .uri("/oauth2/tokenP")
            .body(Map.of("grant_type", "client_credentials", "appkey", appKey, "appsecret", appSecret))
            .retrieve()
            .body(KisTokenResponse.class);
        if (res == null || res.accessToken() == null) {
            throw new IllegalStateException("KIS 토큰 발급 실패: 응답에 access_token 없음");
        }
        return new KisToken(TOKEN_ID, res.accessToken(), now, now.plusSeconds(res.expiresIn()));
    }
}
