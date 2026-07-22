package io.signallink.insight.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class IssueNatureClassifierTest {

    private static final OffsetDateTime T = OffsetDateTime.parse("2026-07-08T09:00+09:00");

    private static IssueRef disclosure(String title) {
        return new IssueRef(IssueType.DISCLOSURE, title, "http://dart/1", T, "수시공시");
    }

    private static IssueRef news(String title) {
        return new IssueRef(IssueType.NEWS, title, "http://news/1", T, "한국경제");
    }

    @Test
    void 근거가_없으면_분류불가다() {
        assertThat(IssueNatureClassifier.classify(List.of())).isEqualTo(IssueNature.UNCLASSIFIED);
        assertThat(IssueNatureClassifier.classify(null)).isEqualTo(IssueNature.UNCLASSIFIED);
    }

    @Test
    void 일회성_이벤트는_ONE_OFF다() {
        assertThat(IssueNatureClassifier.classify(List.of(disclosure("단일판매·공급계약체결"))))
            .isEqualTo(IssueNature.ONE_OFF);
        assertThat(IssueNatureClassifier.classify(List.of(news("대규모 수주 소식에 강세"))))
            .isEqualTo(IssueNature.ONE_OFF);
    }

    @Test
    void 구조적_문제_신호는_STRUCTURAL이다() {
        assertThat(IssueNatureClassifier.classify(List.of(disclosure("영업손실 발생(적자전환)"))))
            .isEqualTo(IssueNature.STRUCTURAL);
        assertThat(IssueNatureClassifier.classify(List.of(news("감사의견 거절로 거래정지"))))
            .isEqualTo(IssueNature.STRUCTURAL);
    }

    @Test
    void 구조적_신호가_하나라도_있으면_일회성보다_우선한다() {
        // 공급계약(일회성) + 적자전환(구조적) 섞이면 보수적으로 STRUCTURAL
        var refs = List.of(disclosure("공급계약 체결"), news("적자 지속 우려"));
        assertThat(IssueNatureClassifier.classify(refs)).isEqualTo(IssueNature.STRUCTURAL);
    }

    @Test
    void 키워드가_없는_뉴스만_있으면_분류불가다() {
        assertThat(IssueNatureClassifier.classify(List.of(news("증권가 관심 집중"))))
            .isEqualTo(IssueNature.UNCLASSIFIED);
    }
}
