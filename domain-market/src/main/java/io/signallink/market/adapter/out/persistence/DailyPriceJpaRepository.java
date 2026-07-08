package io.signallink.market.adapter.out.persistence;

import io.signallink.market.domain.DailyPrice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data JPA 리포지토리 — 영속 어댑터 내부 구현 디테일. */
public interface DailyPriceJpaRepository extends JpaRepository<DailyPrice, Long> {

    boolean existsByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate);

    boolean existsByStockCode(String stockCode);

    List<DailyPrice> findByStockCodeOrderByTradeDateDesc(String stockCode, Pageable pageable);

    List<DailyPrice> findByTradeDate(LocalDate tradeDate);

    /** 같은 업종 종목들의 당일 등락률(자기 자신 제외) — daily_price ⋈ stock(sector_code)로 조인. */
    @Query("SELECT d.changeRate FROM DailyPrice d, Stock s "
        + "WHERE d.stockCode = s.stockCode AND s.sectorCode = :sector "
        + "AND d.tradeDate = :date AND d.stockCode <> :exclude")
    List<BigDecimal> findSectorChangeRates(
        @Param("sector") String sector,
        @Param("date") LocalDate date,
        @Param("exclude") String exclude);
}
