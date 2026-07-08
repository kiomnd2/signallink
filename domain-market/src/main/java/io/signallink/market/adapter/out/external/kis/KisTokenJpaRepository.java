package io.signallink.market.adapter.out.external.kis;

import org.springframework.data.jpa.repository.JpaRepository;

/** KIS 토큰 캐시 저장소 (단일 행). */
public interface KisTokenJpaRepository extends JpaRepository<KisToken, Integer> {
}
