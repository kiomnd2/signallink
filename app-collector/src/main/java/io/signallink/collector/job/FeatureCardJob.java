package io.signallink.collector.job;

import io.signallink.common.TimeUtils;
import io.signallink.common.notify.DiscordNotifier;
import io.signallink.insight.application.service.FeatureCardPipelineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

/**
 * Why 카드 생성 스케줄러 잡(J2) — 장중 30분 주기(기본 평일 09~15시 KST)로 당일 카드를 생성·갱신한다.
 *
 * <p>종목 단위 예외 격리는 {@link FeatureCardPipelineService}가 담당. 배치 자체 실패(선정 단계 등)는
 * 여기서 잡아 Discord로 알리고, 30분 뒤 재실행이 멱등 upsert로 회복한다.
 */
@Component
@RequiredArgsConstructor
public class FeatureCardJob {

    private static final Logger log = LoggerFactory.getLogger(FeatureCardJob.class);

    private final FeatureCardPipelineService pipeline;
    private final DiscordNotifier discordNotifier;

    @Scheduled(cron = "${signallink.feature-card.cron:0 5/30 9-15 * * MON-FRI}", zone = "Asia/Seoul")
    public void generate() {
        log.info("Why 카드 배치 시작");
        try {
            int n = pipeline.generate(TimeUtils.todayKst());
            log.info("Why 카드 배치 종료: {}건", n);
        } catch (RuntimeException e) {
            log.error("Why 카드 배치 실패", e);
            discordNotifier.jobFailed("FeatureCardJob", e);
        }
    }
}
