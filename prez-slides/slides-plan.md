# Plan de présentation — atelier 2 h « CUTE DDD hands-on »

> Slides pour **présenter le workshop et ses étapes**, pas pour refaire le contenu du lab
> (le lab vit dans `docs/2h-workshop/`). On garde le style et le squelette de l'ancienne prez
> (`prez-slides/old-prez/slides/`) : intro → concepts → structure 4C → clôture.
>
> **Contrainte** : texte concis, puces courtes. **Total visé : ~21 slides.**

---

## Intro (4 slides — réutilise les visuels v1)

1. **Sponsors / Forum 2025** — images existantes
2. **Titre** — Guillaume & Sam · SOLID › CUPID › CUTE DDD, les mains dans le code · photo duo
3. **Objectifs** (4 puces)
    - D'un God-model anémique → modèles DDD tactiques *always-valid*
    - Value Objects, agrégats,
    - événements entre Bounded Contexts
    - Prouvé par tests unitaires + PBT + mutation testing (Stryker)
4. **Le code à suivre** — QR code repo NestJS + `pnpm test`

## Le modèle 4C (1 slide)

5. **4Cs : Training from the Back of the Room** — image `4cs.jpg` (v1) ; l'atelier suit ce cycle

## C1 — Connections (2 slides)

6. **Ce que vous connaissez déjà** — OOP classique, SOLID, NestJS/Prisma, exceptions partout
7. **Le point de départ** — un `Product` legacy porté de Spring Boot : une table qui fait tout (catalogue + stock +
   fournisseur + prix) ; service anémique ; primitives nues

## C2 — Concepts (6 slides)

8. **SOLID : le rappel et la limite** — 5 principes en 5 lignes ; sur ce code : SRP/OCP/DIP violés, LSP/ISP muets —
   SOLID dit ce qui est cassé, pas où aller
9. **CUPID** — Composable · Unix · Predictable · Idiomatic · Domain-based ; des *propriétés* (direction), pas des règles
   (pass/fail)
10. **CUTE DDD** — Contextual · Ubiquitous · Testable · Expressive ; la chaîne : SOLID (technique) → CUPID (joyeux) →
    CUTE DDD (métier)
11. **Les règles du jeu du lab** — always-valid : factory statique → `Result<T, DomainError>` ; zéro import framework
    sous `domain/` ; un BC n'importe d'un autre que ses *événements*
12. **L'événement est le contrat** — transformer 1 God table en 3 BC (Procurement / Inventory / Catalog)
    + Pricing en Shared Kernel ; les BC collaborent par des faits, pas par appels — event storming + schéma context map
13. **quel est notre filet de sécurité ?** — tests d'exemple + property-based tests + mutation testing : « si le code
    était subtilement faux, un test le verrait-il ? » (harnais IA ?)

## C3 — Concrete Practice : le déroulé de l'atelier (5 slides)

14. **La carte des 2 heures** — timeline : Part 0 tour du legacy (15') → Part 1 Value Objects (35')
    → Part 2 événements cross-BC (50') → gate + rétro (15')
15. **Part 0 — visite du legacy** — sentir la 💩 (code-smells) sur `product.service.ts` / `schema.prisma` ; poser la
    baseline mutation
16. **Part 1 — le Pricing Shared Kernel** — démo `Margin`, puis à vous : `Price`, `VatRate`,
    `ResellerPriceCalculator` — chaque Value Object est immuable, auto-validant, résiste à 100 % mutation
17. **Part 2 — Inventory → Catalog** — event storming >>
    (noms au passé, infos utiles des évenements doivent être légères), puis coder un consommateur d'évènement.
18. **La gate finale** — `pnpm test` < 100 ms sans Docker ; `pnpm test:mutation` ; démo : un handler qui crashe ne casse
    ni la réservation ni le catalogue

## C4 — Conclusions (2 slides)

19. **Rétro CUPID** — checklist C/U/P/I/D sur le code produit ; « I » sciemment sacrifié — et on sait dire pourquoi
20. **À emporter** — le contrat est porté par les évènements, pas par le modèle ; il faut que les états invalides soient
    inreprésentable ; la mutation révèle les bugs de *modélisation* ; et chez vous : quel est le prochain évènement qui
    vous ferait réagir ?

## Clôture (1 slide + annexes)

21. **Feedback** — QR code OpenFeedback
22. *(annexe)* **Licences & références** — Beerware / WTFPL / HOPL ; cupid.dev, ddd-crew/context-mapping,
    EventStorming — comme dans la v1

---

## Correspondance 4C ↔ déroulé réel

| 4C                | Slides | Moment de l'atelier                 |
|-------------------|--------|-------------------------------------|
| Connections       | 6–7    | Intro + Part 0 (tour du legacy)     |
| Concepts          | 8–13   | Interludes courts entre les parties |
| Concrete Practice | 14–18  | Parts 1 & 2 (les ~85 min de code)   |
| Conclusions       | 19–20  | Rétro finale                        |

## Décisions ouvertes

1. Les slides Concepts (8–13) sont-elles projetées **d'un bloc au début**, ou **intercalées** aux moments correspondants
   du lab (11 avant Part 1, 12 avant Part 2) ?
2. Part 2 : on annonce le mode B (handlers only) sur la slide 17, ou on laisse le choix A/B en salle ?
3. Garde-t-on une slide xp-ddd (visuels XP + DDD de la v1) en C2, ou on la coupe ?
