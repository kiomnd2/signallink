package io.signallink.market.adapter.out.persistence;

import io.signallink.market.domain.StockBeta;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA 리포지토리 — stock_code(String) PK, save가 merge(upsert). */
public interface StockBetaJpaRepository extends JpaRepository<StockBeta, String> {
}
