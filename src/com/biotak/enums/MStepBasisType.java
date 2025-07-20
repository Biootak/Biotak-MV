package com.biotak.enums;

/**
 * Basis for distance increments in M-Step mode.
 */
public enum MStepBasisType {
    C_BASED("Control (C) Increments"),
    M_BASED("M (3×C) Increments");

    private final String label;
    MStepBasisType(String l) { this.label = l; }
    @Override public String toString() { return label; }
} 