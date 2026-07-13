package io.signallink.collector.job;

import io.signallink.market.application.service.InvestorFlowCollectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 투자자 수급 수집 스케줄러 잡 — 잠정/확정 이원화(플랜 §5).
 *
 * <ul>
 *   <li><b>잠정(장중)</b>: 가집계 TR로 시장 단위 순매수 상위를 30분 주기 폴링(하루 4~5회만 갱신 →
 *       멱등 upsert가 중복을 흡수, {@code is_final=false}).</li>
 *   <li><b>확정(마감 후)</b>: {@code inquire-investor}로 유니버스 상위 종목별 확정치를 덮어쓰기
 *       ({@code is_final=true}). 시세 배치(16:30) 이후로 배치해 종목 마스터가 최신인 상태에서 실행.</li>
 * </ul>
 *
 * <p>확정은 종목당 1콜(초당 ~1건, ~300종목 ≈ 5분) — 서비스가 종목 단위 예외 격리를 담당.
 * (@Value 섞인 생성자는 수동 — CLAUDE.md 예외)
 */
@Component
public class InvestorFlowJob {

    private static final Logger log = LoggerFactory.getLogger(InvestorFlowJob.class);

    private final InvestorFlowCollectService investorFlowCollectService;
    private final int confirmedLimit;

    public InvestorFlowJob(
            InvestorFlowCollectService investorFlowCollectService,
            @Value("${signallink.investor-flow.confirmed-limit:300}") int confirmedLimit) {
        this.investorFlowCollectService = investorFlowCollectService;
        this.confirmedLimit = confirmedLimit;
    }

    @Scheduled(cron = "${signallink.investor-flow.provisional-cron:0 0/30 9-15 * * MON-FRI}", zone = "Asia/Seoul")
    public void collectProvisional() {
        log.info("잠정 수급 배치 시작");
        int n = investorFlowCollectService.collectProvisional();
        log.info("잠정 수급 배치 종료: 반영 {}건", n);
    }

    @Scheduled(cron = "${signallink.investor-flow.confirmed-cron:0 40 16 * * MON-FRI}", zone = "Asia/Seoul")
    public void collectConfirmed() {
        log.info("확정 수급 배치 시작: 상위 {}종목", confirmedLimit);
        int n = investorFlowCollectService.collectConfirmedAll(confirmedLimit);
        log.info("확정 수급 배치 종료: 반영 {}건", n);
    }
}
