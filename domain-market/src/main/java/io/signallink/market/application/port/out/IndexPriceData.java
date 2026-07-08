package io.signallink.market.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;

/** KIS 지수 일별시세 게이트웨이가 돌려주는 후보(등락률은 어댑터에서 전일 종가 대비로 계산됨). */
public record IndexPriceData(
    LocalDate tradeDate,
    BigDecimal close,
    BigDecimal changeRate
) {}
