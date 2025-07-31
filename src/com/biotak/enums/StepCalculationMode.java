package com.biotak.enums;

/**
 * Step calculation modes for level drawing.
 * TH_STEP  – equal-distance levels based on TH (classic).
 * SS_LS_STEP – alternating levels using Short Step (SS) and Long Step (LS).
 */
public enum StepCalculationMode {
    TH_STEP("TH-Based"),
    SS_LS_STEP("SS/LS-Based"),
    M_STEP("M (C×3)-Based"),
    E_STEP("E-Based"),
    TP_STEP("TP-Based");

    private final String value;
    StepCalculationMode(String value) { this.value = value; }

    @Override
    public String toString() {
        return value;
    }
} 