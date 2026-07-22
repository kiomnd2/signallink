package io.signallink.insight.application.port.out;

/**
 * 아웃바운드 포트 — Why 카드 영속화. 구현: adapter.out.persistence.FeatureCardPersistenceAdapter.
 * (stock_code, trade_date) 기준 upsert(멱등) — 30분 배치 재실행이 같은 카드를 갱신한다.
 */
public interface FeatureCardRepositoryPort {
    /** @return upsert된 카드의 id (체력 진단이 card_id로 참조). */
    long upsert(FeatureCardUpsert card);
}
