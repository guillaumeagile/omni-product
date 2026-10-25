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

Chaque baby step est une pull request qui fait passer **une seule** règle du rouge au vert. Une GitHub Action (l'arbitre,
`harness-game`) lance le harnais sur la base de la PR puis sur la PR. Elle compare les deux rapports et commente la PR
avec un tableau de score.

### Mode d'emploi

1. **Partez de la branche de la phase** (`java-bdxio-phase1` ou `java-bdxio-phase2`). Travaillez dans le dépôt de
   l'atelier, pas dans un fork : depuis un fork, l'arbitre ne peut pas commenter, sauf si vous l'autorisez dans votre fork.
2. **Nommez la branche** `team-<équipe>/step-<N>/ha<règle>-<quoi>`. Le premier segment est le nom de votre équipe au
   classement.
   - phase 1 : `team-red/step-1/ha9-identifiable`
   - phase 2 : `team-red/step-2/ha4-3-shelf-life`
3. **Choisissez une ligne `[FAIL]`** du harnais. Corrigez cette règle, et seulement elle. A priori, faites dans l'ordre de numérotation des règles.
4. **Lancez `./harness.sh all`** en local avant de pousser. L'arbitre utilise le même harnais.
5. **Ouvrez la PR** avec la règle cible dans le titre : `HA9 green: Product and Supplier implement Identifiable`. Sans
   cible dans le titre ou dans la branche, la PR est hors jeu.
6. **Empilez les PR.** La PR suivante part de la branche précédente et la vise, pour que chaque diff montre une seule
   étape.
7. **Lisez le commentaire de l'arbitre.** Si le check échoue, poussez un correctif : le tableau de score se met à jour.
8. Après chaque fusion, l'issue épinglée **CUPID workshop leaderboard** classe les équipes.

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
| retirez de la complexité cyclomatique de `model/` et `service/` | +1 par 5 points, jusqu'à +5 |
| cassez une règle qui était verte | -5 par règle |

Le code se mesure en **tokens**, pas en lignes : un agent lit et écrit des tokens. Ils sont comptés par
[JTokkit](https://github.com/knuddels/jtokkit) (encodage `o200k_base`). La complexité cyclomatique est mesurée par
[PMD](https://pmd.github.io/pmd/pmd_rules_java_design.html#cyclomaticcomplexity). Une hausse de complexité est
affichée mais jamais pénalisée : un `switch` exhaustif ajoute une branche par cas.

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
