package com.omniproduct.model.valueobject;

import com.omniproduct.model.Amount;
import com.omniproduct.model.TaxRate;

public record Price(Amount base, Amount tax, TaxRate taxRate) {}
