package com.biotak.enums;

/**
 * Defines the possible starting points for the TH level calculations.
 */
public enum THStartPointType {
    MIDPOINT("Midpoint"),
    HISTORICAL_HIGH("Historical High"),
    HISTORICAL_LOW("Historical Low"),
    CUSTOM_PRICE("Custom Price");

    private final String value;
    THStartPointType(String value) { this.value = value; }
    @Override
    public String toString() { return value; }
} 