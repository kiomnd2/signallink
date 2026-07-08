package io.signallink.market.application.service;

import io.signallink.common.TimeUtils;
import io.signallink.market.application.port.out.StockMasterEntry;
import io.signallink.market.application.port.out.StockMasterGatewayPort;
import io.signallink.market.application.port.out.StockRepositoryPort;
import io.signallink.market.domain.Stock;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 종목마스터 동기화 유스케이스 (J1).
 * KIS .mst에서 전체 주식을 받아 market.stock에 upsert(멱등: stock_code PK). 다른 market 수집의 대상 목록이 된다.
 */
@Service
@RequiredArgsConstructor
public class StockMasterSyncService {

    private static final Logger log = LoggerFactory.getLogger(StockMasterSyncService.class);

    private final StockMasterGatewayPort masterGateway;
    private final StockRepositoryPort stockRepository;

    /** @return upsert된 종목 수 */
    public int sync() {
        List<StockMasterEntry> entries = masterGateway.fetchAll();
        OffsetDateTime now = TimeUtils.nowKst();
        List<Stock> stocks = entries.stream()
            .map(e -> new Stock(e.code(), e.name(), e.marketType(), e.sectorCode(), now))
            .toList();
        stockRepository.saveAll(stocks);
        log.info("종목마스터 동기화 완료: {}건 저장", stocks.size());
        return stocks.size();
    }
}
