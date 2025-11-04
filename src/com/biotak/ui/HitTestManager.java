package com.biotak.ui;

import com.motivewave.platform.sdk.common.DrawContext;
import com.motivewave.platform.sdk.draw.Figure;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Centralized hit test manager that handles priority between different UI elements.
 * This ensures that high-priority elements like Ruler get precedence over lower-priority
 * elements like CustomPriceLine when both are in the same area.
 */
public class HitTestManager {
    
    /**
     * Priority levels for different UI elements
     */
    public enum HitTestPriority {
        CRITICAL(150),   // InfoPanel with buttons - MUST always work
        HIGHEST(100),    // Ruler and other critical UI elements
        HIGH(75),        // Important interactive elements
        MEDIUM(50),      // Standard interactive elements
        LOW(25),         // Background elements
        LOWEST(0);       // Non-interactive elements
        
        private final int value;
        
        HitTestPriority(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    /**
     * Represents a UI element that can respond to hit tests
     */
    public interface HitTestable {
        boolean containsPoint(double x, double y, DrawContext ctx);
        HitTestPriority getHitTestPriority();
        String getElementName(); // for debugging
    }
    
    private final List<HitTestable> registeredElements = new CopyOnWriteArrayList<>();
    
    /**
     * Register a UI element for hit testing
     */
    public void registerElement(HitTestable element) {
        if (element != null && !registeredElements.contains(element)) {
            registeredElements.add(element);
        }
    }
    
    /**
     * Unregister a UI element from hit testing
     */
    public void unregisterElement(HitTestable element) {
        registeredElements.remove(element);
    }
    
    /**
     * Clear all registered elements
     */
    public void clearElements() {
        registeredElements.clear();
    }
    
    /**
     * Check if a point hits any registered element, considering priority.
     * Returns the highest priority element that contains the point, or null if none.
     */
    public HitTestable getHighestPriorityHit(double x, double y, DrawContext ctx) {
        HitTestable highestPriorityHit = null;
        int highestPriority = -1;
        
        for (HitTestable element : registeredElements) {
            if (element.containsPoint(x, y, ctx)) {
                int priority = element.getHitTestPriority().getValue();
                if (priority > highestPriority) {
                    highestPriority = priority;
                    highestPriorityHit = element;
                }
            }
        }
        
        return highestPriorityHit;
    }
    
    /**
     * Check if a specific element should respond to hit test at given point.
     * Returns true only if this element is the highest priority at that location.
     */
    public boolean shouldElementRespond(HitTestable requestingElement, double x, double y, DrawContext ctx) {
        HitTestable highestPriorityHit = getHighestPriorityHit(x, y, ctx);
        return highestPriorityHit == requestingElement;
    }
    
    /**
     * Get count of registered elements (for debugging)
     */
    public int getRegisteredElementCount() {
        return registeredElements.size();
    }
    
    /**
     * Get names of all registered elements (for debugging)
     */
    public List<String> getRegisteredElementNames() {
        List<String> names = new ArrayList<>();
        for (HitTestable element : registeredElements) {
            names.add(element.getElementName());
        }
        return names;
    }
}