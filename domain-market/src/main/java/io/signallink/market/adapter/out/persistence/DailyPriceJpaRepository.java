package io.signallink.market.adapter.out.persistence;

import io.signallink.market.domain.DailyPrice;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA 리포지토리 — 영속 어댑터 내부 구현 디테일. */
public interface DailyPriceJpaRepository extends JpaRepository<DailyPrice, Long> {

    boolean existsByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate);

    boolean existsByStockCode(String stockCode);

    List<DailyPrice> findByStockCodeOrderByTradeDateDesc(String stockCode, Pageable pageable);

    List<DailyPrice> findByTradeDate(LocalDate tradeDate);
}
