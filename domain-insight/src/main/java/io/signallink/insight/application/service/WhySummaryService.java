package io.signallink.insight.application.service;

import io.signallink.insight.application.port.out.LlmGatewayPort;
import io.signallink.insight.domain.BannedWordFilter;
import io.signallink.insight.domain.WhyContext;
import io.signallink.insight.domain.WhyPromptComposer;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Why 카드의 "무슨 일이 있었나(what_happened)" LLM 요약(3-E).
 *
 * <p>흐름: <b>뉴스/공시 있는 종목만</b> → <b>일 상한 가드</b> → 페이로드 조립 → LLM 호출 →
 * <b>금지어(조언성) 필터</b> → 성공 시 요약, 그 외 모두 {@link Optional#empty()}(파이프라인이 템플릿 폴백).
 *
 * <p>키 미설정 등으로 {@code signallink.llm.enabled=false}면 호출 자체를 건너뛴다(기본값 false —
 * Gemini 키 발급 전까지 기존 템플릿 동작 유지). 일 상한은 처리 중인 거래일 기준 인메모리 카운터.
 */
@Service
@RequiredArgsConstructor
public class WhySummaryService {

    private static final Logger log = LoggerFactory.getLogger(WhySummaryService.class);

    private final LlmGatewayPort llm;

    @Value("${signallink.llm.enabled:false}")
    private final boolean enabled;

    @Value("${signallink.llm.daily-limit:60}")
    private final int dailyLimit;

    private LocalDate budgetDate;
    private int used;

    /** @return LLM 요약(성공·클린), 비활성/이슈없음/상한초과/실패/금지어면 empty(템플릿 폴백). */
    public Optional<String> summarize(WhyContext ctx, LocalDate tradeDate) {
        if (!enabled) {
            return Optional.empty();
        }
        if (ctx.issues() == null || ctx.issues().isEmpty()) {
            return Optional.empty(); // 근거 이슈 없는 종목은 LLM 생략(비용·품질) → 템플릿
        }
        if (!tryConsumeBudget(tradeDate)) {
            log.info("LLM 일 상한({}) 도달, 템플릿 폴백: {}", dailyLimit, ctx.stockCode());
            return Optional.empty();
        }
        try {
            String summary = llm.generate(WhyPromptComposer.compose(ctx));
            if (summary == null || summary.isBlank()) {
                return Optional.empty();
            }
            summary = summary.strip();
            if (BannedWordFilter.hasAdvisory(summary)) {
                log.warn("LLM 출력 금지어(조언성) 감지, 템플릿 폴백: {}", ctx.stockCode());
                return Optional.empty();
            }
            return Optional.of(summary);
        } catch (RuntimeException e) {
            log.warn("LLM 요약 실패, 템플릿 폴백: {} ({})", ctx.stockCode(), e.toString());
            return Optional.empty();
        }
    }

    /** 거래일이 바뀌면 카운터 리셋. 상한 미만이면 1 소비하고 true. */
    private synchronized boolean tryConsumeBudget(LocalDate tradeDate) {
        if (!tradeDate.equals(budgetDate)) {
            budgetDate = tradeDate;
            used = 0;
        }
        if (used >= dailyLimit) {
            return false;
        }
        used++;
        return true;
    }
}
