# Plan : mise à jour de l'atelier (harnais tree-sitter + ArchUnit)

## Objectif

Garder le scaffold Java / Spring Boot, mais refondre le déroulé de l'atelier sur le modèle de la version C#
(`Cute-KanDDDinsky-2026-CUPID-phase1`) : un code legacy anémique à refactorer pas à pas, **évalué par un
harnais déterministe** (même code en entrée, même verdict en sortie, sans IA).

En C#, le harnais s'appuie sur Roslyn (arbre syntaxique + `Compilation` sémantique). En Java :

| Besoin | C# (Roslyn) | Java |
|---|---|---|
| Forme du source (propriétés, records, setters, annotations, noms) | `SyntaxTree` | **tree-sitter-java** (moteur `AST`) |
| Sémantique (héritage, dépendances entre types, interfaces implémentées) | `Compilation` | **ArchUnit** sur le bytecode (moteur `BYTECODE`) |
| Idiomes | analyzers `.editorconfig` | Error Prone / Checkstyle (à trancher en phase 1) |

## Décisions prises

- Le harnais est écrit en **Java**, dans un **module Gradle `harness/`** de ce repo.
- Le périmètre « pimp » couvre `model/` **et** les services (`service/`, `controller/`).
- La règle « pas de persistance dans le domaine » cible les annotations et types **JPA**
  (`jakarta.persistence.*`, et Spring Data) comme le C# ciblait EF Core.
- ArchUnit est utilisé en complément de tree-sitter ; chaque règle déclare son moteur.
- Chaque phase = une branche + une PR, vérifiée avec `claude --cloud`. Ce plan (Markdown seul) vit sur `main`.

## Phase 1 : cadrage et inventaire

- Lire les règles C# (HA1, HA5 à HA11) et la table `ubiquitous-language-map.json`.
- Produire `docs/harness-rules.md` : pour chaque règle, intention, équivalent Java, moteur (AST / BYTECODE),
  seuil, sévérité (Error / Warning).
- Trancher l'équivalent de HA1 (idiomes) : Error Prone ou Checkstyle.
- Sortie : tableau d'inventaire validé.

## Phase 2 : squelette du harnais

- Module `harness/` : dépendance `tree-sitter` + `tree-sitter-java` (bindings Maven), ArchUnit, JUnit.
- Contrats : `HarnessRule` (`id`, `name`, `severity`, `check(SourceSet) -> List<Violation>`),
  `Violation(file, line, message)`, `HarnessSeverity`.
- `SourceSet` : fichiers `.java` triés (ordre déterministe), arbres tree-sitter, classes ArchUnit importées.
- Runner CLI : `--step 1`, sortie `[PASS] / [FAIL] / [WARN] HAx - nom`, code de sortie 0 seulement si aucune erreur.
- Première règle de bout en bout : HA5, pas de god class, avec son test de pinning.
- Critère : `./gradlew :harness:test` vert ; le runner tourne sur `src/main/java/com/omniproduct`.

## Phase 3 : portage des règles

Une règle à la fois, chacune avec ses tests de pinning (fixtures Java valides et invalides) :

1. HA5 no god class (AST)
2. NoInheritance, ImmutableDataStructures (AST + BYTECODE)
3. SingleConcern + `ubiquitous-language-map.json` (AST)
4. NoPersistenceInDomain, ciblant JPA (AST pour les annotations, BYTECODE pour les dépendances)
5. FanOut : une entité ne référence aucune autre entité, seulement des ids (BYTECODE)
6. EntityMustImplementIdentifiable, NoServicesInEntityConstructor (BYTECODE)
7. RichIdentifier, BareIdentifierIsWeak : ids ULID/UUIDv7 ou record wrapper, pas de `randomUUID()` brut (AST)

## Phase 4 : pimper le code de départ

- Réécrire `model/` (`Product`, `Supplier`, …) en s'inspirant de `OmniProduct-CoreDomain` : legacy anémique
  qui viole volontairement les règles, mais qui compile et dont les tests passent.
- Adapter `service/` et `controller/` en conséquence (même dérive de responsabilités que `ProductService` C#).
- Garder `domain/` (port carbone) et `infrastructure/` en l'état sauf nécessité.
- Critère : le harnais échoue sur le point de départ avec les violations attendues (liste figée dans un test).

## Phase 5 : matériel d'atelier

- `CUPID-step-1-1.md` et `CUPID-step-1-2.md` en version Java ; mettre à jour `docs/Workshop.md`.
- Scripts `cupid-step-1-1.harness.sh` / `.ps1` (build puis harnais).
- `AGENTS.md` (ne pas relâcher les règles pour faire passer l'exercice), `README.md`.
- Slides : alignement avec la version C#.

## Phase 6 : vérification et finition

- Build, tests et harnais vérifiés avec `claude --cloud` sur chaque PR.
- Solution de référence (corrigé) qui passe le harnais pour valider que l'atelier est faisable.
- Relecture finale ; rapport branche / PR / état de la vérification cloud.

## Risques

- Les bindings Java de tree-sitter reposent sur du code natif : vérifier macOS arm64, Linux, Windows.
- ArchUnit exige que le code compile ; le harnais doit signaler clairement « ne compile pas » plutôt qu'un faux PASS.
- Aucun test non joué ne doit être présenté comme passé.
