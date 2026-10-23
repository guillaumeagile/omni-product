package com.omniproduct.harness;

public record Violation(String filePath, int line, String message) {
}
