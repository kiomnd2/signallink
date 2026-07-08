package io.signallink.market.adapter.out.external.kis;

import io.signallink.common.TimeUtils;
import io.signallink.common.client.ExternalApiClient;
import io.signallink.common.client.RateLimiter;
import io.signallink.market.application.port.out.InvestorFlowData;
import io.signallink.market.application.port.out.InvestorFlowGatewayPort;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * KIS 수급 아웃바운드 어댑터 — 확정(inquire-investor)·잠정(가집계) TR 호출.
 * ExternalApiClient + RateLimiter(정책①) + KisTokenProvider 인증.
 * (KisMarketClient와 헤더 구성이 겹침 — 공통 KisClient 베이스 추출은 BACKLOG.)
 */
@Component
public class KisInvestorClient extends ExternalApiClient implements InvestorFlowGatewayPort {

    private final KisTokenProvider tokenProvider;
    private final String appKey;
    private final String appSecret;

    public KisInvestorClient(
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
    public List<InvestorFlowData> fetchConfirmed(String stockCode) {
        KisInvestorResponse res = execute("kis.investor", () -> client().get()
            .uri(uri -> uri.path("/uapi/domestic-stock/v1/quotations/inquire-investor")
                .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                .queryParam("FID_INPUT_ISCD", stockCode)
                .build())
            .headers(kisHeaders("FHKST01010900"))
            .retrieve()
            .body(KisInvestorResponse.class));
        return res == null ? List.of() : res.toData(stockCode);
    }

    @Override
    public List<InvestorFlowData> fetchProvisional() {
        KisProvisionalFlowResponse res = execute("kis.provisional", () -> client().get()
            .uri(uri -> uri.path("/uapi/domestic-stock/v1/quotations/foreign-institution-total")
                .queryParam("FID_COND_MRKT_DIV_CODE", "V")
                .queryParam("FID_COND_SCR_DIV_CODE", "16449")
                .queryParam("FID_INPUT_ISCD", "0000")
                .queryParam("FID_DIV_CLS_CODE", "0")
                .queryParam("FID_RANK_SORT_CLS_CODE", "0")
                .queryParam("FID_ETC_CLS_CODE", "0")
                .build())
            .headers(kisHeaders("FHPTJ04400000"))
            .retrieve()
            .body(KisProvisionalFlowResponse.class));
        return res == null ? List.of() : res.toData(TimeUtils.todayKst());
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
