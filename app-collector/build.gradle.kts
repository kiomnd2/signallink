plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":domain-market"))
    implementation(project(":domain-issue"))
    implementation(project(":domain-macro"))
    implementation(project(":domain-insight"))
    implementation(project(":domain-content"))

    implementation("org.springframework.boot:spring-boot-starter-web") // actuator health 노출용
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
}
