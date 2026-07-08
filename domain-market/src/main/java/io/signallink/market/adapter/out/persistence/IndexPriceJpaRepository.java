package io.signallink.market.adapter.out.persistence;

import io.signallink.market.domain.IndexPrice;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA 리포지토리 — 복합키(IndexPrice.Pk). */
public interface IndexPriceJpaRepository extends JpaRepository<IndexPrice, IndexPrice.Pk> {

    boolean existsByIndexCodeAndTradeDate(String indexCode, LocalDate tradeDate);
}
