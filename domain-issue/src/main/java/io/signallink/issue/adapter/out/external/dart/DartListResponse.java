package io.signallink.issue.adapter.out.external.dart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.signallink.common.TimeUtils;
import io.signallink.issue.application.port.out.DisclosureCandidate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger log = LoggerFactory.getLogger(DartListResponse.class);
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String VIEWER_URL = "https://dart.fss.or.kr/dsaf001/main.do?rcpNo=";

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
        @JsonProperty("rcept_no") String receiptNo,
        @JsonProperty("stock_code") String stockCode,
        @JsonProperty("report_nm") String reportName,
        @JsonProperty("rcept_dt") String receiptDate
    ) {}

    /**
     * 응답 항목을 도메인 경계 타입으로 변환. rcept_dt(YYYYMMDD)는 시간 정보가 없어 KST 자정 기준.
     *
     * <p>결함 있는 항목(필수 필드 누락·날짜 형식 오류)은 로그 후 건너뛴다 — 한 건의 문제가
     * 같은 응답의 정상 항목까지 유실시키지 않도록 한다.
     */
    public List<DisclosureCandidate> toCandidates() {
        if (list == null) {
            return List.of();
        }
        List<DisclosureCandidate> candidates = new ArrayList<>(list.size());
        for (Item item : list) {
            DisclosureCandidate candidate = toCandidate(item);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }
        return candidates;
    }

    /** 변환 불가한 항목이면 null을 반환(호출부에서 skip). */
    private static DisclosureCandidate toCandidate(Item item) {
        if (item.receiptNo() == null || item.receiptDate() == null || item.reportName() == null) {
            log.warn("DART 항목 건너뜀 (필수 필드 누락): rcept_no={} rcept_dt={} report_nm={}",
                item.receiptNo(), item.receiptDate(), item.reportName());
            return null;
        }
        try {
            var disclosedAt = LocalDate.parse(item.receiptDate(), YMD)
                .atStartOfDay(TimeUtils.KST)
                .toOffsetDateTime();
            return new DisclosureCandidate(
                item.receiptNo(),
                item.stockCode(),
                item.reportName(),
                disclosedAt,
                VIEWER_URL + item.receiptNo());
        } catch (DateTimeParseException e) {
            log.warn("DART 항목 건너뜀 (rcept_dt 파싱 실패): rcept_no={} rcept_dt={}",
                item.receiptNo(), item.receiptDate());
            return null;
        }
    }
}
