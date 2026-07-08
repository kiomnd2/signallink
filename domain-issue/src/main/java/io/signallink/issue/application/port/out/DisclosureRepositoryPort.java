package io.signallink.issue.application.port.out;

import io.signallink.issue.domain.Disclosure;

/**
 * 아웃바운드 포트 — 공시 영속화. 구현: adapter.out.persistence.DisclosurePersistenceAdapter.
 * 서비스는 이 인터페이스에만 의존하므로 DB 없이 인메모리 스텁으로 단위 테스트 가능.
 */
public interface DisclosureRepositoryPort {
    boolean existsByReceiptNo(String receiptNo);
    Disclosure save(Disclosure disclosure);
}
