package io.signallink.market.adapter.out.external.kis;

import io.signallink.market.application.port.out.StockMasterEntry;
import io.signallink.market.application.port.out.StockMasterGatewayPort;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 아웃바운드 어댑터 — KIS 종목정보 파일(.mst.zip)을 내려받아 파싱한다(StockMasterGatewayPort 구현).
 * 공개 다운로드(인증 불필요)이며 하루 1회 갱신되므로 토큰·rate limit 대상이 아니다.
 */
@Component
public class StockMasterFileClient implements StockMasterGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(StockMasterFileClient.class);

    private final RestClient client;

    public StockMasterFileClient(
            @Value("${signallink.kis.master-base-url:https://new.real.download.dws.co.kr/common/master}")
            String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public List<StockMasterEntry> fetchAll() {
        List<StockMasterEntry> all = new ArrayList<>();
        all.addAll(load("kospi_code.mst.zip", "KOSPI"));
        all.addAll(load("kosdaq_code.mst.zip", "KOSDAQ"));
        log.info("종목마스터 파일 파싱 완료: 총 {}건(주식)", all.size());
        return all;
    }

    private List<StockMasterEntry> load(String zipName, String marketType) {
        byte[] zip = client.get().uri("/{name}", zipName).retrieve().body(byte[].class);
        if (zip == null) {
            throw new IllegalStateException("종목마스터 다운로드 실패: " + zipName);
        }
        return StockMasterParser.parse(unzipMst(zip), marketType);
    }

    private static byte[] unzipMst(byte[] zipBytes) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            for (ZipEntry entry; (entry = zis.getNextEntry()) != null; ) {
                if (entry.getName().endsWith(".mst")) {
                    return zis.readAllBytes();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        throw new IllegalStateException("zip에서 .mst 항목을 찾지 못함");
    }
}
