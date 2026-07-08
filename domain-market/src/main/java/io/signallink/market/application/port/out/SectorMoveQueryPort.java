package io.signallink.market.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 아웃바운드 포트 — 특정 거래일, 같은 업종(sectorCode) 종목들의 등락률 목록(대상 종목 자신은 제외).
 * 업종 동반등락(sector_sync) 판정 입력. 해당 업종·날짜에 데이터가 없으면 빈 목록.
 */
public interface SectorMoveQueryPort {
    List<BigDecimal> sectorChangeRates(String sectorCode, LocalDate tradeDate, String excludeStockCode);
}
