package io.signallink.insight.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.insight.domain.FeatureCandidate;
import io.signallink.insight.domain.FlowDiagnosis;
import io.signallink.insight.domain.TriggerType;
import io.signallink.market.application.port.out.InvestorFlowRow;
import io.signallink.market.domain.InvestorType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 수급 진단 유스케이스 — 수급 조회 포트 스텁으로 외국인·기관 집계와 잠정 판정을 검증. */
class FlowDiagnoseServiceTest {

    private static final LocalDate D = LocalDate.of(2026, 7, 8);

    private static FeatureCandidate candidate() {
        return new FeatureCandidate("005930", D, new BigDecimal("6.0"), TriggerType.SURGE, 1000L);
    }

    @Test
    void 확정_수급이면_외국인_기관을_집계하고_잠정아님() {
        var service = new FlowDiagnoseService((stock, date) -> List.of(
            new InvestorFlowRow(InvestorType.FOREIGN, 100, 3000, true),
            new InvestorFlowRow(InvestorType.ORG, 50, 1000, true),
            new InvestorFlowRow(InvestorType.PERSON, -150, -4000, true)));

        FlowDiagnosis d = service.diagnose(candidate());

        assertThat(d.provisional()).isFalse();
        assertThat(d.flowSummary()).contains("외국인은 순매수").contains("기관은 순매수");
    }

    @Test
    void 확정치_없이_가집계만_있으면_잠정으로_판정() {
        var service = new FlowDiagnoseService((stock, date) -> List.of(
            new InvestorFlowRow(InvestorType.FOREIGN, 100, 5000, false),
            new InvestorFlowRow(InvestorType.ORG, 20, 200, false)));

        FlowDiagnosis d = service.diagnose(candidate());

        assertThat(d.provisional()).isTrue();
        assertThat(d.flowSummary()).startsWith("장중 잠정 집계로는,");
    }

    @Test
    void 수급_데이터가_없으면_안내_문장이고_잠정아님() {
        var service = new FlowDiagnoseService((stock, date) -> List.of());

        FlowDiagnosis d = service.diagnose(candidate());

        assertThat(d.provisional()).isFalse();
        assertThat(d.flowSummary()).contains("데이터가 아직 없");
    }
}
