package io.signallink.insight.domain;

/**
 * feature_card 노출 상태. 파이프라인이 조립한 카드는 기본 PUBLISHED(노출).
 * LLM 조언성 필터 등에서 문제가 감지되면 SUPPRESSED(비노출)로 내릴 수 있다(3-E/운영).
 */
public enum CardStatus {
    PUBLISHED,
    SUPPRESSED
}
