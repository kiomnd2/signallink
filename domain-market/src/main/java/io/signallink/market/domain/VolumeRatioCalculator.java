package io.signallink.market.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 거래량 비율 = 당일 거래량 / 직전 최대 {@value #WINDOW}영업일 평균 거래량.
 * 특징주 선정의 "거래량 급증(3배)" 판정 기준. 이력이 없거나 평균이 0이면 계산 불가(null).
 */
public final class VolumeRatioCalculator {

    public static final int WINDOW = 20;

    private VolumeRatioCalculator() {}

    /** @param priorVolumes 직전 거래일 거래량(최신순 또는 임의 순서, 최대 20개 사용) */
    public static BigDecimal ratio20d(long todayVolume, List<Long> priorVolumes) {
        if (priorVolumes.isEmpty()) {
            return null;
        }
        int n = Math.min(priorVolumes.size(), WINDOW);
        long sum = 0;
        for (int i = 0; i < n; i++) {
            sum += priorVolumes.get(i);
        }
        if (sum == 0) {
            return null;
        }
        double avg = (double) sum / n;
        return BigDecimal.valueOf(todayVolume / avg).setScale(2, RoundingMode.HALF_UP);
    }
}
