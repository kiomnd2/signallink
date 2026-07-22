package io.signallink.insight.domain;

/**
 * 실적 추세 방향 — 체력 진단 팩트② (health_check.revenue_trend/profit_trend).
 * 최근 분기 매출·영업이익이 개선/유지/악화 중 어느 쪽인지. 데이터 부족은 UNKNOWN.
 */
public enum FinancialTrend {

    IMPROVING("개선"),
    STABLE("유지"),
    DECLINING("악화"),
    UNKNOWN("데이터 부족");

    private final String label;

    FinancialTrend(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
