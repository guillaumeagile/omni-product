# Plan de présentation — « SOLID › CUPID › CUTE DDD » (atelier 2 h)

> Brouillon de structure, à ajuster avant écriture des slides MD (reveal.js).
> Source du déroulé : `docs/2h-workshop/` (Part 0 tour du legacy → Part 1 Value Objects /
> Pricing Shared Kernel → Part 2 événements entre 2 Bounded Contexts → gate + rétro CUPID).
> Ancienne prez (v1, 4 h, C#/FizzBuzz) : `prez-slides/old-prez/slides/` — visuels réutilisables.
>
> **Contrainte** : texte des slides en puces courtes. Le code va dans des blocs `code`, pas en prose.
> **Total visé** : ~44 slides / 120 min, dont ~85 min de hands-on (Blocs 3 et 4).

---

## Décisions ouvertes (à trancher avant le MD)

1. **Poids du bloc théorie (Bloc 1)** — court tel quel (~10 min / 6 slides) ? minimal (~4 min / 3 slides) ? développé
   façon v1 (un principe SOLID par slide + schémas, ~18 min / 10 slides) ?
2. **Part 2 — format** — une seule paire Inventory→Catalog (suit `phase2-cross-bc-events.md`, mode B handlers-only, tout
   le monde code la même chose) ? ou les 3 tracks au choix (suit
   `2h-tactical-hands-on.md`, chaque groupe son BC) ?
3. **Part 1 — objectif hands-on** — `Price` seul garanti pour tous, `VatRate` + `ResellerPriceCalculator`
   en bonus ? ou viser les 3 pour tout le monde ?
4. **Slides de code** — jusqu'où on montre le code réel du repo vs. pseudo-code resserré ?

---

## Bloc 0 — Ouverture (~3 min · 4 slides)

1. **Titre** — Guillaume & Sam · SOLID › CUPID › CUTE DDD · atelier 2 h · photo
2. **Sponsors / Forum 2025** — visuels réutilisés de la v1
3. **Objectifs** (3 puces max)
    - Passer d'un God-model anémique à des modèles DDD tactiques *always-valid*
    - Value Objects, agrégats, événements entre Bounded Contexts
    - Vérifier par tests unitaires + PBT + mutation testing
4. **Le code à suivre** — QR code repo + `pnpm test` / `pnpm test:mutation`

## Bloc 1 — Pourquoi on bouge : SOLID → CUPID → CUTE DDD (~10 min · 6 slides)

5. **SOLID en une slide** — les 5 principes, une ligne chacun (rappel, pas un cours)
6. **Là où SOLID s'arrête** — SRP / OCP / DIP violés dans `product.service.ts` ; LSP non applicable ; ISP absent
7. **CUPID : des propriétés, pas des règles** — Composable · Unix · Predictable · Idiomatic · Domain-based ; *centred
   sets* (direction de voyage, pas pass/fail)
8. **CUTE DDD** — Contextual · Ubiquitous · Testable · Expressive ; le *quoi* stratégique
9. **La chaîne** — SOLID (excellence technique) → CUPID (code joyeux) → CUTE DDD (valeur métier). CUTE guide le *quoi*,
   CUPID le *comment*
10. **Le fil rouge du jour** — 1 God table → 3 BC (Procurement / Inventory / Catalog) + Pricing en Shared Kernel ;
    schéma

## Bloc 2 — Part 0 : visite du legacy (~15 min · 5 slides)

11. **Le God `Product`** — `schema.prisma` : catalogue + stock + fournisseur + prix + entrepôt sur une ligne
12. **`ProductService` anémique** — Prisma + logique métier mélangés ;
    `calculateResellerPrice` (base + marge + TVA sur marge) dans une méthode
13. **Primitive obsession** — nombres nus pour prix, stock, marges, taxes ; aucun invariant
14. **Ce qui manque** — pas de repository, pas de ports/adapters, pas de VO, pas de séparation domaine/infra
15. **La baseline mutation** — score actuel sur le domaine ; « si le code était subtilement faux, un test le
    verrait-il ? »

## Bloc 3 — Part 1 : Value Objects, le Pricing Shared Kernel (~35 min · 8 slides)

16. **Les 2 règles fondatrices**
    - Always-valid : constructeur privé + factory statique → `Result<T, DomainError>` ; pas d'exception pour un échec
      métier attendu
    - Zéro pollution framework : `src/<context>/domain/` n'importe rien de `@nestjs/*` / `@prisma/client`
17. **Démo live — `Margin`** — extraire le `0.18` / `0.20` hardcodé en VO immuable auto-validante (`fromPercentage`,
    `forRegion`, `applyTo`)
18. **Tests de `Margin`** — exemple + PBT (`applyTo` ∈ [0, montant]) ; approche TDD assistée par agent
19. **Le mutant « increvable »** — `region?: string` : pas un trou de test, un bug de modélisation déguisé
    (`docs/2h-workshop/side-notes/agent vs mutants.md`)
20. **À vous — le kernel**
    - A1 `Price` : montant ≥ 0, EUR, 2 décimales ; `add`, `multiply`, `equals`
    - A2 `VatRate` : 0–30 % ; `calculateTax`
    - A3 `ResellerPriceCalculator` : base + marge + TVA base + TVA sur marge
21. **`Price` → `PriceWithTax`** — `withTax` produit un type distinct ; style FP avec les combinators `neverthrow`
22. **La règle métier d'abord** — règle en français dans `spec.md` avant toute ligne de test ; commit après chaque test
    vert et sans mutant
23. **Point d'étape** — `pnpm test` < 100 ms ; viser 100 % mutation sur les fichiers domaine

## Bloc 4 — Part 2 : les événements sont le contrat (~50 min · 12 slides)

24. **Le paysage** — context map : Procurement → Inventory → Catalog → Sales ; les events = stickies oranges
25. **La paire du jour** — Inventory → Catalog : « quand le stock est épuisé, masquer l'article du catalogue »
26. **Comment vous câbleriez ça ?** — tableau des mauvaises réponses (transaction partagée, appel direct de service,
    appel HTTP) vs. publier un fait
27. **Les 3 « aha »** — (1) la tentation du couplage direct (2) event fin vs. gras (3) traduction à la frontière (ACL)
28. **La Boundary Rule** — un BC importe seulement les *types d'événements* d'un autre ; jamais ses agrégats /
    services / repos ; le consommateur importe le producteur
29. **Dessiner le contrat sur papier** — `StockDepleted` : nom + payload ; l'instructeur joue l'avocat du diable de
    l'event gras
30. **La grammaire de nommage** — BNF `<bc>.<event-fact>` ; passé, langage métier ;
    `inventory.stock-depleted` / classe `StockDepleted`
31. **Le bus révélé — tension CUPID « I »** — NestJS a `EventEmitter2` + `@OnEvent` (idiomatique)… pourquoi un bus ~30
    lignes fait main ? tableau du trade-off ; aujourd'hui Composable / Predictable + pédagogie > Idiomatic
32. **Étape 1–2 — producteur** — `StockDepleted` (langage publié) ; `StockItem` enregistre le fait dans
    `reserve`, `pullDomainEvents()` draine via `splice(0)`
33. **Étape 3 — la policy consommatrice** — `WhenStockDepleted` : load → `isNone()` no-op →
    `markUnavailable()` (langage « disponibilité », pas « stock ») → save ; le seul import cross-BC
34. **Étape 4 — les tests** — dernière unité réservée = 1 seul event (tue `=== 0` / `<= 0` / `< 0`) ; policy en mémoire,
    pas de module Nest ; PBT : exactement 1 event par franchissement du zéro
35. **Quatre responsabilités, trois lignes** — agrégat · repository · sac d'events · transport ; comment chacune échoue
    en distribué (lost update, crash entre save et publish)

## Bloc 5 — Part 3 : gate de vérification + rétro (~12 min · 6 slides)

36. **La gate** — `pnpm test` (< 100 ms, pas de Docker) puis `pnpm test:mutation` (100 % sur event / policy)
37. **La démo qui vend l'archi** — abonner un handler cassé qui `throw` ; la réservation réussit quand même, le
    catalogue est quand même à jour → « découplé » au sens mécanique
38. **L'honnêteté : in-process ≠ production** — un process qui crashe perd les events ; réponse prod = Outbox Pattern
    (même contrat, transport durci) ; cohérence éventuelle = prix de l'autonomie
39. **Checklist CUPID** — tableau C / U / P / I / D coché sur le code produit ; « I » sciemment sacrifié — et on sait
    dire pourquoi
40. **Retour à la context map** — la flèche câblée aujourd'hui a son étiquette complète : Catalog en aval d'Inventory,
    via un Published Language derrière un ACL ; 1 phrase = 4 décisions de DDD stratégique
41. **Pour aller plus loin** (mention rapide) — flux inverse `ItemArchived`, fan-out Procurement, « qui calcule le prix
    affiché ? » (Procurement → Catalog)

## Bloc 6 — Clôture (~3 min · 3 slides)

42. **À emporter** — les events sont le contrat, pas le modèle ; always-valid par construction ; mutation testing révèle
    les bugs de modélisation
43. **Feedback** — QR code OpenFeedback
44. **Licences & références** — Beerware / WTFPL / HOPL ; liens CUPID, ddd-crew/context-mapping, EventStorming

---

## Budget temps

| Bloc | Contenu                                 | Durée                               |
|------|-----------------------------------------|-------------------------------------|
| 0    | Ouverture                               | 3 min                               |
| 1    | SOLID → CUPID → CUTE DDD                | 10 min                              |
| 2    | Part 0 — tour du legacy                 | 15 min                              |
| 3    | Part 1 — Value Objects (hands-on)       | 35 min                              |
| 4    | Part 2 — événements cross-BC (hands-on) | 50 min                              |
| 5    | Part 3 — gate + rétro                   | 12 min                              |
| 6    | Clôture                                 | 3 min                               |
|      | **Total**                               | **128 min** (à resserrer de ~8 min) |
