package io.signallink.market.application.service;

import io.signallink.market.application.port.out.InvestorFlowData;
import io.signallink.market.application.port.out.InvestorFlowGatewayPort;
import io.signallink.market.application.port.out.InvestorFlowRepositoryPort;
import io.signallink.market.domain.InvestorFlow;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 수급 수집 유스케이스.
 *
 * <p>장중 잠정(가집계, is_final=false) / 마감 확정(inquire-investor, is_final=true) 이원화.
 * 같은 (stock,date,type)은 upsert로 덮어쓰므로, 마감 확정이 장중 잠정을 대체한다(플랜 §5).
 */
@Service
@RequiredArgsConstructor
public class InvestorFlowCollectService {

    private static final Logger log = LoggerFactory.getLogger(InvestorFlowCollectService.class);

    private final InvestorFlowGatewayPort gateway;
    private final InvestorFlowRepositoryPort repository;

    /** 마감 확정 수급(종목별). @return 반영 건수 */
    public int collectConfirmed(String stockCode) {
        return upsertAll(gateway.fetchConfirmed(stockCode), true);
    }

    /** 장중 잠정 수급(시장 상위 가집계). @return 반영 건수 */
    public int collectProvisional() {
        int n = upsertAll(gateway.fetchProvisional(), false);
        log.info("잠정 수급 수집: {}건", n);
        return n;
    }

    private int upsertAll(List<InvestorFlowData> flows, boolean isFinal) {
        int n = 0;
        for (InvestorFlowData d : flows) {
            try {
                repository.upsert(new InvestorFlow(
                    d.stockCode(), d.tradeDate(), d.investorType(), d.netBuyQty(), d.netBuyAmt(), isFinal));
                n++;
            } catch (RuntimeException e) {
                log.warn("수급 저장 실패, 건너뜀: {} {} ({})", d.stockCode(), d.investorType(), e.toString());
            }
        }
        return n;
    }
}
