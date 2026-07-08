package io.signallink.market.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;

/**
 * 일봉 도메인 모델 (헥사고날 Level 1 — JPA 엔티티 겸용). market.daily_price 매핑.
 * (stock_code, trade_date) 유니크 → 백필·재수집 멱등. is_final: 장중 잠정→마감 확정 구분.
 * change_rate는 KIS 응답에 없어 수집 단계에서 계산해 넣는다. vol_ratio_20d는 적재 후 계산(nullable).
 */
@Entity
@Getter
@Table(name = "daily_price", schema = "market")
public class DailyPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, length = 6)
    private String stockCode;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal close;

    @Column(name = "change_rate", nullable = false, precision = 6, scale = 3)
    private BigDecimal changeRate;

    @Column(nullable = false)
    private long volume;

    @Column(name = "trading_value", nullable = false)
    private long tradingValue;

    @Column(name = "vol_ratio_20d", precision = 6, scale = 2)
    private BigDecimal volRatio20d;

    @Column(name = "is_final", nullable = false)
    private boolean isFinal;

    protected DailyPrice() {} // JPA 전용

    public DailyPrice(String stockCode, LocalDate tradeDate, BigDecimal close, BigDecimal changeRate,
                      long volume, long tradingValue, BigDecimal volRatio20d, boolean isFinal) {
        this.stockCode = stockCode;
        this.tradeDate = tradeDate;
        this.close = close;
        this.changeRate = changeRate;
        this.volume = volume;
        this.tradingValue = tradingValue;
        this.volRatio20d = volRatio20d;
        this.isFinal = isFinal;
    }
}
