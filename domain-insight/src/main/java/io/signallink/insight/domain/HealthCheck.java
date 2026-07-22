package io.signallink.insight.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 체력 진단 도메인 모델 (헥사고날 Level 1 — JPA 엔티티 겸용). insight.health_check 매핑.
 * card_id 유니크 — 카드 1건당 진단 1건(upsert로 갱신). 실적·수급 추세는 후속 슬라이스에서 채워 nullable.
 */
@Entity
@Getter
@Table(name = "health_check", schema = "insight")
public class HealthCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", nullable = false, unique = true)
    private Long cardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_nature", nullable = false, length = 15)
    private IssueNature issueNature;

    @Column(name = "revenue_trend", length = 15)
    private String revenueTrend;

    @Column(name = "profit_trend", length = 15)
    private String profitTrend;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "flow_after", columnDefinition = "jsonb")
    private String flowAfter;

    protected HealthCheck() {} // JPA 전용

    public HealthCheck(Long cardId, IssueNature issueNature, String revenueTrend, String profitTrend,
                       String flowAfter) {
        this.cardId = cardId;
        this.issueNature = issueNature;
        this.revenueTrend = revenueTrend;
        this.profitTrend = profitTrend;
        this.flowAfter = flowAfter;
    }

    /** 같은 카드의 진단 재계산 시 팩트를 덮어쓴다. */
    public void update(IssueNature issueNature, String revenueTrend, String profitTrend, String flowAfter) {
        this.issueNature = issueNature;
        this.revenueTrend = revenueTrend;
        this.profitTrend = profitTrend;
        this.flowAfter = flowAfter;
    }
}
