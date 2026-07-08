package io.signallink.market.domain;

/** 투자자 구분. 확정(inquire-investor)은 3종 전부, 장중 잠정(가집계)은 외국인·기관만. */
public enum InvestorType {
    PERSON,   // 개인
    FOREIGN,  // 외국인
    ORG       // 기관
}
