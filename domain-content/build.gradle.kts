dependencies {
    implementation(project(":common")) // content는 다른 도메인에 의존하지 않는다
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
}
