package io.signallink.insight.adapter.out.external.gemini;

import io.signallink.common.client.ExternalApiClient;
import io.signallink.common.client.RateLimiter;
import io.signallink.insight.application.port.out.LlmGatewayPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Gemini(Generative Language API) 아웃바운드 어댑터 — {@link LlmGatewayPort} 구현.
 *
 * <p>{@link ExternalApiClient}(rate limit + 지수 백오프) 위에서 {@code models/{model}:generateContent}를
 * 호출하고 응답 JSON을 파싱해 생성 텍스트를 돌려준다. 키/모델/URL은 설정 주입(기본값 있음 → 미설정이어도
 * 빈 생성 가능). 차단·빈 응답은 예외로 전파 → 서비스가 템플릿 폴백.
 *
 * <p><b>모델 ID</b>: 기본 {@code gemini-3-flash}(플랜 M3). 실호출 검증 전이라 정확한 ID는 키 발급 시
 * Google API 문서로 확인하고 {@code GEMINI_MODEL}로 덮어쓸 것(BACKLOG).
 */
@Component
public class GeminiClient extends ExternalApiClient implements LlmGatewayPort {

    private final String apiKey;
    private final String model;

    public GeminiClient(
            @Value("${signallink.llm.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${signallink.llm.gemini.api-key:}") String apiKey,
            @Value("${signallink.llm.gemini.model:gemini-3-flash}") String model,
            @Value("${signallink.llm.gemini.permits-per-second:1}") double permitsPerSecond) {
        super(baseUrl, new RateLimiter(permitsPerSecond), 3, 500);
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String generate(String prompt) {
        GeminiResponse res = execute("gemini.generate", () -> client().post()
            .uri(uri -> uri.path("/v1beta/models/" + model + ":generateContent")
                .queryParam("key", apiKey)
                .build())
            .contentType(MediaType.APPLICATION_JSON)
            .body(GeminiRequest.of(prompt))
            .retrieve()
            .body(GeminiResponse.class));

        String text = res == null ? null : res.firstText();
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Gemini 생성 텍스트 없음(차단/빈 응답): finishReason="
                + (res == null ? "null" : res.firstFinishReason()));
        }
        return text;
    }
}
