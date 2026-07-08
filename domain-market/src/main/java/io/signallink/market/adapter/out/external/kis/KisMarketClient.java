package io.signallink.market.adapter.out.external.kis;

import io.signallink.common.client.ExternalApiClient;
import io.signallink.common.client.RateLimiter;
import io.signallink.market.application.port.out.DailyPriceData;
import io.signallink.market.application.port.out.DailyPriceGatewayPort;
import io.signallink.market.application.port.out.IndexPriceData;
import io.signallink.market.application.port.out.IndexPriceGatewayPort;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * KIS 시장데이터 아웃바운드 어댑터 — 일별시세·지수 TR 호출.
 *
 * <p>{@link ExternalApiClient} + {@link RateLimiter}(M0 실측 초당 1건 → 정책① 유량 준수) 위에서 호출하고,
 * {@link KisTokenProvider}가 준 Bearer 토큰 + appkey/appsecret/tr_id 헤더를 붙인다.
 */
@Component
public class KisMarketClient extends ExternalApiClient
        implements DailyPriceGatewayPort, IndexPriceGatewayPort {

    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

    private final KisTokenProvider tokenProvider;
    private final String appKey;
    private final String appSecret;

    public KisMarketClient(
            KisTokenProvider tokenProvider,
            @Value("${signallink.kis.base-url:https://openapi.koreainvestment.com:9443}") String baseUrl,
            @Value("${signallink.kis.app-key:}") String appKey,
            @Value("${signallink.kis.app-secret:}") String appSecret,
            @Value("${signallink.kis.permits-per-second:1.0}") double permitsPerSecond) {
        super(baseUrl, new RateLimiter(permitsPerSecond), 3, 500);
        this.tokenProvider = tokenProvider;
        this.appKey = appKey;
        this.appSecret = appSecret;
    }

    @Override
    public List<DailyPriceData> fetchDailyPrices(String stockCode, LocalDate from, LocalDate to) {
        KisDailyPriceResponse res = execute("kis.daily", () -> client().get()
            .uri(uri -> uri.path("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
                .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                .queryParam("FID_INPUT_ISCD", stockCode)
                .queryParam("FID_INPUT_DATE_1", from.format(YMD))
                .queryParam("FID_INPUT_DATE_2", to.format(YMD))
                .queryParam("FID_PERIOD_DIV_CODE", "D")
                .queryParam("FID_ORG_ADJ_PRC", "0")
                .build())
            .headers(kisHeaders("FHKST03010100"))
            .retrieve()
            .body(KisDailyPriceResponse.class));
        return res == null ? List.of() : res.toData();
    }

    @Override
    public List<IndexPriceData> fetchIndexPrices(String indexCode, LocalDate from, LocalDate to) {
        KisIndexPriceResponse res = execute("kis.index", () -> client().get()
            .uri(uri -> uri.path("/uapi/domestic-stock/v1/quotations/inquire-daily-indexchartprice")
                .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                .queryParam("FID_INPUT_ISCD", indexCode)
                .queryParam("FID_INPUT_DATE_1", from.format(YMD))
                .queryParam("FID_INPUT_DATE_2", to.format(YMD))
                .queryParam("FID_PERIOD_DIV_CODE", "D")
                .build())
            .headers(kisHeaders("FHKUP03500100"))
            .retrieve()
            .body(KisIndexPriceResponse.class));
        return res == null ? List.of() : res.toData();
    }

    private Consumer<HttpHeaders> kisHeaders(String trId) {
        return headers -> {
            headers.setBearerAuth(tokenProvider.bearerToken());
            headers.add("appkey", appKey);
            headers.add("appsecret", appSecret);
            headers.add("tr_id", trId);
            headers.add("custtype", "P");
        };
    }
}
