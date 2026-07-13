package io.signallink.insight.domain;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * "무슨 일이 있었나(what_happened)" 초보자용 템플릿 문장 생성(순수 도메인).
 * 시장분해 결과와 업종 동반 여부를 초보자 언어로 옮긴다. LLM(3-E) 실패 시 폴백 문장이기도 하다.
 * 조언(매수/매도/목표가)은 절대 넣지 않는다.
 */
public final class WhatHappenedComposer {

    private WhatHappenedComposer() {}

    public static String compose(String marketLabel, MarketDecomposition d,
                                 BigDecimal changeRate, BigDecimal indexChangeRate, boolean sectorSync) {
        String dir = changeRate.signum() >= 0 ? "올랐" : "내렸";
        StringBuilder sb = new StringBuilder();

        if (d.marketContrib() == null) {
            sb.append(fmt("오늘 이 종목은 %+.2f%% %s어요. 시장 대비 기준(베타·지수)이 아직 부족해 개별 움직임으로 봤어요.",
                changeRate, dir));
        } else if (d.driver() == Driver.MARKET_DRIVEN) {
            sb.append(fmt("오늘은 %s가 %+.2f%% 움직인 시장 흐름의 영향이 컸어요. ", marketLabel, indexChangeRate))
              .append(fmt("이 종목 등락(%+.2f%%) 중 약 %+.2f%%p가 시장 몫이에요.", changeRate, d.marketContrib()));
        } else {
            sb.append(fmt("시장 영향을 감안한 몫(%+.2f%%p)을 넘어서는 움직임이라, ", d.marketContrib()))
              .append(fmt("이 종목만의 개별 요인이 커 보여요(초과 %+.2f%%p).", d.excess()));
        }

        if (sectorSync) {
            sb.append(" 같은 업종 종목들도 대체로 함께 움직여, 업종 전반의 이슈일 수 있어요.");
        }
        return sb.toString();
    }

    private static String fmt(String pattern, Object... args) {
        return String.format(Locale.KOREA, pattern, args);
    }
}
