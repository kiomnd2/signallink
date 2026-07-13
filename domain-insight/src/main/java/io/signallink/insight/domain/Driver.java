package io.signallink.insight.domain;

/**
 * 등락의 주된 동인 — 시장분해 결과.
 * MARKET_DRIVEN: 시장(지수) 몫이 더 큼(시장 흐름을 따라 움직임).
 * STOCK_SPECIFIC: 시장 몫을 넘어서는 개별 요인이 더 큼.
 */
public enum Driver {
    MARKET_DRIVEN,
    STOCK_SPECIFIC
}
