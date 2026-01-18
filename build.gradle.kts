plugins {
    id("java")
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.omniproduct"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Vavr
    implementation("io.vavr:vavr:0.10.4")

    // Testing - JUnit 5
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // AssertJ - Best assertion library
    testImplementation("org.assertj:assertj-core:3.25.3")
}


tasks.test {
    useJUnitPlatform()
}
