package io.signallink.issue.application.port.out;

import java.time.OffsetDateTime;

/**
 * 아웃바운드 게이트웨이(DART)가 돌려주는 "가공 전 공시 후보".
 *
 * <p>외부 API 응답 형식과 도메인을 분리하는 경계 타입. 카테고리 분류·중복 판별은
 * 유스케이스(DisclosureCollectService)가 수행한다.
 */
public record DisclosureCandidate(
    String receiptNo,
    String stockCode,   // 상장사만 값 존재, 비상장은 blank
    String title,
    OffsetDateTime disclosedAt,
    String dartUrl
) {}
