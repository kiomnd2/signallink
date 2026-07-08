package io.signallink.issue.adapter.out.external.naver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.signallink.issue.application.port.out.NewsCandidate;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 네이버 뉴스 검색 API 응답 매핑.
 *
 * <p>title/description에는 {@code <b>} 태그·HTML 엔티티가 섞여 오므로 제거한다.
 * pubDate는 RFC 1123 형식(예: {@code Mon, 26 Sep 2016 07:50:00 +0900}).
 * 결함 있는 항목은 로그 후 건너뛴다(한 건이 전체를 유실시키지 않음).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverNewsResponse(int total, List<Item> items) {

    private static final Logger log = LoggerFactory.getLogger(NaverNewsResponse.class);

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String title, String originallink, String link, String description, String pubDate) {}

    public List<NewsCandidate> toCandidates(String stockCode) {
        if (items == null) {
            return List.of();
        }
        List<NewsCandidate> candidates = new ArrayList<>(items.size());
        for (Item item : items) {
            NewsCandidate c = toCandidate(stockCode, item);
            if (c != null) {
                candidates.add(c);
            }
        }
        return candidates;
    }

    private static NewsCandidate toCandidate(String stockCode, Item item) {
        if (item.link() == null || item.pubDate() == null || item.title() == null) {
            log.warn("네이버 뉴스 항목 건너뜀 (필수 필드 누락): {}", item.link());
            return null;
        }
        try {
            OffsetDateTime published = OffsetDateTime.parse(item.pubDate(), DateTimeFormatter.RFC_1123_DATE_TIME);
            return new NewsCandidate(
                stockCode,
                trunc(clean(item.title()), 300),
                clean(item.description()),
                trunc(item.link(), 500),
                sourceOf(item.originallink()),
                published);
        } catch (DateTimeParseException e) {
            log.warn("네이버 뉴스 항목 건너뜀 (pubDate 파싱 실패): {}", item.pubDate());
            return null;
        }
    }

    /** HTML 태그 제거 + 주요 엔티티 디코드(&amp;는 이중 디코드 방지 위해 마지막). */
    static String clean(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("<[^>]+>", "")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
            .replace("&#39;", "'").replace("&nbsp;", " ").replace("&amp;", "&")
            .strip();
    }

    static String trunc(String s, int max) {
        return (s == null || s.length() <= max) ? s : s.substring(0, max);
    }

    static String sourceOf(String originalLink) {
        if (originalLink == null) {
            return null;
        }
        try {
            return trunc(URI.create(originalLink).getHost(), 50);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
