package io.signallink.market.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;

/**
 * 투자자별 순매수(수급) 도메인 모델. market.investor_flow 매핑, (stock_code, trade_date, investor_type) 유니크.
 * is_final: 장중 잠정(false) → 마감 확정(true). 같은 키는 upsert로 덮어쓴다(플랜 §5 이원화).
 * net_buy_amt 단위는 KIS 응답 그대로 백만원.
 */
@Entity
@Getter
@Table(name = "investor_flow", schema = "market")
public class InvestorFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, length = 6)
    private String stockCode;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "investor_type", nullable = false, length = 20)
    private InvestorType investorType;

    @Column(name = "net_buy_qty", nullable = false)
    private long netBuyQty;

    @Column(name = "net_buy_amt", nullable = false)
    private long netBuyAmt;

    @Column(name = "is_final", nullable = false)
    private boolean isFinal;

    protected InvestorFlow() {} // JPA 전용

    public InvestorFlow(String stockCode, LocalDate tradeDate, InvestorType investorType,
                        long netBuyQty, long netBuyAmt, boolean isFinal) {
        this.stockCode = stockCode;
        this.tradeDate = tradeDate;
        this.investorType = investorType;
        this.netBuyQty = netBuyQty;
        this.netBuyAmt = netBuyAmt;
        this.isFinal = isFinal;
    }

    /** 잠정→확정 덮어쓰기 등 값 갱신. */
    public void update(long netBuyQty, long netBuyAmt, boolean isFinal) {
        this.netBuyQty = netBuyQty;
        this.netBuyAmt = netBuyAmt;
        this.isFinal = isFinal;
    }
}
