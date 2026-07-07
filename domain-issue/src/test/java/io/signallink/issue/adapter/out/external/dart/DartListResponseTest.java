package io.signallink.issue.adapter.out.external.dart;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.signallink.issue.application.port.out.DisclosureCandidate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** DART 실응답 형태의 픽스처 JSON → DisclosureCandidate 매핑 검증 (외부 호출 없이). */
class DartListResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void 픽스처_JSON을_공시후보로_매핑한다() throws Exception {
        DartListResponse res;
        try (var in = new ClassPathResource("fixtures/dart-list-sample.json").getInputStream()) {
            res = mapper.readValue(in, DartListResponse.class);
        }

        assertThat(res.status()).isEqualTo(DartListResponse.OK);

        List<DisclosureCandidate> candidates = res.toCandidates();
        assertThat(candidates).hasSize(2);

        DisclosureCandidate first = candidates.get(0);
        assertThat(first.receiptNo()).isEqualTo("20240102000123");
        assertThat(first.stockCode()).isEqualTo("005930");
        assertThat(first.title()).isEqualTo("단일판매ㆍ공급계약체결");
        assertThat(first.dartUrl())
            .isEqualTo("https://dart.fss.or.kr/dsaf001/main.do?rcpNo=20240102000123");
        // rcept_dt=20240102 → KST 자정
        assertThat(first.disclosedAt().toString()).startsWith("2024-01-02T00:00+09:00");
    }
}
