package io.signallink.market.adapter.out.persistence;

import io.signallink.market.domain.DailyPrice;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA 리포지토리 — 영속 어댑터 내부 구현 디테일. */
public interface DailyPriceJpaRepository extends JpaRepository<DailyPrice, Long> {

    boolean existsByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate);
}
