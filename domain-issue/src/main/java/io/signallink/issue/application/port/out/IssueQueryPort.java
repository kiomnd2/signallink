package io.signallink.issue.application.port.out;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 아웃바운드 포트 — 종목·시간창 기준 공시/뉴스 조회(insight 이슈매칭이 읽음).
 * 구현: adapter.out.persistence.IssueQueryPersistenceAdapter.
 */
public interface IssueQueryPort {
    /** 종목의 [from, to] 시간창 공시. */
    List<DisclosureRef> disclosures(String stockCode, OffsetDateTime from, OffsetDateTime to);

    /** 종목의 [from, to] 시간창 뉴스. */
    List<NewsRef> news(String stockCode, OffsetDateTime from, OffsetDateTime to);
}
