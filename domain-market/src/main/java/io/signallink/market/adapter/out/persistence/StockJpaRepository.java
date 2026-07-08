package io.signallink.market.adapter.out.persistence;

import io.signallink.market.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA 리포지토리 — 영속 어댑터 내부 구현 디테일. */
public interface StockJpaRepository extends JpaRepository<Stock, String> {
}
