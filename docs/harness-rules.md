# Inventaire des règles du harnais (phase 1)

Source : `Cupid.Harness/Rules/*` de la version C#. Moteur : **AST** = tree-sitter-java sur le source,
**BYTECODE** = ArchUnit sur les classes compilées. Les seuils sont ceux du C# ; ils sont à recaler
sur le Java en phase 3.

| Id | Règle | Intention | Équivalent Java | Moteur | Sévérité |
|---|---|---|---|---|---|
| HA1 | Idiomes | Style idiomatique imposé au build | Error Prone ou Checkstyle, warnings promus en erreurs (voir décision ci-dessous) | build Gradle | Error |
| HA2 | Immutable data structures | Records uniquement, pas de setter, pas de collection mutable | `record` ou classe à champs `final` sans `set*` ; interdire `ArrayList`, `HashMap`, `HashSet`… en champ et type public ; collections vavr/`List.of` acceptées | AST (classes, setters, types déclarés, imports) | Error |
| HA3 | Records are value objects | Un record n'est jamais une entité ; le package de value objects ne contient que des records ; `Price` existe | Record qui implémente `Identifiable` ; `class` dans un package `valueobject` ; record `Price` absent | AST | Error |
| HA4.1 | Composable : pas d'héritage | Composition plutôt qu'héritage de classe | Toute classe ou record avec `extends` autre que `Object`/`Exception` est signalée ; `implements` autorisé | BYTECODE (`superclass`) avec ligne via AST | Error |
| HA4.2 | Pas d'exception dans un constructeur | Une entrée invalide est un résultat, pas une exception levée par `new` | `throw`, `assert`, appel de garde (`requireNonNull`, `checkArgument`…) dans un constructeur ou un constructeur compact | AST | Error |
| HA4.3 | Product porte une ADT | `Product` a une propriété typée par une `sealed interface` de value objects, sans setter | Pas de propriété ADT ; setter sur cette propriété ; alternative qui n'est pas un value object | AST | Error |
| HA4.4 | Validation à la frontière | Un value object a une fabrique `create`/`build`/`of`/`from`/`make` qui renvoie `Either<Erreur, Self>` | Aucune fabrique ; type d'erreur non déclaré dans le code | AST | Error |
| HA4.5 | Match exhaustif sur l'ADT | Les alternatives sont traitées par un `switch` qui les couvre toutes, jamais par `instanceof` | `instanceof` sur une ADT ou une alternative ; aucun `switch` exhaustif | AST | Error |
| HA4.6 | Le modèle ne lit pas l'horloge | Le modèle reçoit `today`, ou un `Clock` injecté | `LocalDate.now()`, `Instant.now()`, `System.currentTimeMillis()`, `new Date()`, `Clock.systemUTC()`… dans `model` | BYTECODE (appels, imports statiques compris) | Error |
| HA4.7 | `canSell` avant `withdraw` | Dans une méthode de vente, le contrôle précède le retrait de stock | `withdraw` sans `canSell` qui le précède dans la même méthode | AST | Warning |
| HA5 | Pas de god class | Au plus 4 propriétés publiques par type (hors `*Id`) | Compter les composants de `record`, les getters publics et les champs publics, hors noms finissant par `Id` | AST | Error |
| HA6 | Single concern | Les méthodes publiques d'une classe couvrent au plus 1 contexte borné ; restrictions de vocabulaire par type | Même `ubiquitous-language-map.json` ; préfixe du nom de méthode comparé aux mots-clés par contexte | AST | Error |
| HA7 | Fan-out | Une entité ne référence aucune autre entité, seulement des ids | Champs, paramètres, retours, locaux, arguments génériques (`List<Supplier>`, `Supplier[]`, `Optional<Supplier>`) | BYTECODE (dépendances de type) + AST pour la ligne | Error |
| HA8 | Pas de persistance dans le domaine | Le modèle ne connaît pas son stockage | Annotations `jakarta.persistence.*` (`@Entity`, `@Table`, `@Column`, `@OneToMany`…) et Spring Data sur les types du domaine ; champs fantômes `*Csv`/`*Json` ; méthodes `sync*`/`hydrate*` | AST (annotations, noms) + BYTECODE (dépendance vers `jakarta.persistence`) | Error |
| HA9 | Entités implémentent `Identifiable` | Une seule réponse à « est-ce une entité ? » | Tout type du package `...model..` implémente `Identifiable` | BYTECODE | Error |
| HA10 | Pas de service dans le constructeur d'une entité | Les services dépendent des entités, jamais l'inverse | Constructeur d'une entité avec un paramètre du package `..service..` ou nommé `*Service` | BYTECODE | Error |
| HA11 | Concept du domaine non primitif | Un membre du modèle dont le nom désigne un concept du vocabulaire porte un type du domaine (record), pas une valeur brute | Champ, paramètre, composant ou retour nommé par le vocabulaire (contexte fort, pas `Lifecycle`) et typé `String`/`int`/`long`/`BigDecimal`… ; identifiants exclus (HA12) | AST | Error |
| HA12 | Identifiants riches | `Id`/`*Id`/`*Ids` : UUID ou ULID, ou record qui en enveloppe un ; jamais `String`/`int`/`long` ; v7 minimum | Voir « UUID v7 » ci-dessous ; interdire `UUID.randomUUID()` (v4) et valeurs nulles/vides en contexte d'id | AST | Error |
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
- **ADT et OneOf** : Java n'a pas `OneOf`. L'union fermée est une `sealed interface` dont les alternatives sont des
  records (`permits`), et `switch` avec patterns est le `Match` exhaustif (HA4.3, HA4.5). `Result` est vavr `Either`,
  déjà présent dans le build (HA4.4). L'héritage de classe reste interdit (HA4.1).
- **Horloge** (HA4.6) : lue dans le bytecode, pour voir les imports statiques et les alias que l'AST rate.
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

L'**étape 1** (`./harness.sh`, `docs/CUPID-step-1.md`) couvre **HA5 à HA12.1** uniquement. L'**étape 2** (`./harness.sh 2`) ajoute HA2, HA3 et HA4.1 à HA4.7 ; `all` lance les deux étapes. Les règles se règlent
dans `Steps.java`.

| Règle | Implémentée | Étape | Notes |
|---|---|---|---|
| HA1 idiomes | non | hors étape 1 | à décider (Error Prone ou Checkstyle) |
| HA5 pas de god class | oui | 1 | seuil 4 (le document C# annonce 6 mais le code C# exécute 4) |
| HA6 single concern | oui | 1 | `ubiquitous-language-map.json` adapté au vocabulaire Java |
| HA7 fan-out | oui | 1 | entité = implémente `Identifiable` ou déclare un champ `id` |
| HA8 pas de persistance | oui | 1 | dépendance vers une techno de stockage (jakarta/javax.persistence, Hibernate, Spring Data/JDBC, java.sql, jOOQ, MyBatis, MongoDB) + champs fantômes `*Json`/`*Csv`/`*Xml`/`*Blob`/`*Row` + méthodes `sync*`/`hydrate*`/`persist*`/`serialize*` |
| HA9 `Identifiable` | oui | 1 | records, interfaces et enums exemptés |
| HA10 pas de service au constructeur | oui | 1 | échoue sur `Supplier` (constructeur ajouté à l'exercice) |
| HA11 concept non primitif | oui | 1 | étroite : nom du vocabulaire (contexte fort) + type brut ; identifiants exclus |
| HA12 identifiants riches | oui | 1 | UUID/Ulid ou record qui en enveloppe un ; UUID v4 détecté |
| HA12.1 identifiant nu | oui | 1 | avertissement seulement |
| HA2 données immuables | oui | 2 | AST, sur `model` |
| HA3 value objects | oui | 2 | record non entité, package `valueobject` ne contient que des records, `Price` exigé |
| HA4.1 pas d'héritage | oui | 2 | BYTECODE, sur `model` et `service` |
| HA4.2 pas d'exception en constructeur | oui | 2 | AST |
| HA4.3 ADT dans `Product` | oui | 2 | sealed interface de records, sans setter |
| HA4.4 fabrique `Either` | oui | 2 | `create`/`build`… renvoie `Either<Erreur, Self>` |
| HA4.5 switch exhaustif | oui | 2 | pas de `instanceof` sur une ADT ; un `switch` couvre les alternatives |
| HA4.6 pas d'horloge | oui | 2 | BYTECODE |
| HA4.7 `canSell` avant `withdraw` | oui | 2 | avertissement |

Périmètre : `com.omniproduct.model` (et `com.omniproduct.service` pour HA4.1, HA4.7, HA6, HA12.1). Le port carbone
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
| HA3 | `implements Identifiable` exact | `Identifiable` comme mot, qualifié ou générique (`com.acme.Identifiable<Id>`, `IdentifiableEntity`) ; package `valueobject` au singulier, pluriel, `value_objects`, `valueObjects` |
| HA4.2 | `throw` seulement | `throw`, `assert` et tout appel dont un mot est `require`, `check`, `throw` ou `assert` (`requireNonNull`, `checkArgument`, `throwIfNull`) |
| HA4.3 | un type précis | ADT = `sealed interface … permits` ; propriété = champ, composant de record ou accesseur sans paramètre ; setter = verbe de mutation (`set`, `change`, `update`, `replace`, `assign`, `put`) suivi des mots de la propriété (`withX` renvoie une copie, permis) |
| HA4.4 | `create` et type d'erreur nommé | fabrique = `create`/`build`/`of`/`from`/`make` en mot (`fromRawValue`) ; retour à deux arguments dont l'un est le type et l'autre un type déclaré dans le code ; nom du générique libre (`Either`, `Result`…) |
| HA4.5 | `if` ou `switch` au hasard | `instanceof` sur une alternative ; `switch` exhaustif = un `case` (type ou record) par alternative d'une ADT dont toutes les alternatives sont des records |
| HA4.6 | `LocalDate.now()` seulement | appel du JDK (`java.time`, `Calendar`, `System`, `new Date()`) dont un mot est `now`, `today`, `clock`, `utc`, `system`, `instance`, ou `currentTimeMillis`/`nanoTime` ; `Duration.toMillis()` n'est pas une horloge |
| HA4.7 | `canSell` nommé exactement | méthode dont un mot est `sell` ; le premier `withdraw` (mot) doit être précédé d'un appel `can` + `sell` dans l'ordre du source |
| HA12 | noms `id`/`*Id`/`*Ids`, liste de types interdits, `randomUUID()` d'un identifiant | un mot `id`/`identifier`/`uuid`/`guid`/`uid` dans le nom (casse et séparateur libres) ; **tout** type autre qu'UUID/Ulid ou record qui en enveloppe un ; tout UUID non v7 construit dans le modèle (`randomUUID`, import statique, `UUID::randomUUID`, `nameUUIDFromBytes`, `new UUID`, `UuidCreator.get*` hors `getTimeOrderedEpoch`) |

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
- HA3 : une classe immuable écrite à la main (sans `record`) n'est pas un value object pour la règle ; `Price` est
  un nom imposé par l'exercice, pas une heuristique.
- HA4.3 : une ADT sans `permits` n'est pas reconnue ; le type de la propriété doit être déclaré dans le code.
- HA4.4 : le type d'erreur est « déclaré dans le code », pas « implémente une interface d'erreur » (pas de base commune
  en Java) ; un `Either<String, Self>` est donc refusé, un `Either<PriceError, Self>` accepté.
- HA4.5 : seuls `instanceof` et `switch` sont vus, pas les casts `(Perishable) s` ; un `default` n'est pas compté comme
  alternative.
- HA4.6 : une horloge lue par une bibliothèque tierce, ou par réflexion, n'est pas vue.
- HA4.7 : la séquence n'est vue que dans une même méthode ; avertissement seulement, il ne fait jamais échouer le harnais.

## Ce que « heuristique » promet

Une heuristique recoupe plusieurs indices indépendants au lieu de se fier à un seul nom : elle est plus difficile à
contourner (moins de faux négatifs) **et** plus juste (moins de faux positifs), parce qu'un indice isolé ne suffit
plus à condamner. C'est l'intention. Les choix qui la tiennent : mots entiers (`valid`, `android` ne sont pas des
identifiants), contexte « faible » qui ne compte qu'en dernier recours (Lifecycle), types lus seulement quand le nom
ne dit rien. Si un faux positif apparaît quand même, c'est un défaut de la règle : on ajoute un indice ou on affine le
mot, on ne désactive pas la règle.
