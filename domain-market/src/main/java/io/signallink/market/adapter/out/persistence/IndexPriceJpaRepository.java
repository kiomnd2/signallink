package io.signallink.market.adapter.out.persistence;

import io.signallink.market.domain.IndexPrice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA 리포지토리 — 복합키(IndexPrice.Pk). */
public interface IndexPriceJpaRepository extends JpaRepository<IndexPrice, IndexPrice.Pk> {

    boolean existsByIndexCodeAndTradeDate(String indexCode, LocalDate tradeDate);

    List<IndexPrice> findByIndexCodeOrderByTradeDateDesc(String indexCode, Pageable pageable);

    Optional<IndexPrice> findByIndexCodeAndTradeDate(String indexCode, LocalDate tradeDate);
}
