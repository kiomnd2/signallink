package io.signallink.market.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 베타 계산 입력용 — 일자별 종가. */
public record DatedClose(LocalDate date, BigDecimal close) {}
