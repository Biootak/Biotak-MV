package com.biotak.ui;

import com.motivewave.platform.sdk.common.Enums.ResizeType;
import com.motivewave.platform.sdk.common.Util;
import com.motivewave.platform.sdk.draw.ResizePoint;
import com.motivewave.platform.sdk.common.DrawContext;

/**
 * Invisible resize point that treats the entire line as hit area
 * This class provides a drag handle that covers the full length of a CustomPriceLine
 * for better user interaction experience.
 */
public class LineResizePoint extends ResizePoint implements HitTestManager.HitTestable {
    private final CustomPriceLine parentLine;
    private HitTestManager hitTestManager; // Reference to the hit test manager
    
    public LineResizePoint(CustomPriceLine parentLine) {
        super(ResizeType.VERTICAL, true); // vertical only for horizontal price line
        this.parentLine = parentLine;
        setSnapToLocation(true);
    }
    
    /**
     * Set the hit test manager reference (called from BiotakTrigger)
     */
    public void setHitTestManager(HitTestManager hitTestManager) {
        this.hitTestManager = hitTestManager;
    }
    
    @Override
    public boolean contains(double x, double y, DrawContext ctx) {
        // Use HitTestManager to determine if this element should respond
        if (hitTestManager != null) {
            return containsPoint(x, y, ctx) && hitTestManager.shouldElementRespond(this, x, y, ctx);
        }
        
        // Fallback to direct hit test if no manager available
        return containsPoint(x, y, ctx);
    }
    
    // HitTestable interface implementation
    @Override
    public boolean containsPoint(double x, double y, DrawContext ctx) {
        // Delegate hit-test to the parent line so clicks anywhere on the line start the drag
        boolean parentExists = (parentLine != null);
        if (parentExists && parentLine.line != null) {
            double distance = Util.distanceFromLine(x, y, parentLine.line);
            return distance < 3.0; // Reduced tolerance for better precision
        }
        return false;
    }
    
    @Override
    public HitTestManager.HitTestPriority getHitTestPriority() {
        return HitTestManager.HitTestPriority.MEDIUM; // CustomPriceLine has medium priority
    }
    
    @Override
    public String getElementName() {
        return "CustomPriceLineResizePoint";
    }
    
    // Keep fully invisible – no drawing
    @Override
    public void draw(java.awt.Graphics2D gc, DrawContext ctx) {}
}
