package io.signallink.insight.domain;

import java.util.Locale;

/**
 * 수급 진단(순수 도메인) — "외국인·기관이 사고 있나, 팔고 있나"를 초보자 언어로 옮긴다.
 *
 * <p>외국인·기관 순매수 금액(백만원)만 본다: 개인은 통상 그 반대편이라 초보자에겐 노이즈이고, 장중
 * 가집계(잠정)도 외국인·기관만 제공하기 때문. |금액|이 {@value #MIN_MEANINGFUL_MILLION}백만원(1억) 미만이면
 * "중립"으로 본다. 조언(매수/매도/목표가)은 절대 넣지 않는다 — 무슨 일이 있었는지만 설명한다.
 */
public final class FlowDiagnoser {

    /** 순매수 금액(백만원) 유의미 최소치 = 1억 원. */
    public static final long MIN_MEANINGFUL_MILLION = 100;

    private FlowDiagnoser() {}

    /**
     * @param foreignNetAmtMillion 외국인 순매수 금액(백만원), 데이터 없으면 null
     * @param orgNetAmtMillion     기관 순매수 금액(백만원), 데이터 없으면 null
     * @param provisional          장중 가집계(잠정) 여부
     */
    public static FlowDiagnosis diagnose(Long foreignNetAmtMillion, Long orgNetAmtMillion, boolean provisional) {
        if (foreignNetAmtMillion == null && orgNetAmtMillion == null) {
            return new FlowDiagnosis("외국인·기관 수급 데이터가 아직 없어요.", provisional);
        }
        int foreign = direction(foreignNetAmtMillion);
        int org = direction(orgNetAmtMillion);

        String prefix = provisional ? "장중 잠정 집계로는, " : "";
        String body;
        if (foreign == 0 && org == 0) {
            body = "외국인·기관 모두 뚜렷한 매매는 없었어요.";
        } else {
            String sentence = actor("외국인", foreign, foreignNetAmtMillion)
                + ", " + actor("기관", org, orgNetAmtMillion) + ".";
            body = sentence + nuance(foreign, org);
        }
        return new FlowDiagnosis(prefix + body, provisional);
    }

    /** 순매수(+1) / 순매도(-1) / 중립(0). null·소액은 중립. */
    private static int direction(Long amtMillion) {
        if (amtMillion == null || Math.abs(amtMillion) < MIN_MEANINGFUL_MILLION) {
            return 0;
        }
        return amtMillion > 0 ? 1 : -1;
    }

    private static String actor(String name, int dir, Long amtMillion) {
        return switch (dir) {
            case 1 -> String.format(Locale.KOREA, "%s은 순매수(약 %,d억 원)", name, eok(amtMillion));
            case -1 -> String.format(Locale.KOREA, "%s은 순매도(약 %,d억 원)", name, eok(amtMillion));
            default -> name + "은 매매 중립";
        };
    }

    /** 방향이 같으면 동반, 엇갈리면 엇갈림. 한쪽만 움직였으면 덧붙임 없음. */
    private static String nuance(int foreign, int org) {
        if (foreign == 0 || org == 0) {
            return "";
        }
        if (foreign == org) {
            return foreign > 0
                ? " 두 주체가 함께 사들여 수급이 우호적이었어요."
                : " 두 주체가 함께 팔아 수급 부담이 있었어요.";
        }
        return " 외국인과 기관의 방향이 서로 엇갈렸어요.";
    }

    /** 백만원 → 억 원(반올림, 절대값). */
    private static long eok(Long amtMillion) {
        return Math.round(Math.abs(amtMillion) / 100.0);
    }
}
