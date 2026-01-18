plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-source", "25", "-target", "25", "--enable-preview"))
}

repositories {
    mavenCentral()
}

dependencies {
    // Vavr
    implementation("io.vavr:vavr:0.10.4")

    // Testing - JUnit 5
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // AssertJ - Best assertion library
    testImplementation("org.assertj:assertj-core:3.25.3")
}


tasks.test {
    useJUnitPlatform()
    jvmArgs = listOf("--enable-preview")
}
