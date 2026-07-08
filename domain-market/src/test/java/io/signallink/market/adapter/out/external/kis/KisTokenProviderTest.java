package io.signallink.market.adapter.out.external.kis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/** 토큰 재사용/갱신 판단 로직(만료 여유 10분) 검증 — HTTP 없이 순수. */
class KisTokenProviderTest {

    private final OffsetDateTime now = OffsetDateTime.parse("2026-07-08T09:00+09:00");

    @Test
    void 만료까지_여유가_있으면_재사용_가능() {
        KisToken token = new KisToken(1, "t", now.minusHours(1), now.plusHours(23));
        assertThat(KisTokenProvider.isUsable(token, now)).isTrue();
    }

    @Test
    void 만료_여유가_10분_미만이면_재사용_불가() {
        KisToken token = new KisToken(1, "t", now.minusHours(24), now.plusMinutes(5));
        assertThat(KisTokenProvider.isUsable(token, now)).isFalse();
    }

    @Test
    void 캐시가_없으면_재사용_불가() {
        assertThat(KisTokenProvider.isUsable(null, now)).isFalse();
    }
}
