package com.biotak.enums;

/**
 * Enum representing different states of the ruler tool
 */
public enum RulerState {
    /**
     * Ruler is inactive/disabled
     */
    INACTIVE,
    
    /**
     * Ruler is active and waiting for user to click and select the start point
     */
    WAITING_FOR_START,
    
    /**
     * User has selected start point and ruler is waiting for end point selection
     */
    WAITING_FOR_END,
    
    /**
     * Ruler is fully drawn with both start and end points selected
     */
    ACTIVE
}
