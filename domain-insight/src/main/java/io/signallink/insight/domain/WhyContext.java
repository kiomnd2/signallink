package io.signallink.insight.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * LLM 요약(3-E)에 넣을 한 종목의 분석 사실 묶음 — 파이프라인이 3-B~3-D 결과를 모아 조립한다.
 * {@link WhyPromptComposer}가 이 사실만 근거로 프롬프트를 만든다(모델이 지어내지 않도록).
 *
 * <p>{@code marketContrib}/{@code driver}는 베타·지수가 부족하면 null일 수 있다.
 */
public record WhyContext(
    String stockCode,
    BigDecimal changeRate,
    TriggerType triggerType,
    BigDecimal marketContrib,
    Driver driver,
    boolean sectorSync,
    String flowSummary,
    List<IssueRef> issues) {}
