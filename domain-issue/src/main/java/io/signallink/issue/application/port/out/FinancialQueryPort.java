package io.signallink.issue.application.port.out;

import java.util.Optional;

/**
 * 종목 재무 조회 포트 — insight(체력 진단)가 issue 도메인에서 재무 시계열을 읽는 경계.
 * 구현: application.service.DartFinancialQueryService (corp_code 해석 + DART 재무 게이트웨이 호출).
 *
 * <p>corp_code 미해석·조회 실패 시 {@link Optional#empty()} — 체력 진단은 "데이터 없음"으로 우아하게 비표시.
 */
public interface FinancialQueryPort {

    /** 최근 분기 매출·영업이익 시계열(최신 앞). 없으면 empty. */
    Optional<CompanyFinancials> recentQuarters(String stockCode);
}
