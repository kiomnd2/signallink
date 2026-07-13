package io.signallink.market.application.port.out;

/** 아웃바운드 포트 — 종목 기본 정보 조회(시장·업종). 없으면 null. */
public interface StockInfoQueryPort {
    StockInfo find(String stockCode);
}
