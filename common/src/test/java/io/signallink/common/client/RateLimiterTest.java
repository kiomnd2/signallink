package io.signallink.common.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RateLimiterTest {

    @Test
    void 초당_한도만큼_간격을_강제한다() {
        RateLimiter limiter = new RateLimiter(20); // 50ms 간격
        long start = System.nanoTime();
        for (int i = 0; i < 4; i++) {
            limiter.acquire();
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(elapsedMs).isGreaterThanOrEqualTo(140); // 3 간격 × 50ms (오차 허용)
    }

    @Test
    void 첫_호출은_대기하지_않는다() {
        RateLimiter limiter = new RateLimiter(1);
        long start = System.nanoTime();
        limiter.acquire();
        assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(100);
    }

    @Test
    void 잘못된_한도는_거부한다() {
        assertThatThrownBy(() -> new RateLimiter(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
