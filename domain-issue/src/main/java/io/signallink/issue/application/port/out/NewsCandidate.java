package io.signallink.issue.application.port.out;

import java.time.OffsetDateTime;

/** 네이버 뉴스 게이트웨이가 돌려주는 "가공 전 뉴스 후보"(HTML 태그 제거된 상태). */
public record NewsCandidate(
    String stockCode,
    String title,
    String summary,
    String url,
    String source,
    OffsetDateTime publishedAt
) {}
