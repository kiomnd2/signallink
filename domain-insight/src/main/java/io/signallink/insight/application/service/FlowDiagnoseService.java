package io.signallink.insight.application.service;

import io.signallink.insight.domain.FeatureCandidate;
import io.signallink.insight.domain.FlowDiagnosis;
import io.signallink.insight.domain.FlowDiagnoser;
import io.signallink.market.application.port.out.InvestorFlowQueryPort;
import io.signallink.market.application.port.out.InvestorFlowRow;
import io.signallink.market.domain.InvestorType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 수급 진단 유스케이스 — 특징주 한 건에 대해 market의 수급 조회 포트로 당일 외국인·기관 순매수를 읽어
 * {@link FlowDiagnosis}(flow_summary + 잠정 여부)를 만든다. 확정치가 없으면 장중 가집계(잠정)로 판정.
 */
@Service
@RequiredArgsConstructor
public class FlowDiagnoseService {

    private final InvestorFlowQueryPort flowQuery;

    public FlowDiagnosis diagnose(FeatureCandidate c) {
        List<InvestorFlowRow> rows = flowQuery.flowsOn(c.stockCode(), c.tradeDate());

        Long foreign = null;
        Long org = null;
        boolean anyProvisional = false;
        for (InvestorFlowRow r : rows) {
            if (r.investorType() == InvestorType.FOREIGN) {
                foreign = r.netBuyAmt();
            } else if (r.investorType() == InvestorType.ORG) {
                org = r.netBuyAmt();
            }
            if (!r.isFinal()) {
                anyProvisional = true;
            }
        }
        boolean provisional = !rows.isEmpty() && anyProvisional;

        return FlowDiagnoser.diagnose(foreign, org, provisional);
    }
}
