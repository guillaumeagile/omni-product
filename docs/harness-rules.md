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
| HA11 | Concept du domaine non primitif | Un membre du modèle dont le nom désigne un concept du vocabulaire porte un type du domaine (record), pas une valeur brute | Champ, paramètre, composant ou retour nommé par le vocabulaire (contexte fort, pas `Lifecycle`) et typé `String`/`int`/`long`/`BigDecimal`… ; identifiants exclus (HA12) | AST | Error |
| HA12 | Identifiants riches | `Id`/`*Id`/`*Ids` générés par le système : UUID ou ULID, ou record qui en enveloppe un ; jamais `String`/`int`/`long` ; v7 minimum. Un identifiant naturel (`naturalIdentifiers` : siren, siret, tva, vat, sku, ean, gtin, isbn) en est exempté | Voir « UUID v7 » ci-dessous ; interdire `UUID.randomUUID()` (v4) et valeurs nulles/vides en contexte d'id | AST | Error |
| HA12.1 | Identifiant nu = faible | Primitive obsession sur les ids | Tout `Id`/`*Id`/`*Ids` dont le type n'est pas déclaré dans le source (String, int, UUID…) | AST | Warning |

## Adaptations propres à Java

- **UUID v7** : le JDK 21 n'a pas de `UUID.v7()`. Le legacy et les corrigés utiliseront une bibliothèque
  (`com.github.f4b6a3:uuid-creator` ou `ulid-creator`) ; à choisir en phase 4. La règle HA12 reconnaît
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

HA5 → HA4, HA2 → HA6 → HA8 → HA7 → HA9, HA10 → HA12, HA12.1.

## État d'implémentation et étapes

L'**étape 1** (`./harness.sh`, `docs/CUPID-step-1.md`) couvre **HA5 à HA12.1** uniquement. Les règles se règlent
dans `Steps.java`.

| Règle | Implémentée | Étape | Notes |
|---|---|---|---|
| HA1 idiomes | non | hors étape 1 | à décider (Error Prone ou Checkstyle) |
| HA2 données immuables | oui (`ImmutableDataStructuresRule`) | étape suivante | écrite et testée, pas branchée dans l'étape 1 |
| HA4 pas d'héritage | oui (`NoInheritanceRule`) | 2 | écrite et testée, retirée de l'étape 1 (non câblée dans `Steps.java`) |
| HA5 pas de god class | oui | 1 | seuil 4 (le document C# annonce 6 mais le code C# exécute 4) |
| HA6 single concern | oui | 1 | `ubiquitous-language-map.json` adapté au vocabulaire Java |
| HA7 fan-out | oui | 1 | entité = implémente `Identifiable` ou déclare un champ `id` |
| HA8 pas de persistance | oui | 1 | dépendance vers une techno de stockage (jakarta/javax.persistence, Hibernate, Spring Data/JDBC, java.sql, jOOQ, MyBatis, MongoDB) + champs fantômes `*Json`/`*Csv`/`*Xml`/`*Blob`/`*Row` + méthodes `sync*`/`hydrate*`/`persist*`/`serialize*` |
| HA9 `Identifiable` | oui | 1 | records, interfaces et enums exemptés |
| HA10 pas de service au constructeur | oui | 1 | échoue sur `Supplier` (constructeur ajouté à l'exercice) |
| HA11 concept non primitif | oui | 1 | étroite : nom du vocabulaire (contexte fort) + type brut ; identifiants exclus |
| HA12 identifiants riches | oui | 1 | UUID/Ulid ou record qui en enveloppe un ; UUID v4 détecté |
| HA12.1 identifiant nu | oui | 1 | avertissement seulement |

Périmètre : `com.omniproduct.model` (et `com.omniproduct.service` pour HA4, HA6, HA12.1). Le port carbone
(`domain/`) et l'infrastructure ne font pas partie de l'exercice.

## Heuristiques, pas correspondances exactes

Une règle qui compare des noms exacts se contourne en renommant (`fetchSlug` au lieu de `getSlug`, `productID` au lieu
de `productId`, `PriceManager` au lieu de `PriceService`). Le harnais raisonne donc sur des **mots** (`Words` : découpe
camelCase / snake_case / acronymes, minuscules, singulier) et sur plusieurs indices indépendants. Un faux négatif
trivial est un défaut de la règle.

| Règle | Avant (exact) | Maintenant (heuristique) |
|---|---|---|
| HA5 | `getX` / `isX` publics | tout accesseur sans paramètre qui renvoie une valeur (`getX`, `fetchX`, `x()`), les setters, les champs publics ; lire + écrire une propriété compte une fois ; ids exclus par mot |
| HA6 | préfixe du nom après un verbe connu | mot ou suite de mots **n'importe où** dans le nom ; le mot-clé le plus spécifique gagne ; contexte « faible » (Lifecycle) seulement en dernier recours ; si le nom est générique, on lit les types de retour/paramètres, sauf les types d'identifiant (`ProductId` n'est d'aucun contexte) ; API = tout sauf `private` (+ champs publics) |
| HA8 | `jakarta.persistence`, Spring Data | + javax, Hibernate, JDBC, `java.sql`, jOOQ, MyBatis, MongoDB ; + forme stockée (`xxxJson`, `yyy_csv`) et `sync*`/`hydrate*` |
| HA9 | interface nommée `Identifiable` | inchangé : c'est le contrat explicite de l'exercice |
| HA10 | type du package `..service` ou nom en `Service` | package (`service`, `repository`, `application`, `infrastructure`…), suffixe (`Manager`, `Handler`, `Client`, `Gateway`, `Facade`, `Factory`…), annotation Spring, types dans les génériques ; entité = champ `id`, `<classe>Id` ou `@Id` |
| HA12 | noms `id`/`*Id`/`*Ids`, liste de types interdits, `randomUUID()` d'un identifiant | un mot `id`/`identifier`/`uuid`/`guid`/`uid` dans le nom (casse et séparateur libres) ; généré par défaut, naturel seulement si un mot du nom est dans `naturalIdentifiers` (`tvaId`, `supplierSiren`) : renommer `productId` en `ownerId` n'échappe pas à la règle, mais renommer un id en `tvaId` oui (limite connue, visible en revue) ; **tout** type autre qu'UUID/Ulid ou record qui en enveloppe un ; tout UUID non v7 construit dans le modèle (`randomUUID`, import statique, `UUID::randomUUID`, `nameUUIDFromBytes`, `new UUID`, `UuidCreator.get*` hors `getTimeOrderedEpoch`) |

Chaque contournement est écrit dans `HeuristicEvasionTest` (et son pendant `SingleConcernRuleTest`) : si une règle
régresse vers l'égalité de chaînes, un test échoue.

### Passe faux négatifs (tests `FalseNegativeProbeTest`, `ArchEvasionTest`, fixtures `*/evasion`)

Corrigés pendant la passe (chaque cas est un test) :

- HA5 : les membres publics d'une **interface** (publics par défaut) ; les constantes `public static` ne comptent pas.
- HA6 : un mot-clé multi-mots se trouve dans n'importe quel ordre (`getLabelForDisplay`) ; le vocabulaire français
  `Tva` est rangé dans Pricing.
- HA7 : une entité qui atteint une autre entité **à travers un value object** (`Basket -> Offer(Supplier)`) est signalée.
- HA8 : verbes `flush` et `reload` ; forme stockée `...Serialized` et `...Dump`.
- HA9 : un **record qui porte un identifiant** est une entité : il doit implémenter `Identifiable`. Avant, il échappait
  à HA7, HA9 et HA10. Un record sans identifiant reste un value object.
- HA10 : un record entité est aussi contrôlé.
- HA12 : `var` n'est plus une fausse alerte ; le paramètre d'un setter d'identifiant (`setSupplierId(String value)`) est
  vérifié ; les identifiants métier `siren`, `siret`, `sku`, `ean`, `gtin`, `isbn` sont des identifiants.

Restent des faux négatifs, assumés (par conception ou faute de moyen sûr de les voir) :

- HA6 : un vocabulaire métier manquant (`getCost` pour Pricing) ; un nom générique (`apply`, `process`) sans type
  du vocabulaire. Le remède est d'enrichir `ubiquitous-language-map.json`, pas de durcir la règle.
- HA7 : la fuite est suivie sur **un** niveau de value object seulement.
- HA8 : `toJson`/`fromJson` ne sont pas signalés (risque de faux positif sur un export légitime) ; `@Transactional`
  (Spring) n'est pas dans la liste (pas de Spring sur le classpath des tests).
- HA10 : un service injecté par setter, par champ `@Autowired`, par une méthode d'usine statique, ou avec un nom neutre
  (`PriceCalculator`, `Notifier`) n'est pas vu.
- HA12 : le flux de données n'est pas suivi (un `Long` converti en `UUID` par un helper) ; un argument littéral passé à
  un constructeur (`new Product("ABC")`) n'est pas un paramètre nommé ; un identifiant nommé `key`, `code` ou
  `reference` n'est pas reconnu.
- HA5 : un accesseur qui prend un paramètre n'est pas une propriété ; `protected` n'est pas compté.

## Ce que « heuristique » promet

Une heuristique recoupe plusieurs indices indépendants au lieu de se fier à un seul nom : elle est plus difficile à
contourner (moins de faux négatifs) **et** plus juste (moins de faux positifs), parce qu'un indice isolé ne suffit
plus à condamner. C'est l'intention. Les choix qui la tiennent : mots entiers (`valid`, `android` ne sont pas des
identifiants), contexte « faible » qui ne compte qu'en dernier recours (Lifecycle), types lus seulement quand le nom
ne dit rien. Si un faux positif apparaît quand même, c'est un défaut de la règle : on ajoute un indice ou on affine le
mot, on ne désactive pas la règle.
