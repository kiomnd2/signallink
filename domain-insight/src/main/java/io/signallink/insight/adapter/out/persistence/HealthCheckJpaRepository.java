package io.signallink.insight.adapter.out.persistence;

import io.signallink.insight.domain.HealthCheck;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA 리포지토리 — 영속 어댑터 내부 구현 디테일. */
public interface HealthCheckJpaRepository extends JpaRepository<HealthCheck, Long> {

    Optional<HealthCheck> findByCardId(Long cardId);
}
