package io.signallink.issue.adapter.out.external.naver;

import io.signallink.common.client.ExternalApiClient;
import io.signallink.common.client.RateLimiter;
import io.signallink.issue.application.port.out.NaverNewsGatewayPort;
import io.signallink.issue.application.port.out.NewsCandidate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 네이버 뉴스 검색 아웃바운드 어댑터 (NaverNewsGatewayPort 구현).
 * {@link ExternalApiClient} 위에서 호출 → rate limit(정책①) + 백오프 재시도 적용. 인증은 헤더 방식.
 */
@Component
public class NaverNewsClient extends ExternalApiClient implements NaverNewsGatewayPort {

    private final String clientId;
    private final String clientSecret;

    public NaverNewsClient(
            @Value("${signallink.issue.naver.base-url:https://openapi.naver.com}") String baseUrl,
            @Value("${signallink.issue.naver.client-id:}") String clientId,
            @Value("${signallink.issue.naver.client-secret:}") String clientSecret,
            @Value("${signallink.issue.naver.permits-per-second:5}") double permitsPerSecond) {
        super(baseUrl, new RateLimiter(permitsPerSecond), 3, 500);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public List<NewsCandidate> search(String stockCode, String query) {
        NaverNewsResponse res = execute("naver.news", () -> client().get()
            .uri(uri -> uri.path("/v1/search/news.json")
                .queryParam("query", query)
                .queryParam("display", 10)
                .queryParam("sort", "date")
                .build())
            .header("X-Naver-Client-Id", clientId)
            .header("X-Naver-Client-Secret", clientSecret)
            .retrieve()
            .body(NaverNewsResponse.class));
        return res == null ? List.of() : res.toCandidates(stockCode);
    }
}
