package com.omniproduct.model;

import java.time.LocalDate;

public record NonExpiring(LocalDate bestBeforeDate) implements ShelfLife {}
