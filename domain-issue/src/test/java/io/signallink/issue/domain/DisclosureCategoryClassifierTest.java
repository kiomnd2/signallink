package io.signallink.issue.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DisclosureCategoryClassifierTest {

    private final DisclosureCategoryClassifier classifier = new DisclosureCategoryClassifier();

    @Test
    void 공급계약_제목은_수주계약() {
        assertThat(classifier.classify("단일판매ㆍ공급계약체결")).isEqualTo("수주계약");
    }

    @Test
    void 잠정실적_제목은_실적() {
        assertThat(classifier.classify("연결재무제표기준영업(잠정)실적(공정공시)")).isEqualTo("실적");
    }

    @Test
    void 유상증자_제목은_자금조달() {
        assertThat(classifier.classify("유상증자결정")).isEqualTo("자금조달");
    }

    @Test
    void 매칭되는_키워드가_없으면_기타() {
        assertThat(classifier.classify("기업설명회(IR)개최")).isEqualTo("기타");
    }

    @Test
    void 제목이_null이면_기타() {
        assertThat(classifier.classify(null)).isEqualTo("기타");
    }
}
