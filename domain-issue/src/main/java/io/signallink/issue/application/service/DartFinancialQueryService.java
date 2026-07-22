package io.signallink.issue.application.service;

import io.signallink.common.TimeUtils;
import io.signallink.issue.application.port.out.CompanyFinancials;
import io.signallink.issue.application.port.out.CorpCodeResolver;
import io.signallink.issue.application.port.out.DartFinancialGatewayPort;
import io.signallink.issue.application.port.out.FinancialQueryPort;
import io.signallink.issue.application.port.out.QuarterAccount;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 종목 재무 조회 유스케이스({@link FinancialQueryPort}) — corp_code 해석 후 DART 재무 게이트웨이로 최근
 * 분기 매출·영업이익을 최신순으로 최대 {@value #MAX_QUARTERS}개 모은다.
 *
 * <p>corp_code 미해석이면 즉시 empty(호출 없음). 보고서별 데이터 없음(empty)은 건너뛴다.
 * 어느 보고서까지 존재하는지는 실데이터로 튜닝 필요 → 최근 2개 사업연도의 분기/사업 보고서를 최신순 후보로 훑는다(BACKLOG).
 */
@Service
@RequiredArgsConstructor
public class DartFinancialQueryService implements FinancialQueryPort {

    private static final int MAX_QUARTERS = 4;

    private final CorpCodeResolver corpCodeResolver;
    private final DartFinancialGatewayPort gateway;

    @Override
    public Optional<CompanyFinancials> recentQuarters(String stockCode) {
        Optional<String> corpCode = corpCodeResolver.corpCode(stockCode);
        if (corpCode.isEmpty()) {
            return Optional.empty();
        }
        List<QuarterAccount> collected = new ArrayList<>(MAX_QUARTERS);
        for (Report r : recentReports()) {
            if (collected.size() >= MAX_QUARTERS) {
                break;
            }
            gateway.fetchQuarter(corpCode.get(), r.year(), r.reprtCode())
                .ifPresent(collected::add);
        }
        return collected.isEmpty() ? Optional.empty()
            : Optional.of(new CompanyFinancials(stockCode, collected));
    }

    /** 최신순 후보 보고서 (현재연도 분기 → 전년도 사업/분기 → 전전년도 사업). */
    private List<Report> recentReports() {
        int y = TimeUtils.todayKst().getYear();
        return List.of(
            new Report(y, "11014"), new Report(y, "11012"), new Report(y, "11013"),
            new Report(y - 1, "11011"), new Report(y - 1, "11014"),
            new Report(y - 1, "11012"), new Report(y - 1, "11013"),
            new Report(y - 2, "11011"));
    }

    private record Report(int year, String reprtCode) {}
}
