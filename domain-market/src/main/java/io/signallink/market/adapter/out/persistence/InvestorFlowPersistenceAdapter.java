package io.signallink.market.adapter.out.persistence;

import io.signallink.market.application.port.out.InvestorFlowRepositoryPort;
import io.signallink.market.domain.InvestorFlow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 아웃바운드 어댑터 — 수급 upsert. (stock,date,type) 있으면 값 갱신(잠정→확정), 없으면 삽입. */
@Component
@RequiredArgsConstructor
public class InvestorFlowPersistenceAdapter implements InvestorFlowRepositoryPort {

    private final InvestorFlowJpaRepository jpa;

    @Override
    public void upsert(InvestorFlow flow) {
        jpa.findByStockCodeAndTradeDateAndInvestorType(
                flow.getStockCode(), flow.getTradeDate(), flow.getInvestorType())
            .ifPresentOrElse(
                existing -> {
                    existing.update(flow.getNetBuyQty(), flow.getNetBuyAmt(), flow.isFinal());
                    jpa.save(existing);
                },
                () -> jpa.save(flow));
    }
}
