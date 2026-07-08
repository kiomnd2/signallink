package io.signallink.issue.application.port.out;

import java.util.List;

/**
 * 아웃바운드 포트 — 네이버 뉴스 검색. 구현: adapter.out.external.naver.NaverNewsClient.
 * @param stockCode 후보에 태깅할 종목코드
 * @param query     검색어(보통 종목명)
 */
public interface NaverNewsGatewayPort {
    List<NewsCandidate> search(String stockCode, String query);
}
