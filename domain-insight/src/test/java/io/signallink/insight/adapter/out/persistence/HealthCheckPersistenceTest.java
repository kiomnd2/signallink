package io.signallink.insight.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.insight.application.port.out.FeatureCardUpsert;
import io.signallink.insight.application.port.out.HealthCheckUpsert;
import io.signallink.insight.domain.CardStatus;
import io.signallink.insight.domain.HealthCheck;
import io.signallink.insight.domain.IssueNature;
import io.signallink.insight.domain.TriggerType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 실제 Postgres(Testcontainers)에서 insight.health_check 매핑을 검증한다 — card_id FK·유니크와 upsert 갱신.
 * Docker 미가동 환경에서는 자동 skip.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import({HealthCheckPersistenceAdapter.class, FeatureCardPersistenceAdapter.class})
class HealthCheckPersistenceTest {

    private static final LocalDate D = LocalDate.of(2026, 7, 8);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("db/insight-schema.sql");

    @Autowired
    private HealthCheckPersistenceAdapter adapter;
    @Autowired
    private FeatureCardPersistenceAdapter cardAdapter;
    @Autowired
    private HealthCheckJpaRepository jpa;

    private long newCard() {
        return cardAdapter.upsert(new FeatureCardUpsert("005930", D, new BigDecimal("6.000"),
            TriggerType.SURGE, new BigDecimal("2.000"), true, "삼성전자가 올랐어요.",
            "수급 중립", null, List.of(), false, CardStatus.PUBLISHED));
    }

    @Test
    void 카드에_체력진단_이슈성격_태그를_저장한다() {
        long cardId = newCard();

        adapter.upsert(HealthCheckUpsert.ofNature(cardId, IssueNature.ONE_OFF));

        HealthCheck saved = jpa.findByCardId(cardId).orElseThrow();
        assertThat(saved.getIssueNature()).isEqualTo(IssueNature.ONE_OFF);
        assertThat(saved.getRevenueTrend()).isNull();   // 실적 추세는 3-7c에서
        assertThat(saved.getFlowAfter()).isNull();       // 수급 추세는 3-7b(J6)에서
    }

    @Test
    void 같은_카드_진단은_upsert로_갱신된다() {
        long cardId = newCard();

        adapter.upsert(HealthCheckUpsert.ofNature(cardId, IssueNature.UNCLASSIFIED));
        adapter.upsert(new HealthCheckUpsert(cardId, IssueNature.STRUCTURAL, "DECLINING", "DECLINING", null));

        assertThat(jpa.findAll()).hasSize(1);
        HealthCheck saved = jpa.findByCardId(cardId).orElseThrow();
        assertThat(saved.getIssueNature()).isEqualTo(IssueNature.STRUCTURAL);
        assertThat(saved.getRevenueTrend()).isEqualTo("DECLINING");
    }
}
