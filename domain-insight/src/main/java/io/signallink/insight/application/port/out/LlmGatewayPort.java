package io.signallink.insight.application.port.out;

/**
 * 아웃바운드 포트 — LLM 텍스트 생성. 구현: adapter.out.external.gemini.GeminiClient.
 *
 * <p>프롬프트를 보내 생성 텍스트(평문)를 받는다. 전송/응답 오류·차단·빈 응답이면 {@link RuntimeException}을
 * 던진다 — 호출측(WhySummaryService)이 잡아 템플릿 폴백으로 되돌린다("조용한 실패" 방지).
 */
public interface LlmGatewayPort {
    String generate(String prompt);
}
