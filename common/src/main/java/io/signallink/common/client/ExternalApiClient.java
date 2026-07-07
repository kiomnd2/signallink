package io.signallink.common.client;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * 외부 API 클라이언트 베이스: rate limit + 지수 백오프 재시도.
 *
 * <p>사용 예 (domain-market의 KIS 클라이언트):
 * <pre>{@code
 * class KisClient extends ExternalApiClient {
 *     KisClient() { super("https://openapi.koreainvestment.com:9443", new RateLimiter(5), 3, 500); }
 *     RankResponse fluctuationRank() {
 *         return execute("kis.fluctuation", () ->
 *             client().get().uri("/uapi/...").header("tr_id", "FHPST01700000")
 *                     .retrieve().body(RankResponse.class));
 *     }
 * }
 * }</pre>
 */
public abstract class ExternalApiClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiClient.class);

    private final RestClient restClient;
    private final RateLimiter rateLimiter;
    private final int maxAttempts;
    private final long baseBackoffMillis;

    protected ExternalApiClient(String baseUrl, RateLimiter rateLimiter,
                                int maxAttempts, long baseBackoffMillis) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.rateLimiter = rateLimiter;
        this.maxAttempts = maxAttempts;
        this.baseBackoffMillis = baseBackoffMillis;
    }

    protected RestClient client() {
        return restClient;
    }

    /** rate limit을 적용하고, 재시도 가능한 오류(429/5xx/IO)면 지수 백오프로 재시도한다. */
    protected <T> T execute(String opName, Supplier<T> call) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            rateLimiter.acquire();
            try {
                return call.get();
            } catch (RuntimeException e) {
                if (!isRetryable(e) || attempt == maxAttempts) {
                    throw e;
                }
                last = e;
                long backoff = backoffMillis(attempt);
                log.warn("{} 실패 (attempt {}/{}), {}ms 후 재시도: {}",
                        opName, attempt, maxAttempts, backoff, e.getMessage());
                sleep(backoff);
            }
        }
        throw last; // 도달 불가
    }

    private boolean isRetryable(RuntimeException e) {
        if (e instanceof ResourceAccessException) {
            return true; // 타임아웃·커넥션 오류
        }
        if (e instanceof RestClientResponseException re) {
            int s = re.getStatusCode().value();
            return s == 429 || s >= 500;
        }
        return false;
    }

    private long backoffMillis(int attempt) {
        long exp = baseBackoffMillis * (1L << (attempt - 1));
        return exp + (long) (Math.random() * baseBackoffMillis); // 지터
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("retry backoff interrupted", ie);
        }
    }
}
