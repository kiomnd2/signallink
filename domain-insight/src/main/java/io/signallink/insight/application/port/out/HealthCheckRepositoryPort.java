package io.signallink.insight.application.port.out;

/**
 * 아웃바운드 포트 — 체력 진단 영속화. 구현: adapter.out.persistence.HealthCheckPersistenceAdapter.
 * {@code card_id} 기준 upsert(멱등) — 30분 배치 재실행/후속 잡(J6)이 같은 진단을 갱신한다.
 */
public interface HealthCheckRepositoryPort {
    void upsert(HealthCheckUpsert healthCheck);
}
