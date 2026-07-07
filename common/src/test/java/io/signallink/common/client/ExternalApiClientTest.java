package io.signallink.common.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

class ExternalApiClientTest {

    /** 테스트용 최소 구현 — 백오프 10ms로 빠르게. */
    static class TestClient extends ExternalApiClient {
        TestClient() {
            super("http://localhost", new RateLimiter(1000), 3, 10);
        }

        <T> T run(java.util.function.Supplier<T> call) {
            return execute("test.op", call);
        }
    }

    @Test
    void IO_오류는_재시도_후_성공한다() {
        AtomicInteger calls = new AtomicInteger();
        String result = new TestClient().run(() -> {
            if (calls.incrementAndGet() < 3) {
                throw new ResourceAccessException("timeout");
            }
            return "ok";
        });
        assertThat(result).isEqualTo("ok");
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void 서버_5xx는_재시도하고_최대_횟수_초과_시_던진다() {
        AtomicInteger calls = new AtomicInteger();
        assertThatThrownBy(() -> new TestClient().run(() -> {
            calls.incrementAndGet();
            throw HttpServerErrorException.create(
                    HttpStatus.SERVICE_UNAVAILABLE, "503", null, null, null);
        })).isInstanceOf(HttpServerErrorException.class);
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void 클라이언트_4xx는_재시도하지_않는다() {
        AtomicInteger calls = new AtomicInteger();
        assertThatThrownBy(() -> new TestClient().run(() -> {
            calls.incrementAndGet();
            throw HttpClientErrorException.create(
                    HttpStatus.BAD_REQUEST, "400", null, null, null);
        })).isInstanceOf(HttpClientErrorException.class);
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void 유량_초과_429는_재시도한다() {
        AtomicInteger calls = new AtomicInteger();
        String result = new TestClient().run(() -> {
            if (calls.incrementAndGet() == 1) {
                throw HttpClientErrorException.create(
                        HttpStatus.TOO_MANY_REQUESTS, "429", null, null, null);
            }
            return "ok";
        });
        assertThat(result).isEqualTo("ok");
        assertThat(calls.get()).isEqualTo(2);
    }
}
