package io.signallink.issue.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.common.TimeUtils;
import io.signallink.issue.application.port.out.DartGatewayPort;
import io.signallink.issue.application.port.out.DisclosureCandidate;
import io.signallink.issue.application.port.out.DisclosureRepositoryPort;
import io.signallink.issue.domain.Disclosure;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** 포트 스텁만으로 수집 유스케이스 로직 검증 (외부 API/DB 없음). */
class DisclosureCollectServiceTest {

    private final OffsetDateTime now = TimeUtils.nowKst();

    @Test
    void 상장사_신규_공시만_저장하고_카테고리를_분류한다() {
        List<DisclosureCandidate> given = List.of(
            candidate("20240102000001", "005930", "단일판매ㆍ공급계약체결"), // 상장·신규 → 수주계약
            candidate("20240102000002", "", "비상장사 공시"),               // 비상장 → 스킵
            candidate("20240102000003", "000660", "유상증자결정")           // 상장·신규 → 자금조달
        );
        DartGatewayPort gateway = (from, to) -> given;
        InMemoryRepo repo = new InMemoryRepo();

        int saved = new DisclosureCollectService(gateway, repo).collectRecent(1);

        assertThat(saved).isEqualTo(2);
        assertThat(repo.stored).extracting(Disclosure::getCategory)
            .containsExactly("수주계약", "자금조달");
    }

    @Test
    void 이미_수집한_공시는_중복_저장하지_않는다() {
        DisclosureCandidate dup = candidate("20240102000009", "005930", "공급계약체결");
        DartGatewayPort gateway = (from, to) -> List.of(dup);
        InMemoryRepo repo = new InMemoryRepo();
        repo.seen.add("20240102000009"); // 이미 존재

        int saved = new DisclosureCollectService(gateway, repo).collectRecent(1);

        assertThat(saved).isZero();
        assertThat(repo.stored).isEmpty();
    }

    @Test
    void 한_건_저장이_실패해도_나머지는_저장된다() {
        List<DisclosureCandidate> given = List.of(
            candidate("20240102000001", "005930", "공급계약체결"),  // 정상 저장
            candidate("20240102000002", "000660", "유상증자결정"),  // 저장 시 예외 → 건너뜀
            candidate("20240102000003", "035420", "소송 제기")       // 정상 저장
        );
        DartGatewayPort gateway = (from, to) -> given;
        // 2번째 rcept_no 저장 시 UNIQUE 제약 위반을 시뮬레이션 (오버랩 폴링 레이스 등)
        InMemoryRepo repo = new InMemoryRepo() {
            @Override
            public Disclosure save(Disclosure disclosure) {
                if ("20240102000002".equals(disclosure.getReceiptNo())) {
                    throw new DataIntegrityViolationException("duplicate rcept_no");
                }
                return super.save(disclosure);
            }
        };

        int saved = new DisclosureCollectService(gateway, repo).collectRecent(1);

        assertThat(saved).isEqualTo(2);
        assertThat(repo.stored).extracting(Disclosure::getReceiptNo)
            .containsExactly("20240102000001", "20240102000003");
    }

    private DisclosureCandidate candidate(String receiptNo, String stockCode, String title) {
        return new DisclosureCandidate(receiptNo, stockCode, title, now,
            "https://dart.fss.or.kr/dsaf001/main.do?rcpNo=" + receiptNo);
    }

    /** 포트 스텁: 인메모리 저장 + 존재 여부. */
    static class InMemoryRepo implements DisclosureRepositoryPort {
        final List<Disclosure> stored = new ArrayList<>();
        final Set<String> seen = new HashSet<>();

        @Override
        public boolean existsByReceiptNo(String receiptNo) {
            return seen.contains(receiptNo);
        }

        @Override
        public Disclosure save(Disclosure disclosure) {
            stored.add(disclosure);
            seen.add(disclosure.getReceiptNo());
            return disclosure;
        }
    }
}
