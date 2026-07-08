package io.signallink.insight.domain;

import java.math.BigDecimal;

/**
 * 시장분해 결과 — 종목 등락률을 "시장 몫"과 "개별 초과분"으로 나눈 것.
 * marketContrib = β × 지수등락률(시장을 따라 움직였을 몫). excess = 등락률 − marketContrib(개별 요인).
 * 베타·지수가 없어 분해 불가하면 marketContrib=null, excess=등락률 전체, driver=STOCK_SPECIFIC.
 */
public record MarketDecomposition(BigDecimal marketContrib, BigDecimal excess, Driver driver) {}
