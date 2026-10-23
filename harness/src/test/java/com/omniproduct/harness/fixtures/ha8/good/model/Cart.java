package com.omniproduct.harness.fixtures.ha8.good.model;

import java.util.ArrayList;
import java.util.List;

// Témoin conforme (HA8) : le panier ne sait pas comment il est stocké. Il n'importe aucune technologie de persistance,
// ne porte aucune forme sérialisée (pas de champ en JSON, CSV, XML, blob ou row) et ne synchronise ni n'hydrate rien.
// Il garde en revanche des noms proches du vocabulaire de stockage (items, total) pour que la règle reste juste.
public class Cart {
    private final List<Line> items = new ArrayList<>();

    public void addItem(Line line) {
        items.add(line);
    }

    public int total() {
        return items.stream().mapToInt(Line::amount).sum();
    }

    public record Line(String sku, int amount) {
    }
}
