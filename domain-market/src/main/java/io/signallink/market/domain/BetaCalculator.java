package io.signallink.market.domain;

import io.signallink.market.application.port.out.DatedClose;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 베타 = Cov(종목 일수익률, 지수 일수익률) / Var(지수 일수익률).
 *
 * <p>종목·지수 종가를 공통 거래일로 정렬해 일수익률을 구한 뒤 계산한다. 공통 거래일이
 * {@value #MIN_RETURNS}개 수익률 미만이거나 지수 분산이 0이면 계산 불가(null).
 */
public final class BetaCalculator {

    public static final int MIN_RETURNS = 20;

    private BetaCalculator() {}

    /** @return beta_60d (소수 3자리), 데이터 부족 시 null */
    public static BigDecimal beta60d(List<DatedClose> stockCloses, List<DatedClose> indexCloses) {
        Map<java.time.LocalDate, BigDecimal> indexByDate = new HashMap<>();
        for (DatedClose i : indexCloses) {
            indexByDate.put(i.date(), i.close());
        }
        List<DatedClose> aligned = stockCloses.stream()
            .filter(s -> indexByDate.containsKey(s.date()))
            .sorted(Comparator.comparing(DatedClose::date))
            .toList();
        if (aligned.size() < MIN_RETURNS + 1) {
            return null;
        }

        int n = aligned.size() - 1;
        double[] stockReturns = new double[n];
        double[] indexReturns = new double[n];
        for (int i = 1; i < aligned.size(); i++) {
            stockReturns[i - 1] = returnRate(aligned.get(i - 1).close(), aligned.get(i).close());
            indexReturns[i - 1] = returnRate(indexByDate.get(aligned.get(i - 1).date()),
                indexByDate.get(aligned.get(i).date()));
        }

        double varIndex = variance(indexReturns);
        if (varIndex == 0.0) {
            return null;
        }
        double beta = covariance(stockReturns, indexReturns) / varIndex;
        return BigDecimal.valueOf(beta).setScale(3, RoundingMode.HALF_UP);
    }

    private static double returnRate(BigDecimal prev, BigDecimal cur) {
        double p = prev.doubleValue();
        return p == 0.0 ? 0.0 : (cur.doubleValue() - p) / p;
    }

    private static double mean(double[] a) {
        double sum = 0;
        for (double v : a) {
            sum += v;
        }
        return sum / a.length;
    }

    private static double variance(double[] a) {
        double m = mean(a);
        double sum = 0;
        for (double v : a) {
            sum += (v - m) * (v - m);
        }
        return sum / a.length;
    }

    private static double covariance(double[] a, double[] b) {
        double ma = mean(a);
        double mb = mean(b);
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += (a[i] - ma) * (b[i] - mb);
        }
        return sum / a.length;
    }
}
