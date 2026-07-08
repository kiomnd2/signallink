package io.signallink.market.adapter.out.external.kis;

import io.signallink.market.application.port.out.StockMasterEntry;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * KIS 종목정보 파일(.mst) 파서.
 *
 * <p>포맷(M0 방식 실검증): cp949(MS949) 인코딩, 288바이트 고정 레코드.
 * <pre>
 *   단축코드 = row[0..9)      (예: 005930)
 *   표준코드 = row[9..21)     (ISIN)
 *   종목명   = row[21..len-228)
 *   tail     = row[len-228..) (228자 고정 ASCII 필드; tail[1..3) = 증권그룹구분코드)
 * </pre>
 * 주식만 남긴다: 증권그룹구분 {@code "ST"} + 6자리 종목코드 (ETF=EF·ETN=EN·리츠=RT 등 제외).
 * 업종(sectorCode)은 tail 오프셋이 미확정이라 현재 null (BACKLOG 후속).
 */
public final class StockMasterParser {

    private static final Charset CP949 = Charset.forName("MS949");
    private static final int TAIL_LEN = 228;
    private static final int NAME_START = 21;
    private static final String STOCK_GROUP = "ST";
    private static final Pattern CODE_6 = Pattern.compile("\\d{6}");

    private StockMasterParser() {}

    public static List<StockMasterEntry> parse(byte[] content, String marketType) {
        String text = new String(content, CP949);
        List<StockMasterEntry> stocks = new ArrayList<>();
        for (String row : text.split("\\r?\\n")) {
            if (row.length() <= NAME_START + TAIL_LEN) {
                continue; // 빈 줄·비정상 레코드
            }
            String shortCode = row.substring(0, 9).strip();
            String group = row.substring(row.length() - TAIL_LEN + 1, row.length() - TAIL_LEN + 3);
            if (!STOCK_GROUP.equals(group) || !CODE_6.matcher(shortCode).matches()) {
                continue; // 주식 + 6자리만
            }
            String name = row.substring(NAME_START, row.length() - TAIL_LEN).strip();
            stocks.add(new StockMasterEntry(shortCode, name, marketType, null));
        }
        return stocks;
    }
}
