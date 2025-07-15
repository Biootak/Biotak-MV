package com.biotak.enums;

/**
 * Defines the possible positions for the information panel on the chart.
 */
public enum PanelPosition {
    TOP_RIGHT("Top Right"),
    TOP_LEFT("Top Left"),
    BOTTOM_RIGHT("Bottom Right"),
    BOTTOM_LEFT("Bottom Left");

    private final String value;
    
    PanelPosition(String value) { 
        this.value = value; 
    }
    
    @Override
    public String toString() { 
        return value; 
    }
} 