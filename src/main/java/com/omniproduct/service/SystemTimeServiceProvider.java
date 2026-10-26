package com.omniproduct.service;

import com.omniproduct.model.TimeServiceProvider;

import java.time.LocalDate;

public class SystemTimeServiceProvider implements TimeServiceProvider {

    @Override
    public LocalDate today() { return LocalDate.now(); }
}
