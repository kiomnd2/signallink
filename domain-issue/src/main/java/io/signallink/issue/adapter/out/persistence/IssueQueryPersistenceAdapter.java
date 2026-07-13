package io.signallink.issue.adapter.out.persistence;

import io.signallink.issue.application.port.out.DisclosureRef;
import io.signallink.issue.application.port.out.IssueQueryPort;
import io.signallink.issue.application.port.out.NewsRef;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 아웃바운드 어댑터 — 종목·시간창 공시/뉴스 조회(insight 이슈매칭용). */
@Component
@RequiredArgsConstructor
public class IssueQueryPersistenceAdapter implements IssueQueryPort {

    private final DisclosureJpaRepository disclosureJpa;
    private final NewsJpaRepository newsJpa;

    @Override
    public List<DisclosureRef> disclosures(String stockCode, OffsetDateTime from, OffsetDateTime to) {
        return disclosureJpa.findByStockCodeAndDisclosedAtBetween(stockCode, from, to).stream()
            .map(d -> new DisclosureRef(d.getTitle(), d.getCategory(), d.getDisclosedAt(), d.getDartUrl()))
            .toList();
    }

    @Override
    public List<NewsRef> news(String stockCode, OffsetDateTime from, OffsetDateTime to) {
        return newsJpa.findByStockCodeAndPublishedAtBetween(stockCode, from, to).stream()
            .map(n -> new NewsRef(n.getTitle(), n.getSource(), n.getPublishedAt(), n.getUrl()))
            .toList();
    }
}
