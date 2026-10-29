# CUPID étape 1 : parcours en autonomie

Ce guide est pour faire l'étape 1 seul. Il explique ce que le harnais vérifie, dans quel ordre travailler et comment
lire sa sortie. Il ne donne pas la réponse : la conception des parties Storage, Catalog, Pricing, Supplier,
Notification et Transport vous appartient.

Lisez d'abord [docs/CUPID-step-1.md](docs/CUPID-step-1.md) pour l'énoncé du problème et la définition des règles
(HA5 à HA12.1). Ce guide est le mode d'emploi.

## 1. Ce que vous modifiez

Le code à travailler se trouve dans deux packages :

- `src/main/java/com/omniproduct/model` : `Product` et `Supplier`, les entités legacy
- `src/main/java/com/omniproduct/service` : `ProductService`, `ProductTransportService`, `SupplierService`

Ne touchez pas à `domain/` ni à `infrastructure/` : ils sont hors exercice.

`Product` mélange catalogue (slug, remises, images), prix, stockage (stock, quantité, entrepôt), fournisseur et
transport (kilos, volume). `Supplier` détient ses produits et prend des services dans son constructeur. Les entités
portent aussi des annotations JPA et se référencent directement entre elles.

L'objectif de l'étape 1 est de séparer ces préoccupations pour que chaque classe n'ait qu'une seule raison de
changer, et de ne garder dans chaque classe que ce dont son contexte borné a besoin. Il ne s'agit pas de réécrire
toute l'application, mais de faire passer le modèle au harnais.

## 2. Lancer le harnais tôt et souvent

```
./harness.sh
```

Le script compile d'abord le projet. Un projet qui ne compile pas ne passe jamais. Codes de sortie :

- `0` : toutes les vérifications passent
- `1` : au moins une vérification échoue
- `2` : le harnais n'a pas pu tourner

Les lignes `[WARN]` ne font jamais échouer le harnais, mais les avertissements HA12.1 valent la peine d'être lus.

Lancez-le une fois sur le code d'origine, avant toute modification. Vous devriez voir des échecs sur la plupart des
règles : c'est le point de départ, pas un problème de votre installation. Gardez la sortie pour comparer avec les
exécutions suivantes.

Ne modifiez rien sous `harness/` pour faire passer une vérification. Si une règle vous semble fausse, notez
pourquoi et signalez-le à l'animateur de l'atelier. Une règle assouplie pour coller à une solution n'est plus une
évaluation.

## 3. Travailler dans cet ordre

Chaque règle vise un type de mauvaise odeur. Certaines règles sont plus faciles à satisfaire une fois d'autres
faites : cet ordre marche mieux que de corriger les échecs dans l'ordre où ils s'affichent.

1. **Inventorier les préoccupations.** Listez chaque membre public de `Product` et `Supplier`, et indiquez à quel
   contexte borné il appartient. Utilisez le vocabulaire de
   `harness/src/main/resources/ubiquitous-language-map.json`. Les mots de ce fichier sont ceux que le harnais
   recherche, c'est donc un bon guide pour les noms que vous rencontrerez.
2. **Découper les entités.** Extrayez une classe par préoccupation (HA5, HA6). Gardez chaque classe extraite petite.
3. **Couper les références entre entités** (HA7). Une entité ne se relie à une autre que par son identifiant.
4. **Retirer la persistance du modèle** (HA8). Le modèle n'importe pas JPA. Si le mapping reste nécessaire, il vit
   dans une classe séparée, hors de `model`.
5. **Faire déclarer à chaque classe du modèle ce qu'elle est** (HA9). Soit elle implémente `Identifiable`, soit
   c'est un record.
6. **Retirer les services des constructeurs d'entités** (HA10). Déplacez l'orchestration dans un service.
7. **Donner un type aux concepts du domaine** (HA11). Une valeur brute qui porte un concept (`String slug`,
   `long price`, `int quantity`) devient un record du domaine (`record Price(BigDecimal amount)`).
8. **Rendre les identifiants riches** (HA12). Chaque identifiant restant devient un `UUID` v7 ou un `Ulid`, ou un
   record qui en enveloppe un. Traitez-le en dernier, une fois les classes stabilisées, puisque c'est le point qui
   touche le plus de code.

Lancez `./harness.sh` après chaque étape. Un petit changement qui fait passer une règle de FAIL à PASS vaut mieux
qu'un gros refactoring que vous ne pouvez pas vérifier.

## 4. Règle par règle

Pour chaque règle, la liste de contrôle indique ce que le harnais regarde. Le texte des règles dans
[docs/CUPID-step-1.md](docs/CUPID-step-1.md) fait autorité ; ce qui suit n'est qu'une aide à la lecture.

### HA5 - pas de classe Dieu

Une classe du modèle peut avoir au plus 4 propriétés publiques. Comptent les champs publics, les accesseurs publics
`getX()`/`isX()` et les composants de record. Les propriétés nommées `id` ou se terminant par `Id` ne comptent pas.
Si une classe dépasse la limite, regardez lesquelles de ses propriétés appartiennent à une autre préoccupation : c'est
généralement celle à extraire.

### HA6 - une seule préoccupation

Les méthodes publiques d'une classe doivent appartenir à une préoccupation au plus. Le harnais lit le nom de la
méthode, retire le verbe initial (`get`, `set`, `find`, `create`, ...) et cherche un mot du fichier de vocabulaire.
`getSlug` relève du Catalogue, `setStock` du Stockage. Les noms génériques comme `getId` ou `getName` n'appartiennent
à aucune préoccupation et ne comptent pas.

Le fichier liste aussi, dans la section `restrictions`, le vocabulaire interdit dans certains types. Si un type garde
une méthode d'une préoccupation interdite, il échoue même s'il est par ailleurs propre.

### HA7 - fan-out

S'applique aux entités : classes du modèle qui implémentent `Identifiable` ou déclarent un champ `id`. Une entité ne
peut référencer aucun autre type d'entité : ni dans un champ, ni dans un paramètre, ni dans un type de retour, ni
dans un argument générique comme `List<Product>`. Reliez les entités par leur identifiant. Les objets de valeur et
les services ne comptent pas.

Attention : un objet de valeur qui contient une entité compte quand même contre son propriétaire.

### HA8 - pas de persistance dans le domaine

Aucune classe de `model` ne doit dépendre de `jakarta.persistence`, de Spring Data ni d'une autre API de
persistance. Le harnais cherche aussi les champs dont le nom se termine par `Csv`, `Json`, `Xml`, `Blob`, `Row`,
`Serialized` ou `Dump`, et les méthodes nommées `sync`, `hydrate`, `persist`, `serialize`, `deserialize`, `flush` ou
`reload`. Ce sont les signes habituels d'un mapping écrit à la main. Sortez le mapping du modèle plutôt que de le
renommer.

### HA9 - les entités implémentent `Identifiable`

Toute classe déclarée dans `model` doit implémenter une interface nommée `Identifiable`, sauf si c'est un record, une
interface ou un enum. Créez l'interface si nécessaire. Une classe qui est en réalité un objet de valeur doit devenir
un record.

### HA10 - pas de services dans les constructeurs d'entités

Une entité (voir HA9) ne doit pas prendre un service en paramètre de constructeur. Un service est un type de
`com.omniproduct.service` ou un type dont le nom se termine par `Service`. Passez à la place les valeurs dont
l'entité a besoin, ou déplacez l'appel dans le service qui crée l'entité.

### HA11 - pas de valeur brute pour un concept du domaine

Un champ, un paramètre, un composant de record ou un retour de méthode dont le nom désigne un concept du vocabulaire
(`harness/src/main/resources/ubiquitous-language-map.json`, par exemple `slug`, `price`, `quantity`, `kilos`) ne
peut pas être un `String`, un `int`, un `long`, un `Double` ou un `BigDecimal` nu. Il lui faut un type du domaine,
un record qui enveloppe la valeur. Les noms génériques (`name`, `description`) ne sont pas concernés, et les
identifiants relèvent de HA12.

### HA12 - les identifiants sont des UUID v7 ou des ULID

Chaque `id`, `*Id` et `*Ids` du modèle (champs, variables locales, composants de record, types de retour des
getters, paramètres) doit être un `UUID` ou un `Ulid`, ou un record qui enveloppe exactement l'un des deux, comme
`record ProductId(UUID value)`. Un identifiant en `String`, `int` ou `long` échoue. Pour les collections et les maps
`*Ids`, les types des éléments et des clés sont vérifiés.

Le JDK ne fournit pas de générateur UUID v7. Ajoutez une bibliothèque comme
[uuid-creator](https://github.com/f4b6a3/uuid-creator) (`UuidCreator.getTimeOrderedEpoch()`) ou
[ulid-creator](https://github.com/f4b6a3/ulid-creator) (`UlidCreator.getUlid()`). `UUID.randomUUID()` produit une
version 4 et échoue là où il construit un identifiant.

Seuls les identifiants générés par le système sont concernés. Un **identifiant naturel**, dont le code est l'identité
et est attribué hors du système (SIREN, SIRET, numéro de TVA `tvaId`, SKU, EAN, GTIN, ISBN), n'a pas besoin d'UUID :
un record court comme `record Siren(String value)` suffit. La liste est `naturalIdentifiers` dans
`harness/src/main/resources/ubiquitous-language-map.json` ; tout autre identifiant est généré.

L'avertissement HA12.1 n'est pas un échec : il signale tout identifiant dont le type est une classe de bibliothèque
brute, comme `UUID` ou `String`, afin qu'un `ProductId` ne soit pas confondu avec un `SupplierId`. Vous pouvez le
laisser, mais c'est un bon indice d'endroit où un record aiderait. Un record permet aussi de passer plus tard de
l'UUID v7 au ULID sans toucher au modèle.

## 5. Lire un échec

Une ligne d'échec nomme la règle, la classe et, quand le harnais peut la retrouver, le numéro de ligne. Commencez par
la première règle en échec dans la sortie et corrigez cette classe avant de passer à la suivante. Plusieurs échecs ont
souvent la même cause : sortir une préoccupation de `Product` peut corriger HA5, HA6 et HA7 en même temps.

Quand un résultat vous surprend, relisez le texte de la règle dans [docs/CUPID-step-1.md](docs/CUPID-step-1.md) avant
de modifier le code. Si le code est correct pour le domaine et que le harnais proteste quand même, notez le cas et
signalez-le à l'animateur plutôt que de contourner la règle.

## 6. Terminé

L'étape 1 est terminée quand `./harness.sh` sort avec le code `0`. Les avertissements sont autorisés. Avant de
considérer l'étape comme finie :

- le harnais sort avec `0` sur votre machine, après une compilation propre
- vous savez expliquer, pour chaque classe extraite, quelle préoccupation elle porte et pourquoi

Gardez la sortie de votre dernière exécution : c'est la preuve que l'animateur demandera.
