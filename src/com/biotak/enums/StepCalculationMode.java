package com.biotak.enums;

/**
 * Step calculation modes for level drawing.
 * TH_STEP  – equal-distance levels based on TH (classic).
 * SS_LS_STEP – alternating levels using Short Step (SS) and Long Step (LS).
 */
public enum StepCalculationMode {
    TH_STEP("TH-Based"),
    SS_LS_STEP("SS/LS-Based");

    private final String value;
    StepCalculationMode(String value) { this.value = value; }

    @Override
    public String toString() {
        return value;
    }
} 