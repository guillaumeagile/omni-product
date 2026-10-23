package com.omniproduct.harness.fixtures.ha4.good.model;

// Témoin conforme : interface, record, enum et exception sont permis.
public class Composed implements Comparable<Composed> {
    public record Part(String name) {
    }

    public enum Kind {
        A, B
    }

    public static class Oops extends RuntimeException {
    }

    @Override
    public int compareTo(Composed other) {
        return 0;
    }
}
