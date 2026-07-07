package io.signallink.common.client;

/**
 * 외부 API 호출용 블로킹 rate limiter (초당 N건).
 * KIS 신규 고객 한도가 확정되면(M0 실측) permitsPerSecond를 설정값으로 주입한다.
 */
public final class RateLimiter {

    private final long minIntervalNanos;
    private long nextFreeAt = 0L;

    public RateLimiter(double permitsPerSecond) {
        if (permitsPerSecond <= 0) {
            throw new IllegalArgumentException("permitsPerSecond must be > 0");
        }
        this.minIntervalNanos = (long) (1_000_000_000L / permitsPerSecond);
    }

    /** 다음 호출이 허용될 때까지 블로킹한다. */
    public synchronized void acquire() {
        long now = System.nanoTime();
        long waitNanos = nextFreeAt - now;
        nextFreeAt = Math.max(now, nextFreeAt) + minIntervalNanos;
        if (waitNanos > 0) {
            try {
                Thread.sleep(waitNanos / 1_000_000, (int) (waitNanos % 1_000_000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("rate limiter interrupted", e);
            }
        }
    }
}
