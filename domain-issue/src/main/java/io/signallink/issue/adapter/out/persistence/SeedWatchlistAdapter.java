package io.signallink.issue.adapter.out.persistence;

import io.signallink.issue.application.port.out.WatchStock;
import io.signallink.issue.application.port.out.WatchlistPort;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 아웃바운드 어댑터 — 시드 CSV(seed/watchlist.csv, {@code code,name})로 워치리스트를 제공.
 * 특징주(J2, M3) 완성 후 market.stock/특징주 기반 어댑터로 교체 예정 (플랜 §8).
 */
@Component
public class SeedWatchlistAdapter implements WatchlistPort {

    private static final String SEED = "seed/watchlist.csv";

    @Override
    public List<WatchStock> watchlist() {
        List<WatchStock> stocks = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource(SEED).getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("code,")) {
                    continue; // 빈 줄·헤더 스킵
                }
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    stocks.add(new WatchStock(parts[0].strip(), parts[1].strip()));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return stocks;
    }
}
