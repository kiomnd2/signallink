package io.signallink.issue.adapter.out.persistence;

import io.signallink.issue.application.port.out.NewsRepositoryPort;
import io.signallink.issue.domain.News;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 아웃바운드 어댑터 — NewsRepositoryPort를 Spring Data JPA로 구현. */
@Component
@RequiredArgsConstructor
public class NewsPersistenceAdapter implements NewsRepositoryPort {

    private final NewsJpaRepository jpa;

    @Override
    public boolean existsByStockCodeAndUrl(String stockCode, String url) {
        return jpa.existsByStockCodeAndUrl(stockCode, url);
    }

    @Override
    public News save(News news) {
        return jpa.save(news);
    }
}
