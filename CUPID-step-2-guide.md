# CUPID étape 2 : parcours en autonomie

Ce guide est pour faire l'étape 2 seul. Il explique ce que le harnais vérifie, dans quel ordre travailler et comment
lire sa sortie. Il ne donne pas la réponse : le choix des types, des alternatives et des fabriques vous appartient.

Lisez d'abord [docs/CUPID-step1-2.md](docs/CUPID-step1-2.md) pour l'énoncé (immutabilité, `Price`, ADT, composition)
et la définition des règles (HA2, HA3, HA4.1 à HA4.7). Ce guide est le mode d'emploi.

`./harness.sh 2` lance aussi toutes les règles de l'étape 1 (HA5 à HA12.1) : si l'étape 1 est rouge, l'étape 2 ne
peut pas passer. Le code de départ de la phase 2 a fait l'étape 1 (la persistance et ses tests CRUD ont été retirés,
les concepts et les identifiants sont des records), sauf HA7 : la hiérarchie `Perishable`/`NonExpiring` référence
l'entité `Product`, et c'est l'exercice HA4.1 de l'étape 2 qui la fera disparaître.

## 1. Ce que vous modifiez

Le code à travailler se trouve dans deux packages :

- `src/main/java/com/omniproduct/model` : les entités et les value objects (`Product`, `Price`, `Supplier`, ceux que
  vous créez)
- `src/main/java/com/omniproduct/service` : `ProductService`, `ProductTransportService`

Ne touchez pas à `domain/` ni à `infrastructure/` : ils sont hors exercice.

L'étape 2 ne découpe plus les entités. Elle change la manière dont elles s'expriment :

- les données sont des `record` immuables, sans setter ;
- les alternatives (un produit périssable ou non périssable, une vente possible ou refusée, un retrait réussi ou non)
  sont des `sealed interface` dont chaque alternative est un record ;
- les échecs attendus sont renvoyés comme des valeurs, pas levés comme des exceptions ;
- il n'y a plus d'héritage entre classes, et le modèle ne lit plus l'horloge.

## 2. Lancer le harnais tôt et souvent

```
./harness.sh 2
```

Le script compile d'abord le projet. Un projet qui ne compile pas ne passe jamais. Codes de sortie :

- `0` : toutes les vérifications passent (les étapes 1 et 2)
- `1` : au moins une vérification échoue
- `2` : le harnais n'a pas pu tourner

Les lignes `[WARN]` ne font jamais échouer le harnais. HA4.7 en est une : elle ne bloque pas, mais elle compte pour le
score du jeu.

Lancez-le une fois avant toute modification, pour voir votre point de départ. Vous verrez des échecs sur HA7, HA2,
HA3 et HA4 : c'est normal. Ne modifiez rien sous `harness/` pour faire passer une vérification. Si une règle vous semble
fausse, notez pourquoi et signalez-le à l'animateur de l'atelier.

Pour le jeu, lancez aussi `./harness.sh 2 --json rapport.json` : c'est ce rapport que l'arbitre compare entre votre
branche et la branche de base.

## 3. Travailler dans cet ordre

Un baby step = une PR = une règle qui passe de rouge à vert (voir la section « The game » de
[CUPID-step-1.md](CUPID-step-1.md), et « How to work » : chaque PR est un draft qui vise `java-bdxio-phase2` et n'est jamais fusionné ; chaque
branche part du baby step précédent). L'ordre ci-dessous est celui qui
coûte le moins de réécriture : une règle en amont rend souvent la suivante plus facile.

1. **HA2 : données immuables.** Transformez les classes de données en `record`, retirez les setters et les champs
   mutables, et remplacez les collections modifiables par des collections immuables.
2. **HA3 : les records sont des value objects.** Créez `Price` comme record dans un package `valueobject`, sans
   VAT. Aucun record ne doit implémenter `Identifiable`, et le package `valueobject` ne contient que des records.
3. **HA4.1 : pas d'héritage.** Aucune classe ni aucun record ne doit étendre une autre classe. Les interfaces et
   `implements` sont permis. Composez au lieu d'étendre.
   Le code de départ contient une hiérarchie volontaire : `Perishable` et `NonExpiring` étendent `Product`. Supprimez
   ces deux classes et remplacez-les par une ADT (`sealed interface` + records), comme décrit à l'étape 6 (HA4.3).
4. **HA4.2 : pas d'exception dans un constructeur.** Retirez les `throw`, `assert` et gardes (`requireNonNull`,
   `checkArgument`...) des constructeurs. La validation passe par une fabrique (étape 5).
5. **HA4.4 : fabrique à résultat scellé.** Un value object a une méthode statique `create`, `build`, `of`, `from` ou
   `make` qui renvoie un `sealed interface` dont les alternatives sont le value object et un record d'erreur.
6. **HA4.3 : ADT dans `Product` (boss).** `Product` a une propriété typée par un `sealed interface` dont toutes les
   alternatives sont des value objects, sans setter. Le choix des alternatives est le cœur de l'exercice : deux
   alternatives, les dates de péremption, pas un booléen.
7. **HA4.5 : switch exhaustif (boss).** Les résultats et alternatives sont traités par un `switch` qui couvre
   chaque cas. Plus de `instanceof`, de cast, ni de résultat scellé ignoré.
8. **HA4.6 : le modèle ne lit pas l'horloge.** Remplacez `LocalDate.now()`, `Instant.now()`,
   `System.currentTimeMillis()`, `new Date()`, `Clock.systemUTC()` par une date passée en paramètre, ou par un
   `Clock` injecté.
9. **HA4.7 : `canSell` avant `withdraw` (avertissement).** Dans la méthode de vente, appelez le contrôle avant le
   retrait de stock.

Lancez `./harness.sh 2` après chaque étape. Un petit changement qui fait passer une règle de FAIL à PASS vaut mieux
qu'un gros refactoring que vous ne pouvez pas vérifier.

## 4. Règle par règle

Le texte des règles dans [docs/CUPID-step1-2.md](docs/CUPID-step1-2.md) fait autorité ; ce qui suit n'est qu'une aide
à la lecture.

### HA2 - données immuables

Les données sont des `record`, sans setter. Les champs sont `final`. Aucune collection modifiable (`ArrayList`,
`HashMap`, `HashSet`...) n'apparaît en champ ni dans un type public. Utilisez `List.of`, `Map.of`, ou des collections
immuables.

### HA3 - un record n'est jamais une entité

Un record n'implémente pas `Identifiable`. Le package `valueobject` ne contient que des records : une `class` y est
signalée. Un record `Price` doit exister dans ce package. `Price` ne contient plus la TVA : la TVA se calcule à part,
dans l'ordre marge, puis frais de transport si présents, puis TVA si applicable.

### HA4.1 - pas d'héritage

Aucune classe ni aucun record ne déclare de superclasse autre que `Object`. Cette règle vérifie le bytecode, donc
l'héritage est vu même quand il passe par un alias. Les `implements` sont autorisés.

### HA4.2 - pas d'exception dans un constructeur

Un constructeur (ou un constructeur compact de record) ne contient ni `throw`, ni `assert`, ni appel dont un mot est
`require`, `check`, `throw` ou `assert`. Si la donnée est invalide, la fabrique renvoie un résultat d'erreur.

### HA4.3 - Product porte une ADT

`Product` a au moins une propriété (champ, composant de record ou accesseur sans paramètre) typée par un
`sealed interface` avec `permits`. Ses alternatives doivent être des value objects. Aucun setter ne doit la modifier :
un nom de méthode qui commence par `set`, `change`, `update`, `replace`, `assign` ou `put` et contient le nom de la
propriété est refusé. Un `withX` qui renvoie une copie est permis.

### HA4.4 - fabrique à résultat scellé

Au moins un value object a une fabrique (`create`, `build`, `of`, `from`, `make`, en mot dans le nom) qui renvoie :
- soit un `sealed interface` dont une alternative est le value object et une autre un record (`PerishableCreation`),
- soit un type générique scellé paramétré par le value object (`Validated<Perishable>`).

Un `Either` ou un `Optional` n'est pas accepté à cette étape.

### HA4.5 - switch exhaustif sur l'ADT

Trois choses sont refusées dans `model` et `service` :
- `instanceof` ou cast vers une alternative, y compris écrit avec un nom qualifié ;
- un résultat scellé ignoré : un appel en simple instruction à une méthode qui renvoie une ADT (`product.canSell(today);`) ;
- aucun `switch` qui couvre toutes les alternatives d'une ADT dont toutes les alternatives sont des records.

Le harnais reconnaît l'ignorance d'un résultat au nom de la méthode, pas à son type. Une surcharge homonyme qui ne
renvoie pas d'ADT est donc aussi signalée : renommez-la si c'est gênant.

### HA4.6 - le modèle ne lit pas l'horloge

Aucun appel du JDK qui lit l'heure : `java.time` (`now`, `today`), `Calendar`, `System.currentTimeMillis()`,
`System.nanoTime()`, `new Date()`, `Clock.systemUTC()`. `Duration.toMillis()` n'est pas une horloge et n'est pas
signalée. Pour tester une date, passez `today` en paramètre.

### HA4.7 - canSell avant withdraw (avertissement)

Dans une méthode dont le nom contient `sell`, le premier appel à `withdraw` doit être précédé, dans le même corps de
méthode, d'un appel dont le nom contient `can` et `sell`. Un `[WARN]` ne fait pas échouer le harnais, mais le
résultat compte au jeu (+3 quand il passe).

## 5. Lire un échec

Une ligne d'échec nomme la règle, la classe et le numéro de ligne quand le harnais peut le retrouver. Commencez par la
première règle en échec et corrigez cette classe avant la suivante. Plusieurs échecs ont souvent la même cause : une
ADT bien placée dans `Product` peut corriger HA4.3 et rendre HA4.5 plus facile à satisfaire.

Si un résultat vous surprend, relisez la règle dans [docs/CUPID-step1-2.md](docs/CUPID-step1-2.md) avant de modifier le
code. Si le code est correct pour le domaine et que le harnais proteste quand même, notez le cas et signalez-le à
l'animateur plutôt que de contourner la règle.

## 6. Terminé

L'étape 2 est terminée quand `./harness.sh 2` sort avec le code `0`. Les avertissements sont autorisés. Avant de
considérer l'étape comme finie :

- le harnais sort avec `0` sur votre machine, après une compilation propre ;
- vous savez expliquer pourquoi `Product` porte une ADT plutôt qu'un booléen, et ce qui se passe quand on ajoute une
  troisième alternative (le compilateur signale chaque `switch` à compléter) ;
- chaque règle que vous avez fait passer l'a été dans sa propre PR, avec son titre `HAx green: ...`.

Gardez la sortie de votre dernière exécution : c'est la preuve que l'animateur demandera.
