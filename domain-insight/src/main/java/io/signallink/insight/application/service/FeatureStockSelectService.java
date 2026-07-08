package io.signallink.insight.application.service;

import io.signallink.insight.domain.FeatureCandidate;
import io.signallink.insight.domain.FeatureStockSelector;
import io.signallink.market.application.port.out.DailyPriceSnapshotQueryPort;
import io.signallink.market.application.port.out.StockDaySnapshot;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 특징주 선정 유스케이스 — 당일 시세 스냅샷을 market 조회 포트로 읽어 트리거·거래대금 상위로 선정한다.
 * 선정 결과({@link FeatureCandidate})는 후속 분석(3-B~3-E)과 카드 생성 파이프라인(3-F)의 입력이 된다.
 */
@Service
@RequiredArgsConstructor
public class FeatureStockSelectService {

    private static final Logger log = LoggerFactory.getLogger(FeatureStockSelectService.class);

    private final DailyPriceSnapshotQueryPort snapshotQuery;

    public List<FeatureCandidate> select(LocalDate tradeDate, int topN) {
        List<StockDaySnapshot> snapshots = snapshotQuery.snapshotsOn(tradeDate);
        List<FeatureCandidate> selected = FeatureStockSelector.select(snapshots, tradeDate, topN);
        log.info("특징주 선정: {} 전체 {}종목 → {}건 선정(상위 {})",
            tradeDate, snapshots.size(), selected.size(), topN);
        return selected;
    }

    public List<FeatureCandidate> select(LocalDate tradeDate) {
        return select(tradeDate, FeatureStockSelector.DEFAULT_TOP_N);
    }
}
