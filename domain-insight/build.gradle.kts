dependencies {
    implementation(project(":common"))
    implementation(project(":domain-market"))  // insight -> market (읽기)
    implementation(project(":domain-issue"))   // insight -> issue (읽기)
    implementation(project(":domain-macro"))   // insight -> macro (읽기)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.postgresql:postgresql")
}
