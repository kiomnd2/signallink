package io.signallink.market.application.port.out;

import java.time.LocalDate;
import java.util.List;

/**
 * 아웃바운드 포트 — 종목·거래일 기준 수급 조회(insight 수급진단이 읽음).
 * 구현: adapter.out.persistence.InvestorFlowPersistenceAdapter.
 */
public interface InvestorFlowQueryPort {
    /** 해당 종목·거래일의 투자자별 수급 행. 데이터 없으면 빈 리스트. */
    List<InvestorFlowRow> flowsOn(String stockCode, LocalDate tradeDate);
}
