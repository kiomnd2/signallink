package io.signallink.market.adapter.out.external.kis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.signallink.market.application.port.out.IndexPriceData;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KIS 국내업종 기간별시세(FHKUP03500100) 응답. output2가 일자별 배열(최신일 우선).
 *
 * <p>지수는 응답에 전일대비가 없어(실검증 확인), 등락률을 <b>직전 행(전일) 종가 대비</b>로 계산한다.
 * 배열 마지막(가장 오래된) 행은 직전 값이 없어 등락률 0.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KisIndexPriceResponse(
    @JsonProperty("rt_cd") String rtCd,
    @JsonProperty("msg1") String msg,
    @JsonProperty("output2") List<Row> output2
) {
    private static final Logger log = LoggerFactory.getLogger(KisIndexPriceResponse.class);
    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Row(
        @JsonProperty("stck_bsop_date") String date,
        @JsonProperty("bstp_nmix_prpr") String close
    ) {}

    public List<IndexPriceData> toData() {
        if (output2 == null) {
            return List.of();
        }
        List<IndexPriceData> result = new ArrayList<>(output2.size());
        for (int i = 0; i < output2.size(); i++) {
            Row row = output2.get(i);
            if (row.date() == null || row.close() == null || row.close().isBlank()) {
                continue;
            }
            try {
                BigDecimal close = new BigDecimal(row.close());
                BigDecimal changeRate = changeRateVsPrev(close, i);
                result.add(new IndexPriceData(LocalDate.parse(row.date(), YMD), close, changeRate));
            } catch (RuntimeException e) {
                log.warn("지수 행 건너뜀: date={} ({})", row.date(), e.toString());
            }
        }
        return result;
    }

    /** 다음(더 오래된) 행의 종가를 전일 종가로 보고 등락률 계산. 없으면 0. */
    private BigDecimal changeRateVsPrev(BigDecimal close, int index) {
        if (index + 1 >= output2.size()) {
            return BigDecimal.ZERO;
        }
        String prevClose = output2.get(index + 1).close();
        if (prevClose == null || prevClose.isBlank()) {
            return BigDecimal.ZERO;
        }
        BigDecimal prior = new BigDecimal(prevClose);
        return prior.signum() == 0
            ? BigDecimal.ZERO
            : close.subtract(prior).multiply(HUNDRED).divide(prior, 3, RoundingMode.HALF_UP);
    }
}
