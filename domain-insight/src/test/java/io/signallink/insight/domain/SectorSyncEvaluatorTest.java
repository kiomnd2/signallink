package io.signallink.insight.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 업종 동반등락 — 같은 업종이 같은 방향으로, 의미 있는 폭으로 함께 움직였나. */
class SectorSyncEvaluatorTest {

    private static List<BigDecimal> rates(String... v) {
        return List.of(v).stream().map(BigDecimal::new).toList();
    }

    @Test
    void 업종이_같은_방향으로_크게_움직이면_동반() {
        assertThat(SectorSyncEvaluator.isSync(new BigDecimal("5.0"), rates("3.0", "2.5", "4.0", "2.0"))).isTrue();
    }

    @Test
    void 방향이_뒤섞이면_비동반() {
        assertThat(SectorSyncEvaluator.isSync(new BigDecimal("5.0"), rates("3.0", "-2.5", "-4.0", "1.0"))).isFalse();
    }

    @Test
    void 움직임이_미미하면_비동반() {
        assertThat(SectorSyncEvaluator.isSync(new BigDecimal("5.0"), rates("0.2", "0.1", "0.3", "0.2"))).isFalse();
    }

    @Test
    void 표본이_너무_적으면_비동반() {
        assertThat(SectorSyncEvaluator.isSync(new BigDecimal("5.0"), rates("3.0", "2.0"))).isFalse();
        assertThat(SectorSyncEvaluator.isSync(new BigDecimal("5.0"), List.of())).isFalse();
    }
}
