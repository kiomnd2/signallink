package io.signallink.market.application.port.out;

import java.time.LocalDate;
import java.util.List;

/**
 * 아웃바운드 포트 — 특정 거래일의 전 종목 일봉 스냅샷 조회.
 * insight의 특징주 선정이 이 포트로 당일 시세를 읽는다(타 도메인 DB 직접 접근 금지).
 */
public interface DailyPriceSnapshotQueryPort {
    List<StockDaySnapshot> snapshotsOn(LocalDate tradeDate);
}
