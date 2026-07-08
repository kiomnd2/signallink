package io.signallink.market.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** vol_ratio_20d = 당일 거래량 / 직전 20영업일 평균. */
class VolumeRatioCalculatorTest {

    @Test
    void 당일_거래량이_평균의_3배면_3() {
        assertThat(VolumeRatioCalculator.ratio20d(300L, Collections.nCopies(20, 100L)))
            .isEqualByComparingTo("3.00");
    }

    @Test
    void 이력이_20개_미만이어도_있는_만큼으로_계산() {
        assertThat(VolumeRatioCalculator.ratio20d(200L, List.of(100L, 100L)))
            .isEqualByComparingTo("2.00");
    }

    @Test
    void 이력이_없거나_평균이_0이면_null() {
        assertThat(VolumeRatioCalculator.ratio20d(100L, List.of())).isNull();
        assertThat(VolumeRatioCalculator.ratio20d(100L, List.of(0L, 0L))).isNull();
    }
}
