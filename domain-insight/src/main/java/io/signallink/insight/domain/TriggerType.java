package io.signallink.insight.domain;

/**
 * 특징주 선정 트리거 — 왜 이 종목이 오늘의 특징주로 뽑혔는지.
 * SURGE: 등락률 ±5% 이상. VOLUME: 거래량이 20일 평균의 3배 이상.
 * (둘 다 해당하면 SURGE 우선 — 가격 변동이 더 직관적인 신호)
 */
public enum TriggerType {
    SURGE,
    VOLUME
}
