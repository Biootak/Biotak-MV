package com.biotak.ui;

import com.motivewave.platform.sdk.common.Coordinate;
import com.motivewave.platform.sdk.common.PathInfo;
import com.motivewave.platform.sdk.common.Util;
import com.motivewave.platform.sdk.draw.Figure;
import com.motivewave.platform.sdk.draw.ResizePoint;
import com.motivewave.platform.sdk.common.DrawContext;

/**
 * Custom draggable price line class with invisible line-wide ResizePoint
 * This class handles the visual representation and interaction of a horizontal price line
 * that can be dragged to set custom price levels.
 */
public class CustomPriceLine extends Figure {
    java.awt.geom.Line2D line; // Package-private for LineResizePoint access
    private double price;
    private long startTime, endTime;
    private ResizePoint lineResizePoint; // Invisible ResizePoint covering the entire line
    private PathInfo pathInfo; // Store path info for drawing
    
    public CustomPriceLine(long startTime, long endTime, double price, PathInfo pathInfo) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
        this.pathInfo = pathInfo;
        
        // Create a special invisible resize point whose hit area is the whole line
        this.lineResizePoint = new LineResizePoint(this);
        // Position at end of line for value/time reference
        this.lineResizePoint.setLocation(endTime, price);
    }
    
    public void updatePrice(double newPrice) {
        
        // Logger.debug("=== PRICE UPDATE START ===");
        // Logger.debug("CustomPriceLine.updatePrice() called at time: " + currentTime);
        // Logger.debug("Old price: " + oldPrice);
        // Logger.debug("New price: " + newPrice);
        // Logger.debug("Price change: " + (newPrice - oldPrice));
        // Logger.debug("LineResizePoint exists: " + (lineResizePoint != null));
        
        this.price = newPrice;
        // Logger.debug("Price field updated to: " + this.price);
        
        if (lineResizePoint != null) {
            long oldTime = lineResizePoint.getTime();
            lineResizePoint.setLocation(oldTime, newPrice);
            // Logger.debug("LineResizePoint location updated to: (" + oldTime + ", " + newPrice + ")");
        }
        
        // Logger.debug("=== PRICE UPDATE END ===");
    }
    
    public ResizePoint getLineResizePoint() {
        return lineResizePoint;
    }
    
    @Override
    public boolean contains(double x, double y, DrawContext ctx) {
        return line != null && Util.distanceFromLine(x, y, line) < 6;
    }
    
    @Override
    public void layout(DrawContext ctx) {
        
        // Logger.debug("=== LAYOUT UPDATE START ===");
        // Logger.debug("CustomPriceLine.layout() called at time: " + currentTime);
        // Logger.debug("Current price: " + price);
        // Logger.debug("Start time: " + startTime + ", End time: " + endTime);
        
        var start = ctx.translate(new Coordinate(startTime, price));
        var end = ctx.translate(new Coordinate(endTime, price));
        // Logger.debug("Translated start point: (" + start.getX() + ", " + start.getY() + ")");
        // Logger.debug("Translated end point: (" + end.getX() + ", " + end.getY() + ")");
        
        line = new java.awt.geom.Line2D.Double(start, end);
        // Logger.debug("Line geometry updated");
        
        // Update the invisible ResizePoint position
        if (lineResizePoint != null) {
            lineResizePoint.setLocation(endTime, price);
            // Logger.debug("LineResizePoint position updated to: (" + endTime + ", " + price + ")");
        }
        
        // Logger.debug("=== LAYOUT UPDATE END ===");
    }
    
    @Override
    public void draw(java.awt.Graphics2D gc, DrawContext ctx) {
        if (line == null || pathInfo == null) return;
        gc.setStroke(ctx.isSelected() ? pathInfo.getSelectedStroke() : pathInfo.getStroke());
        gc.setColor(pathInfo.getColor());
        gc.draw(line);
    }
}
