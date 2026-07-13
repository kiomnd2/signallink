package io.signallink.insight.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.insight.domain.IssueMatch;
import io.signallink.insight.domain.IssueType;
import io.signallink.insight.domain.FeatureCandidate;
import io.signallink.insight.domain.TriggerType;
import io.signallink.issue.application.port.out.DisclosureRef;
import io.signallink.issue.application.port.out.IssueQueryPort;
import io.signallink.issue.application.port.out.NewsRef;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 이슈 매칭 유스케이스 — issue 조회 포트 스텁으로 시간창 조회·매핑·정렬을 검증. */
class IssueMatchServiceTest {

    private static final LocalDate D = LocalDate.of(2026, 7, 8);

    private static FeatureCandidate candidate() {
        return new FeatureCandidate("005930", D, new BigDecimal("6.0"), TriggerType.SURGE, 1000L);
    }

    private static OffsetDateTime at(int hour) {
        return OffsetDateTime.of(2026, 7, 8, hour, 0, 0, 0, ZoneOffset.ofHours(9));
    }

    @Test
    void 공시와_뉴스를_읽어_공시우선으로_매핑한다() {
        var service = new IssueMatchService(new IssueQueryPort() {
            @Override public List<DisclosureRef> disclosures(String s, OffsetDateTime f, OffsetDateTime t) {
                return List.of(new DisclosureRef("단일판매공급계약", "수시공시", at(9), "http://dart/1"));
            }
            @Override public List<NewsRef> news(String s, OffsetDateTime f, OffsetDateTime t) {
                return List.of(new NewsRef("실적 기대감", "연합뉴스", at(13), "http://news/1"));
            }
        });

        IssueMatch m = service.match(candidate());

        assertThat(m.refs()).hasSize(2);
        assertThat(m.primary().type()).isEqualTo(IssueType.DISCLOSURE);
        assertThat(m.primary().title()).isEqualTo("단일판매공급계약");
        assertThat(m.primary().detail()).isEqualTo("수시공시");   // 공시 detail = 카테고리
        assertThat(m.refs().get(1).detail()).isEqualTo("연합뉴스"); // 뉴스 detail = 출처
    }

    @Test
    void 시간창은_거래일_KST_하루로_조회한다() {
        var captured = new OffsetDateTime[2];
        var service = new IssueMatchService(new IssueQueryPort() {
            @Override public List<DisclosureRef> disclosures(String s, OffsetDateTime f, OffsetDateTime t) {
                captured[0] = f;
                captured[1] = t;
                return List.of();
            }
            @Override public List<NewsRef> news(String s, OffsetDateTime f, OffsetDateTime t) {
                return List.of();
            }
        });

        service.match(candidate());

        assertThat(captured[0]).isEqualTo(at(0));                 // 2026-07-08 00:00 +09:00
        assertThat(captured[1].toLocalDate()).isEqualTo(D);       // 같은 날 끝(23:59:59.999...)
        assertThat(captured[1].getHour()).isEqualTo(23);
    }

    @Test
    void 근거가_없으면_빈_매칭() {
        var service = new IssueMatchService(new IssueQueryPort() {
            @Override public List<DisclosureRef> disclosures(String s, OffsetDateTime f, OffsetDateTime t) {
                return List.of();
            }
            @Override public List<NewsRef> news(String s, OffsetDateTime f, OffsetDateTime t) {
                return List.of();
            }
        });

        assertThat(service.match(candidate()).isEmpty()).isTrue();
    }
}
