package io.signallink.market.application.port.out;

/**
 * 종목마스터 게이트웨이가 돌려주는 항목(가공 전). 외부 파일 포맷과 도메인을 분리하는 경계 타입.
 * sectorCode는 현재 null(업종 오프셋은 후속 — BACKLOG).
 */
public record StockMasterEntry(
    String code,
    String name,
    String marketType,   // KOSPI | KOSDAQ
    String sectorCode
) {}
