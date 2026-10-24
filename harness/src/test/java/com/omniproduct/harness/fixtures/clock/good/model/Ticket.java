package com.omniproduct.harness.fixtures.clock.good.model;

import java.time.Duration;
import java.time.LocalDate;

public class Ticket {

    private final LocalDate due;

    public Ticket(LocalDate due) {
        this.due = LocalDate.of(due.getYear(), due.getMonth(), due.getDayOfMonth());
    }

    public boolean overdue(LocalDate today) {
        return today.isAfter(due) && Duration.ofDays(1).toMillis() > 0;
    }
}
