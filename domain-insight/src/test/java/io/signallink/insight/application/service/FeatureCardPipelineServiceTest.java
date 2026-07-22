package io.signallink.insight.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.insight.application.port.out.FeatureCardRepositoryPort;
import io.signallink.insight.application.port.out.FeatureCardUpsert;
import io.signallink.insight.domain.CardStatus;
import io.signallink.issue.application.port.out.DisclosureRef;
import io.signallink.issue.application.port.out.IssueQueryPort;
import io.signallink.issue.application.port.out.NewsRef;
import io.signallink.market.application.port.out.InvestorFlowRow;
import io.signallink.market.application.port.out.StockDaySnapshot;
import io.signallink.market.application.port.out.StockInfo;
import io.signallink.market.domain.InvestorType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** J2 파이프라인 — 선정→분석→카드 upsert 조립과 종목 단위 예외 격리를 검증(모든 협력자는 실제 서비스+람다 포트). */
class FeatureCardPipelineServiceTest {

    private static final LocalDate D = LocalDate.of(2026, 7, 8);

    private final CapturingCardRepo cardRepo = new CapturingCardRepo();

    /** 두 종목이 SURGE로 선정되도록 스냅샷 구성. */
    private FeatureStockSelectService selectWith(String... stockCodes) {
        return new FeatureStockSelectService(tradeDate -> {
            List<StockDaySnapshot> out = new ArrayList<>();
            long tv = stockCodes.length;
            for (String code : stockCodes) {
                out.add(new StockDaySnapshot(code, new BigDecimal("6.0"), null, tv--));
            }
            return out;
        });
    }

    private MarketDecomposeService marketService() {
        return new MarketDecomposeService(
            stockCode -> new StockInfo("KOSPI", "G45"),
            stockCode -> new BigDecimal("1.0"),
            (indexCode, date) -> new BigDecimal("2.0"),
            (sector, date, exclude) -> List.of());
    }

    private IssueMatchService issueServiceWithDisclosure() {
        return new IssueMatchService(new IssueQueryPort() {
            @Override public List<DisclosureRef> disclosures(String s, OffsetDateTime f, OffsetDateTime t) {
                return List.of(new DisclosureRef("공급계약", "수시공시",
                    OffsetDateTime.parse("2026-07-08T09:00+09:00"), "http://dart/1"));
            }
            @Override public List<NewsRef> news(String s, OffsetDateTime f, OffsetDateTime t) {
                return List.of();
            }
        });
    }

    /** LLM 비활성 서비스 — summarize()가 항상 empty를 돌려 템플릿 폴백(llm_used=false). */
    private WhySummaryService llmDisabled() {
        return new WhySummaryService(prompt -> "미사용", false, 60);
    }

    @Test
    void 선정된_종목마다_분석을_조립해_카드로_upsert한다() {
        var flowService = new FlowDiagnoseService((stock, date) -> List.of(
            new InvestorFlowRow(InvestorType.FOREIGN, 100, 3000, true)));
        var pipeline = new FeatureCardPipelineService(
            selectWith("005930", "000660"), marketService(), flowService,
            issueServiceWithDisclosure(), llmDisabled(), cardRepo);

        int done = pipeline.generate(D);

        assertThat(done).isEqualTo(2);
        assertThat(cardRepo.saved).hasSize(2);
        FeatureCardUpsert first = cardRepo.saved.get(0);
        assertThat(first.stockCode()).isEqualTo("005930");   // 거래대금 상위부터
        assertThat(first.status()).isEqualTo(CardStatus.PUBLISHED);
        assertThat(first.llmUsed()).isFalse();
        assertThat(first.whatHappened()).isNotBlank();
        assertThat(first.flowSummary()).contains("외국인은 순매수");
        assertThat(first.sourceRefs()).hasSize(1);           // 공시 1건
        assertThat(first.marketContrib()).isEqualByComparingTo("2.000");
    }

    @Test
    void LLM_요약_성공시_그_문장을_whatHappened로_쓰고_llmUsed_true다() {
        var flowService = new FlowDiagnoseService((stock, date) -> List.of(
            new InvestorFlowRow(InvestorType.FOREIGN, 100, 3000, true)));
        // 이슈가 있으면(공시) LLM 호출됨 → 클린 요약 반환하는 스텁
        var llmEnabled = new WhySummaryService(
            prompt -> "오늘은 공급계약 공시가 나오면서 주가가 크게 올랐어요.", true, 60);
        var pipeline = new FeatureCardPipelineService(
            selectWith("005930"), marketService(), flowService,
            issueServiceWithDisclosure(), llmEnabled, cardRepo);

        pipeline.generate(D);

        FeatureCardUpsert card = cardRepo.saved.get(0);
        assertThat(card.llmUsed()).isTrue();
        assertThat(card.whatHappened()).isEqualTo("오늘은 공급계약 공시가 나오면서 주가가 크게 올랐어요.");
    }

    @Test
    void 한_종목이_실패해도_나머지_카드는_계속_생성된다() {
        // 000660 수급 조회에서 예외 → 그 종목만 건너뜀
        var flowService = new FlowDiagnoseService((stock, date) -> {
            if ("000660".equals(stock)) {
                throw new RuntimeException("수급 조회 실패");
            }
            return List.of(new InvestorFlowRow(InvestorType.FOREIGN, 100, 3000, true));
        });
        var pipeline = new FeatureCardPipelineService(
            selectWith("005930", "000660"), marketService(), flowService,
            issueServiceWithDisclosure(), llmDisabled(), cardRepo);

        int done = pipeline.generate(D);

        assertThat(done).isEqualTo(1);
        assertThat(cardRepo.saved).extracting(FeatureCardUpsert::stockCode).containsExactly("005930");
    }

    static class CapturingCardRepo implements FeatureCardRepositoryPort {
        final List<FeatureCardUpsert> saved = new ArrayList<>();
        @Override public void upsert(FeatureCardUpsert card) { saved.add(card); }
    }
}
