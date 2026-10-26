# Inventaire des règles du harnais (phase 1)

Source : `Cupid.Harness/Rules/*` de la version C#. Moteur : **AST** = tree-sitter-java sur le source,
**BYTECODE** = ArchUnit sur les classes compilées. Les seuils sont ceux du C# ; ils sont à recaler
sur le Java en phase 3.

| Id | Règle | Intention | Équivalent Java | Moteur | Sévérité |
|---|---|---|---|---|---|
| HA1 | Idiomes | Style idiomatique imposé au build | Error Prone ou Checkstyle, warnings promus en erreurs (voir décision ci-dessous) | build Gradle | Error |
| HA2 | Immutable data structures | Records, ou classes dont tous les champs sont `final` ; pas de setter, pas de collection mutable, pas de tableau | Champ non `final` dans une classe (statiques et classes imbriquées compris, chaque déclarateur de `String a, b;`) ; setter public `set*` ; collection de `java.util` (`List`, `Map`, `ArrayList`…, résolue par les imports) ; tableau (`String[]`), même dans un champ `final` ou un composant de record ; collections vavr/`List.of` acceptées | AST (champs, setters, types déclarés, imports) | Error |
| HA3 | Records are value objects | Un record n'est jamais une entité ; le package de value objects ne contient que des records ; `Price` existe | Record qui implémente `Identifiable` ; `class` dans un package `valueobject` ; record `Price` absent | AST | Error |
| HA4.1 | Composable : pas d'héritage | Composition plutôt qu'héritage de classe | Toute classe ou record avec `extends` autre que `Object`/`Exception` est signalée ; `implements` autorisé | BYTECODE (`superclass`) avec ligne via AST | Error |
| HA4.2 | Pas d'exception dans un constructeur | Une entrée invalide est un résultat, pas une exception levée par `new` | `throw`, `assert`, appel de garde (`requireNonNull`, `checkArgument`…) dans un constructeur ou un constructeur compact | AST | Error |
| HA4.3 | Product porte une ADT | `Product` a une propriété typée par une `sealed interface` de value objects, sans setter | Pas de propriété ADT ; setter sur cette propriété ; alternative qui n'est pas un value object | AST | Error |
| HA4.4 | Validation à la frontière | Un value object a une fabrique `create`/`build`/`of`/`from`/`make` qui renvoie un résultat scellé (lui-même ou un record d'erreur), ou une ADT de Vavr (`Either`, `Validation`, `Try`, `Option`) dont le succès est le value object | Aucune fabrique ; résultat ni scellé ni Vavr (`Optional` du JDK, `Either` maison) ; value object du côté de l'erreur (`Either<Price, String>`) ; erreur scellée qui n'est pas un record | AST | Error |
| HA4.5 | Match exhaustif sur l'ADT | Les alternatives, et les résultats scellés, sont traités par un `switch` qui les couvre toutes | `instanceof` ou cast vers une ADT ou une alternative ; résultat scellé ignoré (appel en simple instruction) ; aucun `switch` exhaustif | AST | Error |
| HA4.6 | Le modèle ne lit pas l'horloge | Le modèle reçoit `today`, ou un `Clock` injecté | `LocalDate.now()`, `Instant.now()`, `System.currentTimeMillis()`, `new Date()`, `Clock.systemUTC()`… dans `model` | BYTECODE (appels, imports statiques compris) | Error |
| HA4.7 | La vente dépend de `canSell` | Une vente ne retire du stock que si le domaine l'a permise | Le retrait doit se trouver dans une branche (`if`, ternaire, `switch`) ou une lambda (`map`, `Option.when`) qui dépend de la décision, ou après une clause de garde qui renvoie le refus ; un `canSell` ignoré ne garde rien | AST | Error |
| HA5 | Pas de god class | Au plus 4 propriétés publiques par type (hors `*Id`) | Compter les composants de `record`, les getters publics et les champs publics, hors noms finissant par `Id` | AST | Error |
| HA6 | Single concern | Les méthodes publiques d'une classe couvrent au plus 1 contexte borné ; restrictions de vocabulaire par type | Même `ubiquitous-language-map.json` ; préfixe du nom de méthode comparé aux mots-clés par contexte | AST | Error |
| HA7 | Fan-out | Une entité ne référence aucune autre entité, seulement des ids | Champs, paramètres, retours, locaux, arguments génériques (`List<Supplier>`, `Supplier[]`, `Optional<Supplier>`) | BYTECODE (dépendances de type) + AST pour la ligne | Error |
| HA8 | Pas de persistance dans le domaine | Le modèle ne connaît pas son stockage | Annotations `jakarta.persistence.*` (`@Entity`, `@Table`, `@Column`, `@OneToMany`…) et Spring Data sur les types du domaine ; champs fantômes `*Csv`/`*Json` ; méthodes `sync*`/`hydrate*` | AST (annotations, noms) + BYTECODE (dépendance vers `jakarta.persistence`) | Error |
| HA9 | Entités implémentent `Identifiable` | Une seule réponse à « est-ce une entité ? » | Tout type du package `...model..` implémente `Identifiable` | BYTECODE | Error |
| HA10 | Pas de service dans le constructeur d'une entité | Les services dépendent des entités, jamais l'inverse | Constructeur d'une entité avec un paramètre du package `..service..` ou nommé `*Service` | BYTECODE | Error |
| HA11 | Concept du domaine non primitif | Un membre du modèle dont le nom désigne un concept du vocabulaire porte un type du domaine (record), pas une valeur brute | Champ, paramètre, composant ou retour nommé par le vocabulaire (contexte fort, pas `Lifecycle`) et typé `String`/`int`/`long`/`BigDecimal`… ; identifiants exclus (HA12) | AST | Error |
| HA12 | Identifiants riches | `Id`/`*Id`/`*Ids` générés par le système : UUID ou ULID, ou record qui en enveloppe un ; jamais `String`/`int`/`long` ; v7 minimum. Un identifiant naturel (`naturalIdentifiers` : siren, siret, tva, vat, sku, ean, gtin, isbn) en est exempté | Voir « UUID v7 » ci-dessous ; interdire `UUID.randomUUID()` (v4) et valeurs nulles/vides en contexte d'id | AST | Error |
| HA12.1 | Identifiant nu = faible | Primitive obsession sur les ids | Tout `Id`/`*Id`/`*Ids` dont le type n'est pas déclaré dans le source (String, int, UUID…) | AST | Warning |
| HA14 | Collections en pipeline | Une collection se transforme par un Stream, pas en remplissant un accumulateur | Boucle (`for`, for-each, `while`, `do`) ou `forEach`/`peek` qui modifie une variable déclarée hors d'elle | AST | Error |
| HA15 | La fabrique valide par composition | La fabrique d'un value object exprime sa validation comme une chaîne d'étapes (`Option.of(...).filter(...).flatMap(...)`, `Validation.combine(...).ap(...)`), pas comme un branchement | `if`, ternaire ou `switch` dans la fabrique ou dans un helper privé qu'elle appelle ; `throw`, `assert` ou appel de garde (`requireNonNull`…) ; fabrique dont le retour promet un échec (générique, ADT scellée) sans aucune lambda ni référence de méthode ; **aucune** fabrique dont le résultat peut échouer (rouge par absence, comme HA4.4) | AST | Error |
| HA21 | Un conteneur Vavr se consomme par `fold` | Un `Either`, `Option`, `Try` ou `Validation` n'est jamais ouvert : on le replie, on le transforme (`map`, `flatMap`) ou on lui donne une valeur par défaut (`getOrElse(valeur)`) | Appel, ou référence de méthode, sur `io.vavr.control.*` ou `io.vavr.Value` dont le nom, lu en mots, est `get` sans argument, `get` + `left`/`error`/`cause` (`getLeft`, `getError`, `getCause`), contient `throw` (`getOrElseThrow`, `orElseThrow`), ou est `is` + `left`/`right`/`empty`/`defined`/`present`/`success`/`failure`/`valid`/`invalid` | BYTECODE | Error |

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
  records (`permits`), et `switch` avec patterns est le `Match` exhaustif (HA4.3, HA4.5). Un résultat est aussi une
  ADT scellée avec des échecs nommés (`sealed interface SaleDecision permits Sellable, PastSellByDate`), pas un
  `Either<String, T>`. La fabrique d'un value object (HA4.4) peut renvoyer une ADT de Vavr (`Either`, `Validation`) ; le chaînage `map`/`flatMap` reste un atout de la phase 3 (voir
  `docs/PLAN-workshop-game.md`). L'héritage de classe reste interdit (HA4.1).
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

L'**étape 1** (`./harness.sh`, `docs/CUPID-step-1.md`) couvre **HA5 à HA12.1** uniquement. L'**étape 2** (`./harness.sh 2`) ajoute HA2, HA3 et HA4.1 à HA4.7 ; l'**étape 3** (`./harness.sh 3`, `docs/CUPID-step-3.md`) ajoute HA14, HA15 et HA21 (style fonctionnel : pipelines, fabrique par composition, conteneurs Vavr consommés par `fold`) ; `all` lance toutes les étapes, c'est-à-dire l'étape 3. La numérotation reprend à HA14 : on saute le 13. Les règles se règlent
dans `Steps.java`.

| Règle | Implémentée | Étape | Notes |
|---|---|---|---|
| HA1 idiomes | non | hors étape 1 | à décider (Error Prone ou Checkstyle) |
| HA5 pas de god class | oui | 1 | seuil 4 (le document C# annonce 6 mais le code C# exécute 4) |
| HA6 single concern | oui | 1 | `ubiquitous-language-map.json` adapté au vocabulaire Java |
| HA7 fan-out | oui | 1 | entité = implémente `Identifiable` ou déclare un champ `id` |
| HA8 pas de persistance | oui | 1 | dépendance vers une techno de stockage (jakarta/javax.persistence, Hibernate, Spring Data/JDBC, java.sql, jOOQ, MyBatis, MongoDB) + champs fantômes `*Json`/`*Csv`/`*Xml`/`*Blob`/`*Row` + méthodes `sync*`/`hydrate*`/`persist*`/`serialize*` |
| HA9 `Identifiable` | oui | 1 | records sans identifiant, interfaces et enums exemptés ; un record qui porte un identifiant est signalé, le message dit d'en faire une classe (HA3 lui interdit `Identifiable`) |
| HA10 pas de service au constructeur | oui | 1 | échoue sur `Supplier` (constructeur ajouté à l'exercice) |
| HA11 concept non primitif | oui | 1 | étroite : nom du vocabulaire (contexte fort) + type brut ; identifiants exclus |
| HA12 identifiants riches | oui | 1 | UUID/Ulid ou record qui en enveloppe un ; UUID v4 détecté |
| HA12.1 identifiant nu | oui | 1 | avertissement seulement |
| HA2 données immuables | oui | 2 | AST, sur `model` ; une entité reste une classe (HA9) : elle passe si ses champs sont `final` |
| HA3 value objects | oui | 2 | record non entité, package `valueobject` ne contient que des records, `Price` exigé |
| HA4.1 pas d'héritage | oui | 2 | BYTECODE, sur `model` et `service` |
| HA4.2 pas d'exception en constructeur | oui | 2 | AST |
| HA4.3 ADT dans `Product` | oui | 2 | sealed interface de records, sans setter |
| HA4.4 fabrique à résultat scellé | oui | 2 | `create`/`build`… renvoie une ADT scellée (le value object ou un record d'erreur) ou une ADT de Vavr du value object |
| HA4.5 switch exhaustif | oui | 2 | pas de `instanceof`, de cast ni de résultat scellé ignoré ; un `switch` couvre les alternatives ; model et service |
| HA4.6 pas d'horloge | oui | 2 | BYTECODE |
| HA4.7 la vente dépend de `canSell` | oui | 2 | erreur ; vérifie la dépendance au résultat, plus l'ordre du texte |
| HA14 collections en pipeline | oui | 3 | échoue sur `CatalogService.catalogLabels` (exercice ajouté) |
| HA15 la fabrique valide par composition | oui | 3 | AST, sur `model` ; **rouge par absence** sur le legacy (aucune fabrique dont le résultat peut échouer, comme HA4.4) et rouge dès qu'une fabrique branche ou lance (preuve par mutation sur `Slug`) |
| HA21 un conteneur Vavr se consomme par `fold` | oui | 3 | bytecode, sur `model` et `service` ; **verte** sur le départ de la phase 3 (garde-fou : aucun Vavr dans le départ, elle rougit quand une équipe ouvre un `Either`/`Option` avec `get()`, `isLeft()`…) ; mutation `ha21-vavr-escape-hatches` |

Périmètre : `com.omniproduct.model` (et `com.omniproduct.service` pour HA4.1, HA4.7, HA6, HA12.1, HA14). Le port carbone
(`domain/`) et l'infrastructure ne font pas partie de l'exercice.

### Mutations du harnais

Une règle verte sur le legacy (le défaut n'y est pas encore) n'est prouvée que par une **mutation**. Elles sont rejouables :
`harness/mutations/<nom>/src/...` contient le fichier mutant (même chemin que dans le dépôt, il doit compiler) et
`expected.txt` une ligne `<règle> <fichier>:<ligne>` par détection attendue. `harness/mutate.sh [nom...]` applique le mutant,
lance `./harness.sh all`, vérifie que **cette règle** signale **cette ligne** (une règle rouge ailleurs ne valide pas
l'attente d'une autre), puis restaure `src/` même en cas d'échec ; il refuse de partir d'un `src/main/java` modifié. Le
workflow `le-maitre-des-jeux` le joue sur chaque PR d'animateur (étape « Mutation du harnais ») et échoue si un défaut
n'est pas vu. Mutations actuelles : `ha15-factory-branches-and-throws` (`Slug.of` avec `if` + `throw`) et `ha21-vavr-escape-hatches` (`Slug.of` qui ouvre un `Either` avec `isLeft()` et `get()` ; une règle bytecode n'affiche que le nom du fichier, `expected.txt` attend donc `Slug.java:7`).

### Propositions de règles (TODO, phase 3)

Idées nées de la lecture du bloc « PMD quality metrics » de la PR #22 (HA4.7, 0 point PMD : fan-out 61 → 64,
complexité cognitive 2 → 3, **0 lambda** alors que le barème en récompense jusqu'à +10). Le gain PMD annoncé est une
**hypothèse** : chaque règle s'écrit, se câble, puis se **joue dans une PR dédiée** pour lire le gain réel dans le
commentaire de l'arbitre. Une règle à la fois ; les numéros sont provisoires et reprennent à HA16 (HA14 et HA15 sont les deux premières jouées en phase 3, le 13 est sauté ; **HA21 est prise** par la règle Vavr, HA16 à HA20 restent à écrire). HA14 (pipelines) et HA15 (fabrique par
composition) sont nées de cette liste.

| TODO | Règle | Cible dans le code d'aujourd'hui | Gain PMD supposé | CUPID | Détection envisagée |
|---|---|---|---|---|---|
| TODO HA16 | Une décision du domaine se compose, elle ne se teste pas : `if (!canSell) return none()` devient `Option.when(canSell, …)` / `filter` / `map` | la clause de garde de `ProductService.sellOne` | cognitive −1, nesting −1, +1 à +2 lambdas (+2 à +4) | Composable, Idiomatic | durcissement de HA4.7 : dans `service`, un `if` dont la condition porte une décision `can*` et dont la branche est un `return` ; Warning d'abord |
| TODO HA17 | Le service ne reconstruit pas un value object champ par champ (*Tell, don't ask*) : `new ProductStock(new Quantity(stock.stock().value() - 1), stock.quantity(), stock.warehouse())` devient `stock.withdrawOne()` | `ProductService.withdrawStock` | fan-out du service −2 (+1) | Domain-based | méthode de `service` qui appelle ≥ 2 accesseurs d'un même paramètre pour construire un objet du type de ce paramètre |
| TODO HA18 | Un record imbriqué dans une entité est un concept à part : `Product.Warehouse`, `Product.Supplier` (collision avec l'entité `Supplier`), `Supplier.Contact`, `Supplier.Address` | `Product`, `Supplier` | fan-out max −2 à −4 (+1 à +2) ; le compteur « Types » monte, non noté par choix | Domain-based, Composable | record déclaré dans le corps d'une classe qui implémente `Identifiable` |
| TODO HA19 | Pas de code mort ni de doublon de décision dans le modèle : `Perishable.isPastSellBy(TimeServiceProvider)` n'est appelé nulle part et duplique `Product.canSellOn(today)` avec une autre convention d'horloge | `Perishable` | fan-out −1, WMC −1 | Unix philosophy, Predictable | méthode publique du modèle sans référence dans `model`, `service` et les tests ; variante : une seule convention d'horloge (`today` passé **ou** fournisseur injecté) |
| TODO HA20 | HA11 étendu au `service` : pas de primitives aux frontières (`calculateShipmentCarbon(String, Double, Double)`) | `ProductTransportService` | ≈ nul (fan-out +1 possible) : règle de design pur, pas de score | Domain-based | même heuristique que HA11, périmètre `service` |

Ajustements du **barème PMD** à envisager, indépendants des règles :

- TODO Affectations : 8 des 9 affectations sont les `this.x = x` des constructeurs de `Product` et `Supplier`, que
  HA3/HA9 obligent à rester des classes. Ne pas compter les affectations de champ dans un constructeur (ne garder que
  les réaffectations et les variables locales), sinon la métrique reste inerte toute la phase 2.
- TODO TCC min = 0 : ce sont `ProductService` et `SystemTimeServiceProvider`, sans champ. Non actionnable sans mutiler
  le code : laisser à 0 point ou la calculer sur `model` seul.

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
| HA3 | `implements Identifiable` exact | `Identifiable` comme mot, qualifié ou générique (`com.acme.Identifiable<Id>`, `IdentifiableEntity`) ; package `valueobject` au singulier, pluriel, `value_objects`, `valueObjects` |
| HA4.2 | `throw` seulement | `throw`, `assert` et tout appel dont un mot est `require`, `check`, `throw` ou `assert` (`requireNonNull`, `checkArgument`, `throwIfNull`) |
| HA4.3 | un type précis | ADT = `sealed interface … permits` ; propriété = champ, composant de record ou accesseur sans paramètre ; setter = verbe de mutation (`set`, `change`, `update`, `replace`, `assign`, `put`) suivi des mots de la propriété (`withX` renvoie une copie, permis) |
| HA4.4 | `create` et type d'erreur nommé | fabrique = `create`/`build`/`of`/`from`/`make` en mot (`fromRawValue`) ; retour = ADT dont une alternative est le value object et une autre un record (`PerishableCreation`), ou ADT générique paramétrée par le value object (`Validated<Perishable>`), ou ADT de Vavr (`Either`, `Validation`, `Try`, `Option`, importée de `io.vavr` ou nommée en entier) dont le dernier argument de type est le value object (`Either<InvalidDates, Perishable>`) |
| HA4.5 | `if` ou `switch` au hasard | `instanceof` et cast (qualifiés compris) vers une alternative ; appel à une méthode du code qui renvoie une ADT, laissé en simple instruction ; `switch` exhaustif = un `case` (type ou record) par alternative d'une ADT dont toutes les alternatives sont des records |
| HA4.6 | `LocalDate.now()` seulement | appel du JDK (`java.time`, `Calendar`, `System`, `new Date()`) dont un mot est `now`, `today`, `clock`, `utc`, `system`, `instance`, ou `currentTimeMillis`/`nanoTime` ; `Duration.toMillis()` n'est pas une horloge |
| HA4.7 | l'ordre des lignes, `sell` et `withdraw` nommés | vente = méthode non privée qui retire, sauf nom d'opération de stock (`restock`, `discard`, `adjust`, `transfer`, `withdraw`…) ; un mot de vente (`sell`, `sale`, `sold`, `buy`, `purchase`, `checkout`, `order`) force le contrôle. Retrait = appel ou référence de méthode `withdraw`/`decrement`/`deduct`/`destock`/`unstock`, ou construction/appel qui parle de `stock`/`inventory` avec une soustraction (`-`, `minus`, `subtract`), ou appel d'une méthode du fichier qui retire (transitivement). Décision = appel `can` + `sell`/`sale`, `sellable`/`saleable`, `sell`/`sale` + `decide`/`decision`/`allowed`/`permitted`, ou variable locale qui en porte une. Gardé = dans la branche d'un `if`/ternaire/`switch` dont la condition porte la décision, dans une lambda ou référence de méthode passée à un appel dont le receveur ou un autre argument la porte, ou après une clause de garde `if (décision) return/throw`. Limites : un nom d'opération de stock (`withdrawOne`) échappe au contrôle ; la branche `else` d'un `if (canSell)` compte comme gardée ; les variables ne sont suivies que dans la méthode |
| HA12 | noms `id`/`*Id`/`*Ids`, liste de types interdits, `randomUUID()` d'un identifiant | un mot `id`/`identifier`/`uuid`/`guid`/`uid` dans le nom (casse et séparateur libres) ; généré par défaut, naturel seulement si un mot du nom est dans `naturalIdentifiers` (`tvaId`, `supplierSiren`) : renommer `productId` en `ownerId` n'échappe pas à la règle, mais renommer un id en `tvaId` oui (limite connue, visible en revue) ; **tout** type autre qu'UUID/Ulid ou record qui en enveloppe un ; tout UUID non v7 construit dans le modèle (`randomUUID`, import statique, `UUID::randomUUID`, `nameUUIDFromBytes`, `new UUID`, `UuidCreator.get*` hors `getTimeOrderedEpoch`) |
| HA14 | `for` seulement, `List.add` seulement | boucles `for`, for-each, `while`, `do` et `forEach`/`forEachOrdered`/`peek` (lambda ou référence de méthode, `result::add`) ; modifier = affectation (`=`, `+=`, `++`) ou appel d'un verbe de remplissage (`add`, `put`, `append`, `push`, `offer`, `insert`, `set`, `remove`, `merge`, `compute`, `increment`, `accumulate`, `accept`, `clear`) sur une variable, un champ (`this.x`), une constante ou un tableau non déclarés dans l'itération ; `Collections.addAll(result, …)` vise son premier argument. Limites : une boucle qui ne remplit rien (recherche avec `return` anticipé, simple affichage) n'est pas visée ; un remplissage délégué à une méthode d'un autre nom (`register(p)`) n'est pas suivi |
| HA15 | `if` dans une méthode nommée `of` | fabrique = méthode statique non privée d'un record non entité, dont le nom porte un mot de fabrique (`create`/`build`/`of`/`from`/`make`) **ou** dont le type de retour nomme le record (`Option<Perishable>`, `PerishableCreation`, `Perishable`) : `parse` n'échappe pas. Lus : le corps, et transitivement les méthodes du même type appelées (`validate(a, b)`, `this.validate`, `Perishable.validate`), chaque défaut signalé une fois. Branchement = `if`, ternaire, `switch`, même dans une lambda ; lancer = `throw`, `assert`, appel dont un mot est `require`/`check`/`assert`/`throw`, qualifié ou non. « Ne décide rien » = retour générique ou ADT scellée du code, sans aucune `lambda_expression` ni `method_reference` atteignable ; une fabrique qui renvoie le record nu n'est pas jugée sur son retour (c'est HA4.4) mais ne compte pas comme existante : la règle est **rouge par absence** tant qu'aucun value object n'a de fabrique à résultat générique ou scellé (une règle verte parce qu'il n'y a rien à examiner ne prouve rien et ne laisse rien à jouer) |
| HA21 | `get()` seulement, `Either` seulement | lecture en mots du nom de l'appel, sur le paquet `io.vavr.control` et sur `io.vavr.Value` (où `Option` et `Try` héritent `get`) : `get`, `getLeft`/`getError`/`getCause`, tout mot `throw`, `is` + mot d'état ; les références de méthode (`Either::get`), les imports statiques et les variables typées par un supertype sont vus par le bytecode ; `get(int)` d'une liste et `Optional.get()` du JDK ne sont pas visés |

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
- HA2 : un champ `final` d'un type mutable autre qu'une collection ou un tableau (`Date`, `StringBuilder`, une classe
  du code) n'est pas vu ; les paramètres varargs (`String...`) ne sont pas signalés.
- HA3 : une classe immuable écrite à la main (sans `record`) n'est pas un value object pour la règle ; `Price` est
  un nom imposé par l'exercice, pas une heuristique.
- HA4.3 : une ADT sans `permits` n'est pas reconnue ; le type de la propriété doit être déclaré dans le code.
- HA4.4 : dans un résultat scellé, l'erreur est un record déclaré dans le code, pas « implémente une interface d'erreur »
  (pas de base commune en Java). Avec Vavr, le type d'erreur n'est pas vérifié (`Either<String, Price>` passe) et
  `Option`/`Try` passent sans erreur nommée. Le type Vavr est reconnu par l'import ou le nom qualifié, sans résolution
  de type : un import `io.vavr.*` (le paquet racine) ne suffit pas.
- HA4.5 : le résultat ignoré est reconnu au **nom** de la méthode (pas de résolution de type) : une surcharge homonyme
  qui renvoie `void` serait signalée à tort. Un résultat rangé dans une variable puis jamais lu n'est pas vu. Un
  `default` n'est pas compté comme alternative.
- HA4.6 : une horloge lue par une bibliothèque tierce, ou par réflexion, n'est pas vue.
- HA4.7 : la décision n'est suivie que dans une même méthode (variables locales) ; un retrait délégué à une autre classe n'est vu que par les mots de son nom.
- HA21 : seuls les conteneurs de `io.vavr.control` et `io.vavr.Value` sont surveillés ; `Lazy`, `Future` et les collections Vavr
  ne le sont pas. Un conteneur ouvert par réflexion, ou par une bibliothèque tierce, n'est pas vu. La règle interdit aussi
  `isEmpty()`/`isDefined()` : elle juge la forme (on ne teste pas un conteneur, on le replie), pas le risque d'exception.
  Elle est verte tant qu'aucun Vavr n'est utilisé : elle protège les règles à jouer, elle n'en est pas une.
- HA15 : un branchement déplacé dans **un autre type** (`Dates.ordered(a, b)` statique ailleurs dans `model`) n'est pas suivi ; une
  fabrique qui compose une seule étape triviale (`Option.of(x).map(Slug::new)`, sans `filter`) passe : la règle voit la forme,
  pas la force de la validation. Les `&&`/`||` dans une lambda de `filter` ne sont pas des branchements pour elle.

## Ce que « heuristique » promet

Une heuristique recoupe plusieurs indices indépendants au lieu de se fier à un seul nom : elle est plus difficile à
contourner (moins de faux négatifs) **et** plus juste (moins de faux positifs), parce qu'un indice isolé ne suffit
plus à condamner. C'est l'intention. Les choix qui la tiennent : mots entiers (`valid`, `android` ne sont pas des
identifiants), contexte « faible » qui ne compte qu'en dernier recours (Lifecycle), types lus seulement quand le nom
ne dit rien. Si un faux positif apparaît quand même, c'est un défaut de la règle : on ajoute un indice ou on affine le
mot, on ne désactive pas la règle.
