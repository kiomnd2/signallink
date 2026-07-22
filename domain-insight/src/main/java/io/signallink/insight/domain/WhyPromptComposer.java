package io.signallink.insight.domain;

import java.util.Locale;

/**
 * "왜 움직였나" LLM 프롬프트 조립(순수 도메인). {@link WhyContext}의 사실만 넣고, 조언 금지·초보자 언어를
 * 규칙으로 명시한다. 출력은 설명 문장(평문)이며, 응답 JSON 파싱은 어댑터가, 금지어 필터는 서비스가 담당.
 */
public final class WhyPromptComposer {

    private WhyPromptComposer() {}

    public static String compose(WhyContext c) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 주식 초보자에게 오늘 특정 종목이 왜 움직였는지 쉽고 정확하게 설명하는 도우미입니다.\n");
        sb.append("아래 '사실'만 근거로, 초보자도 이해할 수 있는 2~3문장으로 설명하세요.\n\n");

        sb.append("[규칙]\n");
        sb.append("- 매수/매도/목표가 등 투자 조언이나 전망은 절대 쓰지 않습니다.\n");
        sb.append("- 사실에 없는 내용을 지어내지 않습니다.\n");
        sb.append("- 어려운 전문용어는 풀어서 씁니다.\n");
        sb.append("- 존댓말로 담백하게 씁니다.\n\n");

        sb.append("[사실]\n");
        sb.append(fmt("- 종목코드: %s%n", c.stockCode()));
        sb.append(fmt("- 오늘 등락률: %+.2f%%%n", c.changeRate()));
        sb.append(fmt("- 특징주 사유: %s%n", triggerLabel(c.triggerType())));
        sb.append(fmt("- 시장 vs 개별 요인: %s%n", marketVsStock(c)));
        sb.append(fmt("- 같은 업종 동반 등락: %s%n", c.sectorSync() ? "예(업종 전반 이슈 가능)" : "아니오"));
        sb.append(fmt("- 수급(외국인·기관): %s%n", blankToNone(c.flowSummary())));
        sb.append("- 관련 이슈:\n").append(issueLines(c));

        sb.append("\n설명 문장만 출력하세요.");
        return sb.toString();
    }

    private static String triggerLabel(TriggerType t) {
        if (t == TriggerType.VOLUME) {
            return "거래량이 평소(20일 평균)의 3배 이상으로 급증";
        }
        return "가격이 크게 움직임(±5% 이상 급등락)";
    }

    private static String marketVsStock(WhyContext c) {
        if (c.marketContrib() == null) {
            return "시장 대비 기준(베타·지수)이 아직 부족해 개별 움직임으로 봄";
        }
        if (c.driver() == Driver.MARKET_DRIVEN) {
            return fmt("시장(지수) 흐름의 영향이 큼 — 등락 중 약 %+.2f%%p가 시장 몫", c.marketContrib());
        }
        return fmt("시장 몫(%+.2f%%p)을 넘어서는 개별 요인이 큼", c.marketContrib());
    }

    private static String issueLines(WhyContext c) {
        if (c.issues() == null || c.issues().isEmpty()) {
            return "  (특별히 매칭된 공시·뉴스 없음)\n";
        }
        StringBuilder sb = new StringBuilder();
        for (IssueRef r : c.issues()) {
            String kind = r.type() == IssueType.DISCLOSURE ? "공시" : "뉴스";
            sb.append(fmt("  - [%s] %s%n", kind, blankToNone(r.title())));
        }
        return sb.toString();
    }

    private static String blankToNone(String s) {
        return (s == null || s.isBlank()) ? "정보 없음" : s;
    }

    private static String fmt(String pattern, Object... args) {
        return String.format(Locale.KOREA, pattern, args);
    }
}
