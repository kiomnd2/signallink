package io.signallink.common.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

/**
 * Discord 웹훅 알림. 배치 실패·일일 리포트용.
 * webhookUrl이 비어 있으면 no-op (로컬 개발). 알림 실패가 본 작업을 죽이지 않도록 예외를 삼킨다.
 */
public class DiscordNotifier {

    private static final Logger log = LoggerFactory.getLogger(DiscordNotifier.class);
    private static final int DISCORD_MAX_CONTENT = 2000;

    private final String webhookUrl;
    private final RestClient restClient;

    public DiscordNotifier(String webhookUrl) {
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
        this.restClient = RestClient.create();
    }

    public void send(String message) {
        if (webhookUrl.isEmpty()) {
            log.debug("Discord 웹훅 미설정 — 알림 생략: {}", message);
            return;
        }
        String content = message.length() > DISCORD_MAX_CONTENT
                ? message.substring(0, DISCORD_MAX_CONTENT - 1) + "…"
                : message;
        try {
            // Jackson 의존 없이 동작하도록 수동 직렬화 (common은 spring-web만 보유)
            String json = "{\"content\":\"" + escapeJson(content) + "\"}";
            restClient.post()
                    .uri(webhookUrl)
                    .header("Content-Type", "application/json")
                    .body(json)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Discord 알림 실패 (본 작업엔 영향 없음): {}", e.getMessage());
        }
    }

    /** 배치 잡 실패 보고용 헬퍼. */
    public void jobFailed(String jobName, Throwable t) {
        send("🚨 [%s] 실패: %s".formatted(jobName, t.toString()));
    }

    private static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
