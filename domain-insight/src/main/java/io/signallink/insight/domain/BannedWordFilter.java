package io.signallink.insight.domain;

import java.util.List;

/**
 * LLM 출력 금지어(조언성 문구) 필터 — 순수 도메인.
 *
 * <p>기획상 서비스는 "왜 움직였나"만 설명하고 <b>투자 조언(매수/매도/목표가 등)은 절대 넣지 않는다.</b>
 * LLM이 규칙을 어기고 조언성 문구를 생성하면 이 필터가 걸러 템플릿 폴백으로 되돌린다(프롬프트+출력 이중 차단).
 *
 * <p><b>오탐 방지</b>: 수급 설명의 사실 표현("외국인이 <i>순매수</i>했어요", "기관 <i>매수세</i>")은
 * 조언이 아니므로 스캔 전에 제거한다 — 그러지 않으면 정상 카드가 매번 폴백된다.
 */
public final class BannedWordFilter {

    private BannedWordFilter() {}

    /** 조언이 아닌 사실 표현(수급 등) — 스캔 전에 제거해 오탐을 막는다. */
    private static final List<String> FACTUAL_ALLOW = List.of(
        "순매수", "순매도", "매수세", "매도세", "매수량", "매도량",
        "매수 규모", "매도 규모", "매수액", "매도액", "매수 우위", "매도 우위");

    /** 조언·전망성 표현 — 하나라도 있으면 폴백. */
    private static final List<String> ADVISORY = List.of(
        "매수", "매도", "사세요", "파세요", "사라", "팔아라", "팔아",
        "목표가", "목표주가", "적정주가", "적정가", "투자의견", "추천",
        "비중확대", "비중축소", "비중 확대", "비중 축소",
        "손절", "익절", "저평가", "고평가", "강력매수",
        "불타기", "물타기", "줍줍", "담아라", "담을");

    /** 조언성 문구를 포함하면 true. */
    public static boolean hasAdvisory(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String scan = text;
        for (String factual : FACTUAL_ALLOW) {
            scan = scan.replace(factual, "");
        }
        for (String w : ADVISORY) {
            if (scan.contains(w)) {
                return true;
            }
        }
        return false;
    }
}
