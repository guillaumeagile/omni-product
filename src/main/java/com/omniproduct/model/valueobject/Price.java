package com.omniproduct.model.valueobject;

import com.omniproduct.model.Amount;

public record Price(Amount base, Amount tax) {}
