package io.signallink.issue.adapter.out.external.dart;

import io.signallink.common.client.ExternalApiClient;
import io.signallink.common.client.RateLimiter;
import io.signallink.issue.application.port.out.DartGatewayPort;
import io.signallink.issue.application.port.out.DisclosureCandidate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * DART 공시검색 API 아웃바운드 어댑터 (DartGatewayPort 구현).
 *
 * <p>{@link ExternalApiClient}(rate limit + 지수 백오프 재시도) 위에서 list.json을 호출하고
 * 응답을 {@link DisclosureCandidate}로 변환한다. 키/URL은 설정에서 주입(기본값 있음 → 미설정이어도 빈 생성 가능).
 */
@Component
public class DartClient extends ExternalApiClient implements DartGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(DartClient.class);
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final String apiKey;

    public DartClient(
            @Value("${signallink.issue.dart.base-url:https://opendart.fss.or.kr}") String baseUrl,
            @Value("${signallink.issue.dart.api-key:}") String apiKey,
            @Value("${signallink.issue.dart.permits-per-second:5}") double permitsPerSecond) {
        super(baseUrl, new RateLimiter(permitsPerSecond), 3, 500);
        this.apiKey = apiKey;
    }

    @Override
    public List<DisclosureCandidate> fetchDisclosures(LocalDate from, LocalDate to) {
        // TODO(M2): page_count 100 초과 시 페이지네이션(total_page 순회) — 현재는 단일 페이지.
        DartListResponse res = execute("dart.list", () -> client().get()
            .uri(uri -> uri.path("/api/list.json")
                .queryParam("crtfc_key", apiKey)
                .queryParam("bgn_de", from.format(YMD))
                .queryParam("end_de", to.format(YMD))
                .queryParam("page_count", 100)
                .build())
            .retrieve()
            .body(DartListResponse.class));

        if (res == null || DartListResponse.NO_DATA.equals(res.status())) {
            return List.of(); // 조회 기간 내 공시 없음
        }
        if (!DartListResponse.OK.equals(res.status())) {
            log.warn("DART 응답 비정상: status={} message={}", res.status(), res.message());
            return List.of();
        }
        return res.toCandidates();
    }
}
