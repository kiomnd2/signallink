package io.signallink.insight.adapter.out.external.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * GeminiClient HTTP 경로 검증 — 목 서버로 요청 조립(경로·모델·키·바디)과 응답 파싱을 확인한다.
 * 실제 Gemini 호출/실키 없이 {@link io.signallink.common.client.ExternalApiClient} 위의 배선을 본다.
 */
class GeminiClientHttpTest {

    private MockWebServer server;
    private GeminiClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        String baseUrl = "http://" + server.getHostName() + ":" + server.getPort();
        // permits 크게 → 테스트에서 rate limit 대기 없음
        client = new GeminiClient(baseUrl, "TEST_KEY", "test-model", 1000);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void 모델과_키_경로로_프롬프트를_전송한다() throws Exception {
        server.enqueue(ok("좋은 하루예요."));

        client.generate("이 종목 왜 올랐는지 설명해줘");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        HttpUrl url = request.getRequestUrl();
        assertThat(url.encodedPath()).isEqualTo("/v1beta/models/test-model:generateContent");
        assertThat(url.queryParameter("key")).isEqualTo("TEST_KEY");

        String body = request.getBody().readUtf8();
        assertThat(body).contains("이 종목 왜 올랐는지 설명해줘");   // 프롬프트가 contents에 실림
        assertThat(body).contains("generationConfig");
    }

    @Test
    void 정상_응답에서_생성_텍스트를_추출한다() {
        server.enqueue(ok("오늘은 공급계약 공시로 주가가 올랐어요."));

        assertThat(client.generate("prompt")).isEqualTo("오늘은 공급계약 공시로 주가가 올랐어요.");
    }

    @Test
    void 후보가_없으면_예외로_전파한다() {
        // 안전필터 차단 등으로 candidates 비어 있는 경우 → 조용한 폴백 대신 예외(서비스가 잡아 폴백)
        server.enqueue(json("{\"candidates\":[]}"));

        assertThatThrownBy(() -> client.generate("prompt"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Gemini");
    }

    /** 생성 텍스트 하나를 담은 정상 응답. */
    private static MockResponse ok(String text) {
        return json("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"" + text + "\"}]},"
            + "\"finishReason\":\"STOP\"}]}");
    }

    private static MockResponse json(String body) {
        return new MockResponse()
            .addHeader("Content-Type", "application/json;charset=UTF-8")
            .setBody(body);
    }
}
