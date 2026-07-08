package io.signallink.collector.job;

import io.signallink.common.TimeUtils;
import io.signallink.market.application.service.PriceBackfillService;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 수동 2년 백필 러너 — {@code signallink.backfill.enabled=true}일 때만 등록되어 부팅 시 1회 실행.
 * (@Value·명시 로직이 섞여 생성자는 수동, CLAUDE.md 컨벤션 예외)
 */
@Component
@ConditionalOnProperty(name = "signallink.backfill.enabled", havingValue = "true")
public class BackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BackfillRunner.class);

    private final PriceBackfillService backfillService;
    private final int limit;
    private final int years;

    public BackfillRunner(
            PriceBackfillService backfillService,
            @Value("${signallink.backfill.limit:300}") int limit,
            @Value("${signallink.backfill.years:2}") int years) {
        this.backfillService = backfillService;
        this.limit = limit;
        this.years = years;
    }

    @Override
    public void run(ApplicationArguments args) {
        LocalDate to = TimeUtils.todayKst();
        LocalDate from = to.minusYears(years);
        log.info("2년 백필 시작: 상위 {}종목 [{}~{}]", limit, from, to);
        int total = backfillService.backfill(limit, from, to);
        log.info("2년 백필 종료: 총 {}건 적재", total);
    }
}
