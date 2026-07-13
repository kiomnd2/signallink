package io.signallink.insight.adapter.out.persistence;

import io.signallink.insight.domain.IssueRef;
import java.util.List;

/**
 * 근거(IssueRef) 리스트 → source_refs(jsonb) 문자열 직렬화.
 *
 * <p>common.DiscordNotifier와 같은 이유로 Jackson 의존 없이 수동 직렬화한다(도메인 모듈 의존 최소화).
 * 구조가 고정(배열 of {type,title,url,occurredAt,detail})이라 안전하며, 문자열은 JSON 이스케이프한다.
 */
final class SourceRefsJson {

    private SourceRefsJson() {}

    /** 빈 리스트는 "[]". null 필드는 JSON null. */
    static String serialize(List<IssueRef> refs) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < refs.size(); i++) {
            IssueRef r = refs.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"type\":").append(str(r.type() == null ? null : r.type().name()))
              .append(",\"title\":").append(str(r.title()))
              .append(",\"url\":").append(str(r.url()))
              .append(",\"occurredAt\":").append(str(r.occurredAt() == null ? null : r.occurredAt().toString()))
              .append(",\"detail\":").append(str(r.detail()))
              .append('}');
        }
        return sb.append(']').toString();
    }

    /** JSON 문자열 리터럴(또는 null). */
    private static String str(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
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
        return sb.append('"').toString();
    }
}
