package io.signallink.insight.domain;

import java.util.List;
import java.util.Locale;

/**
 * 이슈 성격 태그 분류(순수 도메인) — 매칭된 공시·뉴스({@link IssueRef})의 제목·카테고리를 룰로 훑어
 * {@link IssueNature}를 판정한다.
 *
 * <p>판정 우선순위: <b>구조적 키워드가 하나라도 있으면 STRUCTURAL</b>(오래 갈 이슈를 놓치지 않는 쪽으로 보수적)
 * → 일회성 키워드가 있으면 ONE_OFF → 그 외/근거 없음은 UNCLASSIFIED. LLM 보조 분류는 후속(BACKLOG).
 */
public final class IssueNatureClassifier {

    private IssueNatureClassifier() {}

    /** 구조적 문제 신호 — 실적·존속·지배구조 등 오래 갈 수 있는 이슈. */
    private static final List<String> STRUCTURAL = List.of(
        "적자", "영업손실", "순손실", "자본잠식", "감자", "상장폐지", "관리종목", "거래정지",
        "횡령", "배임", "분식", "감사의견", "의견거절", "한정", "부도", "회생", "파산",
        "규제", "제재", "과징금", "리콜", "영업정지", "실적 악화", "가이던스 하향", "구조조정");

    /** 일회성 사건 신호 — 단발성 이벤트. */
    private static final List<String> ONE_OFF = List.of(
        "공급계약", "수주", "계약 체결", "단일판매", "자기주식", "자사주", "배당", "무상증자",
        "특허", "신제품", "임상", "수출", "낙찰", "인수", "합병", "제휴", "협약", "소송");

    /** 근거 리스트로 이슈 성격을 판정한다. */
    public static IssueNature classify(List<IssueRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return IssueNature.UNCLASSIFIED;
        }
        boolean oneOff = false;
        for (IssueRef r : refs) {
            String hay = haystack(r);
            if (containsAny(hay, STRUCTURAL)) {
                return IssueNature.STRUCTURAL; // 보수적: 구조적 신호 하나라도 나오면 즉시
            }
            if (containsAny(hay, ONE_OFF)) {
                oneOff = true;
            }
        }
        return oneOff ? IssueNature.ONE_OFF : IssueNature.UNCLASSIFIED;
    }

    private static String haystack(IssueRef r) {
        String title = r.title() == null ? "" : r.title();
        String detail = r.detail() == null ? "" : r.detail();
        return (title + " " + detail).toLowerCase(Locale.KOREA);
    }

    private static boolean containsAny(String hay, List<String> keywords) {
        for (String k : keywords) {
            if (hay.contains(k.toLowerCase(Locale.KOREA))) {
                return true;
            }
        }
        return false;
    }
}
