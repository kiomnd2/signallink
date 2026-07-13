package io.signallink.insight.application.service;

import io.signallink.insight.application.port.out.FeatureCardRepositoryPort;
import io.signallink.insight.application.port.out.FeatureCardUpsert;
import io.signallink.insight.domain.CardStatus;
import io.signallink.insight.domain.FeatureCandidate;
import io.signallink.insight.domain.FlowDiagnosis;
import io.signallink.insight.domain.IssueMatch;
import io.signallink.insight.domain.MarketAnalysis;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Why 카드 생성 파이프라인(J2) — 특징주 선정(3-A) → 시장분해(3-B)·수급진단(3-C)·이슈매칭(3-D)을 조립해
 * feature_card로 upsert한다. LLM(3-E)은 아직 미배선이라 what_happened는 템플릿 폴백, llm_used=false.
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
    private final FeatureCardRepositoryPort cardRepository;

    /** @return upsert한 카드 수 */
    public int generate(LocalDate tradeDate) {
        List<FeatureCandidate> candidates = selectService.select(tradeDate);
        int done = 0;
        for (FeatureCandidate c : candidates) {
            try {
                MarketAnalysis market = marketDecomposeService.analyze(c);
                FlowDiagnosis flow = flowDiagnoseService.diagnose(c);
                IssueMatch issues = issueMatchService.match(c);

                cardRepository.upsert(new FeatureCardUpsert(
                    c.stockCode(), c.tradeDate(), c.changeRate(), c.triggerType(),
                    market.marketContrib(), market.sectorSync(), market.whatHappened(),
                    flow.flowSummary(), null, issues.refs(), false, CardStatus.PUBLISHED));
                done++;
            } catch (RuntimeException e) {
                log.warn("카드 생성 실패, 종목 건너뜀: {} ({})", c.stockCode(), e.toString());
            }
        }
        log.info("Why 카드 파이프라인: {} 후보 {}건 → 카드 {}건 upsert", tradeDate, candidates.size(), done);
        return done;
    }
}
