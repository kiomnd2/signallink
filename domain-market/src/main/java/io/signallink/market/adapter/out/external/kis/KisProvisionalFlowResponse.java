package io.signallink.market.adapter.out.external.kis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.signallink.market.application.port.out.InvestorFlowData;
import io.signallink.market.domain.InvestorType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * KIS 외인·기관 매매 가집계(FHPTJ04400000) 응답 — 장중 잠정 수급.
 * output이 시장 순매수 상위 종목 리스트. 종목마다 외국인·기관 2종을 반환(개인은 없음).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KisProvisionalFlowResponse(
    @JsonProperty("rt_cd") String rtCd,
    @JsonProperty("output") List<Row> output
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Row(
        @JsonProperty("mksc_shrn_iscd") String code,
        @JsonProperty("frgn_ntby_qty") String foreignQty,
        @JsonProperty("orgn_ntby_qty") String orgQty,
        @JsonProperty("frgn_ntby_tr_pbmn") String foreignAmt,
        @JsonProperty("orgn_ntby_tr_pbmn") String orgAmt
    ) {}

    public List<InvestorFlowData> toData(LocalDate tradeDate) {
        if (output == null) {
            return List.of();
        }
        List<InvestorFlowData> result = new ArrayList<>(output.size() * 2);
        for (Row row : output) {
            if (row.code() == null || row.code().isBlank()) {
                continue;
            }
            result.add(new InvestorFlowData(row.code(), tradeDate, InvestorType.FOREIGN,
                KisInvestorResponse.num(row.foreignQty()), KisInvestorResponse.num(row.foreignAmt())));
            result.add(new InvestorFlowData(row.code(), tradeDate, InvestorType.ORG,
                KisInvestorResponse.num(row.orgQty()), KisInvestorResponse.num(row.orgAmt())));
        }
        return result;
    }
}
