dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework:spring-web")                    // RestClient — KIS 어댑터
    implementation("org.springframework.boot:spring-boot-starter-json") // Jackson — KIS JSON 파싱

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.postgresql:postgresql")
}
