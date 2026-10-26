# CUPID étape 3 : parcours en autonomie

Ce guide est pour faire l'étape 3 seul. Il explique ce que le harnais vérifie, dans quel ordre travailler et comment
lire sa sortie. Il ne donne pas la réponse : le choix des étapes du pipeline et de la forme de la fabrique vous
appartient.

Lisez d'abord [docs/CUPID-step-3.md](docs/CUPID-step-3.md) pour l'énoncé (pipelines, composition) et la définition des
règles (HA14 et HA15). Ce guide est le mode d'emploi. Les règles sont numérotées à partir de HA14 : le 13 est sauté.

`./harness.sh 3` lance aussi toutes les règles des étapes 1 et 2 : le code de départ de la phase 3 les a faites, elles
sont vertes. Si l'une d'elles rougit pendant votre travail, c'est une régression de votre changement : corrigez-la sur la
même branche, ou réparez-la dans un baby step suivant.

## 1. Ce que vous modifiez

Le code à travailler se trouve dans deux packages :

- `src/main/java/com/omniproduct/model` : les entités et les value objects
- `src/main/java/com/omniproduct/service` : `ProductService`, `CatalogService`, `ProductTransportService`

Ne touchez pas à `domain/` ni à `infrastructure/` : ils sont hors exercice.

L'étape 3 ne change ni les types ni les entités. Elle change la manière dont le code déplace les données :

- une collection se transforme par un pipeline (`Stream`), pas par une boucle qui remplit un accumulateur ;
- une fabrique valide son entrée en composant des étapes (`filter`, `flatMap`, `map`, `combine`), pas avec des `if`.

## 2. Lancer le harnais tôt et souvent

```
./harness.sh 3
```

Le script compile d'abord le projet. Un projet qui ne compile pas ne passe jamais. Codes de sortie :

- `0` : toutes les vérifications passent (les étapes 1, 2 et 3)
- `1` : au moins une vérification échoue
- `2` : le harnais n'a pas pu tourner

Lancez-le une fois avant toute modification, pour voir votre point de départ : HA14 et HA15 sont rouges, c'est
l'exercice. Ne modifiez rien sous `harness/` pour faire passer une vérification. Si une règle vous semble fausse, notez
pourquoi et signalez-le à l'animateur de l'atelier.

Pour le jeu, lancez aussi `./harness.sh 3 --json rapport.json` : c'est ce rapport que l'arbitre compare entre votre
branche et la branche de base.

Le test `CatalogServiceTest` décrit ce que le catalogue affiche : lancez `gradle test` à chaque pas du refactoring de
`catalogLabels`, c'est votre filet. Les fabriques en bas du fichier sont les seuls endroits qui savent construire un
produit.

## 3. Travailler dans cet ordre

Un baby step = une PR = une règle qui passe de rouge à vert (voir la section « The game » de
[CUPID-step-1.md](CUPID-step-1.md) : chaque PR est un draft qui vise `java-bdxio-phase3` et n'est jamais fusionné ;
chaque branche part du baby step précédent).

1. **HA14 : collections en pipeline.** Remplacez la boucle de `CatalogService.catalogLabels` par un `Stream`
   (`filter`, `map`, `toList`). Plus de liste déclarée hors du pipeline que l'on remplit.
2. **HA15 : la fabrique valide par composition.** Réécrivez la fabrique d'un value object (par exemple celle de
   `Perishable`) comme une chaîne d'étapes : plus de `if`, de ternaire, de `switch`, de `throw` ni de
   `requireNonNull`, ni dans la fabrique ni dans un helper privé qu'elle appelle.

Lancez `./harness.sh 3` après chaque étape. Un petit changement qui fait passer une règle de FAIL à PASS vaut mieux
qu'un gros refactoring que vous ne pouvez pas vérifier.

## 4. Règle par règle

Le texte des règles dans [docs/CUPID-step-3.md](docs/CUPID-step-3.md) fait autorité ; ce qui suit n'est qu'une aide à
la lecture.

### HA14 - collections en pipeline

Le harnais signale une boucle (`for`, for-each, `while`, `do`, ou `forEach`/`forEachOrdered`/`peek`) qui modifie une
variable déclarée hors d'elle : `add`, `put`, `append`, une affectation, un `+=`, un `++`. Une boucle qui ne fait que
lire ou déléguer n'est pas visée. Un pipeline qui se termine par `toList()`, `collect(...)` ou `reduce(...)` n'a pas
d'accumulateur à surveiller.

### HA15 - la fabrique valide par composition

Une fabrique est une méthode statique d'un value object (un record qui n'est pas une entité) dont le nom contient
`create`, `build`, `of`, `from` ou `make`, ou dont le type de retour nomme le record. Le harnais lit son corps et
celui des méthodes du même type qu'elle appelle :

- il signale un `if`, un ternaire, un `switch`, un `throw`, un `assert`, un appel de garde (`requireNonNull`,
  `checkArgument`...) ;
- il signale une fabrique dont le résultat peut échouer (`Option`, `Either`, `Validation`, ou une ADT scellée) mais qui
  ne contient aucune lambda ni référence de méthode : elle ne décide rien ;
- la règle est rouge tant qu'aucun value object n'a de fabrique à résultat faillible, comme HA4.4.

## 5. Lire un échec

Une ligne d'échec nomme la règle, la classe et le numéro de ligne quand le harnais peut le retrouver. Commencez par la
première règle en échec et corrigez cette classe avant la suivante.

Si un résultat vous surprend, relisez la règle dans [docs/CUPID-step-3.md](docs/CUPID-step-3.md) avant de modifier le
code. Si le code est correct pour le domaine et que le harnais proteste quand même, notez le cas et signalez-le à
l'animateur plutôt que de contourner la règle.

## 6. Terminé

L'étape 3 est terminée quand `./harness.sh 3` sort avec le code `0`. Avant de considérer l'étape comme finie :

- le harnais sort avec `0` sur votre machine, après une compilation propre, et `gradle test` est vert ;
- vous savez expliquer ce que gagne la fabrique à être une chaîne d'étapes plutôt qu'un `if` (ajouter une condition
  ajoute une étape, sans changer la forme du code) ;
- chaque règle que vous avez fait passer l'a été dans sa propre PR, avec son titre `HAx green: ...`.

Gardez la sortie de votre dernière exécution : c'est la preuve que l'animateur demandera.
