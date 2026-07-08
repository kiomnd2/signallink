package io.signallink.collector.job;

import io.signallink.issue.application.service.DisclosureCollectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 공시 수집 스케줄러 잡(J3-A) — 장중 30분 주기(기본 평일 08~20시 KST).
 *
 * <p>주기를 겹치게(daysBack) 폴링하고 rcept_no로 멱등 처리하므로 유실 없이 무인 운전한다.
 * 건별 예외 격리는 {@link DisclosureCollectService}가 담당. (@Value 섞인 생성자는 수동 — CLAUDE.md 예외)
 */
@Component
public class DisclosureJob {

    private static final Logger log = LoggerFactory.getLogger(DisclosureJob.class);

    private final DisclosureCollectService disclosureCollectService;
    private final int daysBack;

    public DisclosureJob(
            DisclosureCollectService disclosureCollectService,
            @Value("${signallink.issue.disclosure.days-back:1}") int daysBack) {
        this.disclosureCollectService = disclosureCollectService;
        this.daysBack = daysBack;
    }

    @Scheduled(cron = "${signallink.issue.disclosure.cron:0 0/30 8-20 * * MON-FRI}", zone = "Asia/Seoul")
    public void collect() {
        log.info("공시 수집 배치 시작 (최근 {}일)", daysBack);
        int saved = disclosureCollectService.collectRecent(daysBack);
        log.info("공시 수집 배치 종료: 신규 {}건", saved);
    }
}
