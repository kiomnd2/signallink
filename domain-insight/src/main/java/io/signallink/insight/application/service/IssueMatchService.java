package io.signallink.insight.application.service;

import io.signallink.common.TimeUtils;
import io.signallink.insight.domain.FeatureCandidate;
import io.signallink.insight.domain.IssueMatch;
import io.signallink.insight.domain.IssueMatcher;
import io.signallink.insight.domain.IssueRef;
import io.signallink.insight.domain.IssueType;
import io.signallink.issue.application.port.out.DisclosureRef;
import io.signallink.issue.application.port.out.IssueQueryPort;
import io.signallink.issue.application.port.out.NewsRef;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 이슈 매칭 유스케이스 — 특징주 한 건의 거래일(KST 하루 시간창)에 걸린 공시·뉴스를 issue 조회 포트로
 * 읽어 근거 후보({@link IssueMatch})를 정리한다. 공시 우선·최신순·최대 3건은 {@link IssueMatcher}가 판정.
 */
@Service
@RequiredArgsConstructor
public class IssueMatchService {

    private final IssueQueryPort issueQuery;

    public IssueMatch match(FeatureCandidate c) {
        OffsetDateTime from = c.tradeDate().atStartOfDay(TimeUtils.KST).toOffsetDateTime();
        OffsetDateTime to = c.tradeDate().atTime(LocalTime.MAX).atZone(TimeUtils.KST).toOffsetDateTime();

        List<IssueRef> disclosures = issueQuery.disclosures(c.stockCode(), from, to).stream()
            .map(IssueMatchService::toRef)
            .toList();
        List<IssueRef> news = issueQuery.news(c.stockCode(), from, to).stream()
            .map(IssueMatchService::toRef)
            .toList();

        return IssueMatcher.match(disclosures, news);
    }

    private static IssueRef toRef(DisclosureRef d) {
        return new IssueRef(IssueType.DISCLOSURE, d.title(), d.dartUrl(), d.disclosedAt(), d.category());
    }

    private static IssueRef toRef(NewsRef n) {
        return new IssueRef(IssueType.NEWS, n.title(), n.url(), n.publishedAt(), n.source());
    }
}
