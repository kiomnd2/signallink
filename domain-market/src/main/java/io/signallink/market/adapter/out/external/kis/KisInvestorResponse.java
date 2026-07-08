package io.signallink.market.adapter.out.external.kis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.signallink.market.application.port.out.InvestorFlowData;
import io.signallink.market.domain.InvestorType;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * KIS 종목별 투자자(FHKST01010900, inquire-investor) 응답 — 마감 확정 수급.
 * output이 일자별 배열(최신일 우선). 최신 거래일의 개인·외국인·기관 3종을 반환한다.
 * (금액 *_tr_pbmn 단위는 백만원)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KisInvestorResponse(
    @JsonProperty("rt_cd") String rtCd,
    @JsonProperty("output") List<Row> output
) {
    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Row(
        @JsonProperty("stck_bsop_date") String date,
        @JsonProperty("prsn_ntby_qty") String personQty,
        @JsonProperty("frgn_ntby_qty") String foreignQty,
        @JsonProperty("orgn_ntby_qty") String orgQty,
        @JsonProperty("prsn_ntby_tr_pbmn") String personAmt,
        @JsonProperty("frgn_ntby_tr_pbmn") String foreignAmt,
        @JsonProperty("orgn_ntby_tr_pbmn") String orgAmt
    ) {}

    public List<InvestorFlowData> toData(String stockCode) {
        if (output == null || output.isEmpty() || output.get(0).date() == null) {
            return List.of();
        }
        Row row = output.get(0); // 최신 거래일
        LocalDate date = LocalDate.parse(row.date(), YMD);
        return List.of(
            new InvestorFlowData(stockCode, date, InvestorType.PERSON, num(row.personQty()), num(row.personAmt())),
            new InvestorFlowData(stockCode, date, InvestorType.FOREIGN, num(row.foreignQty()), num(row.foreignAmt())),
            new InvestorFlowData(stockCode, date, InvestorType.ORG, num(row.orgQty()), num(row.orgAmt())));
    }

    static long num(String s) {
        return (s == null || s.isBlank()) ? 0L : Long.parseLong(s.trim());
    }
}
