package io.signallink.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** 엔티티 작성 패턴 예시 — 다른 테이블도 이 형태를 따른다 (스키마 지정 필수). */
@Entity
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

    protected Stock() {}

    public Stock(String stockCode, String name, String marketType, String sectorCode, OffsetDateTime updatedAt) {
        this.stockCode = stockCode;
        this.name = name;
        this.marketType = marketType;
        this.sectorCode = sectorCode;
        this.updatedAt = updatedAt;
    }

    public String getStockCode() { return stockCode; }
    public String getName() { return name; }
    public String getMarketType() { return marketType; }
    public String getSectorCode() { return sectorCode; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
