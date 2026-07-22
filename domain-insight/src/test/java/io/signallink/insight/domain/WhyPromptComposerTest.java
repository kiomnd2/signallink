package io.signallink.insight.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class WhyPromptComposerTest {

    private static WhyContext ctx(BigDecimal marketContrib, Driver driver, List<IssueRef> issues) {
        return new WhyContext("005930", new BigDecimal("6.20"), TriggerType.SURGE,
            marketContrib, driver, true, "외국인은 순매수, 기관은 순매도예요.", issues);
    }

    @Test
    void 사실과_조언금지_규칙을_프롬프트에_넣는다() {
        IssueRef disc = new IssueRef(IssueType.DISCLOSURE, "단일판매·공급계약체결",
            "http://dart/1", OffsetDateTime.parse("2026-07-08T09:00+09:00"), "수시공시");
        String prompt = WhyPromptComposer.compose(
            ctx(new BigDecimal("2.00"), Driver.STOCK_SPECIFIC, List.of(disc)));

        assertThat(prompt).contains("005930");
        assertThat(prompt).contains("+6.20%");                 // 등락률
        assertThat(prompt).contains("단일판매·공급계약체결");    // 이슈 제목
        assertThat(prompt).contains("외국인은 순매수");          // 수급
        assertThat(prompt).contains("투자 조언");                // 조언 금지 규칙 명시
        assertThat(prompt).contains("개별 요인이 큼");           // STOCK_SPECIFIC 설명
    }

    @Test
    void 베타부족이면_개별움직임으로_설명하고_이슈없음을_표기한다() {
        String prompt = WhyPromptComposer.compose(ctx(null, null, List.of()));

        assertThat(prompt).contains("베타·지수");
        assertThat(prompt).contains("매칭된 공시·뉴스 없음");
    }
}
