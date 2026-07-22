package io.signallink.issue.adapter.out.external.dart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.signallink.issue.application.port.out.DisclosureCandidate;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
 * DartClient HTTP 경로 검증 — 목 서버로 요청 조립(경로·쿼리·키)과 응답 처리를 확인한다.
 * 실제 DART 호출/실키 없이, {@link ExternalApiClient} 위의 list.json 배선이 맞는지 본다.
 * (상태코드 분기 자체는 {@code DartClientTest.interpret()}에서 순수 검증.)
 */
class DartClientHttpTest {

    private MockWebServer server;
    private DartClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        String baseUrl = "http://" + server.getHostName() + ":" + server.getPort();
        // permits 크게 → 테스트에서 rate limit 대기 없음
        client = new DartClient(baseUrl, "TEST_KEY", 1000);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void list_json을_키와_기간_파라미터로_호출한다() throws Exception {
        server.enqueue(json(fixture("fixtures/dart-list-sample.json")));

        client.fetchDisclosures(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 3));

        RecordedRequest request = server.takeRequest();
        HttpUrl url = request.getRequestUrl();
        assertThat(url.encodedPath()).isEqualTo("/api/list.json");
        assertThat(url.queryParameter("crtfc_key")).isEqualTo("TEST_KEY");
        assertThat(url.queryParameter("bgn_de")).isEqualTo("20240102");
        assertThat(url.queryParameter("end_de")).isEqualTo("20240103");
        assertThat(url.queryParameter("page_count")).isEqualTo("100");
    }

    @Test
    void 정상_응답을_공시_후보로_매핑한다() throws Exception {
        server.enqueue(json(fixture("fixtures/dart-list-sample.json")));

        List<DisclosureCandidate> candidates =
            client.fetchDisclosures(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 3));

        assertThat(candidates).extracting(DisclosureCandidate::stockCode)
            .containsExactly("005930", "000660");
        assertThat(candidates).extracting(DisclosureCandidate::receiptNo)
            .containsExactly("20240102000123", "20240102000456");
    }

    @Test
    void 데이터없음_013_응답은_빈_목록으로_처리한다() throws Exception {
        server.enqueue(json("{\"status\":\"013\",\"message\":\"조회된 데이터가 없습니다.\"}"));

        assertThat(client.fetchDisclosures(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 3)))
            .isEmpty();
    }

    @Test
    void 오류_상태_바디는_예외로_전파한다() {
        // DART는 인증 실패·한도 초과를 HTTP 200 + 바디 status로 알린다 → 조용한 중단 방지 위해 예외
        server.enqueue(json("{\"status\":\"020\",\"message\":\"요청 제한을 초과하였습니다.\"}"));

        assertThatThrownBy(() ->
            client.fetchDisclosures(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 3)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("020");
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
