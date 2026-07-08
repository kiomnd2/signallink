package io.signallink.issue.application.port.out;

import java.util.List;

/**
 * 아웃바운드 포트 — 뉴스 조회 대상 종목 목록.
 * 현재 구현: 시드 CSV(SeedWatchlistAdapter). 추후 특징주(J2)/market.stock 어댑터로 교체 (플랜 §8).
 */
public interface WatchlistPort {
    List<WatchStock> watchlist();
}
