package io.signallink.insight.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.insight.domain.IssueRef;
import io.signallink.insight.domain.IssueType;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/** source_refs 수동 JSON 직렬화 — 이스케이프·null·빈 리스트. */
class SourceRefsJsonTest {

    private static final OffsetDateTime T = OffsetDateTime.of(2026, 7, 8, 9, 0, 0, 0, ZoneOffset.ofHours(9));

    @Test
    void 빈_리스트는_빈_배열() {
        assertThat(SourceRefsJson.serialize(List.of())).isEqualTo("[]");
    }

    @Test
    void 필드를_JSON_객체로_직렬화한다() {
        var ref = new IssueRef(IssueType.NEWS, "실적 기대", "http://news/1", T, "연합뉴스");
        String json = SourceRefsJson.serialize(List.of(ref));
        assertThat(json)
            .startsWith("[{").endsWith("}]")
            .contains("\"type\":\"NEWS\"")
            .contains("\"title\":\"실적 기대\"")
            .contains("\"url\":\"http://news/1\"")
            .contains("\"detail\":\"연합뉴스\"")
            .contains("\"occurredAt\":\"2026-07-08T09:00+09:00\"");
    }

    @Test
    void 따옴표와_역슬래시를_이스케이프한다() {
        var ref = new IssueRef(IssueType.DISCLOSURE, "계약 \"체결\" \\건", "http://dart/1", T, null);
        String json = SourceRefsJson.serialize(List.of(ref));
        assertThat(json)
            .contains("\\\"체결\\\"")
            .contains("\\\\건")
            .contains("\"detail\":null");
    }

    @Test
    void 여러_건은_쉼표로_구분() {
        var a = new IssueRef(IssueType.DISCLOSURE, "A", "http://a", T, "공시");
        var b = new IssueRef(IssueType.NEWS, "B", "http://b", T, "뉴스");
        String json = SourceRefsJson.serialize(List.of(a, b));
        assertThat(json).contains("},{");
    }
}
