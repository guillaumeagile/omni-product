# C3 · Concrete Practice

Le déroulé des 2 heures

--

## La carte des 2 heures

| Temps | Étape |
|------:|-------|
| 15 min | **Part 0** — visite du legacy |
| 35 min | **Part 1** — Value Objects (Pricing Shared Kernel) |
| 50 min | **Part 2** — événements entre 2 Bounded Contexts |
| 15 min | **Part 3** — gate de vérification + rétro |

---

## Part 0 — visite du legacy

- Ouvrir `product.service.ts` et `schema.prisma`
- Nommer les **code-smells** : God entity, primitive obsession, logique anémique
- Lancer `pnpm test:mutation` → **poser la baseline**

C'est le point de comparaison de toute la suite.

---

## Part 1 — le Pricing Shared Kernel

**Démo** : `Margin` — extraire le `0.18` hardcodé en Value Object immuable, auto-validant.

**À vous** :

- `Price` — montant ≥ 0, EUR, 2 décimales ; `add`, `multiply`, `equals`
- `VatRate` — taux 0–30 % ; `calculateTax`
- `ResellerPriceCalculator` — base + marge + TVA base + TVA sur marge

Chaque VO : immuable, `Result` en sortie, **100 % de mutants tués**.

---

## Part 1 — la discipline

```
1. écrire la règle métier en français dans spec.md
2. un test qui échoue
3. le code minimal qui passe
4. pnpm test:mutation — 0 survivant
5. commit
```

---

## Part 2 — Inventory → Catalog

**Le fait** : « quand le stock est épuisé, masquer l'article du catalogue »

1. **EventStorming** — dessiner `StockDepleted` sur papier
   - nom au **passé**, langage métier
   - payload **léger** : `productId` seulement
2. **Coder le consommateur** — la policy `WhenStockDepleted` sur le bus in-memory
   - traduit « stock » → « disponibilité » (ACL en miniature)
   - seul import cross-BC autorisé : le **type d'événement**

---

## Part 2 — la grammaire de nommage

```bnf
<event-name>  ::= <bc> "." <event-fact>
<event-fact>  ::= mot(s) kebab-case, au passé
```

| BC | fait | nom sur le bus | classe |
|----|------|----------------|--------|
| `inventory` | `stock-depleted` | `inventory.stock-depleted` | `StockDepleted` |

Si le nom n'est pas au passé → c'est une *commande* déguisée. On redessine.

---

## Part 3 — la gate finale

```bash
pnpm test           # < 100 ms, sans Docker
pnpm test:mutation  # 100 % sur les fichiers event / policy
```

**La démo qui vend l'archi** : on abonne un handler qui `throw`.
La réservation réussit quand même. Le catalogue est quand même à jour.
→ *découplé*, au sens mécanique.

Note:
On dit aussi l'honnêteté : in-process + try/catch, un crash perd les events.
La réponse prod, c'est l'Outbox Pattern — même contrat, transport durci. Discussion, pas de code.
