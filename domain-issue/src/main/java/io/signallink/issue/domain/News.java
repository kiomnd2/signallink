package io.signallink.issue.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;

/**
 * 뉴스 도메인 모델 (헥사고날 Level 1 — JPA 엔티티 겸용). issue.news 매핑.
 * (stock_code, url) 유니크 → 종목별 중복 뉴스 방지.
 */
@Entity
@Getter
@Table(name = "news", schema = "issue")
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, length = 6)
    private String stockCode;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(length = 50)
    private String source;

    @Column(name = "published_at", nullable = false)
    private OffsetDateTime publishedAt;

    protected News() {} // JPA 전용

    public News(String stockCode, String title, String summary, String url, String source,
                OffsetDateTime publishedAt) {
        this.stockCode = stockCode;
        this.title = title;
        this.summary = summary;
        this.url = url;
        this.source = source;
        this.publishedAt = publishedAt;
    }
}
