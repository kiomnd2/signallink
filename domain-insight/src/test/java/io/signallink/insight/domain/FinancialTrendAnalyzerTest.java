package io.signallink.insight.domain;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.issue.application.port.out.CompanyFinancials;
import io.signallink.issue.application.port.out.QuarterAccount;
import java.util.List;
import org.junit.jupiter.api.Test;

class FinancialTrendAnalyzerTest {

    /** 최신 앞(recent-first) 시계열. */
    private static CompanyFinancials fin(QuarterAccount... quarters) {
        return new CompanyFinancials("005930", List.of(quarters));
    }

    @Test
    void 데이터_2개_미만이면_UNKNOWN이다() {
        assertThat(FinancialTrendAnalyzer.analyze(null)).isEqualTo(FinancialTrends.UNKNOWN);
        assertThat(FinancialTrendAnalyzer.analyze(fin(new QuarterAccount(2026, 3, 100, 10))))
            .isEqualTo(FinancialTrends.UNKNOWN);
    }

    @Test
    void 최신이_오래된값보다_충분히_크면_개선이다() {
        // 매출 120←100(+20%), 영업이익 30←20(+50%)
        var t = FinancialTrendAnalyzer.analyze(fin(
            new QuarterAccount(2026, 3, 120, 30),
            new QuarterAccount(2025, 4, 100, 20)));
        assertThat(t.revenue()).isEqualTo(FinancialTrend.IMPROVING);
        assertThat(t.operatingProfit()).isEqualTo(FinancialTrend.IMPROVING);
    }

    @Test
    void 최신이_충분히_작으면_악화다() {
        var t = FinancialTrendAnalyzer.analyze(fin(
            new QuarterAccount(2026, 3, 80, 5),
            new QuarterAccount(2025, 4, 100, 20)));
        assertThat(t.revenue()).isEqualTo(FinancialTrend.DECLINING);
        assertThat(t.operatingProfit()).isEqualTo(FinancialTrend.DECLINING);
    }

    @Test
    void 변화가_임계치_안이면_유지다() {
        // +2% (임계 5% 미만)
        var t = FinancialTrendAnalyzer.analyze(fin(
            new QuarterAccount(2026, 3, 102, 20),
            new QuarterAccount(2025, 4, 100, 20)));
        assertThat(t.revenue()).isEqualTo(FinancialTrend.STABLE);
        assertThat(t.operatingProfit()).isEqualTo(FinancialTrend.STABLE);
    }

    @Test
    void 적자가_줄면_영업이익_개선이다() {
        // 영업이익 -50←-100 (적자 축소) → 개선
        var t = FinancialTrendAnalyzer.analyze(fin(
            new QuarterAccount(2026, 3, 100, -50),
            new QuarterAccount(2025, 4, 100, -100)));
        assertThat(t.operatingProfit()).isEqualTo(FinancialTrend.IMPROVING);
        assertThat(t.revenue()).isEqualTo(FinancialTrend.STABLE);
    }
}
