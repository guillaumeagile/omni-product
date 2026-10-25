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

    // Complexité cyclomatique : la règle CyclomaticComplexity de PMD, outil éprouvé, plutôt qu'un calcul maison
    implementation("net.sourceforge.pmd:pmd-java:7.7.0")

    // Tokens : JTokkit (BPE de tiktoken, encodage o200k_base), plutôt qu'un découpage maison
    implementation("com.knuddels:jtokkit:1.1.0")

    // Essai (branche testCK) : métriques CK de couplage, cohésion et style, affichées par l'arbitre sans points
    implementation("com.github.mauricioaniche:ck:0.7.0")

    // Lecture de ubiquitous-language-map.json
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

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
