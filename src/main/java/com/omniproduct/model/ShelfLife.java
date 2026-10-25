package com.omniproduct.model;

public sealed interface ShelfLife permits Perishable, NonExpiring {}
