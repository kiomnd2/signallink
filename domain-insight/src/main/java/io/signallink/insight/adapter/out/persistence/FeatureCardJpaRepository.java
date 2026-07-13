package io.signallink.insight.adapter.out.persistence;

import io.signallink.insight.domain.FeatureCard;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA 리포지토리 — 영속 어댑터 내부 구현 디테일. */
public interface FeatureCardJpaRepository extends JpaRepository<FeatureCard, Long> {

    Optional<FeatureCard> findByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate);
}
