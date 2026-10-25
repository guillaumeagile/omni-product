# Workshop CUPID-AI assisted

Toute la documentation est dans [docs](docs).

On part d'une base de code Java / Spring Boot : un début de clean archi, mais pas de découpage en Bounded Context, donc
une big ball of mud.

Objectif : refactoriser du legacy vers du moderne, from SOLID to Cute DDD.

1. respecter le Ubiquitous Language et qu'il s'applique au code
3. séparer les Bounded Context (phase 1)
4. voulez-vous moderniser le code ? CUPID is the way ! (phase 1 & 2)

## Les deux phases

L'atelier CUPID se joue en deux phases. Chaque phase vit sur sa propre branche, avec son propre code de départ et son
propre harnais :

| Phase | Branche | Thème | Règles | Énoncé | Guide en autonomie |
|---|---|---|---|---|---|
| 1 | `java-bdxio-phase1` | Unix philosophy : une classe, une raison de changer | HA5 à HA12.1 | [CUPID-step-1.md](docs/CUPID-step-1.md) | [CUPID-step-1-guide.md](CUPID-step-1-guide.md) |
| 2 | `java-bdxio-phase2` | Composable : value objects, ADT, composition plutôt qu'héritage | celles de la phase 1, plus HA2, HA3, HA4.1 à HA4.7 | [CUPID-step1-2.md](https://github.com/guillaumeagile/omni-product/blob/java-bdxio-phase2/docs/CUPID-step1-2.md) | [CUPID-step-2-guide.md](https://github.com/guillaumeagile/omni-product/blob/java-bdxio-phase2/CUPID-step-2-guide.md) |

Les fichiers de la phase 2 n'existent que sur sa branche : les liens de la phase 2 pointent donc vers GitHub.

Le code à modifier est dans `src/main/java/com/omniproduct/model` et `src/main/java/com/omniproduct/service`. Ne
touchez ni à `domain/`, ni à `infrastructure/`, ni à `harness/`.

## Le harnais

Le harnais est déterministe : le même code donne toujours le même verdict. Il analyse les sources avec tree-sitter et
le bytecode avec ArchUnit, sans IA. Il faut un JDK 21 et Gradle.

| Commande | Phase 1 | Phase 2 |
|---|---|---|
| règles de l'étape en cours | `./harness.sh 1` (ou `./harness.sh`) | `./harness.sh 2` (lance aussi les règles de l'étape 1) |
| toutes les règles, comme l'arbitre | `./harness.sh all` | `./harness.sh all` |
| rapport JSON en plus de la sortie texte | `./harness.sh all --json rapport.json` | `./harness.sh all --json rapport.json` |

Codes de sortie : `0` toutes les règles passent, `1` au moins une échoue, `2` le harnais n'a pas pu tourner (par
exemple, le projet ne compile pas). Une ligne `[WARN]` ne fait jamais échouer le harnais, mais elle compte au jeu.

Lancez le harnais une fois avant de modifier quoi que ce soit : c'est votre point de départ. Sur le legacy, il est
rouge, et c'est voulu.

## Le jeu : une PR par baby step

Chaque baby step est une pull request (PR) qui fait passer **une seule** règle du rouge au vert. Chaque PR passe
devant **l'arbitre CUPID** : une GitHub Action (le check `harness-game`) qui lance le harnais sur la base de la PR, puis
sur la PR elle-même. L'arbitre compare les deux résultats, commente la PR avec un tableau de score et met le check au
vert ou au rouge.

### 1. Préparer la branche

1. **Partez de la branche de la phase** (`java-bdxio-phase1` ou `java-bdxio-phase2`). Travaillez dans le dépôt de
   l'atelier, pas dans un fork : depuis un fork, l'arbitre ne peut pas commenter, sauf si vous l'autorisez dans votre fork.
2. **Nommez la branche** `team-<équipe>/step-<N>/ha<règle>-<quoi>`. Le premier segment est le nom de votre équipe au
   classement.
   - phase 1 : `team-red/step-1/ha9-identifiable`
   - phase 2 : `team-red/step-2/ha4-3-shelf-life`
3. **Choisissez une ligne `[FAIL]`** du harnais. Corrigez cette règle, et seulement elle. A priori, faites dans l'ordre
   de numérotation des règles.
4. **Lancez `./harness.sh all`** en local avant de pousser. L'arbitre utilise le même harnais.

```
git switch java-bdxio-phase1
git switch -c team-red/step-1/ha9-identifiable
# ... le refactoring ...
./harness.sh all
git commit -am "HA9 green: Product and Supplier implement Identifiable"
git push -u origin team-red/step-1/ha9-identifiable
```

### 2. Créer la PR, vers la bonne base

La **base** d'une PR est la branche dans laquelle elle serait fusionnée. GitHub propose `main` par défaut, et c'est le
piège : `main` n'a pas d'arbitre, la PR est alors « hors jeu ».

- **Première PR de la phase** : base `java-bdxio-phase1` (ou `java-bdxio-phase2` en phase 2).
- **PR suivantes** : base = la branche de votre baby step précédent. Les PR s'empilent, et chaque diff ne montre
  qu'une étape.

Sur GitHub, après le push : bouton **Compare & pull request**, puis, dans la liste **base:** en haut à gauche,
choisissez `java-bdxio-phase1` avant de cliquer sur **Create pull request**. En ligne de commande :

```
gh pr create --base java-bdxio-phase1 --title "HA9 green: Product and Supplier implement Identifiable"
```

**Le titre nomme la règle cible** (`HA9 green: ...`). Sans cible dans le titre ni dans la branche, la PR est hors jeu.
Dans la description, collez la sortie du harnais avant et après.

Vous vous êtes trompé de base ? Bouton **Edit** à côté du titre de la PR, changez la base : l'arbitre repasse tout
seul.

### 3. Lire le verdict de l'arbitre

Une minute ou deux après l'ouverture de la PR, l'arbitre commente la PR :

```
🎮 Arbitre CUPID — HA5
✅ Baby step accepted
- Tokens in model/ and service/: 2638 → 3141 (+503).
- Cyclomatic complexity of model/ and service/: 81 → 101 (+20).

| Rule    | Base         | This PR      | Points |
| HA5 🎯  | 🔴 FAIL (2)  | 🟢 PASS      | +10    |
| HA6     | 🔴 FAIL (8)  | 🔴 FAIL (6)  |        |
| HA8     | 🔴 FAIL (30) | 🔴 FAIL (40) |        |
| HA9     | 🔴 FAIL (5)  | 🔴 FAIL (10) |        |
Score: +17 · Level: Unix Apprentice (1/8) · Badges: 🍼 Baby step, 🧼 No regression
```

L'arbitre écrit aussi le score du round en tête de la **description** de la PR (verdict, points, niveau, badges),
dans une section qu'il réécrit à chaque passage : ne la modifiez pas, le reste de la description vous appartient.

Le check est vert si la PR est acceptée, rouge sinon. Dans ce cas, poussez un correctif sur la même branche : l'arbitre
repasse et met son commentaire à jour. Le détail est aussi dans l'onglet **Checks** de la PR.

### 4. Relire le code : Files changed

Le score ne suffit pas. Ouvrez l'onglet **Files changed** de la PR et faites une **revue de code**, à la main ou avec
un agent, pour relier chaque ligne du verdict à un changement de code :

- **La règle cible** : quelle modification l'a fait passer au vert ? Est-ce un vrai découpage, ou seulement un
  renommage qui trompe la règle ?
- **Les autres règles** : pourquoi le nombre de violations bouge-t-il ? Dans l'exemple, HA5 passe au vert, mais HA8
  passe de 30 à 40 violations et HA9 de 5 à 10 : les nouvelles classes `@Embeddable` portent des annotations JPA et
  n'implémentent pas `Identifiable`. Une règle déjà rouge qui empire ne coûte pas de points, mais c'est du travail pour les PR suivantes.
- **Les tokens et la complexité** : +503 tokens et +20 de complexité, est-ce le prix normal d'un découpage, ou du code
  dupliqué ?
- **Les services** : la PR les a-t-elle touchés ? Fallait-il ?

Pour une revue agentique, donnez à l'agent le diff et le commentaire de l'arbitre, par exemple avec Claude Code :

```
gh pr diff <numéro> > pr.diff
gh pr view <numéro> --comments > verdict.txt
claude "Relis pr.diff. Pour chaque ligne du verdict de l'arbitre (verdict.txt), indique le changement de code qui
l'explique, et signale ce qui fait passer une règle sans corriger le défaut qu'elle vise."
```

Écrivez vos conclusions en commentaire de la PR avant de la fusionner : c'est là que se fait l'apprentissage.

### 5. Fusionner et monter au classement

Après chaque fusion, l'issue épinglée **CUPID workshop leaderboard** classe les équipes. La PR suivante part de la
branche que vous venez de terminer.

### Quand une PR est acceptée

Une PR est acceptée quand :

- la règle cible passe au vert ;
- aucune autre règle ne régresse ;
- le projet compile ;
- `harness/` n'est pas modifié. Une PR qui le modifie est rejetée avec 0 point.

### Barème

| Vous... | Points |
|---|---|
| faites passer la règle cible au vert | +10 |
| battez un boss (HA4.3 ADT, HA4.5 switch exhaustif, phase 2) | +20 |
| résolvez un avertissement (`[WARN]` : HA12.1, HA4.7) | +3 |
| retirez des violations d'une règle encore rouge | +1 par violation, jusqu'à +5 |
| ne faites passer qu'une seule règle au vert (un vrai baby step) | +5 |
| gardez un petit diff (400 tokens modifiés au plus dans `src/main/java`) | +3 |
| retirez des tokens de `model/` et `service/` | +1 par 100 tokens, jusqu'à +5 |
| retirez de la complexité cyclomatique de `model/` et `service/` | +1 par 2 points, jusqu'à +10 |
| améliorez les mesures CK de `model/` et `service/` (couplage, cohésion, mutabilité, style) | bonus sans plafond total, détail ci-dessous |
| cassez une règle qui était verte | -5 par règle |

Le code se mesure en **tokens**, pas en lignes : un agent lit et écrit des tokens. Ils sont comptés par
[JTokkit](https://github.com/knuddels/jtokkit) (encodage `o200k_base`). La complexité cyclomatique est mesurée par
[PMD](https://pmd.github.io/pmd/pmd_rules_java_design.html#cyclomaticcomplexity). Une hausse de complexité est
affichée mais jamais pénalisée : un `switch` exhaustif ajoute une branche par cas.

Les mesures [CK](https://github.com/mauricioaniche/ck) sont détaillées dans un bloc repliable du commentaire de
l'arbitre. Une mesure qui s'améliore rapporte des points ; une mesure qui se dégrade ne coûte rien, mais regardez
pourquoi dans **Files changed**. Le nombre de classes est affiché sans points : découper n'est pas un but en soi.

| Mesure CK | Points |
|---|---|
| CBO total (couplage entre classes) | +1 par 5 retirés, jusqu'à +3 |
| CBO max (la classe la plus couplée) | +1 par 2 retirés, jusqu'à +2 |
| WMC max (la classe la plus complexe) | +1 par 3 retirés, jusqu'à +3 |
| LCOM* max (manque de cohésion, de 0 à 1) | +1 par 0,1 retiré, jusqu'à +2 |
| champs mutables (non `final`) | +1 par 2 retirés, jusqu'à +3 |
| affectations | +1 par 5 retirées, jusqu'à +2 |
| boucles | +1 par boucle retirée, jusqu'à +2 |
| lambdas | +1 par 2 ajoutées, jusqu'à +1 |
| imbrication max | +1 par niveau retiré, jusqu'à +2 |

CK ne voit pas les records (limite de CK 0.7.0) : en phase 2, un record n'est pas mesuré.

### Niveaux et badges

Le niveau dépend de la part des règles bloquantes (hors `[WARN]`) au vert dans votre PR :

| Niveau | Règles bloquantes au vert |
|---|---|
| Legacy Survivor | aucune |
| Unix Apprentice | moins d'un tiers |
| Single Concern Adept | moins des deux tiers |
| Composable Craftsman | au moins les deux tiers, mais pas toutes |
| CUPID Master | toutes |

| Badge | Obtenu quand |
|---|---|
| 🍼 Baby step | une seule règle passe au vert |
| 🧼 No regression | la PR est acceptée sans régression |
| 🤏 Small diff | le diff fait 400 tokens ou moins |
| 👑 Boss defeated | HA4.3 ou HA4.5 passe au vert (phase 2) |
| 🧠 Compiler-checked | HA4.5 passe au vert (phase 2) |

### Règles du jeu

- Ne modifiez jamais `harness/` pour faire passer une règle. Si une règle vous semble fausse, notez pourquoi et
  signalez-le à l'animateur.
- Un échec que vous provoquez en corrigeant autre chose se dit dans la PR : ne le cachez pas.
- Le diff de chaque PR est la leçon : regardez ce que chaque règle coûte en code.
