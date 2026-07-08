package io.signallink.market.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지수 일별 시세 도메인 모델. market.index_price 매핑. (index_code, trade_date) 복합 PK.
 * change_rate는 KIS 응답에 없어 수집 단계에서 계산해 넣는다.
 */
@Entity
@Getter
@Table(name = "index_price", schema = "market")
@IdClass(IndexPrice.Pk.class)
public class IndexPrice {

    @Id
    @Column(name = "index_code", length = 10)
    private String indexCode;

    @Id
    @Column(name = "trade_date")
    private LocalDate tradeDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal close;

    @Column(name = "change_rate", nullable = false, precision = 6, scale = 3)
    private BigDecimal changeRate;

    protected IndexPrice() {} // JPA 전용

    public IndexPrice(String indexCode, LocalDate tradeDate, BigDecimal close, BigDecimal changeRate) {
        this.indexCode = indexCode;
        this.tradeDate = tradeDate;
        this.close = close;
        this.changeRate = changeRate;
    }

    /** 복합 PK 클래스 (@IdClass) — 필드명이 엔티티 @Id 필드와 일치해야 한다. */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private String indexCode;
        private LocalDate tradeDate;
    }
}
