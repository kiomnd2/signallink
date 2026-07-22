dependencies {
    implementation(project(":common"))
    implementation(project(":domain-market"))  // insight -> market (읽기)
    implementation(project(":domain-issue"))   // insight -> issue (읽기)
    implementation(project(":domain-macro"))   // insight -> macro (읽기)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework:spring-web")                    // RestClient — Gemini 어댑터용
    implementation("org.springframework.boot:spring-boot-starter-json") // Jackson — Gemini 응답 파싱

    // Gemini 어댑터 HTTP 경로 테스트: 목 서버로 경로·모델·키·바디 조립 검증 (실키 불필요)
    // 버전 명시 — spring-boot 3.4.1 BOM은 okhttp3를 관리하지 않음. 4.x = classic MockResponse API.
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.postgresql:postgresql")
}
