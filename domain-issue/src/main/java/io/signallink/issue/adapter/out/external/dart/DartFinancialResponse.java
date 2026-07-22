package io.signallink.issue.adapter.out.external.dart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.signallink.issue.application.port.out.QuarterAccount;
import java.util.List;
import java.util.Optional;

/**
 * DART 단일회사 주요계정(fnlttSinglAcnt.json) 응답 매핑.
 *
 * <p>{@code list}에서 매출액·영업이익 계정의 당기금액(thstrm_amount)을 뽑아 {@link QuarterAccount}로 만든다.
 * 연결재무제표(CFS)가 있으면 우선, 없으면 개별(OFS). 금액은 콤마 포함 문자열 → long 파싱.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DartFinancialResponse(
    String status,
    String message,
    List<Item> list) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
        @JsonProperty("account_nm") String accountName,
        @JsonProperty("fs_div") String fsDiv,          // CFS(연결)/OFS(개별)
        @JsonProperty("thstrm_amount") String amount) {}

    /** 매출액·영업이익을 뽑아 QuarterAccount로. 둘 다 없으면 empty. */
    public Optional<QuarterAccount> toQuarterAccount(int year, int quarter) {
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }
        Long revenue = pick("매출액");
        Long operatingProfit = pick("영업이익");
        if (revenue == null && operatingProfit == null) {
            return Optional.empty();
        }
        return Optional.of(new QuarterAccount(year, quarter,
            revenue == null ? 0L : revenue,
            operatingProfit == null ? 0L : operatingProfit));
    }

    /** 계정명을 포함하는 행의 당기금액. 연결(CFS) 우선. 없으면 null. */
    private Long pick(String accountKeyword) {
        Long ofs = null;
        for (Item it : list) {
            if (it.accountName() == null || !it.accountName().contains(accountKeyword)) {
                continue;
            }
            Long amount = parseAmount(it.amount());
            if (amount == null) {
                continue;
            }
            if ("CFS".equals(it.fsDiv())) {
                return amount; // 연결 우선
            }
            if (ofs == null) {
                ofs = amount;
            }
        }
        return ofs;
    }

    /** "2,589,355" / "-1,234" → long. 빈 값·형식 오류는 null. */
    static Long parseAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
