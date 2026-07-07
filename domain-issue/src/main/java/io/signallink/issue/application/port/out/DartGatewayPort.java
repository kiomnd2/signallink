package io.signallink.issue.application.port.out;

import java.time.LocalDate;
import java.util.List;

/**
 * 아웃바운드 포트 — 외부 공시 소스(DART)에서 기간 내 공시를 가져온다.
 * 구현: adapter.out.external.dart.DartClient. 테스트에서는 스텁 람다로 대체.
 */
public interface DartGatewayPort {
    List<DisclosureCandidate> fetchDisclosures(LocalDate from, LocalDate to);
}
