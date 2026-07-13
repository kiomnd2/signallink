package io.signallink.insight.domain;

import java.util.List;

/**
 * 이슈 매칭 결과 — 급등락의 근거 후보(공시 우선, 최대 {@value IssueMatcher#MAX_REFS}건).
 * feature_card.source_refs(jsonb)로 이어진다. 후보가 없으면 refs는 빈 리스트.
 */
public record IssueMatch(List<IssueRef> refs) {

    public boolean isEmpty() {
        return refs.isEmpty();
    }

    /** 가장 우선하는 근거(공시 우선, 없으면 최신 뉴스). 없으면 null. */
    public IssueRef primary() {
        return refs.isEmpty() ? null : refs.get(0);
    }
}
