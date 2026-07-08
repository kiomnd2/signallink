package io.signallink.collector.job;

import io.signallink.market.application.service.DailyMarketCollectService;
import io.signallink.market.application.service.StockMasterSyncService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 시세 수집 스케줄러 잡 (J1 종목마스터 + 일일 시세).
 *
 * <p>평일(MON-FRI) 장마감 후 1회 실행. cron의 요일 지정이 곧 휴장일(주말) 가드이며, 공휴일은
 * 멱등 수집이 흡수한다(새 데이터 없으면 0건). (@Value 섞인 생성자는 수동 — CLAUDE.md 예외)
 */
@Component
public class MarketCollectJob {

    private static final Logger log = LoggerFactory.getLogger(MarketCollectJob.class);

    private final StockMasterSyncService stockMasterSyncService;
    private final DailyMarketCollectService dailyMarketCollectService;
    private final int limit;
    private final int recentDays;
    private final List<String> indexCodes;

    public MarketCollectJob(
            StockMasterSyncService stockMasterSyncService,
            DailyMarketCollectService dailyMarketCollectService,
            @Value("${signallink.collect.limit:300}") int limit,
            @Value("${signallink.collect.recent-days:5}") int recentDays,
            @Value("${signallink.collect.index-codes:0001,1001}") List<String> indexCodes) {
        this.stockMasterSyncService = stockMasterSyncService;
        this.dailyMarketCollectService = dailyMarketCollectService;
        this.limit = limit;
        this.recentDays = recentDays;
        this.indexCodes = indexCodes;
    }

    @Scheduled(cron = "${signallink.collect.daily-cron:0 30 16 * * MON-FRI}", zone = "Asia/Seoul")
    public void collectDaily() {
        log.info("일일 시세 배치 시작");
        stockMasterSyncService.sync();                              // J1: 종목마스터 최신화
        dailyMarketCollectService.collectAll(limit, recentDays, indexCodes);
        log.info("일일 시세 배치 종료");
    }
}
