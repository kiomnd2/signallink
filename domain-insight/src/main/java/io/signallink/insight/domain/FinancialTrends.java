package io.signallink.insight.domain;

/** 실적 추세 판정 결과 — 매출·영업이익 각각의 방향. */
public record FinancialTrends(FinancialTrend revenue, FinancialTrend operatingProfit) {

    public static final FinancialTrends UNKNOWN =
        new FinancialTrends(FinancialTrend.UNKNOWN, FinancialTrend.UNKNOWN);
}
