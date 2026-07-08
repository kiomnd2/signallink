package io.signallink.market.application.port.out;

/** 종목 기본 정보 읽기 모델 — 시장분해에서 대표 지수 선택(marketType)·업종 판별(sectorCode)에 쓰인다. */
public record StockInfo(String marketType, String sectorCode) {}
