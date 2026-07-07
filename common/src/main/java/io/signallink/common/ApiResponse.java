package io.signallink.common;

import java.time.OffsetDateTime;

/** 공통 응답 포맷. 모든 조회 응답 meta에 면책 문구를 고정 포함한다. */
public record ApiResponse<T>(T data, Meta meta) {

    public record Meta(OffsetDateTime asOf, boolean isProvisional, String disclaimer) {}

    private static final String DISCLAIMER = "본 정보는 투자 조언이 아닙니다";

    public static <T> ApiResponse<T> of(T data, boolean provisional) {
        return new ApiResponse<>(data, new Meta(TimeUtils.nowKst(), provisional, DISCLAIMER));
    }

    public static <T> ApiResponse<T> of(T data) {
        return of(data, false);
    }
}
