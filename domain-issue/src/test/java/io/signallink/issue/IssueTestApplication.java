package io.signallink.issue;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 테스트 전용 부트 설정 — 라이브러리 모듈(domain-issue)에는 @SpringBootApplication이 없어
 * 슬라이스 테스트(@DataJpaTest)가 구성 클래스를 찾도록 앵커 역할만 한다.
 */
@SpringBootApplication
class IssueTestApplication {
}
