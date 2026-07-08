package io.signallink.issue.application.service;

import io.signallink.issue.application.port.out.NaverNewsGatewayPort;
import io.signallink.issue.application.port.out.NewsCandidate;
import io.signallink.issue.application.port.out.NewsRepositoryPort;
import io.signallink.issue.application.port.out.WatchStock;
import io.signallink.issue.application.port.out.WatchlistPort;
import io.signallink.issue.domain.News;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 뉴스 수집 유스케이스.
 *
 * <p>흐름: 워치리스트 종목마다 → 네이버 뉴스 검색 → (stock_code, url) 신규만 저장.
 * 종목 단위 예외 격리 — 한 종목 실패가 배치 전체를 죽이지 않는다(공시 슬라이스와 동일 원칙).
 */
@Service
@RequiredArgsConstructor
public class NewsCollectService {

    private static final Logger log = LoggerFactory.getLogger(NewsCollectService.class);

    private final NaverNewsGatewayPort naverGateway;
    private final NewsRepositoryPort repository;
    private final WatchlistPort watchlistPort;

    /** @return 신규 저장된 뉴스 수 */
    public int collect() {
        int saved = 0;
        for (WatchStock stock : watchlistPort.watchlist()) {
            try {
                for (NewsCandidate c : naverGateway.search(stock.code(), stock.name())) {
                    if (repository.existsByStockCodeAndUrl(c.stockCode(), c.url())) {
                        continue; // 이미 수집한 뉴스 → 스킵
                    }
                    repository.save(new News(
                        c.stockCode(), c.title(), c.summary(), c.url(), c.source(), c.publishedAt()));
                    saved++;
                }
            } catch (DataIntegrityViolationException e) {
                log.debug("뉴스 중복(제약 위반) 스킵: {}", stock.code());
            } catch (RuntimeException e) {
                log.warn("뉴스 수집 실패, 종목 건너뜀: {} ({})", stock.code(), e.toString());
            }
        }
        log.info("뉴스 수집 완료: 신규 {}건 저장", saved);
        return saved;
    }
}
