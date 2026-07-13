package io.signallink.insight.application.port.out;

import io.signallink.insight.domain.CardStatus;
import io.signallink.insight.domain.IssueRef;
import io.signallink.insight.domain.TriggerType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 카드 upsert 커맨드 — 파이프라인이 조립한 한 종목의 분석 결과. 근거(sourceRefs)는 도메인 {@link IssueRef}
 * 리스트로 넘기고, jsonb 직렬화는 영속 어댑터가 담당(애플리케이션은 기술 의존 없음).
 */
public record FeatureCardUpsert(
    String stockCode,
    LocalDate tradeDate,
    BigDecimal changeRate,
    TriggerType triggerType,
    BigDecimal marketContrib,
    Boolean sectorSync,
    String whatHappened,
    String flowSummary,
    String contextNote,
    List<IssueRef> sourceRefs,
    boolean llmUsed,
    CardStatus status) {}
