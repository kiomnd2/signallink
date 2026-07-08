package io.signallink.issue.adapter.out.external.naver;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.signallink.issue.application.port.out.NewsCandidate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** 네이버 뉴스 응답 파싱 — HTML 태그·엔티티 제거, pubDate 파싱, source 추출. */
class NaverNewsResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void 픽스처를_뉴스후보로_매핑하고_HTML을_제거한다() throws Exception {
        NaverNewsResponse res;
        try (var in = new ClassPathResource("fixtures/naver-news-sample.json").getInputStream()) {
            res = mapper.readValue(in, NaverNewsResponse.class);
        }

        List<NewsCandidate> candidates = res.toCandidates("005930");
        assertThat(candidates).hasSize(2);

        NewsCandidate first = candidates.get(0);
        assertThat(first.stockCode()).isEqualTo("005930");
        assertThat(first.title()).isEqualTo("삼성전자, 2분기 \"깜짝 실적\"");          // <b> 제거 + &quot; 디코드
        assertThat(first.summary()).isEqualTo("삼성전자가 예상을 웃도는 실적을 발표했다 & 주가 상승."); // &amp;
        assertThat(first.url()).isEqualTo("https://n.news.naver.com/mnews/article/001/0001");
        assertThat(first.source()).isEqualTo("www.example-news.com");
        assertThat(first.publishedAt().toString()).startsWith("2026-07-08T08:30+09:00");

        assertThat(candidates.get(1).summary()).isEqualTo("신제품 발표 <현장>");       // &lt; &gt; 디코드
    }
}
