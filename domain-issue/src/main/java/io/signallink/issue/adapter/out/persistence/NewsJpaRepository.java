package io.signallink.issue.adapter.out.persistence;

import io.signallink.issue.domain.News;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA 리포지토리 — 영속 어댑터 내부 구현 디테일. */
public interface NewsJpaRepository extends JpaRepository<News, Long> {

    boolean existsByStockCodeAndUrl(String stockCode, String url);

    List<News> findByStockCodeAndPublishedAtBetween(
        String stockCode, OffsetDateTime from, OffsetDateTime to);
}
