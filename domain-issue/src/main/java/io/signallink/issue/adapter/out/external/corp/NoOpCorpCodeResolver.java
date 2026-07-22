package io.signallink.issue.adapter.out.external.corp;

import io.signallink.issue.application.port.out.CorpCodeResolver;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * corp_code 리졸버 임시 구현 — 항상 {@link Optional#empty()}.
 *
 * <p>실제 매핑(DART corpCode.xml 전체 상장사 다운로드·캐시)은 BACKLOG. 그 전까지 재무 조회는 건너뛰고
 * 체력 진단 실적 추세는 "데이터 없음"으로 비표시된다(우아한 degradation). 실 구현이 생기면 이 빈을 교체.
 */
@Component
public class NoOpCorpCodeResolver implements CorpCodeResolver {

    @Override
    public Optional<String> corpCode(String stockCode) {
        return Optional.empty();
    }
}
