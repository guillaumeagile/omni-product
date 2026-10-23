## CUTE DDD, les mains dans le code

### SOLID › CUPID › CUTE DDD

![Guillaume & Sam](./img/portrait-sam+gui-land.jpg)

Note:
Version courte, 2 h, orientée pratique. On code un vrai refactoring DDD sur une base NestJS.

---

# Objectifs

- D'un **God-model anémique** → des modèles **DDD tactiques, toujours valides**
- **Value Objects** et **agrégats** qui portent leurs invariants
- Des **événements** entre Bounded Contexts, pas des appels directs
- Le tout **prouvé** : tests unitaires + property-based + mutation testing

---

### Le code à suivre

![QR repo](./img/qrcode_repo_omniproduct2026.png)

```bash
pnpm install
pnpm test            # rapide, sans Docker
pnpm test:mutation   # Stryker
```
