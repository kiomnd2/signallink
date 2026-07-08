package io.signallink.issue.adapter.out.external.dart;

/**
 * DART OpenAPI 응답 status 코드. DART는 HTTP 200 + 바디의 status로 결과를 알린다.
 * {@link #OK}/{@link #NO_DATA}만 정상 흐름이고, 그 외(예: 011 인증실패·020 한도초과·800 점검)는 오류로 처리한다.
 */
public final class DartStatus {

    private DartStatus() {}

    /** 정상 처리. */
    public static final String OK = "000";
    /** 조회된 데이터 없음. */
    public static final String NO_DATA = "013";
}
