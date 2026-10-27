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
| HA4.4 | La fabrique `create`/`build`/`of`/`from`/`make` d'un value object renvoie un **type scellé** dont une alternative est le value object et une autre un record d'erreur (`sealed interface PerishableCreation permits Perishable, InvalidDates`), ou une ADT de Vavr (`Either`, `Validation`, `Try`, `Option`) dont le succès est le value object. |
| HA4.5 | En plus du `instanceof` interdit et du `switch` exhaustif exigé : un **cast** vers une alternative est interdit ; un appel qui renvoie un type scellé et dont le **résultat est ignoré** (`product.canSell(today);` en simple instruction) est signalé. |

Tests : contrat (bon / mauvais), évasions (cast qualifié, résultat ignoré dans un lambda), preuve par mutation sur le
legacy. Documents : `docs/harness-rules.md` (inventaire, heuristiques, limites).

## Lot 2 : sortie JSON du harnais

- `harness --phase N --json <fichier>` écrit, en plus de la sortie texte inchangée, un rapport déterministe :

```json
{
  "phase": "1",
  "result": "FAIL",
  "rules": [
    { "id": "HA9", "name": "...", "severity": "ERROR", "status": "FAIL", "violations": 2 },
    { "id": "HA12.1", "name": "...", "severity": "WARNING", "status": "WARN", "violations": 5 }
  ]
}
```

- `harness.sh` transmet l'option ; les codes de sortie 0 / 1 / 2 ne changent pas.
- Tests : le JSON est stable (règles dans l'ordre de `Phases`), il reflète les statuts de la sortie texte.

## Lot 3 : l'arbitre (comparaison base / PR)

Un outil `harness-referee` (Java, dans le module `harness/`) prend deux rapports JSON (base et tête de PR) et la cible
du baby step, et rend un verdict et un score.

**Cible du baby step** : lue dans le titre de la PR (`HA9 green: ...`) ou, à défaut, dans le nom de la branche
(`phase-1/ha9-identifiable`). Sans cible, la PR est hors jeu (commentaire explicatif, pas d'échec).

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
| Avertissement résolu (HA12.1) | +3 |
| Une seule règle au vert dans la PR (vrai baby step) | +5 |
| Petit diff (400 tokens modifiés au plus dans `src/main/java`) | +3 |
| Tokens retirés de `model/` et `service/` | +1 par 100 tokens, plafonné à +5 |
| Complexité cyclomatique retirée (PMD) | +1 par 2 points, plafonné à +10 ; une hausse est affichée, pas pénalisée |
| Mesures de qualité PMD améliorées (fan-out, WMC, TCC, champs mutables, affectations, boucles, lambdas, complexité cognitive ; pas le nombre de types ; CK affiché sans points) | plafond par mesure, pas de plafond total (bonus) ; une dégradation est affichée, pas pénalisée |
| Régression (PASS -> FAIL) | -5 par règle |
| `harness/` modifié | PR rejetée, 0 point |

**Niveaux** par phase, d'après le nombre de règles au vert (`Unix Apprentice` 3/9, `Single Concern Adept` 6/9,
`CUPID Master` 9/9...). **Badges** : `First green`, `No regression streak` (3 PR sans régression),
`Smallest diff`, `Compiler-checked` (premier `switch` exhaustif sur une ADT).

Tests : chaque contrôle et chaque ligne du barème sur des paires de rapports JSON écrites à la main.

## Lot 4 : la GitHub Action

`.github/workflows/harness-game.yml`, déclenchée sur `pull_request` :

1. JDK 21 + Gradle (le `gradle-wrapper.jar` n'est pas versionné : `gradle/actions/setup-gradle` avec une version fixe) ;
2. harnais sur la **base** de la PR (branche de la phase), sur la **branche précédente** de l'équipe (la branche du même
   préfixe dont la PR descend, au `merge-base`) et sur la **tête**, chacun avec `--json` ;
3. `harness-referee` note le round (branche précédente → tête), montre le chemin depuis la phase sans points, et produit le
   commentaire Markdown (tableau de score, niveau, badges) ;
4. un nouveau commentaire à chaque round (les précédents de l'arbitre sont masqués « outdated »), statut du check selon les contrôles ;
5. classement recalculé (voir lot 5) ;
6. rapports JSON publiés comme artefacts du job (traçabilité).

Risque à lever en premier : les bindings tree-sitter (code natif) sur le runner Linux x64.

**Où jouent les étudiants** : une équipe = un préfixe de branche dans le dépôt de l'atelier (`team-red/phase-1/ha9-...`),
les étudiants étant collaborateurs. Les PR internes au dépôt ont un jeton qui peut commenter. Les PR depuis des forks
ont un jeton en lecture seule (`pull_request`), et `pull_request_target` exécuterait leur code avec des droits en
écriture : à éviter.

## Lot 5 : classement

Rien n'est jamais fusionné : les PR restent en draft. Une issue épinglée par phase, « CUPID workshop leaderboard
(java-bdxio-phase1) », est mise à jour par `harness-game` après chaque round : score cumulé par équipe (somme du
dernier score de chaque PR ouverte, chaque round notant son delta avec la branche précédente), niveau, badges. Le score
d'une équipe est recalculé à partir des commentaires de l'arbitre (pas d'état caché).

## Lot 6 : textes étudiants

- `docs/CUPID-phase-1.md` : section « How to work » (fait), plus les règles du jeu (titre de PR, cible, barème, badges).
- `docs/CUPID-phase1-2.md` : version Java du texte (aujourd'hui texte C#) : sealed au lieu de `OneOf`, `switch` au lieu
  de `Match`, `./harness.sh 2`, mêmes règles du jeu.
- `Workshop-CUTE DDD-full day.md` : déroulé de l'atelier en PR et jeu.

## Lot 7 : essai du principe

Deux ou trois PR de test sur une branche d'équipe jetable (une PR verte, une PR avec régression, une PR qui touche
`harness/`) pour voir les commentaires et les statuts, puis suppression des branches et des PR.

## Phase 3 : Vavr

- Chaînage des résultats avec `Either`/`Option` de Vavr (`map`, `flatMap`) sur des alternatives scellées.
- Règles propres : un `Either` se consomme par `fold` ; `get()`, `getLeft()`, `isLeft()`, `isRight()`,
  `getOrElseThrow()` interdits dans le modèle.
- HA4.4 accepte déjà les ADT de Vavr (`Either<Erreur, Self>`, `Validation`, `Try`, `Option`) depuis la phase 2.

## Points ouverts (à trancher par l'animateur)

- Le barème est validé. Les seuils en tokens sont **calibrés** (voir « Calibrage des seuils en tokens » plus bas).
- Le contrôle de l'arbitre reste un check **obligatoire** (décision de l'animateur : il l'a toujours été).
- Classement : inchangé, issue épinglée (par équipe, avec le cumul).

## Ordre

Lot 1 -> Lot 2 -> Lot 3 -> Lot 4 -> Lot 7 -> Lot 5 -> Lot 6. Phase 3 après l'atelier de la phase 2.

## État

| Lot | État | Vérification |
|---|---|---|
| 1 résultats sealed | fait | tests du harnais + mutation sur le legacy (cast et résultat ignoré détectés) |
| 2 rapport JSON | fait | test : le JSON reflète la sortie texte de la phase 2 sur le legacy |
| 3 arbitre | fait | tests sur paires de rapports ; essai local base/tête (baby step HA3 simulé : +18) |
| 4 GitHub Action `harness-game` | écrite | phases rejouées en local ; **pas encore jouée sur GitHub** (lot 7) |
| 5 classement | dans `harness-game` | tests du calcul ; recalculé à chaque round sur les PR ouvertes (l'ancien `harness-leaderboard`, déclenché à la fusion, est retiré) |
| 6 textes étudiants | fait | `CUPID-phase-1.md` (règles du jeu), `CUPID-phase1-2.md` (déjà en Java : `sealed`, `switch`, `./harness.sh 2`), `CUPID-phase-3.md` et `Workshop-CUTE DDD-full day.md` |
| 7 essai du principe | à faire | PR de test à ouvrir puis supprimer |
| mesures CK au score | fait | essai sur PR #3 (+22, dont +5 CK) ; CK 0.7.0 ne voit pas les records (phase 2) |
| mesures tokens + PMD | fait | tokens par JTokkit (o200k_base), complexité par PMD ; rapport JSON (`metrics`), arbitre et Action ; tests sur valeurs de référence |

## Calibrage des seuils en tokens

Mesuré sur les branches de jeu `test-team/*` (diff `-U0` de `src/main/java` entre un baby step et le précédent, jetons
JTokkit `o200k_base`, comme l'Action) :

| Phase | Départ (tokens de `model/` + `service/`) | Tokens modifiés par baby step | Tokens retirés |
|---|---|---|---|
| 1 | 3 141 | 1 371 à 6 060 (médiane ≈ 5 000) | 968 à 2 696 (une règle ajoute 258) |
| 2 | 1 453 | 0 à 532 (7 règles sur 9 sous 200) | -408 à +208 |
| 3 | 1 779 | 206 (une seule règle mesurée, HA15) | 54 |

Avec 400 tokens et 100 par point, le petit diff est hors d'atteinte en phase 1 (aucune règle sous 1 371) et acquis presque
d'office en phase 2, et les tokens retirés plafonnent à +5 dès 500 tokens en phase 1 alors qu'ils ne dépassent pas +2 en
phase 2. Les seuils deviennent donc un réglage **par phase**, posé dans `harness-game.yml` de chaque branche de phase
(`SMALL_DIFF_TOKENS`, `TOKENS_PER_POINT`, transmis à l'arbitre par `--small-diff` et `--tokens-per-point`) :

| Phase | Petit diff | Tokens par point | Effet attendu |
|---|---|---|---|
| 1 | 2 500 | 400 | petit diff accessible aux règles ciblées (HA9 : 1 371), +3 à +5 pour les gros nettoyages |
| 2 | 200 | 50 | environ 2 règles sur 3 sous le seuil ; 208 tokens retirés valent +4 |
| 3 | 200 | 50 | un diff d'une page de code ; 54 tokens retirés valent +1 |

Posé **en phase 3 seulement** (cette branche) ; les valeurs des phases 1 et 2 restent à reporter dans leur workflow, et
à recalibrer sur les premières PR d'équipe réelles (l'échantillon de la phase 3 est d'une règle).
