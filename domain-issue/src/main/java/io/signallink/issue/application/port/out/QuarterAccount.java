package io.signallink.issue.application.port.out;

/**
 * 한 분기(또는 사업연도) 재무 주요계정 — DART 단일회사 주요계정에서 뽑은 매출액·영업이익.
 * 금액 단위는 원. {@code quarter} 1~4 (4 = 사업보고서/연간).
 */
public record QuarterAccount(
    int year,
    int quarter,
    long revenue,
    long operatingProfit) {}
