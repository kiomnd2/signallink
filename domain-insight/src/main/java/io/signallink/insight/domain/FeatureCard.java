package io.signallink.insight.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Why 카드 도메인 모델 (헥사고날 Level 1 — JPA 엔티티 겸용). insight.feature_card 매핑.
 * (stock_code, trade_date) 유니크 — 같은 종목·거래일 카드는 upsert로 갱신(30분 배치 재실행 멱등).
 *
 * <p>source_refs는 근거(공시·뉴스) 배열의 JSON 문자열로 저장(jsonb). created_at은 DB 기본값(now())에 맡긴다.
 */
@Entity
@Getter
@Table(name = "feature_card", schema = "insight")
public class FeatureCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, length = 6)
    private String stockCode;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "change_rate", nullable = false, precision = 6, scale = 3)
    private BigDecimal changeRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 10)
    private TriggerType triggerType;

    @Column(name = "market_contrib", precision = 6, scale = 3)
    private BigDecimal marketContrib;

    @Column(name = "sector_sync")
    private Boolean sectorSync;

    @Column(name = "what_happened", columnDefinition = "text")
    private String whatHappened;

    @Column(name = "flow_summary", columnDefinition = "text")
    private String flowSummary;

    @Column(name = "context_note", columnDefinition = "text")
    private String contextNote;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_refs", columnDefinition = "jsonb")
    private String sourceRefs;

    @Column(name = "llm_used", nullable = false)
    private boolean llmUsed;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private CardStatus status;

    protected FeatureCard() {} // JPA 전용

    public FeatureCard(String stockCode, LocalDate tradeDate, BigDecimal changeRate, TriggerType triggerType,
                       BigDecimal marketContrib, Boolean sectorSync, String whatHappened, String flowSummary,
                       String contextNote, String sourceRefs, boolean llmUsed, CardStatus status) {
        this.stockCode = stockCode;
        this.tradeDate = tradeDate;
        this.changeRate = changeRate;
        this.triggerType = triggerType;
        this.marketContrib = marketContrib;
        this.sectorSync = sectorSync;
        this.whatHappened = whatHappened;
        this.flowSummary = flowSummary;
        this.contextNote = contextNote;
        this.sourceRefs = sourceRefs;
        this.llmUsed = llmUsed;
        this.status = status;
    }

    /** 같은 (종목,거래일) 카드 재생성 시 분석 결과를 덮어쓴다. */
    public void update(BigDecimal changeRate, TriggerType triggerType, BigDecimal marketContrib, Boolean sectorSync,
                       String whatHappened, String flowSummary, String contextNote, String sourceRefs,
                       boolean llmUsed, CardStatus status) {
        this.changeRate = changeRate;
        this.triggerType = triggerType;
        this.marketContrib = marketContrib;
        this.sectorSync = sectorSync;
        this.whatHappened = whatHappened;
        this.flowSummary = flowSummary;
        this.contextNote = contextNote;
        this.sourceRefs = sourceRefs;
        this.llmUsed = llmUsed;
        this.status = status;
    }
}
