![Sponsors 2025](./img/Slide%20Sponsors%20pour%20orateurs%202025.png)

---

![Forum 2025](./img/Slide%20forum%202025.png)

---

# CUTE DDD, les mains dans le code

## SOLID › CUPID › CUTE DDD — atelier 2 h

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

# Le code à suivre

![QR repo](./img/qrcode_repo_FizzBuzz2025.png)

```bash
pnpm install
pnpm test            # rapide, sans Docker
pnpm test:mutation   # Stryker
```
