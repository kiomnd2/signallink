package io.signallink.collector.job;

import io.signallink.market.application.service.StockBetaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 베타 계산 스케줄러 잡(J7) — 주 1회(기본 토요일 06:00 KST). 백필된 일봉·지수로 60일 베타 계산.
 * (@Value 섞인 생성자는 수동 — CLAUDE.md 예외)
 */
@Component
public class StockBetaJob {

    private static final Logger log = LoggerFactory.getLogger(StockBetaJob.class);

    private final StockBetaService stockBetaService;
    private final int limit;

    public StockBetaJob(
            StockBetaService stockBetaService,
            @Value("${signallink.beta.limit:300}") int limit) {
        this.stockBetaService = stockBetaService;
        this.limit = limit;
    }

    @Scheduled(cron = "${signallink.beta.weekly-cron:0 0 6 * * SAT}", zone = "Asia/Seoul")
    public void calculate() {
        log.info("주간 베타 계산 시작: 상위 {}종목", limit);
        int done = stockBetaService.calculateAll(limit);
        log.info("주간 베타 계산 종료: {}종목", done);
    }
}
