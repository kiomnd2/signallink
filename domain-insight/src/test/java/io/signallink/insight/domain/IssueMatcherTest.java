package io.signallink.insight.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 이슈 매칭 — 공시 우선·최신순·최대 3건. */
class IssueMatcherTest {

    private static OffsetDateTime at(int hour) {
        return OffsetDateTime.of(2026, 7, 8, hour, 0, 0, 0, ZoneOffset.ofHours(9));
    }

    private static IssueRef disclosure(String title, int hour) {
        return new IssueRef(IssueType.DISCLOSURE, title, "http://dart/" + title, at(hour), "수시공시");
    }

    private static IssueRef news(String title, int hour) {
        return new IssueRef(IssueType.NEWS, title, "http://news/" + title, at(hour), "연합뉴스");
    }

    @Test
    void 공시가_뉴스보다_먼저_온다() {
        var match = IssueMatcher.match(
            List.of(disclosure("공급계약", 9)),
            List.of(news("장중뉴스", 14)));   // 뉴스가 더 최신이어도

        assertThat(match.refs()).hasSize(2);
        assertThat(match.primary().type()).isEqualTo(IssueType.DISCLOSURE);
        assertThat(match.refs().get(1).type()).isEqualTo(IssueType.NEWS);
    }

    @Test
    void 같은_종류는_최신순() {
        var match = IssueMatcher.match(
            List.of(),
            List.of(news("오전", 9), news("오후", 15), news("점심", 12)));

        assertThat(match.refs()).extracting(IssueRef::title)
            .containsExactly("오후", "점심", "오전");
    }

    @Test
    void 최대_3건으로_자른다() {
        var match = IssueMatcher.match(
            List.of(disclosure("공시1", 9), disclosure("공시2", 10)),
            List.of(news("뉴스1", 11), news("뉴스2", 12)));

        assertThat(match.refs()).hasSize(3);
        // 공시2(10)·공시1(9) 먼저, 그다음 뉴스2(12) → 뉴스1은 잘림
        assertThat(match.refs()).extracting(IssueRef::title)
            .containsExactly("공시2", "공시1", "뉴스2");
    }

    @Test
    void 근거가_없으면_빈_결과() {
        var match = IssueMatcher.match(List.of(), List.of());
        assertThat(match.isEmpty()).isTrue();
        assertThat(match.primary()).isNull();
    }
}
