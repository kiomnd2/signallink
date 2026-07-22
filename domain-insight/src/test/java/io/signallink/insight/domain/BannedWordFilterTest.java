package io.signallink.insight.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BannedWordFilterTest {

    @Test
    void 조언성_문구는_걸러낸다() {
        assertThat(BannedWordFilter.hasAdvisory("지금 매수하기 좋은 자리예요.")).isTrue();
        assertThat(BannedWordFilter.hasAdvisory("목표가는 8만원입니다.")).isTrue();
        assertThat(BannedWordFilter.hasAdvisory("투자의견 매수로 상향.")).isTrue();
        assertThat(BannedWordFilter.hasAdvisory("손절 라인을 지키세요.")).isTrue();
    }

    @Test
    void 수급_사실_표현은_조언이_아니다() {
        // 순매수/순매도/매수세 등은 사실 → 폴백 유발 금지(오탐 방지)
        assertThat(BannedWordFilter.hasAdvisory("외국인이 순매수했어요.")).isFalse();
        assertThat(BannedWordFilter.hasAdvisory("기관은 순매도로 전환했습니다.")).isFalse();
        assertThat(BannedWordFilter.hasAdvisory("외국인 매수세가 강했어요.")).isFalse();
    }

    @Test
    void 사실_설명_문장은_통과한다() {
        assertThat(BannedWordFilter.hasAdvisory(
            "오늘은 코스피가 오른 시장 흐름의 영향이 컸어요. 공급계약 공시도 있었습니다.")).isFalse();
    }

    @Test
    void null_또는_빈문자열은_false다() {
        assertThat(BannedWordFilter.hasAdvisory(null)).isFalse();
        assertThat(BannedWordFilter.hasAdvisory("  ")).isFalse();
    }
}
