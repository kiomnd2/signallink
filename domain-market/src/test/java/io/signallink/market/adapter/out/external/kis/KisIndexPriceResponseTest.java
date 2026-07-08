package io.signallink.market.adapter.out.external.kis;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.signallink.market.application.port.out.IndexPriceData;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** KIS 지수 파싱 — 등락률을 직전(전일) 종가 대비로 계산, 마지막 행은 0. */
class KisIndexPriceResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void 픽스처를_지수로_매핑하고_전일대비_등락률을_계산한다() throws Exception {
        KisIndexPriceResponse res;
        try (var in = new ClassPathResource("fixtures/kis-index-price-sample.json").getInputStream()) {
            res = mapper.readValue(in, KisIndexPriceResponse.class);
        }

        List<IndexPriceData> data = res.toData();
        assertThat(data).hasSize(2);

        IndexPriceData d0 = data.get(0);
        assertThat(d0.tradeDate()).isEqualTo(LocalDate.of(2026, 7, 8));
        assertThat(d0.close()).isEqualByComparingTo("7297.25");
        // (7297.25 - 7452.48) / 7452.48 * 100 = -2.083
        assertThat(d0.changeRate()).isEqualByComparingTo("-2.083");

        // 마지막(가장 오래된) 행은 직전 값이 없어 0
        assertThat(data.get(1).changeRate()).isEqualByComparingTo("0");
    }
}
