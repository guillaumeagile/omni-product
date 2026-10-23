# Inventaire des règles du harnais (phase 1)

Source : `Cupid.Harness/Rules/*` de la version C#. Moteur : **AST** = tree-sitter-java sur le source,
**BYTECODE** = ArchUnit sur les classes compilées. Les seuils sont ceux du C# ; ils sont à recaler
sur le Java en phase 3.

| Id | Règle | Intention | Équivalent Java | Moteur | Sévérité |
|---|---|---|---|---|---|
| HA1 | Idiomes | Style idiomatique imposé au build | Error Prone ou Checkstyle, warnings promus en erreurs (voir décision ci-dessous) | build Gradle | Error |
| HA2 | Immutable data structures | Records uniquement, pas de setter, pas de collection mutable | `record` ou classe à champs `final` sans `set*` ; interdire `ArrayList`, `HashMap`, `HashSet`… en champ et type public ; collections vavr/`List.of` acceptées | AST (setters, champs non `final`, types déclarés) + BYTECODE (types des champs) | Error |
| HA4 | Composable : pas d'héritage | Composition plutôt qu'héritage de classe | Toute classe ou record avec `extends` autre que `Object`/`Exception` est signalée ; `implements` autorisé | BYTECODE (`superclass`) avec ligne via AST | Error |
| HA5 | Pas de god class | Au plus 4 propriétés publiques par type (hors `*Id`) | Compter les composants de `record`, les getters publics et les champs publics, hors noms finissant par `Id` | AST | Error |
| HA6 | Single concern | Les méthodes publiques d'une classe couvrent au plus 1 contexte borné ; restrictions de vocabulaire par type | Même `ubiquitous-language-map.json` ; préfixe du nom de méthode comparé aux mots-clés par contexte | AST | Error |
| HA7 | Fan-out | Une entité ne référence aucune autre entité, seulement des ids | Champs, paramètres, retours, locaux, arguments génériques (`List<Supplier>`, `Supplier[]`, `Optional<Supplier>`) | BYTECODE (dépendances de type) + AST pour la ligne | Error |
| HA8 | Pas de persistance dans le domaine | Le modèle ne connaît pas son stockage | Annotations `jakarta.persistence.*` (`@Entity`, `@Table`, `@Column`, `@OneToMany`…) et Spring Data sur les types du domaine ; champs fantômes `*Csv`/`*Json` ; méthodes `sync*`/`hydrate*` | AST (annotations, noms) + BYTECODE (dépendance vers `jakarta.persistence`) | Error |
| HA9 | Entités implémentent `Identifiable` | Une seule réponse à « est-ce une entité ? » | Tout type du package `...model..` implémente `Identifiable` | BYTECODE | Error |
| HA10 | Pas de service dans le constructeur d'une entité | Les services dépendent des entités, jamais l'inverse | Constructeur d'une entité avec un paramètre du package `..service..` ou nommé `*Service` | BYTECODE | Error |
| HA11 | Identifiants riches | `Id`/`*Id`/`*Ids` : UUID ou ULID, ou record qui en enveloppe un ; jamais `String`/`int`/`long` ; v7 minimum | Voir « UUID v7 » ci-dessous ; interdire `UUID.randomUUID()` (v4) et valeurs nulles/vides en contexte d'id | AST | Error |
| HA11.1 | Identifiant nu = faible | Primitive obsession sur les ids | Tout `Id`/`*Id`/`*Ids` dont le type n'est pas déclaré dans le source (String, int, UUID…) | AST | Warning |

## Adaptations propres à Java

- **UUID v7** : le JDK 21 n'a pas de `UUID.v7()`. Le legacy et les corrigés utiliseront une bibliothèque
  (`com.github.f4b6a3:uuid-creator` ou `ulid-creator`) ; à choisir en phase 4. La règle HA11 reconnaît
  `UuidCreator.getTimeOrderedEpoch()` / `UlidCreator.getUlid()` et signale `UUID.randomUUID()`.
- **Persistance** : le C# visait les data annotations EF Core ; ici ce sont les annotations JPA
  (`jakarta.persistence`) et les dépendances Spring Data (`JpaRepository`) dans les types du domaine.
  Les repositories et la configuration JPA restent de l'infrastructure, comme `DbContext` en C#.
- **Entités** : le C# reconnaît le namespace `Models.*` ; ici le package `com.omniproduct.model..`
  et l'interface marqueur `Identifiable` (à créer en phase 4).
- **Lombok** : si le legacy l'utilise, les getters/setters générés n'existent que dans le bytecode ;
  décision à prendre en phase 4 (probablement ne pas l'utiliser).

## Décision HA1 : Error Prone ou Checkstyle

Recommandation : **Error Prone** branché sur `javac` via Gradle, avec les checks choisis passés en `ERROR`.
Raison : il analyse l'AST compilé (proche des analyzers Roslyn), tourne dans le build sans outil séparé et
produit des diagnostics précis. Checkstyle reste un repli s'il pose problème sur JDK 21 / Gradle 8 (à vérifier
en phase 2). La liste exacte des checks est un livrable de la phase 3.

## Ordre de portage (phase 3)

HA5 → HA4, HA2 → HA6 → HA8 → HA7 → HA9, HA10 → HA11, HA11.1.

## État d'implémentation et étapes

L'**étape 1** (`./harness.sh`, `docs/CUPID-step-1.md`) couvre **HA4 à HA11** uniquement. Les règles se règlent
dans `Steps.java`.

| Règle | Implémentée | Étape | Notes |
|---|---|---|---|
| HA1 idiomes | non | hors étape 1 | à décider (Error Prone ou Checkstyle) |
| HA2 données immuables | oui (`ImmutableDataStructuresRule`) | étape suivante | écrite et testée, pas branchée dans l'étape 1 |
| HA4 pas d'héritage | oui | 1 | modèle + services |
| HA5 pas de god class | oui | 1 | seuil 4 (le document C# annonce 6 mais le code C# exécute 4) |
| HA6 single concern | oui | 1 | `ubiquitous-language-map.json` adapté au vocabulaire Java |
| HA7 fan-out | oui | 1 | entité = implémente `Identifiable` ou déclare un champ `id` |
| HA8 pas de persistance | oui | 1 | dépendance vers une techno de stockage (jakarta/javax.persistence, Hibernate, Spring Data/JDBC, java.sql, jOOQ, MyBatis, MongoDB) + champs fantômes `*Json`/`*Csv`/`*Xml`/`*Blob`/`*Row` + méthodes `sync*`/`hydrate*`/`persist*`/`serialize*` |
| HA9 `Identifiable` | oui | 1 | records, interfaces et enums exemptés |
| HA10 pas de service au constructeur | oui | 1 | échoue sur `Supplier` (constructeur ajouté à l'exercice) |
| HA11 identifiants riches | oui | 1 | UUID/Ulid ou record qui en enveloppe un ; UUID v4 détecté |
| HA11.1 identifiant nu | oui | 1 | avertissement seulement |

Périmètre : `com.omniproduct.model` (et `com.omniproduct.service` pour HA4, HA6, HA11.1). Le port carbone
(`domain/`) et l'infrastructure ne font pas partie de l'exercice.

## Heuristiques, pas correspondances exactes

Une règle qui compare des noms exacts se contourne en renommant (`fetchSlug` au lieu de `getSlug`, `productID` au lieu
de `productId`, `PriceManager` au lieu de `PriceService`). Le harnais raisonne donc sur des **mots** (`Words` : découpe
camelCase / snake_case / acronymes, minuscules, singulier) et sur plusieurs indices indépendants. Un faux négatif
trivial est un défaut de la règle.

| Règle | Avant (exact) | Maintenant (heuristique) |
|---|---|---|
| HA5 | `getX` / `isX` publics | tout accesseur sans paramètre qui renvoie une valeur (`getX`, `fetchX`, `x()`), les setters, les champs publics ; lire + écrire une propriété compte une fois ; ids exclus par mot |
| HA6 | préfixe du nom après un verbe connu | mot ou suite de mots **n'importe où** dans le nom ; le mot-clé le plus spécifique gagne ; contexte « faible » (Lifecycle) seulement en dernier recours ; si le nom est générique, on lit les types de retour/paramètres ; API = tout sauf `private` (+ champs publics) |
| HA8 | `jakarta.persistence`, Spring Data | + javax, Hibernate, JDBC, `java.sql`, jOOQ, MyBatis, MongoDB ; + forme stockée (`xxxJson`, `yyy_csv`) et `sync*`/`hydrate*` |
| HA9 | interface nommée `Identifiable` | inchangé : c'est le contrat explicite de l'exercice |
| HA10 | type du package `..service` ou nom en `Service` | package (`service`, `repository`, `application`, `infrastructure`…), suffixe (`Manager`, `Handler`, `Client`, `Gateway`, `Facade`, `Factory`…), annotation Spring, types dans les génériques ; entité = champ `id`, `<classe>Id` ou `@Id` |
| HA11 | noms `id`/`*Id`/`*Ids`, liste de types interdits, `randomUUID()` d'un identifiant | un mot `id`/`identifier`/`uuid`/`guid`/`uid` dans le nom (casse et séparateur libres) ; **tout** type autre qu'UUID/Ulid ou record qui en enveloppe un ; tout UUID non v7 construit dans le modèle (`randomUUID`, import statique, `UUID::randomUUID`, `nameUUIDFromBytes`, `new UUID`, `UuidCreator.get*` hors `getTimeOrderedEpoch`) |

Chaque contournement est écrit dans `HeuristicEvasionTest` (et son pendant `SingleConcernRuleTest`) : si une règle
régresse vers l'égalité de chaînes, un test échoue.

### Limites connues (faux négatifs restants)

- HA6 : un nom sans aucun mot du vocabulaire (`apply`, `process`) avec des types génériques n'est classé nulle part ;
  la carte `ubiquitous-language-map.json` doit être enrichie avec le métier.
- HA10 : un service injecté par setter ou par champ `@Autowired`, ou pris par une méthode d'usine statique, n'est pas vu.
- HA11 : le flux de données n'est pas suivi (un `Long` converti en `UUID` par un helper n'est pas détecté).
- HA7 : la dépendance est lue dans le bytecode, donc une référence par réflexion ou par `Object` passe.
- Plus une règle est heuristique, plus un faux positif est possible (un mot `id` dans une variable locale sans rapport) :
  on le corrige en affinant le mot, jamais en désactivant la règle.
