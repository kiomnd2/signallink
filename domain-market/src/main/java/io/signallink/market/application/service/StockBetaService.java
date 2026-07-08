package io.signallink.market.application.service;

import io.signallink.common.TimeUtils;
import io.signallink.market.application.port.out.DailyPriceQueryPort;
import io.signallink.market.application.port.out.DatedClose;
import io.signallink.market.application.port.out.IndexPriceQueryPort;
import io.signallink.market.application.port.out.StockBetaRepositoryPort;
import io.signallink.market.application.port.out.StockRepositoryPort;
import io.signallink.market.domain.BetaCalculator;
import io.signallink.market.domain.Stock;
import io.signallink.market.domain.StockBeta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 베타 계산 유스케이스 (J7 주간). 유니버스 상위 limit종목마다 최근 {@value #LOOKBACK}일 종가 + 해당 시장 지수로
 * 60일 베타 계산 후 저장. 백필 데이터로 과거 재현 검증 가능.
 */
@Service
@RequiredArgsConstructor
public class StockBetaService {

    private static final Logger log = LoggerFactory.getLogger(StockBetaService.class);
    private static final int LOOKBACK = BetaCalculator.MIN_RETURNS * 3 + 1; // 여유 있는 조회 창
    private static final String KOSPI_INDEX = "0001";
    private static final String KOSDAQ_INDEX = "1001";

    private final StockRepositoryPort stockRepository;
    private final DailyPriceQueryPort dailyPriceQuery;
    private final IndexPriceQueryPort indexPriceQuery;
    private final StockBetaRepositoryPort betaRepository;

    /** @return 베타를 계산·저장한 종목 수 */
    public int calculateAll(int limit) {
        LocalDate today = TimeUtils.todayKst();
        Map<String, List<DatedClose>> indexCache = new HashMap<>();
        int done = 0;
        for (Stock stock : stockRepository.findAll().stream().limit(limit).toList()) {
            try {
                String indexCode = "KOSDAQ".equals(stock.getMarketType()) ? KOSDAQ_INDEX : KOSPI_INDEX;
                List<DatedClose> index = indexCache.computeIfAbsent(indexCode,
                    c -> indexPriceQuery.recentCloses(c, LOOKBACK));
                List<DatedClose> stockCloses = dailyPriceQuery.recentCloses(stock.getStockCode(), LOOKBACK);

                BigDecimal beta = BetaCalculator.beta60d(stockCloses, index);
                if (beta != null) {
                    betaRepository.save(new StockBeta(stock.getStockCode(), beta, today));
                    done++;
                }
            } catch (RuntimeException e) {
                log.warn("베타 계산 실패, 종목 건너뜀: {} ({})", stock.getStockCode(), e.toString());
            }
        }
        log.info("베타 계산 완료: {}종목", done);
        return done;
    }
}
