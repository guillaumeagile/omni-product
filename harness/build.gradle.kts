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

    // Métriques CK de couplage, cohésion et style (Maurício Aniche), notées par l'arbitre
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

// Les tests du harnais sont ceux de l'animateur, pas des joueurs : ils décrivent le code de départ de l'atelier (ils
// attendent des règles rouges sur le legacy) et échouent dès qu'une équipe progresse. Un `gradle test` à la racine, avec
// ou sans --continue, ne doit donc pas les lancer : ils ne tournent que sur demande explicite, avec -PharnessTests
// (c'est ce que passe `./harness.sh --test`).
val harnessTestsRequested = providers.gradleProperty("harnessTests").isPresent

gradle.taskGraph.whenReady {
    if (!harnessTestsRequested && hasTask(tasks.test.get())) {
        logger.lifecycle("Tests du harnais ignorés : réservés à l'animateur (-PharnessTests). Ne les lancez pas pour jouer.")
    }
}

tasks.test {
    onlyIf("réservé à l'animateur : lancer avec -PharnessTests (./harness.sh --test)") { harnessTestsRequested }
    // Le dry run analyse le vrai code de l'atelier : il doit être compilé avant
    dependsOn(":classes")
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
    }
}
