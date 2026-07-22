package io.signallink.insight.application.service;

import io.signallink.insight.domain.FinancialTrendAnalyzer;
import io.signallink.insight.domain.FinancialTrends;
import io.signallink.issue.application.port.out.FinancialQueryPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 체력 진단 실적 추세(3-7c) — issue의 {@link FinancialQueryPort}로 최근 재무를 읽어 매출·영업이익 방향을 판정.
 *
 * <p>기본 {@code signallink.health.financials-enabled=false} — corp_code 리졸버·DART 재무 키 준비 전까지
 * 호출 자체를 건너뛴다("데이터 없음"). 조회 실패는 잡아 empty(우아한 degradation, 카드 생성은 계속).
 */
@Service
@RequiredArgsConstructor
public class FinancialTrendService {

    private static final Logger log = LoggerFactory.getLogger(FinancialTrendService.class);

    private final FinancialQueryPort financialQuery;

    @Value("${signallink.health.financials-enabled:false}")
    private final boolean enabled;

    /** @return 실적 추세, 비활성/데이터없음/실패면 empty(체력 진단 실적 추세는 "데이터 없음"). */
    public Optional<FinancialTrends> trendsFor(String stockCode) {
        if (!enabled) {
            return Optional.empty();
        }
        try {
            return financialQuery.recentQuarters(stockCode).map(FinancialTrendAnalyzer::analyze);
        } catch (RuntimeException e) {
            log.warn("재무 추세 조회 실패, 건너뜀: {} ({})", stockCode, e.toString());
            return Optional.empty();
        }
    }
}
