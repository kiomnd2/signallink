package io.signallink.market.domain;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.market.application.port.out.DatedClose;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 베타 계산 — 종목 수익률이 지수 수익률의 정확히 2배면 β=2.0, 데이터 부족이면 null. */
class BetaCalculatorTest {

    // 변동 있는 지수 일수익률(분산≠0). 종목은 각 일 정확히 2배 수익률 → β=2.
    private static final double[] INDEX_RETURNS = {
        0.01, -0.02, 0.015, -0.005, 0.02, -0.01, 0.008, -0.012, 0.006, -0.003,
        0.011, -0.007, 0.004, -0.009, 0.013, -0.006, 0.002, -0.011, 0.007, -0.004,
        0.009, -0.002, 0.005, -0.008
    };

    @Test
    void 종목_수익률이_지수의_2배면_베타는_2() {
        List<DatedClose> index = new ArrayList<>();
        List<DatedClose> stock = new ArrayList<>();
        LocalDate d = LocalDate.of(2026, 1, 1);
        double idx = 100.0;
        double stk = 100.0;
        index.add(new DatedClose(d, BigDecimal.valueOf(idx)));
        stock.add(new DatedClose(d, BigDecimal.valueOf(stk)));
        for (double r : INDEX_RETURNS) {
            d = d.plusDays(1);
            idx *= (1 + r);
            stk *= (1 + 2 * r);
            index.add(new DatedClose(d, BigDecimal.valueOf(idx)));
            stock.add(new DatedClose(d, BigDecimal.valueOf(stk)));
        }

        assertThat(BetaCalculator.beta60d(stock, index)).isEqualByComparingTo("2.000");
    }

    @Test
    void 데이터가_부족하면_null() {
        LocalDate d = LocalDate.of(2026, 1, 1);
        List<DatedClose> few = List.of(
            new DatedClose(d, BigDecimal.valueOf(100)),
            new DatedClose(d.plusDays(1), BigDecimal.valueOf(101)),
            new DatedClose(d.plusDays(2), BigDecimal.valueOf(102)));

        assertThat(BetaCalculator.beta60d(few, few)).isNull();
    }
}
