# Workshop CUPID-AI assisted

Un atelier pour transformer une base de code Java / Spring Boot legacy en un code moderne, lisible et aligné sur le
domaine, pas à pas, avec un harnais qui dit objectivement si chaque étape est réussie.

Le détail de chaque cadre est dans [docs/Workshop.md](docs/Workshop.md). Ce README donne la vue d'ensemble, et
indique par où commencer.

## Le point de départ

On part d'une base de code Java / Spring Boot : un début de clean architecture, mais pas de découpage en Bounded
Context. Résultat : une **big ball of mud**. Les classes sont anémiques, certaines portent plusieurs responsabilités,
les identifiants sont des `String` ou des `UUID` nus, et le vocabulaire du métier n'apparaît pas dans le code.

L'objectif est de passer **from SOLID to Cute DDD** : refactoriser ce legacy vers un modèle qui exprime le métier.

## Les principes

L'atelier s'appuie sur trois cadres complémentaires. Ils répondent à trois questions différentes.

| Cadre | Question | Ce qu'il apporte |
|---|---|---|
| **CUTE DDD** | *Quoi* construire ? | Principes stratégiques et tactiques : Contextual, Ubiquitous, Testable, Expressive |
| **CUPID** | *Comment* le coder ? | Propriétés du code, pour un code agréable à faire évoluer : Composable, Unix philosophy, Predictable, Idiomatic, Domain-based |
| **Model Exploration Whirlpool** | *Comment* avancer ? | Un processus collaboratif et itératif : harvester, modéliser, tester le modèle par le code, recommencer |

### CUTE DDD : le domaine d'abord

- **Contextual** : chaque décision découle du contexte métier ; la structure du code suit les Bounded Contexts.
- **Ubiquitous** : le Ubiquitous Language est le vocabulaire commun aux experts et aux développeurs. Il doit se
  retrouver dans les noms de classes, de méthodes et de packages.
- **Testable** : le code se teste à tous les niveaux, unitaire, intégration, acceptance. La testabilité se conçoit dès
  le départ.
- **Expressive** : le code exprime l'intention métier. Un nom doit révéler ce que fait le code, pas comment il le fait.

### CUPID : un code agréable à faire évoluer

CUPID décrit des propriétés vers lesquelles tendre, pas une liste de règles à cocher. Ce sont des *centred sets* :
on se demande si le code va dans le bon sens, pas s'il réussit un test.

- **Composable** : de petits éléments qui se combinent facilement (agrégats, value objects, événements).
- **Unix philosophy** : une classe fait une chose bien, et a une seule raison de changer.
- **Predictable** : le code fait ce qu'on attend, sans effet de bord surprenant ni état invalide.
- **Idiomatic** : le code suit les idiomes du langage et du framework (records, pattern matching, etc.).
- **Domain-based** : le modèle du code reflète le problème métier, dans sa langue et sa structure.

Pour l'atelier, CUTE dit *quoi* viser, et CUPID dit *comment* y arriver.

### Model Exploration Whirlpool : un processus en cycles

Le Whirlpool d'Eric Evans n'est pas un processus de développement, mais un processus de modélisation qui s'intègre à
n'importe quel processus. Il tourne en boucle : **harvester** le savoir implicite, **modéliser** plusieurs variantes,
**coder** une sonde (*code probe*) pour éprouver le modèle, puis **réfléchir** et recommencer.

## Le jeu : un harnais déterministe et des baby steps

Le cœur de l'atelier est un **harnais** : un programme qui évalue le code sans IA. Même code en entrée, même verdict
en sortie. Il analyse les sources avec tree-sitter et le bytecode avec ArchUnit.

- Le code de départ est **rouge** : le harnais échoue sur plusieurs règles, et c'est voulu.
- Chaque règle s'appelle `HAx` et porte une intention (pas de god class, pas d'héritage, identifiants riches, etc.).
- Un **baby step** fait passer **une seule** règle du rouge au vert, dans une pull request (PR) **en brouillon**.
- Un **arbitre** (une GitHub Action, le check `harness-game`) lance le harnais sur chaque PR, commente un tableau de
  score et met à jour le classement.
- On ne fusionne jamais dans une branche de phase : ces branches restent le point de départ commun à toutes les
  équipes. Les baby steps s'enchaînent sur les branches de chaque équipe.

Le harnais ne dit pas *comment* corriger. Il dit *si* la règle est respectée, et les règles guident la refonte.

## Les trois branches de démarrage

L'atelier se joue en **trois phases**. Chaque phase a sa propre branche, son propre code de départ et son propre
harnais. Les phases sont cumulatives : la phase 2 reprend les règles de la phase 1, la phase 3 celles de la phase 2.

| Phase | Branche | Thème | Règles | Énoncé | Guide en autonomie |
|---|---|---|---|---|---|
| 1 | [`java-bdxio-phase1`](https://github.com/guillaumeagile/omni-product/tree/java-bdxio-phase1) | Unix philosophy : une classe, une raison de changer | HA5 à HA12.1 | [CUPID-step-1.md](https://github.com/guillaumeagile/omni-product/blob/java-bdxio-phase1/docs/CUPID-step-1.md) | [CUPID-step-1-guide.md](https://github.com/guillaumeagile/omni-product/blob/java-bdxio-phase1/CUPID-step-1-guide.md) |
| 2 | [`java-bdxio-phase2`](https://github.com/guillaumeagile/omni-product/tree/java-bdxio-phase2) | Composable : value objects, ADT, composition plutôt qu'héritage | celles de la phase 1, plus HA2, HA3, HA4.1 à HA4.9 | [CUPID-step1-2.md](https://github.com/guillaumeagile/omni-product/blob/java-bdxio-phase2/docs/CUPID-step1-2.md) | [CUPID-step-2-guide.md](https://github.com/guillaumeagile/omni-product/blob/java-bdxio-phase2/CUPID-step-2-guide.md) |
| 3 | [`java-bdxio-phase3`](https://github.com/guillaumeagile/omni-product/tree/java-bdxio-phase3) | Composable, style fonctionnel : pipelines et fabriques qui composent | celles de la phase 2, plus HA14 et HA15 (le 13 est sauté) | [CUPID-step-3.md](https://github.com/guillaumeagile/omni-product/blob/java-bdxio-phase3/docs/CUPID-step-3.md) | [CUPID-step-3-guide.md](https://github.com/guillaumeagile/omni-product/blob/java-bdxio-phase3/CUPID-step-3-guide.md) |

**Par où commencer** : choisissez la phase à jouer, puis partez de sa branche.

```bash
git switch -c team-<équipe>/step-1/ha9-identifiable origin/java-bdxio-phase1
```

Les énoncés des phases 2 et 3 n'existent que sur leur branche : leurs liens pointent donc vers GitHub.

Le code à modifier est dans `src/main/java/com/omniproduct/model` et `src/main/java/com/omniproduct/service`. Ne
touchez ni à `domain/`, ni à `infrastructure/`, ni à `harness/`.

## Lancer le harnais

Il faut un JDK 21 et Gradle. Le harnais se lance depuis la racine du dépôt :

| Commande | Effet |
|---|---|
| `./harness.sh` | règles de l'étape en cours |
| `./harness.sh all` | toutes les règles, comme l'arbitre |
| `./harness.sh all --json rapport.json` | le même, avec un rapport JSON |

Codes de sortie : `0` toutes les règles passent, `1` au moins une échoue, `2` le harnais n'a pas pu tourner (par
exemple, le projet ne compile pas). Une ligne `[WARN]` ne fait jamais échouer le harnais, mais elle compte au jeu.

Lancez le harnais **avant** de modifier quoi que ce soit : c'est votre point de départ.

## Pour aller plus loin

- [docs/Workshop.md](docs/Workshop.md) : le cadre complet (CUTE DDD, CUPID, Whirlpool, plan d'atelier, critères de
  réussite).
- [docs/harness-rules.md](docs/harness-rules.md) : l'inventaire des règles du harnais.
- [docs/PLAN-harness.md](docs/PLAN-harness.md) : le plan de construction du harnais et de l'atelier.
- [slides/](slides/README.md) : les slides de la présentation (reveal.js).

> **Mantra de l'atelier** : *harvester l'implicite, le rendre explicite. Modéliser de plusieurs façons, éprouver par
> le code. Continuer à tourner vers une compréhension plus profonde.*
