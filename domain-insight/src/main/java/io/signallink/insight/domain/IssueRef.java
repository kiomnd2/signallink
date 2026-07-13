package io.signallink.insight.domain;

import java.time.OffsetDateTime;

/**
 * 이슈 근거 한 건 — feature_card.source_refs(jsonb) 항목의 도메인 표현.
 * {@code detail}은 공시 카테고리 또는 뉴스 출처.
 */
public record IssueRef(
    IssueType type,
    String title,
    String url,
    OffsetDateTime occurredAt,
    String detail) {}
