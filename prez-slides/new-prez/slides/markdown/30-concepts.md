# C2 · Concepts

Trois cadres, une direction

--

## SOLID — le rappel et la limite

|       |                                               |
|-------|-----------------------------------------------|
| **S** | une seule raison de changer                   |
| **O** | ouvert à l'extension, fermé à la modification |
| **L** | les sous-types sont substituables             |
| **I** | des interfaces petites et ciblées             |
| **D** | dépendre d'abstractions                       |

Sur ce code : **SRP / OCP / DIP violés**, LSP / ISP muets. SOLID dit *ce qui est cassé*, pas *où aller*.

---

## CUPID — des propriétés, pas des règles

- **C**omposable — joue bien avec les autres
- **U**nix philosophy — fait une chose, bien
- **P**redictable — fait ce à quoi on s'attend
- **I**diomatic — naturel dans le langage
- **D**omain-based — le code parle le métier

--

**Composable** — en TypeScript

- chaque unité est une fonction totale `A -> B` (elle renvoie toujours une valeur) ;
- l'absence de résultat et l'échec sont des _valeurs_ : `Option<T>`, `Result<T, E>`
- on enchaîne avec `map` / `andThen` / `pipe`, de gauche à droite — pas de `throw` caché, pas de court-circuit sur
  `null`
- _transparence référentielle_ : on peut remplacer un appel par son résultat sans changer le programme

--

**Unix philosophy** — fait une chose, et le fait bien

- une fonction de décision reçoit des données déjà chargées et renvoie un `Result` ;
- elle ne va rien chercher, ne journalise pas, ne persiste pas

--

**Predictable** — fait ce à quoi on s'attend

- aucune fonction ne `throw` pour un cas _attendu_ ;
- mêmes causes, mêmes effets : garder le cœur pur sans horloge, sans aléatoire, sans lecture en base

--

**Idiomatic** — naturel dans le langage

- utiliser d'abord l'algèbre de TypeScript : unions discriminées, `readonly`, scalaires _branded_, `switch` exhaustif,
  `Omit` / `Pick` / types mappés
- se standardiser sur **`neverthrow`** : `ok` / `err` / `map` / `andThen` / `match` est le _seul_ dialecte fonctionnel
  du repo

--

**Domain-based** — parle du problème, pas de la tuyauterie

- `Money`, `ProductName`, `Quantity` sont des types, pas des primitives ; un cycle de vie est une machine à états, pas
  des booléens
- le code métier n'importe jamais `BadRequestException` ni les types Prisma

---

## CUTE DDD — le cap stratégique

- **C**ontextual · **U**biquitous · **T**estable · **E**xpressive

.

```
SOLID          →   CUPID          →   CUTE DDD
excellence         `code joyeux`        valeur métier
académique
```

---

## hands-on lab

1. **Toujours valide /always valid**
   - constructeur privé
   - factory statique
   - `Result<T, DomainError>`
   - pas d'exceptions
2. **Domaine pur** — zéro import `@nestjs/*` ou `@prisma/client` sous `domain/`
3. **Frontière** — un BC n'importe d'un autre que ses **types d'événements**

---

## L'événement est le contrat

Transformer **1 God table** en **3 Bounded Contexts** :

```
Procurement  ──(event)──▶  Inventory  ──(event)──▶  Catalog
                                 │
                          Pricing (Shared Kernel)
```

Les BC collaborent en **énonçant des faits**, pas en s'appelant. Outil : **EventStorming** + **context map**.

---

## Quel est notre filet de sécurité ?

- **Tests d'exemple** — le cas nominal et les bords
- **Property-based tests** — l'invariant tient pour *toute* entrée
- **Mutation testing** — « si le code était subtilement faux, un test le verrait-il ? »

Note:
Un mutant qui survit n'est pas toujours un bug — mais souvent un trou de test, parfois un bug de modélisation déguisé.
On en verra un en Part 1.
