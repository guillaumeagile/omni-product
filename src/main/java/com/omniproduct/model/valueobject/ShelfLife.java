package com.omniproduct.model.valueobject;

import java.time.LocalDate;

public record ShelfLife(LocalDate sellByDate, LocalDate useByDate) {}
