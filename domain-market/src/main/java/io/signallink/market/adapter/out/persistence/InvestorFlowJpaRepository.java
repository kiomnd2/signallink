package io.signallink.market.adapter.out.persistence;

import io.signallink.market.domain.InvestorFlow;
import io.signallink.market.domain.InvestorType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA 리포지토리 — 영속 어댑터 내부 구현 디테일. */
public interface InvestorFlowJpaRepository extends JpaRepository<InvestorFlow, Long> {

    Optional<InvestorFlow> findByStockCodeAndTradeDateAndInvestorType(
        String stockCode, LocalDate tradeDate, InvestorType investorType);
}
