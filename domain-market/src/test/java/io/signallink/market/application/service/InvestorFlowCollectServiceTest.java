package io.signallink.market.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.signallink.market.application.port.out.InvestorFlowData;
import io.signallink.market.application.port.out.InvestorFlowGatewayPort;
import io.signallink.market.application.port.out.InvestorFlowRepositoryPort;
import io.signallink.market.domain.InvestorFlow;
import io.signallink.market.domain.InvestorType;
import java.time.LocalDate;
import java.util.ArrayList;
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

        int n = new InvestorFlowCollectService(gateway, repo).collectConfirmed("005930");

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

        int n = new InvestorFlowCollectService(gateway, repo).collectProvisional();

        assertThat(n).isEqualTo(2);
        assertThat(repo.upserted).noneMatch(InvestorFlow::isFinal);
    }

    record StubGateway(List<InvestorFlowData> confirmed, List<InvestorFlowData> provisional)
            implements InvestorFlowGatewayPort {
        @Override public List<InvestorFlowData> fetchConfirmed(String stockCode) { return confirmed; }
        @Override public List<InvestorFlowData> fetchProvisional() { return provisional; }
    }

    static class CapturingRepo implements InvestorFlowRepositoryPort {
        final List<InvestorFlow> upserted = new ArrayList<>();
        @Override public void upsert(InvestorFlow flow) { upserted.add(flow); }
    }
}
