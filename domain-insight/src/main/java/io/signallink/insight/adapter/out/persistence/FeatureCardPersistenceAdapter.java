package io.signallink.insight.adapter.out.persistence;

import io.signallink.insight.application.port.out.FeatureCardRepositoryPort;
import io.signallink.insight.application.port.out.FeatureCardUpsert;
import io.signallink.insight.domain.FeatureCard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 아웃바운드 어댑터 — 카드 upsert. (stock,date) 있으면 갱신, 없으면 삽입. source_refs는 여기서 jsonb 직렬화. */
@Component
@RequiredArgsConstructor
public class FeatureCardPersistenceAdapter implements FeatureCardRepositoryPort {

    private final FeatureCardJpaRepository jpa;

    @Override
    public void upsert(FeatureCardUpsert c) {
        String sourceRefs = SourceRefsJson.serialize(c.sourceRefs());
        jpa.findByStockCodeAndTradeDate(c.stockCode(), c.tradeDate())
            .ifPresentOrElse(
                existing -> {
                    existing.update(c.changeRate(), c.triggerType(), c.marketContrib(), c.sectorSync(),
                        c.whatHappened(), c.flowSummary(), c.contextNote(), sourceRefs, c.llmUsed(), c.status());
                    jpa.save(existing);
                },
                () -> jpa.save(new FeatureCard(c.stockCode(), c.tradeDate(), c.changeRate(), c.triggerType(),
                    c.marketContrib(), c.sectorSync(), c.whatHappened(), c.flowSummary(), c.contextNote(),
                    sourceRefs, c.llmUsed(), c.status())));
    }
}
