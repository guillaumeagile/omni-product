plugins {
    java
    application
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // AST du source : tree-sitter (bindings natifs macOS / Linux / Windows)
    implementation("io.github.bonede:tree-sitter:0.26.6")
    implementation("io.github.bonede:tree-sitter-java:0.23.5")

    // Sémantique sur le bytecode compilé
    implementation("com.tngtech.archunit:archunit:1.5.1")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.25.3")
    // Les fixtures de test portent de vraies annotations JPA
    testImplementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
}

application {
    mainClass = "com.omniproduct.harness.HarnessMain"
}

tasks.test {
    // Le dry run analyse le vrai code de l'atelier : il doit être compilé avant
    dependsOn(":classes")
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
    }
}
