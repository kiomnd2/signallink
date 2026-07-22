package io.signallink.issue.application.port.out;

import java.util.Optional;

/**
 * 종목코드(6자리) → DART 고유번호(corp_code, 8자리) 변환 포트.
 *
 * <p>DART 재무 API는 stock_code가 아니라 corp_code로 조회하므로 매핑이 필요하다. 완전한 구현은 DART
 * corpCode.xml(전체 상장사 매핑) 다운로드·캐시 → BACKLOG. 그 전까지 {@link Optional#empty()}면
 * 재무 조회를 건너뛰고 체력 진단은 "데이터 없음"이 된다.
 */
public interface CorpCodeResolver {

    Optional<String> corpCode(String stockCode);
}
