package io.signallink.market.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 아웃바운드 포트 — 특정 거래일의 지수 등락률 조회. 데이터 없으면 null. */
public interface IndexChangeQueryPort {
    BigDecimal changeRateOn(String indexCode, LocalDate tradeDate);
}
