package io.signallink.issue.application.port.out;

import java.time.OffsetDateTime;

/** 이슈 매칭용 뉴스 조회 결과(insight가 읽음). */
public record NewsRef(
    String title,
    String source,
    OffsetDateTime publishedAt,
    String url
) {}
