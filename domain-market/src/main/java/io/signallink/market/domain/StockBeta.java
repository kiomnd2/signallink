package io.signallink.market.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;

/**
 * 종목 베타 도메인 모델. market.stock_beta 매핑, stock_code가 PK.
 * 재계산 시 save가 id 기준 merge(upsert)로 갱신한다.
 */
@Entity
@Getter
@Table(name = "stock_beta", schema = "market")
public class StockBeta {

    @Id
    @Column(name = "stock_code", length = 6)
    private String stockCode;

    @Column(name = "beta_60d", nullable = false, precision = 6, scale = 3)
    private BigDecimal beta60d;

    @Column(name = "calculated_at", nullable = false)
    private LocalDate calculatedAt;

    protected StockBeta() {} // JPA 전용

    public StockBeta(String stockCode, BigDecimal beta60d, LocalDate calculatedAt) {
        this.stockCode = stockCode;
        this.beta60d = beta60d;
        this.calculatedAt = calculatedAt;
    }
}
