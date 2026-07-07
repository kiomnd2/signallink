package io.signallink.issue.adapter.out.external.dart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.signallink.common.TimeUtils;
import io.signallink.issue.application.port.out.DisclosureCandidate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * DART 공시검색 API(list.json) 응답 매핑.
 *
 * <p>성공 시 {@code status="000"}, 조회 결과 없음은 {@code "013"}.
 * 응답의 snake_case 필드는 {@link JsonProperty}로 매핑한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DartListResponse(
    String status,
    String message,
    List<Item> list
) {
    public static final String OK = "000";
    public static final String NO_DATA = "013";

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String VIEWER_URL = "https://dart.fss.or.kr/dsaf001/main.do?rcpNo=";

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
        @JsonProperty("rcept_no") String receiptNo,
        @JsonProperty("stock_code") String stockCode,
        @JsonProperty("report_nm") String reportName,
        @JsonProperty("rcept_dt") String receiptDate
    ) {}

    /** 응답 항목을 도메인 경계 타입으로 변환. rcept_dt(YYYYMMDD)는 시간 정보가 없어 KST 자정 기준. */
    public List<DisclosureCandidate> toCandidates() {
        if (list == null) {
            return List.of();
        }
        return list.stream().map(DartListResponse::toCandidate).toList();
    }

    private static DisclosureCandidate toCandidate(Item item) {
        var disclosedAt = LocalDate.parse(item.receiptDate(), YMD)
            .atStartOfDay(TimeUtils.KST)
            .toOffsetDateTime();
        return new DisclosureCandidate(
            item.receiptNo(),
            item.stockCode(),
            item.reportName(),
            disclosedAt,
            VIEWER_URL + item.receiptNo());
    }
}
