package io.signallink.insight.adapter.out.external.gemini;

import java.util.List;

/**
 * Gemini generateContent 요청 바디. 레코드 컴포넌트명이 그대로 JSON 필드명(camelCase)이 되어 API와 일치한다.
 */
public record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {

    /** 단일 텍스트 프롬프트 요청. */
    public static GeminiRequest of(String prompt) {
        return new GeminiRequest(
            List.of(new Content(List.of(new Part(prompt)))),
            new GenerationConfig(0.4, 512));
    }

    public record Content(List<Part> parts) {}

    public record Part(String text) {}

    public record GenerationConfig(double temperature, int maxOutputTokens) {}
}
