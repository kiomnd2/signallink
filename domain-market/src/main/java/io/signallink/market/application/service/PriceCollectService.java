package io.signallink.market.application.service;

import io.signallink.market.application.port.out.DailyPriceData;
import io.signallink.market.application.port.out.DailyPriceGatewayPort;
import io.signallink.market.application.port.out.DailyPriceRepositoryPort;
import io.signallink.market.application.port.out.IndexPriceData;
import io.signallink.market.application.port.out.IndexPriceGatewayPort;
import io.signallink.market.application.port.out.IndexPriceRepositoryPort;
import io.signallink.market.domain.DailyPrice;
import io.signallink.market.domain.IndexPrice;
import io.signallink.market.domain.VolumeRatioCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 시세(일봉·지수) 수집 유스케이스.
 *
 * <p>기간 시세를 조회해 이미 있는 날짜는 건너뛰고(멱등) 신규만 저장한다.
 * vol_ratio_20d는 20영업일 이력이 필요해 여기선 null로 두고 후속(백필·별도 계산)에서 채운다.
 * is_final=true(기간별시세 TR은 마감 확정 일봉).
 */
@Service
@RequiredArgsConstructor
public class PriceCollectService {

    private static final Logger log = LoggerFactory.getLogger(PriceCollectService.class);

    private final DailyPriceGatewayPort dailyGateway;
    private final IndexPriceGatewayPort indexGateway;
    private final DailyPriceRepositoryPort dailyRepository;
    private final IndexPriceRepositoryPort indexRepository;

    /** 한 종목의 [from,to] 일봉을 수집(기존 날짜 스킵). @return 신규 저장 수 */
    public int collectDaily(String stockCode, LocalDate from, LocalDate to) {
        List<DailyPrice> prices = dailyGateway.fetchDailyPrices(stockCode, from, to).stream()
            .filter(d -> !dailyRepository.existsByStockCodeAndTradeDate(stockCode, d.tradeDate()))
            .map(d -> toEntity(stockCode, d))
            .toList();
        dailyRepository.saveAll(prices);
        log.info("일봉 수집: {} [{}~{}] 신규 {}건", stockCode, from, to, prices.size());
        return prices.size();
    }

    /** 한 지수의 [from,to] 시세를 수집(기존 날짜 스킵). @return 신규 저장 수 */
    public int collectIndex(String indexCode, LocalDate from, LocalDate to) {
        List<IndexPrice> prices = indexGateway.fetchIndexPrices(indexCode, from, to).stream()
            .filter(d -> !indexRepository.existsByIndexCodeAndTradeDate(indexCode, d.tradeDate()))
            .map(d -> new IndexPrice(indexCode, d.tradeDate(), d.close(), d.changeRate()))
            .toList();
        indexRepository.saveAll(prices);
        log.info("지수 수집: {} [{}~{}] 신규 {}건", indexCode, from, to, prices.size());
        return prices.size();
    }

    private static DailyPrice toEntity(String stockCode, DailyPriceData d) {
        return new DailyPrice(stockCode, d.tradeDate(), d.close(), d.changeRate(),
            d.volume(), d.tradingValue(), null, true);
    }

    private static final int VOL_RATIO_BUFFER_DAYS = 40; // 20영업일 prior 확보용(달력일 버퍼)

    /**
     * [from,to] 일봉을 수집하되 각 신규 바에 vol_ratio_20d를 계산해 채운다.
     * 20영업일 prior 확보를 위해 from 이전 버퍼까지 KIS에서 조회하고, 평균은 응답 시계열에서 인메모리로 계산한다.
     */
    public int collectDailyWithVolRatio(String stockCode, LocalDate from, LocalDate to) {
        List<DailyPriceData> asc = dailyGateway
            .fetchDailyPrices(stockCode, from.minusDays(VOL_RATIO_BUFFER_DAYS), to).stream()
            .sorted(Comparator.comparing(DailyPriceData::tradeDate))
            .toList();

        List<DailyPrice> toSave = new ArrayList<>();
        for (int i = 0; i < asc.size(); i++) {
            DailyPriceData d = asc.get(i);
            if (d.tradeDate().isBefore(from)
                || dailyRepository.existsByStockCodeAndTradeDate(stockCode, d.tradeDate())) {
                continue; // 버퍼 구간·기존 날짜 스킵
            }
            List<Long> prior = new ArrayList<>();
            for (int j = i - 1; j >= 0 && prior.size() < VolumeRatioCalculator.WINDOW; j--) {
                prior.add(asc.get(j).volume());
            }
            BigDecimal volRatio = VolumeRatioCalculator.ratio20d(d.volume(), prior);
            toSave.add(new DailyPrice(stockCode, d.tradeDate(), d.close(), d.changeRate(),
                d.volume(), d.tradingValue(), volRatio, true));
        }
        dailyRepository.saveAll(toSave);
        log.info("일봉+vol_ratio 수집: {} [{}~{}] 신규 {}건", stockCode, from, to, toSave.size());
        return toSave.size();
    }
}
