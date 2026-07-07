dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework:spring-web")                    // RestClient — 외부 API 어댑터용 (common의 spring-web는 전이 안 됨)
    implementation("org.springframework.boot:spring-boot-starter-json") // Jackson — DART/뉴스 JSON 파싱

    // 영속 계층 테스트: 실제 Postgres 컨테이너 (스키마·제약 동일 검증)
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.postgresql:postgresql")
}
