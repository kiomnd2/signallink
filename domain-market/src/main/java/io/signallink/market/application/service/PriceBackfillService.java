package io.signallink.market.application.service;

import io.signallink.market.application.port.out.DailyPriceRepositoryPort;
import io.signallink.market.application.port.out.StockRepositoryPort;
import io.signallink.market.domain.Stock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 2년 일봉 백필 유스케이스.
 *
 * <p>대상 상위 {@code limit}종목마다 [from,to]를 {@value #WINDOW_DAYS}일 창으로 나눠 수집한다
 * (정책③ — KIS 기간별시세 1콜 최대 100건이라 100일 창이면 트레이딩일 &lt;100로 안전).
 * 이미 데이터가 있는 종목은 건너뛰어(재개) 초당 1건 한도에서 반복 실행이 안전하다.
 */
@Service
@RequiredArgsConstructor
public class PriceBackfillService {

    private static final Logger log = LoggerFactory.getLogger(PriceBackfillService.class);
    static final int WINDOW_DAYS = 100;

    private final StockRepositoryPort stockRepository;
    private final DailyPriceRepositoryPort dailyRepository;
    private final PriceCollectService priceCollectService;

    /** @return 신규 적재된 일봉 총 건수 */
    public int backfill(int limit, LocalDate from, LocalDate to) {
        List<Stock> universe = stockRepository.findAll().stream().limit(limit).toList();
        int total = 0;
        int index = 0;
        for (Stock stock : universe) {
            index++;
            String code = stock.getStockCode();
            if (dailyRepository.existsByStockCode(code)) {
                continue; // 이미 백필됨 → 재개 스킵
            }
            try {
                int saved = backfillOne(code, from, to);
                total += saved;
                log.info("백필 {}/{}: {} {}건", index, universe.size(), code, saved);
            } catch (RuntimeException e) {
                log.warn("백필 실패, 종목 건너뜀: {} ({})", code, e.toString());
            }
        }
        log.info("백필 완료: 대상 {}종목, 총 {}건 적재", universe.size(), total);
        return total;
    }

    private int backfillOne(String stockCode, LocalDate from, LocalDate to) {
        int saved = 0;
        LocalDate start = from;
        while (!start.isAfter(to)) {
            LocalDate end = start.plusDays(WINDOW_DAYS - 1);
            if (end.isAfter(to)) {
                end = to;
            }
            saved += priceCollectService.collectDaily(stockCode, start, end);
            start = end.plusDays(1);
        }
        return saved;
    }
}
