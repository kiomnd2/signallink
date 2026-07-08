package io.signallink.market.application.port.out;

import io.signallink.market.domain.InvestorFlow;

/** 아웃바운드 포트 — 수급 영속화. (stock,date,type) 있으면 갱신, 없으면 삽입(잠정→확정 덮어쓰기). */
public interface InvestorFlowRepositoryPort {
    void upsert(InvestorFlow flow);
}
