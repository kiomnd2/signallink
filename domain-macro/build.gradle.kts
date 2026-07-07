dependencies {
    implementation(project(":common"))
    implementation(project(":domain-market")) // macro -> market (읽기)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
}
