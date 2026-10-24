package com.omniproduct.harness.fixtures.clock.bad.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;

import static java.time.Clock.systemUTC;

public class Dated {

    public boolean expired() {
        return LocalDate.now().isAfter(LocalDate.of(2020, 1, 1));
    }

    public Instant stamp() {
        return Instant.now();
    }

    public long millis() {
        return System.currentTimeMillis();
    }

    public Date created() {
        return new Date();
    }

    public Object aliased() {
        return systemUTC();
    }
}
