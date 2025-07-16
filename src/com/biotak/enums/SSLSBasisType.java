package com.biotak.enums;

/**
 * Basis options for calculating SS/LS step distances.
 */
public enum SSLSBasisType {
    STRUCTURE("Structure"),
    PATTERN("Pattern"),
    TRIGGER("Trigger"),
    AUTO("Auto");

    private final String value;
    SSLSBasisType(String value) { this.value = value; }
    @Override
    public String toString() { return value; }
} 