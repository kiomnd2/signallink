package io.signallink.issue.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.signallink.issue.domain.Disclosure;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 실제 Postgres(Testcontainers)에서 issue.disclosure 스키마·제약을 검증한다.
 * Docker 미가동 환경에서는 자동 skip. 스키마는 db/issue-schema.sql로 초기화(운영 V1__init.sql 부분과 동일 유지).
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class DisclosureJpaRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("db/issue-schema.sql");

    @Autowired
    private DisclosureJpaRepository repository;

    @Test
    void 저장하면_receiptNo로_존재를_확인할_수_있다() {
        repository.saveAndFlush(sample("20240102000123"));

        assertThat(repository.existsByReceiptNo("20240102000123")).isTrue();
        assertThat(repository.existsByReceiptNo("00000000000000")).isFalse();
    }

    @Test
    void 같은_receiptNo를_두_번_저장하면_유니크_제약에_걸린다() {
        repository.saveAndFlush(sample("20240102000999"));

        assertThatThrownBy(() -> repository.saveAndFlush(sample("20240102000999")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Disclosure sample(String receiptNo) {
        return new Disclosure("005930", receiptNo, "단일판매ㆍ공급계약체결", "수주계약",
            OffsetDateTime.parse("2024-01-02T00:00+09:00"), "https://dart.fss.or.kr/x");
    }
}
