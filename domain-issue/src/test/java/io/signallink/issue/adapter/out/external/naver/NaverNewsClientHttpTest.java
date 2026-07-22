package io.signallink.issue.adapter.out.external.naver;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.issue.application.port.out.NewsCandidate;
import java.nio.charset.StandardCharsets;
import java.util.List;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * NaverNewsClient HTTP 경로 검증 — 목 서버로 요청 조립(경로·쿼리·인증 헤더)과 응답 매핑을 확인한다.
 * (HTML 제거·pubDate 파싱 등 응답 매핑 세부는 {@code NaverNewsResponseTest}에서 별도 검증.)
 */
class NaverNewsClientHttpTest {

    private MockWebServer server;
    private NaverNewsClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        String baseUrl = "http://" + server.getHostName() + ":" + server.getPort();
        // permits 크게 → 테스트에서 rate limit 대기 없음
        client = new NaverNewsClient(baseUrl, "TEST_ID", "TEST_SECRET", 1000);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void 검색_요청에_쿼리와_인증_헤더를_붙인다() throws Exception {
        server.enqueue(json(fixture("fixtures/naver-news-sample.json")));

        client.search("005930", "삼성전자");

        RecordedRequest request = server.takeRequest();
        HttpUrl url = request.getRequestUrl();
        assertThat(url.encodedPath()).isEqualTo("/v1/search/news.json");
        assertThat(url.queryParameter("query")).isEqualTo("삼성전자");
        assertThat(url.queryParameter("display")).isEqualTo("10");
        assertThat(url.queryParameter("sort")).isEqualTo("date");
        assertThat(request.getHeader("X-Naver-Client-Id")).isEqualTo("TEST_ID");
        assertThat(request.getHeader("X-Naver-Client-Secret")).isEqualTo("TEST_SECRET");
    }

    @Test
    void 응답을_뉴스_후보로_매핑한다() throws Exception {
        server.enqueue(json(fixture("fixtures/naver-news-sample.json")));

        List<NewsCandidate> candidates = client.search("005930", "삼성전자");

        assertThat(candidates).hasSize(2);
        assertThat(candidates).allSatisfy(c -> assertThat(c.stockCode()).isEqualTo("005930"));
        assertThat(candidates.get(0).url())
            .isEqualTo("https://n.news.naver.com/mnews/article/001/0001");
    }

    private static MockResponse json(String body) {
        return new MockResponse()
            .addHeader("Content-Type", "application/json;charset=UTF-8")
            .setBody(body);
    }

    private static String fixture(String path) throws Exception {
        try (var in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
