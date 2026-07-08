package io.signallink.issue.adapter.out.persistence;

import io.signallink.issue.application.port.out.DisclosureRepositoryPort;
import io.signallink.issue.domain.Disclosure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 아웃바운드 어댑터 — DisclosureRepositoryPort를 Spring Data JPA로 구현. */
@Component
@RequiredArgsConstructor
public class DisclosurePersistenceAdapter implements DisclosureRepositoryPort {

    private final DisclosureJpaRepository jpa;

    @Override
    public boolean existsByReceiptNo(String receiptNo) {
        return jpa.existsByReceiptNo(receiptNo);
    }

    @Override
    public Disclosure save(Disclosure disclosure) {
        return jpa.save(disclosure);
    }
}
