package io.signallink.market.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.market.domain.StockBeta;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 실제 Postgres(Testcontainers)에서 stock_beta save가 PK(stock_code) 기준 upsert인지 검증. Docker 없으면 skip. */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class StockBetaPersistenceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("db/market-schema.sql");

    @Autowired
    private StockBetaJpaRepository jpa;

    @Test
    void 같은_종목_베타는_덮어쓴다() {
        jpa.saveAndFlush(new StockBeta("005930", new BigDecimal("1.234"), LocalDate.of(2026, 7, 8)));
        jpa.saveAndFlush(new StockBeta("005930", new BigDecimal("1.500"), LocalDate.of(2026, 7, 9)));

        assertThat(jpa.count()).isEqualTo(1); // PK 기준 upsert
        StockBeta row = jpa.findById("005930").orElseThrow();
        assertThat(row.getBeta60d()).isEqualByComparingTo("1.500");
    }
}
