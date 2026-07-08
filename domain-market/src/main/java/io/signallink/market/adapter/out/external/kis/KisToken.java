package io.signallink.market.adapter.out.external.kis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;

/**
 * KIS 액세스 토큰 캐시(단일 행, id=1). KIS 어댑터의 영속 디테일 — 도메인 모델이 아니라 인프라 상태다.
 * market.kis_token 매핑.
 */
@Entity
@Getter
@Table(name = "kis_token", schema = "market")
public class KisToken {

    @Id
    private Integer id;

    @Column(name = "access_token", nullable = false, length = 400)
    private String accessToken;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    protected KisToken() {}

    public KisToken(Integer id, String accessToken, OffsetDateTime issuedAt, OffsetDateTime expiresAt) {
        this.id = id;
        this.accessToken = accessToken;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }
}
