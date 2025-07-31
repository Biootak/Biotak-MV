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
public class LineResizePoint extends ResizePoint {
    private final CustomPriceLine parentLine;
    
    public LineResizePoint(CustomPriceLine parentLine) {
        super(ResizeType.VERTICAL, true); // vertical only for horizontal price line
        this.parentLine = parentLine;
        setSnapToLocation(true);
    }
    
    @Override
    public boolean contains(double x, double y, DrawContext ctx) {
        // Delegate hit-test to the parent line so clicks anywhere on the line start the drag
        // Use a more generous hit area for better responsiveness
        boolean parentExists = (parentLine != null);
        boolean result = false;
        if (parentExists && parentLine.line != null) {
            double distance = Util.distanceFromLine(x, y, parentLine.line);
            result = distance < 10.0; // 10 pixel tolerance for better hit detection
            // Logger.debug("Line distance: " + distance + ", tolerance: 10.0");
        }
        
        // Log all hit tests for debugging mouse interaction issues
        // Logger.debug("=== LINE HIT TEST ===");
        // Logger.debug("LineResizePoint.contains() called at time: " + currentTime);
        // Logger.debug("Hit test coordinates: (" + x + ", " + y + ")");
        // Logger.debug("Parent line exists: " + parentExists);
        // Logger.debug("Hit test result: " + result);
        
        if (result) {
            // Logger.debug("*** LINE HIT DETECTED *** at (" + x + ", " + y + ")");
            // Logger.debug("ResizePoint location: (" + getTime() + ", " + getValue() + ")");
        }
        // Logger.debug("=== LINE HIT TEST END ===");
        
        return result;
    }
    
    // Keep fully invisible – no drawing
    @Override
    public void draw(java.awt.Graphics2D gc, DrawContext ctx) {}
}
