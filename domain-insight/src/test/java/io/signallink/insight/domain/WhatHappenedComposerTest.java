package io.signallink.insight.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** what_happened 템플릿 — 시장분해 결과를 초보자 언어로. */
class WhatHappenedComposerTest {

    @Test
    void 시장_주도면_시장_흐름을_언급() {
        var d = new MarketDecomposition(new BigDecimal("2.000"), new BigDecimal("0.100"), Driver.MARKET_DRIVEN);
        String s = WhatHappenedComposer.compose("코스피", d, new BigDecimal("2.1"), new BigDecimal("2.0"), false);
        assertThat(s).contains("코스피").contains("시장");
    }

    @Test
    void 개별_주도면_개별_요인을_언급() {
        var d = new MarketDecomposition(new BigDecimal("1.000"), new BigDecimal("5.000"), Driver.STOCK_SPECIFIC);
        String s = WhatHappenedComposer.compose("코스피", d, new BigDecimal("6.0"), new BigDecimal("1.0"), false);
        assertThat(s).contains("개별 요인");
    }

    @Test
    void 베타없으면_개별_움직임_안내() {
        var d = new MarketDecomposition(null, new BigDecimal("4.0"), Driver.STOCK_SPECIFIC);
        String s = WhatHappenedComposer.compose("시장", d, new BigDecimal("4.0"), null, false);
        assertThat(s).contains("개별 움직임");
    }

    @Test
    void 업종_동반이면_업종_문구_추가() {
        var d = new MarketDecomposition(new BigDecimal("2.000"), new BigDecimal("0.100"), Driver.MARKET_DRIVEN);
        String s = WhatHappenedComposer.compose("코스피", d, new BigDecimal("2.1"), new BigDecimal("2.0"), true);
        assertThat(s).contains("업종");
    }

    @Test
    void 조언성_금지어는_넣지_않는다() {
        var d = new MarketDecomposition(new BigDecimal("1.000"), new BigDecimal("5.000"), Driver.STOCK_SPECIFIC);
        String s = WhatHappenedComposer.compose("코스닥", d, new BigDecimal("6.0"), new BigDecimal("1.0"), true);
        assertThat(s).doesNotContain("매수").doesNotContain("매도").doesNotContain("목표가");
    }
}
