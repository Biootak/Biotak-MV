package com.biotak.enums;

/**
 * Defines the different types of step values that the ruler can compare against.
 * This allows users to select which step basis the ruler should use for matching measured legs.
 */
public enum RulerComparisonType {
    /** Match against M (Movement) values - default behavior */
    M("M Values", "Matches against M (Movement) step values across all timeframes"),
    
    /** Match against E (Entry) values */
    E("E Values", "Matches against E (Entry) step values (0.75 × TH)"),
    
    /** Match against TP (Take Profit) values */
    TP("TP Values", "Matches against TP (Take Profit) step values (3 × E)"),
    
    /** Match against TH (Threshold) values */
    TH("TH Values", "Matches against TH (Threshold) step values"),
    
    /** Match against TH×3 (Threshold × 3) values */
    TH3("TH×3 Values", "Matches against TH×3 (Threshold × 3) step values"),
    
    /** Match against SS (Short Step) values */
    SS("SS Values", "Matches against SS (Short Step) values"),
    
    /** Match against LS (Long Step) values */
    LS("LS Values", "Matches against LS (Long Step) values"),
    
    /** Match against ATR×3 values */
    ATR("ATR×3 Values", "Matches against ATR×3 values across all timeframes"),
    
    /** Match against both M and ATR values (legacy behavior) */
    BOTH("M + ATR Values", "Matches against both M and ATR×3 values (shows both results)");
    
    private final String displayName;
    private final String description;
    
    RulerComparisonType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
