package io.signallink.insight.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 수급 진단 — 외국인·기관 순매수(백만원) → 초보자 문장, 잠정 표기. 조언 문구 없음. */
class FlowDiagnoserTest {

    @Test
    void 외국인_기관_동반_순매수는_우호적_문장() {
        // 2500백만원=25억, 1200백만원=12억
        FlowDiagnosis d = FlowDiagnoser.diagnose(2500L, 1200L, false);
        assertThat(d.provisional()).isFalse();
        assertThat(d.flowSummary())
            .contains("외국인은 순매수(약 25억 원)")
            .contains("기관은 순매수(약 12억 원)")
            .contains("함께 사들여");
    }

    @Test
    void 동반_순매도는_수급부담_문장() {
        FlowDiagnosis d = FlowDiagnoser.diagnose(-3000L, -800L, false);
        assertThat(d.flowSummary())
            .contains("외국인은 순매도(약 30억 원)")
            .contains("함께 팔아 수급 부담");
    }

    @Test
    void 방향이_엇갈리면_엇갈림_문장() {
        FlowDiagnosis d = FlowDiagnoser.diagnose(2000L, -1500L, false);
        assertThat(d.flowSummary())
            .contains("외국인은 순매수")
            .contains("기관은 순매도")
            .contains("서로 엇갈렸");
    }

    @Test
    void 소액은_중립으로_본다() {
        // 50백만원 < 1억 최소치 → 중립
        FlowDiagnosis d = FlowDiagnoser.diagnose(50L, -30L, false);
        assertThat(d.flowSummary()).contains("뚜렷한 매매는 없었");
    }

    @Test
    void 장중_잠정이면_잠정_표기와_플래그() {
        FlowDiagnosis d = FlowDiagnoser.diagnose(5000L, 0L, true);
        assertThat(d.provisional()).isTrue();
        assertThat(d.flowSummary()).startsWith("장중 잠정 집계로는,");
    }

    @Test
    void 데이터_없으면_안내_문장() {
        FlowDiagnosis d = FlowDiagnoser.diagnose(null, null, false);
        assertThat(d.flowSummary()).contains("데이터가 아직 없");
    }

    @Test
    void 조언성_금지어는_들어가지_않는다() {
        FlowDiagnosis d = FlowDiagnoser.diagnose(4000L, -2000L, true);
        assertThat(d.flowSummary()).doesNotContain("매수하", "매도하", "추천", "목표가", "사세요", "파세요");
    }
}
