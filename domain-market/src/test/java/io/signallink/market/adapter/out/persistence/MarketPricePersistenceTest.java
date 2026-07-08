package io.signallink.market.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.signallink.market.domain.DailyPrice;
import io.signallink.market.domain.IndexPrice;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 실제 Postgres(Testcontainers)에서 daily_price 유니크·index_price 복합키 검증. Docker 없으면 skip. */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class MarketPricePersistenceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("db/market-schema.sql");

    @Autowired
    private DailyPriceJpaRepository dailyRepository;

    @Autowired
    private IndexPriceJpaRepository indexRepository;

    private static final LocalDate D = LocalDate.of(2026, 7, 8);

    @Test
    void 일봉을_저장하고_종목_날짜로_존재를_확인한다() {
        dailyRepository.saveAndFlush(daily("005930", D));

        assertThat(dailyRepository.existsByStockCodeAndTradeDate("005930", D)).isTrue();
        assertThat(dailyRepository.existsByStockCodeAndTradeDate("005930", D.minusDays(1))).isFalse();
    }

    @Test
    void 같은_종목_날짜_일봉을_두_번_저장하면_유니크_제약에_걸린다() {
        dailyRepository.saveAndFlush(daily("005930", D));

        assertThatThrownBy(() -> dailyRepository.saveAndFlush(daily("005930", D)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 지수를_복합키로_저장_조회한다() {
        indexRepository.saveAndFlush(
            new IndexPrice("0001", D, new BigDecimal("7297.25"), new BigDecimal("-2.100")));

        assertThat(indexRepository.findById(new IndexPrice.Pk("0001", D))).isPresent();
        assertThat(indexRepository.existsByIndexCodeAndTradeDate("0001", D)).isTrue();
    }

    private DailyPrice daily(String stockCode, LocalDate tradeDate) {
        return new DailyPrice(stockCode, tradeDate,
            new BigDecimal("278000.00"), new BigDecimal("-6.080"), 21_442_122L, 6_203_329_208_250L, null, true);
    }
}
