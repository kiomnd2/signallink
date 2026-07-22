package io.signallink.insight.adapter.out.external.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Gemini generateContent 응답 바디(필요 필드만). 안전등급·usageMetadata 등 미사용 필드는 무시한다
 * ({@code ignoreUnknown} — 수동 RestClient는 Spring Boot ObjectMapper 설정을 안 받을 수 있어 명시).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiResponse(List<Candidate> candidates) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(Content content, String finishReason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(List<Part> parts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Part(String text) {}

    /** 첫 후보의 생성 텍스트. 없으면 null. */
    public String firstText() {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        Candidate c = candidates.get(0);
        if (c == null || c.content() == null || c.content().parts() == null
            || c.content().parts().isEmpty()) {
            return null;
        }
        return c.content().parts().get(0).text();
    }

    /** 첫 후보의 종료 사유(빈 응답 원인 진단용: MAX_TOKENS·SAFETY 등). 없으면 null. */
    public String firstFinishReason() {
        if (candidates == null || candidates.isEmpty() || candidates.get(0) == null) {
            return null;
        }
        return candidates.get(0).finishReason();
    }
}
