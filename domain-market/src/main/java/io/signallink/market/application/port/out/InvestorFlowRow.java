package io.signallink.market.application.port.out;

import io.signallink.market.domain.InvestorType;

/**
 * 수급 조회 결과 한 행(투자자 1종). {@code isFinal=false}면 장중 가집계(잠정)라 진단이 "잠정"으로 표기된다.
 * net_buy_amt 단위는 KIS 응답 그대로 백만원.
 */
public record InvestorFlowRow(
    InvestorType investorType,
    long netBuyQty,
    long netBuyAmt,
    boolean isFinal
) {}
