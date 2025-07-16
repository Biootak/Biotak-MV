package com.biotak.enums;

/**
 * Defines which step-drawing algorithm the indicator should use.
 * TH_STEP  – classic equal-distance TH increments.
 * SS_LS_STEP – variable increments alternating between Short-Step (SS) and Long-Step (LS).
 */
public enum StepCalculationMode {
    TH_STEP("TH-Based"),
    SS_LS_STEP("SS/LS-Based");

    private final String value;
    StepCalculationMode(String value) { this.value = value; }
    @Override
    public String toString() { return value; }
} 