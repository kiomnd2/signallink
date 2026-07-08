package io.signallink.market.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;

/**
 * 종목 마스터 도메인 모델 (헥사고날 Level 1 — JPA 엔티티 겸용, docs/ARCHITECTURE.md §4).
 * market.stock 매핑. stock_code(6자리 종목코드)가 자연 키.
 */
@Entity
@Getter
@Table(name = "stock", schema = "market")
public class Stock {

    @Id
    @Column(name = "stock_code", length = 6)
    private String stockCode;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "market_type", nullable = false, length = 10)
    private String marketType; // KOSPI | KOSDAQ

    @Column(name = "sector_code", length = 10)
    private String sectorCode;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Stock() {} // JPA 전용

    public Stock(String stockCode, String name, String marketType, String sectorCode, OffsetDateTime updatedAt) {
        this.stockCode = stockCode;
        this.name = name;
        this.marketType = marketType;
        this.sectorCode = sectorCode;
        this.updatedAt = updatedAt;
    }
}
