package io.signallink.insight.domain;

import io.signallink.issue.application.port.out.CompanyFinancials;
import io.signallink.issue.application.port.out.QuarterAccount;
import java.util.List;

/**
 * 실적 추세 판정(순수 도메인) — 최근 분기 매출·영업이익 시계열(최신 앞)에서 방향(개선/유지/악화)을 뽑는다.
 *
 * <p>가장 최근값과 가장 오래된값을 비교해 상대 변화가 ±{@value #THRESHOLD_PCT}% 밖이면 개선/악화, 안이면 유지.
 * 기준값 부호가 바뀌는 적자↔흑자도 부호 기준으로 처리(적자 축소=개선). 데이터 2개 미만이면 UNKNOWN.
 */
public final class FinancialTrendAnalyzer {

    private FinancialTrendAnalyzer() {}

    /** 유지로 볼 상대 변화 폭(%). */
    private static final double THRESHOLD_PCT = 5.0;

    public static FinancialTrends analyze(CompanyFinancials financials) {
        if (financials == null) {
            return FinancialTrends.UNKNOWN;
        }
        List<QuarterAccount> q = financials.quarters();
        if (q == null || q.size() < 2) {
            return FinancialTrends.UNKNOWN;
        }
        QuarterAccount latest = q.get(0);
        QuarterAccount oldest = q.get(q.size() - 1);
        return new FinancialTrends(
            direction(latest.revenue(), oldest.revenue()),
            direction(latest.operatingProfit(), oldest.operatingProfit()));
    }

    /** 최신 vs 오래된값 → 방향. */
    private static FinancialTrend direction(long latest, long oldest) {
        if (oldest == 0) {
            if (latest > 0) {
                return FinancialTrend.IMPROVING;
            }
            return latest < 0 ? FinancialTrend.DECLINING : FinancialTrend.STABLE;
        }
        double relPct = (double) (latest - oldest) / Math.abs(oldest) * 100.0;
        if (relPct > THRESHOLD_PCT) {
            return FinancialTrend.IMPROVING;
        }
        if (relPct < -THRESHOLD_PCT) {
            return FinancialTrend.DECLINING;
        }
        return FinancialTrend.STABLE;
    }
}
