package io.signallink.issue.application.port.out;

import java.util.List;

/**
 * 한 종목의 최근 재무 시계열 — 체력 진단 실적 추세(매출·영업이익 방향) 분석의 입력.
 * {@code quarters}는 <b>최신이 앞</b>(recent-first). 조회 실패/미매핑 종목은 {@code Optional.empty()}로 표현.
 */
public record CompanyFinancials(
    String stockCode,
    List<QuarterAccount> quarters) {}
