package io.signallink.market.domain;

/**
 * 시장(marketType)을 대표 지수 코드로 매핑 — 시장분해에서 "이 종목이 속한 시장이 얼마나 움직였나"의 기준.
 * 코드는 KIS 지수 시세 TR(FHKUP03500100) 기준: 0001=코스피, 1001=코스닥.
 */
public enum MarketIndex {

    KOSPI("0001", "코스피"),
    KOSDAQ("1001", "코스닥");

    private final String code;
    private final String label;

    MarketIndex(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    /** marketType(KOSPI|KOSDAQ) → 대표 지수. 알 수 없으면 null. */
    public static MarketIndex forMarketType(String marketType) {
        if (marketType == null) {
            return null;
        }
        return switch (marketType) {
            case "KOSPI" -> KOSPI;
            case "KOSDAQ" -> KOSDAQ;
            default -> null;
        };
    }
}
