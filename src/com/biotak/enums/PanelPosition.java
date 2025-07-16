package com.biotak.enums;

/**
 * Defines the possible positions for the information panel on the chart.
 */
public enum PanelPosition {
    TOP_RIGHT("Top Right"),
    TOP_LEFT("Top Left"),
    BOTTOM_RIGHT("Bottom Right"),
<<<<<<< HEAD
    BOTTOM_LEFT("Bottom Left"),
    CENTER("Center");
=======
    BOTTOM_LEFT("Bottom Left");
>>>>>>> 722ba07644c2ccf9e8a54d5e43c2003d41cb74f2

    private final String value;
    
    PanelPosition(String value) { 
        this.value = value; 
    }
    
    @Override
    public String toString() { 
        return value; 
    }
} 