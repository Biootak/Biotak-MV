package com.biotak.util;

import com.motivewave.platform.sdk.draw.Figure;
import com.motivewave.platform.sdk.draw.Line;
import com.motivewave.platform.sdk.common.Coordinate;
import com.motivewave.platform.sdk.common.PathInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Optimized figure management to reduce object creation overhead
 */
public final class FigureManager {
    
    // Object pools for commonly used figures
    private static final ObjectPool<ArrayList<Figure>> figureListPool = 
        new ObjectPool<>(() -> new ArrayList<>(100), 5);
    
    private static final ObjectPool<Coordinate> coordinatePool = 
        new ObjectPool<>(() -> new Coordinate(0, 0.0), 50);
    
    private FigureManager() {}
    
    /**
     * Get a figure list from the pool
     */
    @SuppressWarnings("unchecked")
    public static ArrayList<Figure> getFigureList() {
        ArrayList<Figure> list = figureListPool.acquire();
        list.clear(); // Clear any existing content
        return list;
    }
    
    /**
     * Return a figure list to the pool
     */
    public static void releaseFigureList(ArrayList<Figure> list) {
        if (list != null && list.size() < 200) { // Don't pool very large lists
            figureListPool.release(list);
        }
    }
    
    /**
     * Create a line figure efficiently
     */
    public static Line createLine(long startTime, double startPrice, long endTime, double endPrice, PathInfo path) {
        // Use direct constructor instead of pooling coordinates for lines since they're lightweight
        return new Line(new Coordinate(startTime, startPrice), new Coordinate(endTime, endPrice), path);
    }
    
    /**
     * Batch create multiple horizontal lines efficiently
     */
    public static List<Figure> createHorizontalLines(long startTime, long endTime, 
                                                   double[] prices, PathInfo[] paths) {
        if (prices.length != paths.length) {
            throw new IllegalArgumentException("Prices and paths arrays must have same length");
        }
        
        List<Figure> figures = new ArrayList<>(prices.length);
        for (int i = 0; i < prices.length; i++) {
            if (paths[i] != null) {
                figures.add(createLine(startTime, prices[i], endTime, prices[i], paths[i]));
            }
        }
        return figures;
    }
    
    /**
     * Clear all pools (for cleanup)
     */
    public static void clearAll() {
        figureListPool.clear();
        coordinatePool.clear();
    }
}