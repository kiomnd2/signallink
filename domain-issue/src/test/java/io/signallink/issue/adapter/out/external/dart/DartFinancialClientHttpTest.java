package io.signallink.issue.adapter.out.external.dart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.signallink.issue.application.port.out.QuarterAccount;
import java.util.Optional;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * DartFinancialClient HTTP 경로 검증 — 목 서버로 요청 조립(경로·키·corp_code·연도·보고서코드)과
 * 응답 매핑(연결 우선·콤마 금액)을 확인한다. 실키·실호출 없이 fnlttSinglAcnt.json 배선을 본다.
 */
class DartFinancialClientHttpTest {

    private MockWebServer server;
    private DartFinancialClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        String baseUrl = "http://" + server.getHostName() + ":" + server.getPort();
        client = new DartFinancialClient(baseUrl, "TEST_KEY", 1000);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void 키와_corp_연도_보고서코드로_호출한다() throws Exception {
        server.enqueue(json("{\"status\":\"000\",\"list\":[]}"));

        client.fetchQuarter("00126380", 2025, "11014");

        RecordedRequest request = server.takeRequest();
        HttpUrl url = request.getRequestUrl();
        assertThat(url.encodedPath()).isEqualTo("/api/fnlttSinglAcnt.json");
        assertThat(url.queryParameter("crtfc_key")).isEqualTo("TEST_KEY");
        assertThat(url.queryParameter("corp_code")).isEqualTo("00126380");
        assertThat(url.queryParameter("bsns_year")).isEqualTo("2025");
        assertThat(url.queryParameter("reprt_code")).isEqualTo("11014");
    }

    @Test
    void 매출액과_영업이익을_연결재무제표_우선으로_매핑한다() {
        // 같은 계정이 개별(OFS)·연결(CFS)로 오면 연결 금액을 택한다
        server.enqueue(json("""
            {"status":"000","list":[
              {"account_nm":"매출액","fs_div":"OFS","thstrm_amount":"100"},
              {"account_nm":"매출액","fs_div":"CFS","thstrm_amount":"2,589,355"},
              {"account_nm":"영업이익","fs_div":"CFS","thstrm_amount":"-1,234"}
            ]}"""));

        Optional<QuarterAccount> q = client.fetchQuarter("00126380", 2025, "11014");

        assertThat(q).isPresent();
        assertThat(q.get().quarter()).isEqualTo(3);            // 11014 → 3분기
        assertThat(q.get().revenue()).isEqualTo(2_589_355L);
        assertThat(q.get().operatingProfit()).isEqualTo(-1_234L);
    }

    @Test
    void 데이터없음_013은_empty다() {
        server.enqueue(json("{\"status\":\"013\",\"message\":\"조회된 데이터가 없습니다.\"}"));

        assertThat(client.fetchQuarter("00126380", 2025, "11013")).isEmpty();
    }

    @Test
    void 오류_상태는_예외로_전파한다() {
        server.enqueue(json("{\"status\":\"020\",\"message\":\"요청 제한을 초과하였습니다.\"}"));

        assertThatThrownBy(() -> client.fetchQuarter("00126380", 2025, "11011"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("020");
    }

    private static MockResponse json(String body) {
        return new MockResponse()
            .addHeader("Content-Type", "application/json;charset=UTF-8")
            .setBody(body);
    }
}
