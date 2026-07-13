package io.signallink.collector.job;

import io.signallink.issue.application.service.NewsCollectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 뉴스 수집 스케줄러 잡(J3-B) — 장중 30분 주기(기본 평일 08~20시 KST).
 *
 * <p>워치리스트 종목별로 네이버 뉴스를 폴링하고 (stock_code, url)로 멱등 저장한다.
 * 종목 단위 예외 격리는 {@link NewsCollectService}가 담당.
 */
@Component
public class NewsJob {

    private static final Logger log = LoggerFactory.getLogger(NewsJob.class);

    private final NewsCollectService newsCollectService;

    public NewsJob(NewsCollectService newsCollectService) {
        this.newsCollectService = newsCollectService;
    }

    @Scheduled(cron = "${signallink.issue.news.cron:0 15/30 8-20 * * MON-FRI}", zone = "Asia/Seoul")
    public void collect() {
        log.info("뉴스 수집 배치 시작");
        int saved = newsCollectService.collect();
        log.info("뉴스 수집 배치 종료: 신규 {}건", saved);
    }
}
