package io.signallink.market.adapter.out.external.kis;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.signallink.market.application.port.out.InvestorFlowData;
import io.signallink.market.domain.InvestorType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** 잠정 수급(가집계) 파싱 — 종목마다 외국인·기관 2종. */
class KisProvisionalFlowResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void 종목마다_외국인_기관_2종을_매핑한다() throws Exception {
        KisProvisionalFlowResponse res;
        try (var in = new ClassPathResource("fixtures/kis-provisional-flow.json").getInputStream()) {
            res = mapper.readValue(in, KisProvisionalFlowResponse.class);
        }

        LocalDate today = LocalDate.of(2026, 7, 8);
        List<InvestorFlowData> data = res.toData(today);

        assertThat(data).hasSize(4); // 2종목 × 2종
        assertThat(data.get(0).stockCode()).isEqualTo("034220");
        assertThat(data.get(0).investorType()).isEqualTo(InvestorType.FOREIGN);
        assertThat(data.get(0).netBuyQty()).isEqualTo(1_303_000L);
        assertThat(data.get(0).tradeDate()).isEqualTo(today);
        assertThat(data.get(1).investorType()).isEqualTo(InvestorType.ORG);
        assertThat(data.get(1).netBuyQty()).isEqualTo(322_000L);
    }
}
