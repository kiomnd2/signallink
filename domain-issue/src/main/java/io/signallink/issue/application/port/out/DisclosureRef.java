package io.signallink.issue.application.port.out;

import java.time.OffsetDateTime;

/** 이슈 매칭용 공시 조회 결과(insight가 읽음). */
public record DisclosureRef(
    String title,
    String category,
    OffsetDateTime disclosedAt,
    String dartUrl
) {}
