package io.signallink.issue.adapter.out.external.dart;

import io.signallink.common.client.ExternalApiClient;
import io.signallink.common.client.RateLimiter;
import io.signallink.issue.application.port.out.DartFinancialGatewayPort;
import io.signallink.issue.application.port.out.QuarterAccount;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * DART 단일회사 주요계정 API(fnlttSinglAcnt.json) 아웃바운드 어댑터 ({@link DartFinancialGatewayPort} 구현).
 *
 * <p>{@link ExternalApiClient}(rate limit + 지수 백오프) 위에서 한 보고서의 매출액·영업이익을 가져온다.
 * 데이터없음(013)은 empty, 그 외 비정상 status는 예외(공시 어댑터와 동일 원칙). 키/URL은 설정 주입.
 */
@Component
public class DartFinancialClient extends ExternalApiClient implements DartFinancialGatewayPort {

    private final String apiKey;

    public DartFinancialClient(
            @Value("${signallink.issue.dart.base-url:https://opendart.fss.or.kr}") String baseUrl,
            @Value("${signallink.issue.dart.api-key:}") String apiKey,
            @Value("${signallink.issue.dart.permits-per-second:5}") double permitsPerSecond) {
        super(baseUrl, new RateLimiter(permitsPerSecond), 3, 500);
        this.apiKey = apiKey;
    }

    @Override
    public Optional<QuarterAccount> fetchQuarter(String corpCode, int year, String reprtCode) {
        DartFinancialResponse res = execute("dart.fnlttSinglAcnt", () -> client().get()
            .uri(uri -> uri.path("/api/fnlttSinglAcnt.json")
                .queryParam("crtfc_key", apiKey)
                .queryParam("corp_code", corpCode)
                .queryParam("bsns_year", year)
                .queryParam("reprt_code", reprtCode)
                .build())
            .retrieve()
            .body(DartFinancialResponse.class));
        return interpret(res, year, quarterOf(reprtCode));
    }

    /** 상태 해석: 013(데이터없음)=empty, 000=매핑, 그 외=예외. */
    static Optional<QuarterAccount> interpret(DartFinancialResponse res, int year, int quarter) {
        if (res == null) {
            throw new IllegalStateException("DART 재무 응답 없음(null)");
        }
        if (DartStatus.NO_DATA.equals(res.status())) {
            return Optional.empty();
        }
        if (!DartStatus.OK.equals(res.status())) {
            throw new IllegalStateException(
                "DART 재무 오류 상태: status=" + res.status() + " message=" + res.message());
        }
        return res.toQuarterAccount(year, quarter);
    }

    /** 보고서코드 → 분기(4 = 사업보고서/연간). */
    static int quarterOf(String reprtCode) {
        return switch (reprtCode) {
            case "11013" -> 1; // 1분기보고서
            case "11012" -> 2; // 반기보고서
            case "11014" -> 3; // 3분기보고서
            default -> 4;      // 11011 사업보고서
        };
    }
}
