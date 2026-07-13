package io.signallink.insight.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 이슈 매칭(순수 도메인) — 급등락의 근거 후보를 정리한다.
 *
 * <p>정책(플랜 3-D): <b>공시 우선</b> — 공시는 뉴스보다 항상 앞선다(공시는 사실, 뉴스는 해석). 같은
 * 종류 안에서는 <b>최신순</b>(발생 시각 내림차순). 합쳐서 최대 {@value #MAX_REFS}건으로 자른다.
 * 근거가 없으면 빈 결과 — 이 경우 what_happened/flow_summary만으로 카드를 구성한다.
 */
public final class IssueMatcher {

    public static final int MAX_REFS = 3;

    private IssueMatcher() {}

    public static IssueMatch match(List<IssueRef> disclosures, List<IssueRef> news) {
        List<IssueRef> ordered = new ArrayList<>();
        ordered.addAll(sortedByRecency(disclosures));
        ordered.addAll(sortedByRecency(news));
        return new IssueMatch(ordered.stream().limit(MAX_REFS).toList());
    }

    private static List<IssueRef> sortedByRecency(List<IssueRef> refs) {
        return refs.stream()
            .sorted(Comparator.comparing(IssueRef::occurredAt).reversed())
            .toList();
    }
}
