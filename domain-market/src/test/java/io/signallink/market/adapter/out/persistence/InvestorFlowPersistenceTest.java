package io.signallink.market.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.market.domain.InvestorFlow;
import io.signallink.market.domain.InvestorType;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 실제 Postgres(Testcontainers)에서 수급 upsert — 잠정→확정 덮어쓰기(중복 아님) 검증. Docker 없으면 skip. */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class InvestorFlowPersistenceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("db/market-schema.sql");

    @Autowired
    private InvestorFlowJpaRepository jpa;

    private static final LocalDate D = LocalDate.of(2026, 7, 8);

    @Test
    void 잠정_수급을_확정이_덮어쓴다() {
        InvestorFlowPersistenceAdapter adapter = new InvestorFlowPersistenceAdapter(jpa);

        adapter.upsert(new InvestorFlow("005930", D, InvestorType.FOREIGN, -3_000_000, -800_000, false)); // 잠정
        adapter.upsert(new InvestorFlow("005930", D, InvestorType.FOREIGN, -3_015_122, -870_971, true));   // 확정

        assertThat(jpa.count()).isEqualTo(1); // 덮어씀 — 중복 행 없음
        InvestorFlow row = jpa.findByStockCodeAndTradeDateAndInvestorType("005930", D, InvestorType.FOREIGN)
            .orElseThrow();
        assertThat(row.getNetBuyQty()).isEqualTo(-3_015_122L);
        assertThat(row.isFinal()).isTrue();
    }
}
