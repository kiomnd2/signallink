package io.signallink.market.application.port.out;

import java.math.BigDecimal;

/** 아웃바운드 포트 — 종목 베타(60일) 조회. 계산 이력이 없으면 null. */
public interface StockBetaQueryPort {
    BigDecimal beta60d(String stockCode);
}
