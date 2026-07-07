package io.signallink.issue.adapter.out.external.dart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** DartClient.interpret() — DART 상태코드 해석 (HTTP 없이 순수 검증). */
class DartClientTest {

    @Test
    void 정상_000은_후보로_변환한다() {
        var res = new DartListResponse("000", "정상", List.of(
            new DartListResponse.Item("20240102000001", "005930", "공급계약체결", "20240102")));

        assertThat(DartClient.interpret(res)).hasSize(1);
    }

    @Test
    void 데이터없음_013은_빈_목록으로_정상_처리한다() {
        var res = new DartListResponse("013", "조회된 데이터가 없습니다.", null);

        assertThat(DartClient.interpret(res)).isEmpty();
    }

    @Test
    void 비정상_상태는_예외를_던진다() {
        var res = new DartListResponse("020", "요청 제한을 초과하였습니다.", null); // 한도 초과

        assertThatThrownBy(() -> DartClient.interpret(res))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("020");
    }

    @Test
    void 응답이_null이면_예외를_던진다() {
        assertThatThrownBy(() -> DartClient.interpret(null))
            .isInstanceOf(IllegalStateException.class);
    }
}
