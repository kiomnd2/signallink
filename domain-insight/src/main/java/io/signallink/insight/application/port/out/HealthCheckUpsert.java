package io.signallink.insight.application.port.out;

import io.signallink.insight.domain.IssueNature;

/**
 * 체력 진단 upsert 커맨드 — 카드 1건에 딸린 진단 팩트. {@code card_id} 기준 1:1 upsert.
 *
 * <p>{@code issueNature}(팩트①)는 3-7a에서 채운다. {@code revenueTrend}/{@code profitTrend}(팩트②,
 * DART 재무)는 3-7c, {@code flowAfter}(팩트③, jsonb)는 3-7b(J6)에서 채우고 그 전에는 null.
 */
public record HealthCheckUpsert(
    long cardId,
    IssueNature issueNature,
    String revenueTrend,
    String profitTrend,
    String flowAfter) {

    /** 팩트①만 있는 진단(실적·수급 추세 아직 없음). */
    public static HealthCheckUpsert ofNature(long cardId, IssueNature issueNature) {
        return new HealthCheckUpsert(cardId, issueNature, null, null, null);
    }
}
