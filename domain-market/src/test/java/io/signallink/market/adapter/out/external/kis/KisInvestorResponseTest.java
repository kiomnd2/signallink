package io.signallink.market.adapter.out.external.kis;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.signallink.market.application.port.out.InvestorFlowData;
import io.signallink.market.domain.InvestorType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** 확정 수급 파싱 — 최신 거래일의 개인·외국인·기관 3종(음수 포함) 매핑. */
class KisInvestorResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void 최신일_개인_외국인_기관_3종을_매핑한다() throws Exception {
        KisInvestorResponse res;
        try (var in = new ClassPathResource("fixtures/kis-investor-confirmed.json").getInputStream()) {
            res = mapper.readValue(in, KisInvestorResponse.class);
        }

        List<InvestorFlowData> data = res.toData("005930");

        assertThat(data).hasSize(3);
        assertThat(data).extracting(InvestorFlowData::investorType)
            .containsExactly(InvestorType.PERSON, InvestorType.FOREIGN, InvestorType.ORG);
        assertThat(data.get(0).tradeDate()).isEqualTo(LocalDate.of(2026, 7, 8)); // 최신일
        assertThat(data.get(0).netBuyQty()).isEqualTo(2_035_163L);
        assertThat(data.get(0).netBuyAmt()).isEqualTo(584_689L);
        assertThat(data.get(1).netBuyQty()).isEqualTo(-3_015_122L); // 외국인 음수
    }
}
