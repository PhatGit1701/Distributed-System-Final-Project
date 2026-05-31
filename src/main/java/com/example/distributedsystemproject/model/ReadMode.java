package com.example.distributedsystemproject.model;

public enum ReadMode {
    ONE,
    LATEST,
    QUORUM;

    public static ReadMode fromString(String value) {
        if (value == null || value.isBlank()) {
            return ONE;
        }
        try {
            return ReadMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ONE;
        }
    }
}
