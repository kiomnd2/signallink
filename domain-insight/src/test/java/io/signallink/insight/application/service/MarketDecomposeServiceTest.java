package io.signallink.insight.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.insight.domain.Driver;
import io.signallink.insight.domain.FeatureCandidate;
import io.signallink.insight.domain.MarketAnalysis;
import io.signallink.insight.domain.TriggerType;
import io.signallink.market.application.port.out.StockInfo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 시장분해 유스케이스 — market 조회 포트 스텁으로 분해·업종 판정·문장 조립을 검증. */
class MarketDecomposeServiceTest {

    private static final LocalDate D = LocalDate.of(2026, 7, 8);

    @Test
    void 코스피_종목의_시장분해와_업종동반을_판정한다() {
        var service = new MarketDecomposeService(
            stockCode -> new StockInfo("KOSPI", "G45"),            // 시장·업종
            stockCode -> new BigDecimal("1.0"),                    // β
            (indexCode, date) -> new BigDecimal("2.0"),            // 지수 등락률
            (sector, date, exclude) -> List.of(                    // 업종 동반(같은 방향·큰 폭)
                new BigDecimal("3.0"), new BigDecimal("2.5"), new BigDecimal("2.0")));

        var c = new FeatureCandidate("005930", D, new BigDecimal("2.1"), TriggerType.SURGE, 1000L);
        MarketAnalysis a = service.analyze(c);

        assertThat(a.marketContrib()).isEqualByComparingTo("2.000"); // 1.0 × 2.0
        assertThat(a.driver()).isEqualTo(Driver.MARKET_DRIVEN);      // 시장 몫(2.0) > 초과(0.1)
        assertThat(a.sectorSync()).isTrue();
        assertThat(a.whatHappened()).isNotBlank().contains("코스피");
    }

    @Test
    void 베타나_지수가_없으면_개별로_처리하고_문장도_안내() {
        var service = new MarketDecomposeService(
            stockCode -> new StockInfo("KOSDAQ", null),
            stockCode -> null,                                     // β 없음
            (indexCode, date) -> null,                             // 지수 없음
            (sector, date, exclude) -> List.of());

        var c = new FeatureCandidate("035720", D, new BigDecimal("7.0"), TriggerType.SURGE, 500L);
        MarketAnalysis a = service.analyze(c);

        assertThat(a.marketContrib()).isNull();
        assertThat(a.driver()).isEqualTo(Driver.STOCK_SPECIFIC);
        assertThat(a.sectorSync()).isFalse();
        assertThat(a.whatHappened()).contains("개별 움직임");
    }
}
