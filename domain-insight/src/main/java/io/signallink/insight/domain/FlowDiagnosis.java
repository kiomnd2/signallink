package io.signallink.insight.domain;

/**
 * 수급 진단 결과 — feature_card의 flow_summary로 이어진다.
 * {@code provisional=true}면 장중 가집계 기준(마감 후 확정으로 덮어써짐)이라 카드에 "잠정" 뱃지를 붙인다.
 */
public record FlowDiagnosis(
    String flowSummary,
    boolean provisional) {}
