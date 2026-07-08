package io.signallink.market.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.market.adapter.out.external.kis.KisToken;
import io.signallink.market.adapter.out.external.kis.KisTokenJpaRepository;
import io.signallink.market.domain.Stock;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 실제 Postgres(Testcontainers)에서 market.stock / market.kis_token 매핑 검증. Docker 없으면 skip. */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class MarketPersistenceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("db/market-schema.sql");

    @Autowired
    private StockJpaRepository stockRepository;

    @Autowired
    private KisTokenJpaRepository tokenRepository;

    @Test
    void 종목을_저장하고_개수를_센다() {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-08T09:00+09:00");
        stockRepository.save(new Stock("005930", "삼성전자", "KOSPI", "005", now));
        stockRepository.save(new Stock("000660", "SK하이닉스", "KOSPI", null, now));

        assertThat(stockRepository.count()).isEqualTo(2);
        assertThat(stockRepository.findById("005930")).get()
            .extracting(Stock::getName).isEqualTo("삼성전자");
    }

    @Test
    void KIS_토큰을_단일_행으로_저장_조회한다() {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-08T09:00+09:00");
        tokenRepository.save(new KisToken(1, "access-xyz", now, now.plusHours(24)));

        assertThat(tokenRepository.findById(1)).get()
            .extracting(KisToken::getAccessToken).isEqualTo("access-xyz");
    }
}
