package io.signallink.market.application.port.out;

import java.math.BigDecimal;

/**
 * 특징주 선정 입력용 읽기 모델 — 한 종목의 당일 일봉 요약.
 * insight가 이 스냅샷들로 트리거(±5%·거래량 3배)를 판정하고 거래대금 순으로 선정한다.
 * volRatio20d는 이력 부족 시 null일 수 있다.
 */
public record StockDaySnapshot(
    String stockCode,
    BigDecimal changeRate,
    BigDecimal volRatio20d,
    long tradingValue) {}
