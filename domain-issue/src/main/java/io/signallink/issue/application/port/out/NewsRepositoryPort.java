package io.signallink.issue.application.port.out;

import io.signallink.issue.domain.News;

/** 아웃바운드 포트 — 뉴스 영속화. 구현: adapter.out.persistence.NewsPersistenceAdapter. */
public interface NewsRepositoryPort {
    boolean existsByStockCodeAndUrl(String stockCode, String url);
    News save(News news);
}
