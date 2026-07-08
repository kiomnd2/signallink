package io.signallink.market.application.port.out;

import java.util.List;

/**
 * 아웃바운드 포트 — 종목마스터(전체 상장 종목 목록)를 가져온다.
 * 구현: adapter.out.external.kis.StockMasterFileClient (KIS .mst 파일).
 */
public interface StockMasterGatewayPort {
    List<StockMasterEntry> fetchAll();
}
