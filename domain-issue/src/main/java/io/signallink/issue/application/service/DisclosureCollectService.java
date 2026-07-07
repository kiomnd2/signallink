package io.signallink.issue.application.service;

import io.signallink.common.TimeUtils;
import io.signallink.issue.application.port.out.DartGatewayPort;
import io.signallink.issue.application.port.out.DisclosureCandidate;
import io.signallink.issue.application.port.out.DisclosureRepositoryPort;
import io.signallink.issue.domain.Disclosure;
import io.signallink.issue.domain.DisclosureCategoryClassifier;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 공시 수집 유스케이스.
 *
 * <p>흐름: DART에서 최근 공시 조회 → (상장사 & 신규)만 남김 → 카테고리 분류 → 저장.
 * 아웃바운드 포트에만 의존하므로 외부 API/DB 없이 스텁으로 단위 테스트할 수 있다.
 */
@Service
public class DisclosureCollectService {

    private static final Logger log = LoggerFactory.getLogger(DisclosureCollectService.class);

    private final DartGatewayPort dartGateway;
    private final DisclosureRepositoryPort repository;
    private final DisclosureCategoryClassifier classifier = new DisclosureCategoryClassifier();

    public DisclosureCollectService(DartGatewayPort dartGateway, DisclosureRepositoryPort repository) {
        this.dartGateway = dartGateway;
        this.repository = repository;
    }

    /**
     * 최근 {@code daysBack}일치 공시를 수집한다. 재실행해도 rcept_no 기준으로 중복 저장하지 않는다(멱등).
     *
     * @return 신규 저장된 공시 수
     */
    public int collectRecent(int daysBack) {
        LocalDate today = TimeUtils.todayKst();
        List<DisclosureCandidate> candidates = dartGateway.fetchDisclosures(today.minusDays(daysBack), today);

        int saved = 0;
        for (DisclosureCandidate c : candidates) {
            if (c.stockCode() == null || c.stockCode().isBlank()) {
                continue; // 비상장/시장 외 공시 → 스킵
            }
            try {
                if (repository.existsByReceiptNo(c.receiptNo())) {
                    continue; // 이미 수집한 공시 → 스킵
                }
                String category = classifier.classify(c.title());
                repository.save(new Disclosure(
                    c.stockCode().trim(), c.receiptNo(), c.title(), category, c.disclosedAt(), c.dartUrl()));
                saved++;
            } catch (DataIntegrityViolationException e) {
                // 오버랩 폴링 레이스 등으로 이미 저장됨(UNIQUE 위반) → 멱등 처리, 무시하고 계속
                log.debug("공시 저장 건너뜀 (제약 위반/중복): rcept_no={}", c.receiptNo());
            } catch (RuntimeException e) {
                // 한 건의 실패가 배치 전체를 죽이지 않도록 격리
                log.warn("공시 저장 실패, 건너뜀: rcept_no={} ({})", c.receiptNo(), e.toString());
            }
        }
        log.info("공시 수집: 후보 {}건 중 신규 {}건 저장", candidates.size(), saved);
        return saved;
    }
}
