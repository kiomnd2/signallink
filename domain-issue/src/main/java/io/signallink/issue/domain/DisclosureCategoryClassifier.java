package io.signallink.issue.domain;

import java.util.List;

/**
 * 공시 제목(DART report_nm) 키워드로 카테고리를 분류하는 도메인 규칙.
 *
 * <p>프레임워크 비의존 순수 로직 — 단독 단위 테스트 가능. 규칙은 위에서부터 먼저 걸리는 것을 채택한다.
 * (스펙 확정 시 규칙 확장 — docs/M2-domain-issue-plan.md §4)
 */
public class DisclosureCategoryClassifier {

    public static final String ETC = "기타";

    private static final List<Rule> RULES = List.of(
        new Rule("수주계약", List.of("공급계약", "수주", "단일판매")),
        new Rule("실적",     List.of("잠정실적", "영업(잠정)", "매출액또는손익", "실적")),
        new Rule("자금조달", List.of("유상증자", "전환사채", "신주인수권", "교환사채")),
        new Rule("소송",     List.of("소송", "분쟁")),
        new Rule("지분변동", List.of("최대주주", "대량보유", "주요주주", "지분"))
    );

    /** 제목에 규칙 키워드가 포함되면 해당 카테고리, 아니면 {@link #ETC}. */
    public String classify(String title) {
        if (title == null) {
            return ETC;
        }
        for (Rule rule : RULES) {
            for (String keyword : rule.keywords()) {
                if (title.contains(keyword)) {
                    return rule.category();
                }
            }
        }
        return ETC;
    }

    private record Rule(String category, List<String> keywords) {}
}
