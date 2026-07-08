package io.signallink.insight.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 업종 동반등락 판정(순수 도메인) — 같은 업종이 대상 종목과 "함께 움직였나".
 *
 * <p>같은 업종 종목(자신 제외)의 당일 등락률로 판정한다: (1) 표본이 최소 {@value #MIN_PEERS}개 이상,
 * (2) 대상 종목과 같은 방향으로 움직인 비율이 {@value #MIN_SAME_DIR_RATIO} 이상, (3) 평균 등락 절대값이
 * 의미 있는 수준(≥ {@value #MIN_AVG_ABS_PCT}%). 셋 다 만족하면 업종 전반의 움직임(sector_sync=true).
 */
public final class SectorSyncEvaluator {

    public static final int MIN_PEERS = 3;
    public static final double MIN_SAME_DIR_RATIO = 0.6;
    public static final String MIN_AVG_ABS_PCT = "1.0";

    private static final BigDecimal MIN_AVG_ABS = new BigDecimal(MIN_AVG_ABS_PCT);

    private SectorSyncEvaluator() {}

    public static boolean isSync(BigDecimal changeRate, List<BigDecimal> peerChangeRates) {
        if (peerChangeRates == null || peerChangeRates.size() < MIN_PEERS) {
            return false;
        }
        int dir = changeRate.signum();
        if (dir == 0) {
            return false;
        }
        long sameDir = peerChangeRates.stream().filter(p -> p.signum() == dir).count();
        double ratio = (double) sameDir / peerChangeRates.size();

        BigDecimal avgAbs = peerChangeRates.stream()
            .map(BigDecimal::abs)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(peerChangeRates.size()), 3, RoundingMode.HALF_UP);

        return ratio >= MIN_SAME_DIR_RATIO && avgAbs.compareTo(MIN_AVG_ABS) >= 0;
    }
}
