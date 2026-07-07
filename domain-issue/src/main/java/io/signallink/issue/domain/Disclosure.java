package io.signallink.issue.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * 공시 도메인 모델 (헥사고날 Level 1 — JPA 엔티티 겸용, docs/ARCHITECTURE.md §4).
 *
 * <p>issue.disclosure 테이블 매핑. DART 접수번호(rcept_no)가 자연 유니크 키 → 중복 수집 방지의 기준.
 */
@Entity
@Table(name = "disclosure", schema = "issue")
public class Disclosure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // bigserial
    private Long id;

    @Column(name = "stock_code", nullable = false, length = 6)
    private String stockCode;

    @Column(name = "rcept_no", nullable = false, length = 20, unique = true)
    private String receiptNo;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 30)
    private String category;

    @Column(name = "disclosed_at", nullable = false)
    private OffsetDateTime disclosedAt;

    @Column(name = "dart_url", nullable = false, length = 300)
    private String dartUrl;

    protected Disclosure() {} // JPA 전용

    public Disclosure(String stockCode, String receiptNo, String title, String category,
                      OffsetDateTime disclosedAt, String dartUrl) {
        this.stockCode = stockCode;
        this.receiptNo = receiptNo;
        this.title = title;
        this.category = category;
        this.disclosedAt = disclosedAt;
        this.dartUrl = dartUrl;
    }

    public Long getId() { return id; }
    public String getStockCode() { return stockCode; }
    public String getReceiptNo() { return receiptNo; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public OffsetDateTime getDisclosedAt() { return disclosedAt; }
    public String getDartUrl() { return dartUrl; }
}
