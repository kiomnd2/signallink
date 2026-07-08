package io.signallink.issue.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.issue.application.port.out.NaverNewsGatewayPort;
import io.signallink.issue.application.port.out.NewsCandidate;
import io.signallink.issue.application.port.out.NewsRepositoryPort;
import io.signallink.issue.application.port.out.WatchStock;
import io.signallink.issue.application.port.out.WatchlistPort;
import io.signallink.issue.domain.News;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 뉴스 수집 유스케이스 — 워치리스트 순회·중복 스킵을 포트 스텁으로 검증. */
class NewsCollectServiceTest {

    private final OffsetDateTime now = OffsetDateTime.parse("2026-07-08T09:00+09:00");

    @Test
    void 워치리스트_종목별로_수집하고_이미_있는_뉴스는_스킵한다() {
        WatchlistPort watchlist = () -> List.of(
            new WatchStock("005930", "삼성전자"),
            new WatchStock("000660", "SK하이닉스"));
        NaverNewsGatewayPort gateway = (code, query) -> List.of(
            new NewsCandidate(code, "제목-" + code, "요약", "https://n/" + code, "naver", now));
        InMemoryRepo repo = new InMemoryRepo();
        repo.seen.add("000660|https://n/000660"); // SK 뉴스는 이미 존재

        int saved = new NewsCollectService(gateway, repo, watchlist).collect();

        assertThat(saved).isEqualTo(1); // 삼성전자만 신규
        assertThat(repo.saved).extracting(News::getStockCode).containsExactly("005930");
    }

    static class InMemoryRepo implements NewsRepositoryPort {
        final List<News> saved = new ArrayList<>();
        final Set<String> seen = new HashSet<>();

        @Override public boolean existsByStockCodeAndUrl(String stockCode, String url) {
            return seen.contains(stockCode + "|" + url);
        }

        @Override public News save(News news) {
            saved.add(news);
            seen.add(news.getStockCode() + "|" + news.getUrl());
            return news;
        }
    }
}
