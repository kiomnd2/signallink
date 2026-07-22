package io.signallink.insight.adapter.out.persistence;

import io.signallink.insight.application.port.out.HealthCheckRepositoryPort;
import io.signallink.insight.application.port.out.HealthCheckUpsert;
import io.signallink.insight.domain.HealthCheck;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 아웃바운드 어댑터 — 체력 진단 upsert. card_id 있으면 갱신, 없으면 삽입. */
@Component
@RequiredArgsConstructor
public class HealthCheckPersistenceAdapter implements HealthCheckRepositoryPort {

    private final HealthCheckJpaRepository jpa;

    @Override
    public void upsert(HealthCheckUpsert c) {
        jpa.findByCardId(c.cardId())
            .ifPresentOrElse(
                existing -> {
                    existing.update(c.issueNature(), c.revenueTrend(), c.profitTrend(), c.flowAfter());
                    jpa.save(existing);
                },
                () -> jpa.save(new HealthCheck(
                    c.cardId(), c.issueNature(), c.revenueTrend(), c.profitTrend(), c.flowAfter())));
    }
}
