package io.signallink.issue.adapter.out.external.dart;

import io.signallink.common.client.ExternalApiClient;
import io.signallink.common.client.RateLimiter;
import io.signallink.issue.application.port.out.DartGatewayPort;
import io.signallink.issue.application.port.out.DisclosureCandidate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
        return interpret(res);
    }

    /**
     * 응답 상태를 해석해 후보 목록으로 변환한다.
     *
     * <p>status {@code "013"}(데이터 없음)만 빈 목록으로 정상 처리한다. 그 외 비정상 상태(인증 실패·
     * 한도 초과·점검 등, DART는 HTTP 200 + 바디로 알림)는 <b>예외를 던져</b> 상위 스케줄러가 실패로
     * 인지·알림하게 한다 — "오늘 공시 0건"과 혼동해 조용히 수집이 멈추는 것을 막는다.
     */
    static List<DisclosureCandidate> interpret(DartListResponse res) {
        if (res == null) {
            throw new IllegalStateException("DART 응답 없음(null)");
        }
        if (DartStatus.NO_DATA.equals(res.status())) {
            return List.of();
        }
        if (!DartStatus.OK.equals(res.status())) {
            throw new IllegalStateException(
                "DART 오류 상태: status=" + res.status() + " message=" + res.message());
        }
        return res.toCandidates();
    }
}
