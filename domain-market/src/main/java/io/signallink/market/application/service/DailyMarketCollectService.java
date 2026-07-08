package io.signallink.market.application.service;

import io.signallink.common.TimeUtils;
import io.signallink.market.application.port.out.StockRepositoryPort;
import io.signallink.market.domain.Stock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 일일 시세 수집 유스케이스 — 유니버스 상위 {@code limit}종목의 최근 {@code recentDays}일 일봉 + 지수를 수집.
 *
 * <p>최근 며칠을 겹쳐 조회해 누락일을 메운다(기존 날짜는 {@link PriceCollectService}가 스킵 = 멱등).
 * 종목/지수 단위 예외 격리 — 하나가 실패해도 배치 전체는 계속.
 */
@Service
@RequiredArgsConstructor
public class DailyMarketCollectService {

    private static final Logger log = LoggerFactory.getLogger(DailyMarketCollectService.class);

    private final StockRepositoryPort stockRepository;
    private final PriceCollectService priceCollectService;

    /** @return 신규 적재 총 건수(일봉 + 지수) */
    public int collectAll(int limit, int recentDays, List<String> indexCodes) {
        LocalDate to = TimeUtils.todayKst();
        LocalDate from = to.minusDays(recentDays);
        List<Stock> universe = stockRepository.findAll().stream().limit(limit).toList();

        int total = 0;
        for (Stock stock : universe) {
            try {
                total += priceCollectService.collectDailyWithVolRatio(stock.getStockCode(), from, to);
            } catch (RuntimeException e) {
                log.warn("일봉 수집 실패, 종목 건너뜀: {} ({})", stock.getStockCode(), e.toString());
            }
        }
        for (String indexCode : indexCodes) {
            try {
                total += priceCollectService.collectIndex(indexCode, from, to);
            } catch (RuntimeException e) {
                log.warn("지수 수집 실패, 건너뜀: {} ({})", indexCode, e.toString());
            }
        }
        log.info("일일 시세 수집: 종목 {} + 지수 {} → 신규 {}건", universe.size(), indexCodes.size(), total);
        return total;
    }
}
