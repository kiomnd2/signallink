package io.signallink.insight.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.insight.application.port.out.FeatureCardUpsert;
import io.signallink.insight.domain.CardStatus;
import io.signallink.insight.domain.FeatureCard;
import io.signallink.insight.domain.IssueRef;
import io.signallink.insight.domain.IssueType;
import io.signallink.insight.domain.TriggerType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
 * 실제 Postgres(Testcontainers)에서 insight.feature_card 매핑을 검증한다 — 특히 source_refs(jsonb) 직렬화·
 * 왕복과 (stock,date) upsert. Docker 미가동 환경에서는 자동 skip.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import(FeatureCardPersistenceAdapter.class)
class FeatureCardPersistenceTest {

    private static final LocalDate D = LocalDate.of(2026, 7, 8);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("db/insight-schema.sql");

    @Autowired
    private FeatureCardPersistenceAdapter adapter;

    @Autowired
    private FeatureCardJpaRepository jpa;

    @Test
    void 카드를_저장하고_source_refs_jsonb를_왕복한다() {
        var ref = new IssueRef(IssueType.DISCLOSURE, "단일판매ㆍ공급계약 \"체결\"", "http://dart/1",
            OffsetDateTime.of(2026, 7, 8, 9, 0, 0, 0, ZoneOffset.ofHours(9)), "수시공시");
        adapter.upsert(upsert(new BigDecimal("6.000"), "장중 잠정 집계로는, 외국인은 순매수(약 25억 원).", List.of(ref)));

        FeatureCard saved = jpa.findByStockCodeAndTradeDate("005930", D).orElseThrow();
        assertThat(saved.getChangeRate()).isEqualByComparingTo("6.000");
        assertThat(saved.getStatus()).isEqualTo(CardStatus.PUBLISHED);
        assertThat(saved.isLlmUsed()).isFalse();
        // jsonb 왕복: 이스케이프된 따옴표·한글이 보존된다
        assertThat(saved.getSourceRefs())
            .contains("\"type\":\"DISCLOSURE\"")
            .contains("공급계약")
            .contains("\\\"체결\\\"");
    }

    @Test
    void 같은_종목_거래일은_upsert로_갱신된다() {
        adapter.upsert(upsert(new BigDecimal("6.000"), "첫 요약", List.of()));
        adapter.upsert(upsert(new BigDecimal("7.500"), "갱신 요약", List.of()));

        assertThat(jpa.findAll()).hasSize(1);
        FeatureCard saved = jpa.findByStockCodeAndTradeDate("005930", D).orElseThrow();
        assertThat(saved.getChangeRate()).isEqualByComparingTo("7.500");
        assertThat(saved.getFlowSummary()).isEqualTo("갱신 요약");
        assertThat(saved.getSourceRefs()).isEqualTo("[]");
    }

    private static FeatureCardUpsert upsert(BigDecimal changeRate, String flowSummary, List<IssueRef> refs) {
        return new FeatureCardUpsert("005930", D, changeRate, TriggerType.SURGE,
            new BigDecimal("2.000"), true, "삼성전자가 올랐어요.", flowSummary, null, refs, false, CardStatus.PUBLISHED);
    }
}
