package io.signallink.insight.domain;

import java.math.BigDecimal;

/**
 * 시장분해 + 업종 동반 분석 결과 — feature_card의 market_contrib·sector_sync·what_happened로 이어진다.
 */
public record MarketAnalysis(
    BigDecimal marketContrib,
    Driver driver,
    boolean sectorSync,
    String whatHappened) {}
