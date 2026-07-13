package io.signallink.issue.adapter.out.persistence;

import io.signallink.issue.domain.Disclosure;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA 리포지토리 — 영속 어댑터의 내부 구현 디테일 (외부에는 포트로만 노출). */
public interface DisclosureJpaRepository extends JpaRepository<Disclosure, Long> {

    boolean existsByReceiptNo(String receiptNo);

    List<Disclosure> findByStockCodeAndDisclosedAtBetween(
        String stockCode, OffsetDateTime from, OffsetDateTime to);
}
