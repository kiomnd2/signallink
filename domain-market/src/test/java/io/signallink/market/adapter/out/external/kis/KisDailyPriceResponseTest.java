package io.signallink.market.adapter.out.external.kis;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.signallink.market.application.port.out.DailyPriceData;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** KIS 일별시세 파싱 — 등락률을 전일대비(prdy_vrss)로 계산하는지 검증. */
class KisDailyPriceResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void 픽스처를_일봉으로_매핑하고_등락률을_계산한다() throws Exception {
        KisDailyPriceResponse res;
        try (var in = new ClassPathResource("fixtures/kis-daily-price-sample.json").getInputStream()) {
            res = mapper.readValue(in, KisDailyPriceResponse.class);
        }

        List<DailyPriceData> data = res.toData();
        assertThat(data).hasSize(2);

        DailyPriceData d0 = data.get(0);
        assertThat(d0.tradeDate()).isEqualTo(LocalDate.of(2026, 7, 8));
        assertThat(d0.close()).isEqualByComparingTo("278000");
        assertThat(d0.volume()).isEqualTo(21_442_122L);
        assertThat(d0.tradingValue()).isEqualTo(6_203_329_208_250L);
        // 종가 278000, 전일대비 -18000 → 전일종가 296000 → -18000/296000*100 = -6.081
        assertThat(d0.changeRate()).isEqualByComparingTo("-6.081");

        // 종가 296000, 전일대비 5000 → 전일종가 291000 → 5000/291000*100 = 1.718
        assertThat(data.get(1).changeRate()).isEqualByComparingTo("1.718");
    }
}
