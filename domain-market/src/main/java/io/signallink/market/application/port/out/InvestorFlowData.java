package io.signallink.market.application.port.out;

import io.signallink.market.domain.InvestorType;
import java.time.LocalDate;

/** 수급 게이트웨이가 돌려주는 후보(투자자 1종 단위). is_final은 수집 경로(확정/잠정)에 따라 서비스가 지정. */
public record InvestorFlowData(
    String stockCode,
    LocalDate tradeDate,
    InvestorType investorType,
    long netBuyQty,
    long netBuyAmt
) {}
