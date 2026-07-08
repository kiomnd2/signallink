package io.signallink.insight.application.service;

import io.signallink.insight.domain.FeatureCandidate;
import io.signallink.insight.domain.MarketAnalysis;
import io.signallink.insight.domain.MarketDecomposer;
import io.signallink.insight.domain.MarketDecomposition;
import io.signallink.insight.domain.SectorSyncEvaluator;
import io.signallink.insight.domain.WhatHappenedComposer;
import io.signallink.market.application.port.out.IndexChangeQueryPort;
import io.signallink.market.application.port.out.SectorMoveQueryPort;
import io.signallink.market.application.port.out.StockBetaQueryPort;
import io.signallink.market.application.port.out.StockInfo;
import io.signallink.market.application.port.out.StockInfoQueryPort;
import io.signallink.market.domain.MarketIndex;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 시장분해 유스케이스 — 선정된 특징주 한 건에 대해 β·지수·업종을 market 조회 포트로 읽어
 * "시장 탓 vs 개별 이슈"를 분해하고, 업종 동반 여부와 what_happened 문장을 만든다.
 * 결과({@link MarketAnalysis})는 feature_card의 market_contrib·sector_sync·what_happened로 이어진다.
 */
@Service
@RequiredArgsConstructor
public class MarketDecomposeService {

    private final StockInfoQueryPort stockInfoQuery;
    private final StockBetaQueryPort betaQuery;
    private final IndexChangeQueryPort indexChangeQuery;
    private final SectorMoveQueryPort sectorMoveQuery;

    public MarketAnalysis analyze(FeatureCandidate c) {
        StockInfo info = stockInfoQuery.find(c.stockCode());
        String marketType = info != null ? info.marketType() : null;
        String sectorCode = info != null ? info.sectorCode() : null;

        BigDecimal beta = betaQuery.beta60d(c.stockCode());
        MarketIndex index = MarketIndex.forMarketType(marketType);
        String marketLabel = index != null ? index.label() : "시장";
        BigDecimal indexChange = index != null
            ? indexChangeQuery.changeRateOn(index.code(), c.tradeDate())
            : null;

        MarketDecomposition d = MarketDecomposer.decompose(c.changeRate(), beta, indexChange);

        List<BigDecimal> peers = sectorCode == null
            ? List.of()
            : sectorMoveQuery.sectorChangeRates(sectorCode, c.tradeDate(), c.stockCode());
        boolean sectorSync = SectorSyncEvaluator.isSync(c.changeRate(), peers);

        String what = WhatHappenedComposer.compose(marketLabel, d, c.changeRate(), indexChange, sectorSync);
        return new MarketAnalysis(d.marketContrib(), d.driver(), sectorSync, what);
    }
}
