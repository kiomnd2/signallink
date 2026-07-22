package io.signallink.issue.application.port.out;

import java.util.Optional;

/**
 * DART 재무제표(단일회사 주요계정) 게이트웨이 포트. 구현: adapter.out.external.dart.DartFinancialClient.
 *
 * <p>한 보고서(사업연도 + 보고서코드)의 매출액·영업이익을 가져온다. {@code reprtCode}: 11013(1분기)·
 * 11012(반기)·11014(3분기)·11011(사업보고서). 데이터 없음(013)은 {@link Optional#empty()},
 * 그 외 오류는 예외로 전파(조용한 중단 방지).
 */
public interface DartFinancialGatewayPort {

    Optional<QuarterAccount> fetchQuarter(String corpCode, int year, String reprtCode);
}
