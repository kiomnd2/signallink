package io.signallink.collector.job;

import io.signallink.common.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 스케줄러 동작 확인용 — J1~J7 구현 시 제거. */
@Component
public class HeartbeatJob {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatJob.class);

    @Scheduled(fixedRate = 300_000)
    public void beat() {
        log.info("collector alive at {}", TimeUtils.nowKst());
    }
}
