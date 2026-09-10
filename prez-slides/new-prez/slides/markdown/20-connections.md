# C1 · Connections

Ce que vous apportez déjà

--

## Ce que vous connaissez déjà

- OOP classique : classes, héritage, état mutable
- **SOLID** : SRP, OCP, LSP, ISP, DIP
- NestJS / Prisma, un ORM, des services
- Les exceptions comme flux de contrôle

---

## Le point de départ

Un `Product` legacy porté de Spring Boot :

- **Une table qui fait tout** — catalogue + stock + fournisseur + prix + entrepôt
- Un **service anémique** — Prisma et logique métier mélangés
- Des **primitives nues** — `number` pour prix, marge, TVA, stock ; aucun invariant

Note:
C'est le `product.service.ts` et le `schema.prisma` du repo. On va le regarder ensemble en Part 0.
