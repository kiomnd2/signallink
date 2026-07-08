package io.signallink.insight.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 시장분해(순수 도메인) — "이 종목이 오른/내린 게 시장 때문인가, 개별 이슈 때문인가"를 나눈다.
 *
 * <p>{@code marketContrib = β × 지수등락률}: 이 종목의 시장 민감도(β)를 감안했을 때 지수 흐름만으로
 * 설명되는 등락 몫. {@code excess = 등락률 − marketContrib}: 그걸 넘어서는 개별 요인. 두 절대값을
 * 비교해 더 큰 쪽을 주된 동인({@link Driver})으로 본다. β·지수가 없으면 전부 개별로 처리한다.
 */
public final class MarketDecomposer {

    private static final int SCALE = 3; // feature_card.market_contrib numeric(6,3)

    private MarketDecomposer() {}

    public static MarketDecomposition decompose(BigDecimal changeRate, BigDecimal beta, BigDecimal indexChangeRate) {
        if (beta == null || indexChangeRate == null) {
            return new MarketDecomposition(null, changeRate, Driver.STOCK_SPECIFIC);
        }
        BigDecimal contrib = beta.multiply(indexChangeRate).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal excess = changeRate.subtract(contrib).setScale(SCALE, RoundingMode.HALF_UP);
        Driver driver = contrib.abs().compareTo(excess.abs()) >= 0
            ? Driver.MARKET_DRIVEN
            : Driver.STOCK_SPECIFIC;
        return new MarketDecomposition(contrib, excess, driver);
    }
}
