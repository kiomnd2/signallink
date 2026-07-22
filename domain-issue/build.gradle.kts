dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework:spring-web")                    // RestClient — 외부 API 어댑터용 (common의 spring-web는 전이 안 됨)
    implementation("org.springframework.boot:spring-boot-starter-json") // Jackson — DART/뉴스 JSON 파싱

    // 외부 API 어댑터 HTTP 경로 테스트: 목 HTTP 서버로 URL·쿼리·헤더 조립 검증 (실키 불필요)
    // 버전 명시 — spring-boot 3.4.1 BOM은 okhttp3를 관리하지 않음. 4.x = classic MockResponse API.
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

    // 영속 계층 테스트: 실제 Postgres 컨테이너 (스키마·제약 동일 검증)
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.postgresql:postgresql")
}
