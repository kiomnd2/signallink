package io.signallink.insight.domain;

import io.signallink.market.application.port.out.StockDaySnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * 특징주 선정 규칙(순수 도메인 로직).
 *
 * <p>당일 전 종목 스냅샷에서 <b>등락률 ±5% 이상</b> 또는 <b>거래량 20일 평균의 3배 이상</b>인 종목을
 * 트리거로 걸러, <b>거래대금 상위 {@value #DEFAULT_TOP_N}종목</b>을 선정한다. 거래대금은 "얼마나 크게
 * 거래됐나"라 초보자가 주목할 만한 종목을 자연스럽게 상위로 올린다.
 */
public final class FeatureStockSelector {

    /** 등락률 트리거 임계(절대값, %) */
    public static final BigDecimal SURGE_PCT = new BigDecimal("5.0");
    /** 거래량 급증 트리거 임계(20일 평균 대비 배수) */
    public static final BigDecimal VOLUME_MULT = new BigDecimal("3.0");
    /** 기본 선정 종목 수 */
    public static final int DEFAULT_TOP_N = 40;

    private FeatureStockSelector() {}

    public static List<FeatureCandidate> select(List<StockDaySnapshot> snapshots, LocalDate tradeDate, int topN) {
        return snapshots.stream()
            .map(s -> classify(s, tradeDate))
            .filter(java.util.Objects::nonNull)
            .sorted(Comparator.comparingLong(FeatureCandidate::tradingValue).reversed())
            .limit(topN)
            .toList();
    }

    /** 트리거에 걸리면 후보로, 아니면 null. */
    private static FeatureCandidate classify(StockDaySnapshot s, LocalDate tradeDate) {
        boolean surge = s.changeRate() != null && s.changeRate().abs().compareTo(SURGE_PCT) >= 0;
        boolean volume = s.volRatio20d() != null && s.volRatio20d().compareTo(VOLUME_MULT) >= 0;
        if (!surge && !volume) {
            return null;
        }
        TriggerType trigger = surge ? TriggerType.SURGE : TriggerType.VOLUME;
        return new FeatureCandidate(s.stockCode(), tradeDate, s.changeRate(), trigger, s.tradingValue());
    }
}
