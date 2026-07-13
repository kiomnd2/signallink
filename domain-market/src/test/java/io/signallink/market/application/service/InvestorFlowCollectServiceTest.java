package io.signallink.market.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.market.application.port.out.InvestorFlowData;
import io.signallink.market.application.port.out.InvestorFlowGatewayPort;
import io.signallink.market.application.port.out.InvestorFlowRepositoryPort;
import io.signallink.market.application.port.out.StockRepositoryPort;
import io.signallink.market.domain.InvestorFlow;
import io.signallink.market.domain.InvestorType;
import io.signallink.market.domain.Stock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 수급 수집 — 확정=is_final true / 잠정=false, upsert 호출을 포트 스텁으로 검증. */
class InvestorFlowCollectServiceTest {

    private static final LocalDate D = LocalDate.of(2026, 7, 8);

    @Test
    void 확정_수급은_is_final_true로_저장한다() {
        InvestorFlowGatewayPort gateway = new StubGateway(
            List.of(new InvestorFlowData("005930", D, InvestorType.PERSON, 100, 1),
                    new InvestorFlowData("005930", D, InvestorType.FOREIGN, -200, -2),
                    new InvestorFlowData("005930", D, InvestorType.ORG, 300, 3)),
            List.of());
        CapturingRepo repo = new CapturingRepo();

        int n = new InvestorFlowCollectService(gateway, repo, new StubStockRepo()).collectConfirmed("005930");

        assertThat(n).isEqualTo(3);
        assertThat(repo.upserted).allMatch(InvestorFlow::isFinal);
    }

    @Test
    void 잠정_수급은_is_final_false로_저장한다() {
        InvestorFlowGatewayPort gateway = new StubGateway(
            List.of(),
            List.of(new InvestorFlowData("034220", D, InvestorType.FOREIGN, 1303000, 14600),
                    new InvestorFlowData("034220", D, InvestorType.ORG, 322000, 3600)));
        CapturingRepo repo = new CapturingRepo();

        int n = new InvestorFlowCollectService(gateway, repo, new StubStockRepo()).collectProvisional();

        assertThat(n).isEqualTo(2);
        assertThat(repo.upserted).noneMatch(InvestorFlow::isFinal);
    }

    @Test
    void 확정_전체수집은_유니버스를_limit까지_종목별로_조회한다() {
        // 종목당 확정 1건씩 반환하는 게이트웨이
        InvestorFlowGatewayPort gateway = new InvestorFlowGatewayPort() {
            @Override public List<InvestorFlowData> fetchConfirmed(String stockCode) {
                return List.of(new InvestorFlowData(stockCode, D, InvestorType.FOREIGN, 10, 1));
            }
            @Override public List<InvestorFlowData> fetchProvisional() { return List.of(); }
        };
        CapturingRepo repo = new CapturingRepo();
        StubStockRepo stocks = new StubStockRepo(
            stock("005930"), stock("000660"), stock("035720"));

        // limit=2 → 상위 2종목만
        int n = new InvestorFlowCollectService(gateway, repo, stocks).collectConfirmedAll(2);

        assertThat(n).isEqualTo(2);
        assertThat(repo.upserted).hasSize(2).allMatch(InvestorFlow::isFinal);
    }

    private static Stock stock(String code) {
        return new Stock(code, code, "KOSPI", null, null);
    }

    record StubGateway(List<InvestorFlowData> confirmed, List<InvestorFlowData> provisional)
            implements InvestorFlowGatewayPort {
        @Override public List<InvestorFlowData> fetchConfirmed(String stockCode) { return confirmed; }
        @Override public List<InvestorFlowData> fetchProvisional() { return provisional; }
    }

    static class StubStockRepo implements StockRepositoryPort {
        private final List<Stock> stocks;
        StubStockRepo(Stock... s) { this.stocks = List.of(s); }
        @Override public Stock save(Stock stock) { return stock; }
        @Override public void saveAll(Collection<Stock> stocks) { }
        @Override public long count() { return stocks.size(); }
        @Override public List<Stock> findAll() { return stocks; }
    }

    static class CapturingRepo implements InvestorFlowRepositoryPort {
        final List<InvestorFlow> upserted = new ArrayList<>();
        @Override public void upsert(InvestorFlow flow) { upserted.add(flow); }
    }
}
