package com.omniproduct.domain.port;

import java.util.List;
import java.util.Optional;

// Port de persistance du cœur fonctionnel : le cœur ne sait pas comment les valeurs sont stockées.
public interface Store<T> {

    Optional<T> findById(String id);

    List<T> findAll();

    void save(String id, T value);

    void deleteById(String id);

    void deleteAll();

    boolean existsById(String id);
}
