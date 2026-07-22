package io.signallink.insight.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.insight.application.port.out.LlmGatewayPort;
import io.signallink.insight.domain.Driver;
import io.signallink.insight.domain.IssueRef;
import io.signallink.insight.domain.IssueType;
import io.signallink.insight.domain.TriggerType;
import io.signallink.insight.domain.WhyContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class WhySummaryServiceTest {

    private static final LocalDate D = LocalDate.of(2026, 7, 8);
    private static final String CLEAN = "오늘은 공급계약 공시가 나오면서 주가가 올랐어요.";

    private static WhyContext withIssue() {
        IssueRef disc = new IssueRef(IssueType.DISCLOSURE, "공급계약", "http://dart/1",
            OffsetDateTime.parse("2026-07-08T09:00+09:00"), "수시공시");
        return ctx(List.of(disc));
    }

    private static WhyContext ctx(List<IssueRef> issues) {
        return new WhyContext("005930", new BigDecimal("6.0"), TriggerType.SURGE,
            new BigDecimal("2.0"), Driver.STOCK_SPECIFIC, false, "수급 중립", issues);
    }

    @Test
    void 비활성이면_LLM을_호출하지_않고_empty다() {
        boolean[] called = {false};
        LlmGatewayPort llm = p -> { called[0] = true; return CLEAN; };
        var svc = new WhySummaryService(llm, false, 60);

        assertThat(svc.summarize(withIssue(), D)).isEmpty();
        assertThat(called[0]).isFalse();
    }

    @Test
    void 이슈가_없으면_LLM을_호출하지_않고_empty다() {
        boolean[] called = {false};
        LlmGatewayPort llm = p -> { called[0] = true; return CLEAN; };
        var svc = new WhySummaryService(llm, true, 60);

        assertThat(svc.summarize(ctx(List.of()), D)).isEmpty();
        assertThat(called[0]).isFalse();
    }

    @Test
    void 클린한_요약은_그대로_반환한다() {
        var svc = new WhySummaryService(p -> CLEAN, true, 60);
        assertThat(svc.summarize(withIssue(), D)).contains(CLEAN);
    }

    @Test
    void 금지어가_섞이면_empty다() {
        var svc = new WhySummaryService(p -> "지금 매수하기 좋아요.", true, 60);
        assertThat(svc.summarize(withIssue(), D)).isEmpty();
    }

    @Test
    void LLM이_예외를_던지면_empty다() {
        var svc = new WhySummaryService(p -> { throw new RuntimeException("timeout"); }, true, 60);
        assertThat(svc.summarize(withIssue(), D)).isEmpty();
    }

    @Test
    void 빈_응답은_empty다() {
        var svc = new WhySummaryService(p -> "   ", true, 60);
        assertThat(svc.summarize(withIssue(), D)).isEmpty();
    }

    @Test
    void 일_상한을_넘으면_이후_호출은_empty다() {
        var svc = new WhySummaryService(p -> CLEAN, true, 1);

        assertThat(svc.summarize(withIssue(), D)).isPresent();   // 1건 소비
        assertThat(svc.summarize(withIssue(), D)).isEmpty();     // 상한 초과
    }

    @Test
    void 거래일이_바뀌면_상한이_리셋된다() {
        var svc = new WhySummaryService(p -> CLEAN, true, 1);

        assertThat(svc.summarize(withIssue(), D)).isPresent();
        assertThat(svc.summarize(withIssue(), D)).isEmpty();
        assertThat(svc.summarize(withIssue(), D.plusDays(1))).isPresent(); // 새 거래일 → 리셋
    }
}
