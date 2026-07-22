package io.signallink.insight.application.service;

import io.signallink.insight.application.port.out.FeatureCardRepositoryPort;
import io.signallink.insight.application.port.out.FeatureCardUpsert;
import io.signallink.insight.application.port.out.HealthCheckRepositoryPort;
import io.signallink.insight.application.port.out.HealthCheckUpsert;
import io.signallink.insight.domain.CardStatus;
import io.signallink.insight.domain.FeatureCandidate;
import io.signallink.insight.domain.FlowDiagnosis;
import io.signallink.insight.domain.IssueMatch;
import io.signallink.insight.domain.IssueNature;
import io.signallink.insight.domain.IssueNatureClassifier;
import io.signallink.insight.domain.MarketAnalysis;
import io.signallink.insight.domain.WhyContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Why 카드 생성 파이프라인(J2) — 특징주 선정(3-A) → 시장분해(3-B)·수급진단(3-C)·이슈매칭(3-D) →
 * LLM 요약(3-E)을 조립해 feature_card로 upsert하고, 이어서 체력 진단 이슈 성격 태그(3-7a)를 기록한다.
 * LLM 성공 시 what_happened는 그 요약·llm_used=true, 그 외(비활성·이슈없음·상한초과·실패·금지어)는
 * 시장분해 템플릿 폴백·llm_used=false.
 *
 * <p><b>종목 단위 예외 격리</b>: 한 종목 분석/저장 실패가 배치 전체를 죽이지 않는다. 배치 자체(선정 단계)
 * 실패는 호출측(잡)이 잡아 Discord로 알린다.
 */
@Service
@RequiredArgsConstructor
public class FeatureCardPipelineService {

    private static final Logger log = LoggerFactory.getLogger(FeatureCardPipelineService.class);

    private final FeatureStockSelectService selectService;
    private final MarketDecomposeService marketDecomposeService;
    private final FlowDiagnoseService flowDiagnoseService;
    private final IssueMatchService issueMatchService;
    private final WhySummaryService whySummaryService;
    private final FeatureCardRepositoryPort cardRepository;
    private final HealthCheckRepositoryPort healthCheckRepository;

    /** @return upsert한 카드 수 */
    public int generate(LocalDate tradeDate) {
        List<FeatureCandidate> candidates = selectService.select(tradeDate);
        int done = 0;
        for (FeatureCandidate c : candidates) {
            try {
                MarketAnalysis market = marketDecomposeService.analyze(c);
                FlowDiagnosis flow = flowDiagnoseService.diagnose(c);
                IssueMatch issues = issueMatchService.match(c);

                WhyContext ctx = new WhyContext(
                    c.stockCode(), c.changeRate(), c.triggerType(),
                    market.marketContrib(), market.driver(), market.sectorSync(),
                    flow.flowSummary(), issues.refs());
                Optional<String> llmSummary = whySummaryService.summarize(ctx, c.tradeDate());
                String whatHappened = llmSummary.orElseGet(market::whatHappened);

                long cardId = cardRepository.upsert(new FeatureCardUpsert(
                    c.stockCode(), c.tradeDate(), c.changeRate(), c.triggerType(),
                    market.marketContrib(), market.sectorSync(), whatHappened,
                    flow.flowSummary(), null, issues.refs(), llmSummary.isPresent(), CardStatus.PUBLISHED));

                // 체력 진단 팩트① 이슈 성격 태그(3-7a). 실적·수급 추세(3-7c·3-7b)는 후속 슬라이스가 채운다.
                IssueNature nature = IssueNatureClassifier.classify(issues.refs());
                healthCheckRepository.upsert(HealthCheckUpsert.ofNature(cardId, nature));
                done++;
            } catch (RuntimeException e) {
                log.warn("카드 생성 실패, 종목 건너뜀: {} ({})", c.stockCode(), e.toString());
            }
        }
        log.info("Why 카드 파이프라인: {} 후보 {}건 → 카드 {}건 upsert", tradeDate, candidates.size(), done);
        return done;
    }
}
