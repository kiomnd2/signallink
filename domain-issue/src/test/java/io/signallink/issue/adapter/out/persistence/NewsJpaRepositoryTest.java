package io.signallink.issue.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.signallink.issue.domain.News;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 실제 Postgres(Testcontainers)에서 issue.news 스키마·(stock_code,url) 유니크 검증. Docker 없으면 skip. */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class NewsJpaRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("db/issue-schema.sql");

    @Autowired
    private NewsJpaRepository repository;

    @Test
    void 저장하면_stockCode_url로_존재를_확인한다() {
        repository.saveAndFlush(sample("https://n.news.naver.com/1"));

        assertThat(repository.existsByStockCodeAndUrl("005930", "https://n.news.naver.com/1")).isTrue();
        assertThat(repository.existsByStockCodeAndUrl("005930", "https://n.news.naver.com/x")).isFalse();
    }

    @Test
    void 같은_종목_url을_두_번_저장하면_유니크_제약에_걸린다() {
        repository.saveAndFlush(sample("https://n.news.naver.com/dup"));

        assertThatThrownBy(() -> repository.saveAndFlush(sample("https://n.news.naver.com/dup")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private News sample(String url) {
        return new News("005930", "삼성전자 뉴스", "요약", url, "naver",
            OffsetDateTime.parse("2026-07-08T09:00+09:00"));
    }
}
