plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

val camelVersion = "4.4.0"

dependencies {
    implementation(project(":shared"))

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Apache Camel — Spring Boot integration
    implementation("org.apache.camel.springboot:camel-spring-boot-starter:$camelVersion")
    implementation("org.apache.camel.springboot:camel-http-starter:$camelVersion")
    implementation("org.apache.camel.springboot:camel-jackson-starter:$camelVersion")
    implementation("org.apache.camel.springboot:camel-rest-starter:$camelVersion")
    implementation("org.apache.camel.springboot:camel-log-starter:$camelVersion")

    // Jackson Kotlin support
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.apache.camel:camel-test-spring-junit5:$camelVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}
