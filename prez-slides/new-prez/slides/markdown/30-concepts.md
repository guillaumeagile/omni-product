# C2 · Concepts

Trois cadres, une direction

--

## SOLID — le rappel et la limite

| | |
|---|---|
| **S** | une seule raison de changer |
| **O** | ouvert à l'extension, fermé à la modification |
| **L** | les sous-types sont substituables |
| **I** | des interfaces petites et ciblées |
| **D** | dépendre d'abstractions |

Sur ce code : **SRP / OCP / DIP violés**, LSP / ISP muets.
SOLID dit *ce qui est cassé*, pas *où aller*.

---

## CUPID — des propriétés, pas des règles

- **C**omposable — joue bien avec les autres
- **U**nix philosophy — fait une chose, bien
- **P**redictable — fait ce à quoi on s'attend
- **I**diomatic — naturel dans le langage
- **D**omain-based — le code parle le métier

> Des *directions de voyage*, pas des cases à cocher.

---

## CUTE DDD — le cap stratégique

- **C**ontextual · **U**biquitous · **T**estable · **E**xpressive

La chaîne :

```
SOLID          →   CUPID          →   CUTE DDD
excellence         code joyeux        valeur métier
technique
```

CUTE dit *quoi* construire ; CUPID dit *comment*.

---

## Les règles du jeu du lab

1. **Toujours valide** — constructeur privé, factory statique
   qui renvoie `Result<T, DomainError>` ; pas d'exception pour un échec métier attendu
2. **Domaine pur** — zéro import `@nestjs/*` ou `@prisma/client` sous `domain/`
3. **Frontière** — un BC n'importe d'un autre que ses **types d'événements**

---

## L'événement est le contrat

Transformer **1 God table** en **3 Bounded Contexts** :

```
Procurement  ──StockReceived──▶  Inventory  ──StockDepleted──▶  Catalog
                                     │
                              Pricing (Shared Kernel)
```

Les BC collaborent en **énonçant des faits**, pas en s'appelant.
Outil : **EventStorming** + **context map**.

---

## Quel est notre filet de sécurité ?

- **Tests d'exemple** — le cas nominal et les bords
- **Property-based tests** — l'invariant tient pour *toute* entrée
- **Mutation testing** — « si le code était subtilement faux,
  un test le verrait-il ? »

Note:
Un mutant qui survit n'est pas toujours un bug — mais souvent un trou de test,
parfois un bug de modélisation déguisé. On en verra un en Part 1.
