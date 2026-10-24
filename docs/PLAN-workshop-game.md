# Plan : résultats sealed, baby steps vérifiés en PR, atelier en forme de jeu

Branche de travail : `java-bdxio-phase2` (pas de PR vers `main`). Les PR de l'atelier sont celles des étudiants ;
les nôtres ne servent qu'à tester le principe et seront effacées.

## Décisions prises

- **ADT et résultats : `sealed interface` + records**, en Java natif. Le `switch` exhaustif sur un type scellé est
  l'équivalent de `OneOf.Match`, vérifié par le compilateur. Les échecs ont un nom métier
  (`sealed interface SaleDecision permits Sellable, PastSellByDate`), pas un `Either<String, T>`.
- **Vavr est un atout de la phase 3**, pas une norme : chaînage `map`/`flatMap` des résultats, avec ses propres règles
  (`fold` obligatoire, pas de `get()`).
- **Un baby step = une PR = une règle HAx qui passe au vert.** Une GitHub Action le vérifie à partir de la sortie JSON
  du harnais (pas en lisant le log texte, trop fragile), puis commente la PR avec un tableau de score.

## Lot 1 : résultats sealed dans le harnais

| Règle | Changement |
|---|---|
| HA4.4 | La fabrique `create`/`build`/`of`/`from`/`make` d'un value object renvoie un **type scellé** dont une alternative est le value object et une autre un record d'erreur (`sealed interface PerishableCreation permits Perishable, InvalidDates`). `Either` n'est plus attendu. |
| HA4.5 | En plus du `instanceof` interdit et du `switch` exhaustif exigé : un **cast** vers une alternative est interdit ; un appel qui renvoie un type scellé et dont le **résultat est ignoré** (`product.canSell(today);` en simple instruction) est signalé. |

Tests : contrat (bon / mauvais), évasions (cast qualifié, résultat ignoré dans un lambda), preuve par mutation sur le
legacy. Documents : `docs/harness-rules.md` (inventaire, heuristiques, limites).

## Lot 2 : sortie JSON du harnais

- `harness --step N --json <fichier>` écrit, en plus de la sortie texte inchangée, un rapport déterministe :

```json
{
  "step": "1",
  "result": "FAIL",
  "rules": [
    { "id": "HA9", "name": "...", "severity": "ERROR", "status": "FAIL", "violations": 2 },
    { "id": "HA12.1", "name": "...", "severity": "WARNING", "status": "WARN", "violations": 5 }
  ]
}
```

- `harness.sh` transmet l'option ; les codes de sortie 0 / 1 / 2 ne changent pas.
- Tests : le JSON est stable (règles dans l'ordre de `Steps`), il reflète les statuts de la sortie texte.

## Lot 3 : l'arbitre (comparaison base / PR)

Un outil `harness-referee` (Java, dans le module `harness/`) prend deux rapports JSON (base et tête de PR) et la cible
du baby step, et rend un verdict et un score.

**Cible du baby step** : lue dans le titre de la PR (`HA9 green: ...`) ou, à défaut, dans le nom de la branche
(`step-1/ha9-identifiable`). Sans cible, la PR est hors jeu (commentaire explicatif, pas d'échec).

**Contrôles du principe** (bloquants si on en fait un check obligatoire) :

1. la règle cible passe de `FAIL` à `PASS` (ou de `WARN` à `PASS` pour un avertissement) ;
2. aucune autre règle ne passe de `PASS` à `FAIL` (régression) ;
3. une seule règle passe au vert ; deux ou plus, ce n'est plus un baby step (avertissement, score réduit) ;
4. `harness/` n'est pas modifié (vérifié par l'Action sur la liste des fichiers changés) ;
5. le projet compile (un rapport absent, ou un code de sortie 2, est un échec).

**Score** (barème à valider, voir « Points ouverts ») :

| Événement | Points |
|---|---|
| Règle cible au vert | +10 |
| Règle « boss » (HA4.3, HA4.5) au vert | +20 |
| Violations en baisse sur une règle encore rouge | +1 par violation retirée, plafonné à +5 |
| Avertissement résolu (HA12.1, HA4.7) | +3 |
| Une seule règle au vert dans la PR (vrai baby step) | +5 |
| Petit diff (moins de N lignes hors tests) | +3 |
| Régression (PASS -> FAIL) | -5 par règle |
| `harness/` modifié | PR rejetée, 0 point |

**Niveaux** par étape, d'après le nombre de règles au vert (`Unix Apprentice` 3/9, `Single Concern Adept` 6/9,
`CUPID Master` 9/9...). **Badges** : `First green`, `No regression streak` (3 PR sans régression),
`Smallest diff`, `Compiler-checked` (premier `switch` exhaustif sur une ADT).

Tests : chaque contrôle et chaque ligne du barème sur des paires de rapports JSON écrites à la main.

## Lot 4 : la GitHub Action

`.github/workflows/harness-game.yml`, déclenchée sur `pull_request` :

1. JDK 21 + Gradle (le `gradle-wrapper.jar` n'est pas versionné : `gradle/actions/setup-gradle` avec une version fixe) ;
2. harnais sur la **base** de la PR, puis sur sa **tête**, chacun avec `--json` ;
3. `harness-referee` compare et produit le commentaire Markdown (tableau de score, niveau, badges) ;
4. commentaire posté ou mis à jour sur la PR (`gh pr comment --edit-last`), statut du check selon les contrôles ;
5. rapports JSON publiés comme artefacts du job (traçabilité).

Risque à lever en premier : les bindings tree-sitter (code natif) sur le runner Linux x64.

**Où jouent les étudiants** : une équipe = un préfixe de branche dans le dépôt de l'atelier (`team-red/step-1/ha9-...`),
les étudiants étant collaborateurs. Les PR internes au dépôt ont un jeton qui peut commenter. Les PR depuis des forks
ont un jeton en lecture seule (`pull_request`), et `pull_request_target` exécuterait leur code avec des droits en
écriture : à éviter.

## Lot 5 : classement

Une issue épinglée « Leaderboard » mise à jour par l'Action après chaque PR fusionnée : score cumulé par équipe,
niveau, badges. Le score d'une équipe est recalculé à partir des commentaires de l'arbitre (pas d'état caché).

## Lot 6 : textes étudiants

- `docs/CUPID-step-1.md` : section « How to work » (fait), plus les règles du jeu (titre de PR, cible, barème, badges).
- `docs/CUPID-step1-2.md` : version Java du texte (aujourd'hui texte C#) : sealed au lieu de `OneOf`, `switch` au lieu
  de `Match`, `./harness.sh 2`, mêmes règles du jeu.
- `docs/Workshop.md` : déroulé de l'atelier en PR et jeu.

## Lot 7 : essai du principe

Deux ou trois PR de test sur une branche d'équipe jetable (une PR verte, une PR avec régression, une PR qui touche
`harness/`) pour voir les commentaires et les statuts, puis suppression des branches et des PR.

## Phase 3 : Vavr

- Chaînage des résultats avec `Either`/`Option` de Vavr (`map`, `flatMap`) sur des alternatives scellées.
- Règles propres : un `Either` se consomme par `fold` ; `get()`, `getLeft()`, `isLeft()`, `isRight()`,
  `getOrElseThrow()` interdits dans le modèle.
- HA4.4 accepte alors aussi `Either<Erreur, Self>`.

## Points ouverts (à trancher par l'animateur)

- Le barème et les seuils (N lignes pour un « petit diff », niveaux).
- Le contrôle de l'arbitre est-il un check **obligatoire** (la PR ne peut pas être fusionnée) ou seulement un commentaire ?
- Classement : issue épinglée, ou page partagée.

## Ordre

Lot 1 -> Lot 2 -> Lot 3 -> Lot 4 -> Lot 7 -> Lot 5 -> Lot 6. Phase 3 après l'atelier de l'étape 2.
