package com.omniproduct.infrastructure.persistence;

import com.omniproduct.domain.port.Store;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Adaptateur en mémoire : un dictionnaire id -> valeur, dans l'ordre d'insertion.
public abstract class InMemoryStore<T> implements Store<T> {

    protected final Map<String, T> values = new LinkedHashMap<>();

    @Override
    public Optional<T> findById(String id) {
        return Optional.ofNullable(values.get(id));
    }

    @Override
    public List<T> findAll() {
        return List.copyOf(values.values());
    }

    @Override
    public void save(String id, T value) {
        values.put(id, value);
    }

    @Override
    public void deleteById(String id) {
        values.remove(id);
    }

    @Override
    public void deleteAll() {
        values.clear();
    }

    @Override
    public boolean existsById(String id) {
        return values.containsKey(id);
    }
}
