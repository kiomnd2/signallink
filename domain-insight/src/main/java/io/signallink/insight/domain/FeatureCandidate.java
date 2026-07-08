package io.signallink.insight.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 특징주 선정 결과 한 건 — 이후 3-B~3-E 분석 파이프라인의 입력이 된다.
 * (아직 feature_card로 영속되기 전의 선정 후보)
 */
public record FeatureCandidate(
    String stockCode,
    LocalDate tradeDate,
    BigDecimal changeRate,
    TriggerType triggerType,
    long tradingValue) {}
