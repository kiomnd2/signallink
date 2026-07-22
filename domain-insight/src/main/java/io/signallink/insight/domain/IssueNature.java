package io.signallink.insight.domain;

/**
 * 이슈 성격 태그 — 체력 진단 팩트 ① (health_check.issue_nature).
 *
 * <p>급등락을 부른 이슈가 <b>일회성 사건</b>인지 <b>구조적 문제</b>인지 분류한다. "이 악재 오래 갈까?"의
 * 첫 판단 재료. 애매하면 {@link #UNCLASSIFIED}("분류 불가")로 두고 결론은 사용자에게 맡긴다(조언 금지).
 */
public enum IssueNature {

    /** 일회성 사건 — 단발 수주/계약·자사주·배당·소송 등 지나가는 이벤트. */
    ONE_OFF("일회성 사건"),

    /** 구조적 문제 — 적자 전환·주력사업 규제·상장폐지 위험 등 오래 갈 수 있는 이슈. */
    STRUCTURAL("구조적 문제"),

    /** 분류 불가 — 근거가 없거나 룰로 판단하기 애매. */
    UNCLASSIFIED("분류 불가");

    private final String label;

    IssueNature(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
