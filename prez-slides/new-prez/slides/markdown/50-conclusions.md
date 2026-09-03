# C4 · Conclusions

--

## Rétro CUPID

| | Le code produit |
|---|---|
| **C** | les BC composent *par événements*, pas par imports |
| **U** | Inventory gère le stock, Catalog la présentation |
| **P** | invariants par construction, `Result` explicite |
| **I** | **sacrifié** aujourd'hui (bus fait main) — et on sait dire pourquoi |
| **D** | `StockDepleted`, `markUnavailable` — le sticky devenu code |

---

## À emporter

- Le **contrat**, c'est l'**événement**, pas le modèle partagé
- Un état invalide doit être **inreprésentable**
- La **mutation** révèle les bugs de *modélisation*, pas seulement de test
- Chez vous : **quel est le prochain fait, et qui y réagit ?**
