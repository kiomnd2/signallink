package io.signallink.market.application.port.out;

import java.util.List;

/**
 * 아웃바운드 포트 — 투자자 수급 조회. 구현: adapter.out.external.kis.KisInvestorClient.
 */
public interface InvestorFlowGatewayPort {
    /** 마감 확정 수급 — 종목별 최신 거래일의 개인·외국인·기관 3종. */
    List<InvestorFlowData> fetchConfirmed(String stockCode);

    /** 장중 잠정 수급 — 가집계(시장 순매수 상위)의 외국인·기관. */
    List<InvestorFlowData> fetchProvisional();
}
