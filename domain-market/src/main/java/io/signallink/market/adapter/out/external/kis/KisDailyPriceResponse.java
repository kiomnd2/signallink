package io.signallink.market.adapter.out.external.kis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.signallink.market.application.port.out.DailyPriceData;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KIS 국내주식 기간별시세(FHKST03010100) 응답. output2가 일자별 배열.
 *
 * <p>등락률은 응답에 없어(실검증 확인) 전일대비(prdy_vrss)로 계산한다:
 * 전일종가 = 종가 − 전일대비, 등락률 = 전일대비 / 전일종가 × 100.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KisDailyPriceResponse(
    @JsonProperty("rt_cd") String rtCd,
    @JsonProperty("msg1") String msg,
    @JsonProperty("output2") List<Row> output2
) {
    private static final Logger log = LoggerFactory.getLogger(KisDailyPriceResponse.class);
    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Row(
        @JsonProperty("stck_bsop_date") String date,
        @JsonProperty("stck_clpr") String close,
        @JsonProperty("acml_vol") String volume,
        @JsonProperty("acml_tr_pbmn") String tradingValue,
        @JsonProperty("prdy_vrss") String priorDiff
    ) {}

    public List<DailyPriceData> toData() {
        if (output2 == null) {
            return List.of();
        }
        List<DailyPriceData> result = new ArrayList<>(output2.size());
        for (Row row : output2) {
            DailyPriceData d = toData(row);
            if (d != null) {
                result.add(d);
            }
        }
        return result;
    }

    private static DailyPriceData toData(Row row) {
        if (row.date() == null || row.close() == null || row.close().isBlank()) {
            return null; // 빈 행
        }
        try {
            LocalDate date = LocalDate.parse(row.date(), YMD);
            BigDecimal close = new BigDecimal(row.close());
            BigDecimal priorDiff = new BigDecimal(blankToZero(row.priorDiff()));
            BigDecimal priorClose = close.subtract(priorDiff);
            BigDecimal changeRate = priorClose.signum() == 0
                ? BigDecimal.ZERO
                : priorDiff.multiply(HUNDRED).divide(priorClose, 3, RoundingMode.HALF_UP);
            return new DailyPriceData(date, close, changeRate,
                Long.parseLong(blankToZero(row.volume())), Long.parseLong(blankToZero(row.tradingValue())));
        } catch (RuntimeException e) {
            log.warn("일봉 행 건너뜀: date={} ({})", row.date(), e.toString());
            return null;
        }
    }

    private static String blankToZero(String s) {
        return (s == null || s.isBlank()) ? "0" : s;
    }
}
