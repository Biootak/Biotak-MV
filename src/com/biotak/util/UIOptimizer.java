package com.biotak.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UI and graphics optimization utilities
 */
public final class UIOptimizer {
    
    // Cache for commonly used colors
    private static final ConcurrentHashMap<Integer, Color> colorCache = new ConcurrentHashMap<>();
    
    // Cache for fonts
    private static final ConcurrentHashMap<String, Font> fontCache = new ConcurrentHashMap<>();
    
    // Cache for font metrics
    private static final ConcurrentHashMap<Font, FontMetrics> fontMetricsCache = new ConcurrentHashMap<>();
    
    // Pre-allocated graphics objects
    private static final ThreadLocal<Graphics2D> TEMP_GRAPHICS = ThreadLocal.withInitial(() -> {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        return img.createGraphics();
    });
    
    // Common colors as constants to avoid repeated creation
    public static final Color TRANSPARENT_BLACK = new Color(0, 0, 0, 0);
    public static final Color SEMI_TRANSPARENT_BLACK = new Color(0, 0, 0, 128);
    public static final Color SEMI_TRANSPARENT_WHITE = new Color(255, 255, 255, 128);
    
    // Common strokes
    public static final BasicStroke THIN_STROKE = new BasicStroke(1.0f);
    public static final BasicStroke MEDIUM_STROKE = new BasicStroke(2.0f);
    public static final BasicStroke THICK_STROKE = new BasicStroke(3.0f);
    public static final BasicStroke DASHED_STROKE = new BasicStroke(1.0f, 
        BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f, 3.0f}, 0.0f);
    
    private UIOptimizer() {}
    
    /**
     * Get cached color to avoid repeated object creation
     */
    public static Color getCachedColor(int r, int g, int b, int a) {
        int key = (a << 24) | (r << 16) | (g << 8) | b;
        return colorCache.computeIfAbsent(key, k -> new Color(r, g, b, a));
    }
    
    /**
     * Get cached color with full opacity
     */
    public static Color getCachedColor(int r, int g, int b) {
        return getCachedColor(r, g, b, 255);
    }
    
    /**
     * Get cached font
     */
    public static Font getCachedFont(String name, int style, int size) {
        String key = name + "_" + style + "_" + size;
        return fontCache.computeIfAbsent(key, k -> new Font(name, style, size));
    }
    
    /**
     * Get cached font metrics
     */
    public static FontMetrics getCachedFontMetrics(Font font) {
        return fontMetricsCache.computeIfAbsent(font, f -> {
            Graphics2D g = TEMP_GRAPHICS.get();
            return g.getFontMetrics(f);
        });
    }
    
    /**
     * Fast string width calculation using cached font metrics
     */
    public static int getStringWidth(String text, Font font) {
        if (text == null || text.isEmpty()) return 0;
        FontMetrics fm = getCachedFontMetrics(font);
        return fm.stringWidth(text);
    }
    
    /**
     * Fast string height calculation using cached font metrics
     */
    public static int getStringHeight(Font font) {
        FontMetrics fm = getCachedFontMetrics(font);
        return fm.getHeight();
    }
    
    /**
     * Optimized rectangle drawing with caching
     */
    public static void drawOptimizedRect(Graphics2D g, int x, int y, int width, int height, 
                                       Color fillColor, Color borderColor, BasicStroke stroke) {
        // Set fill color and fill rectangle
        if (fillColor != null) {
            g.setColor(fillColor);
            g.fillRect(x, y, width, height);
        }
        
        // Set border color and stroke, then draw border
        if (borderColor != null && stroke != null) {
            g.setColor(borderColor);
            g.setStroke(stroke);
            g.drawRect(x, y, width, height);
        }
    }
    
    /**
     * Optimized rounded rectangle drawing
     */
    public static void drawOptimizedRoundRect(Graphics2D g, int x, int y, int width, int height, 
                                            int arcWidth, int arcHeight, Color fillColor, 
                                            Color borderColor, BasicStroke stroke) {
        // Set fill color and fill rectangle
        if (fillColor != null) {
            g.setColor(fillColor);
            g.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
        }
        
        // Set border color and stroke, then draw border
        if (borderColor != null && stroke != null) {
            g.setColor(borderColor);
            g.setStroke(stroke);
            g.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
        }
    }
    
    /**
     * Optimized text drawing with anti-aliasing
     */
    public static void drawOptimizedText(Graphics2D g, String text, int x, int y, 
                                       Font font, Color color, boolean antiAlias) {
        if (text == null || text.isEmpty()) return;
        
        // Store original settings
        Font originalFont = g.getFont();
        Color originalColor = g.getColor();
        Object originalAntiAlias = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        
        try {
            // Set new settings
            g.setFont(font);
            g.setColor(color);
            
            if (antiAlias) {
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                                 RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            }
            
            // Draw text
            g.drawString(text, x, y);
            
        } finally {
            // Restore original settings
            g.setFont(originalFont);
            g.setColor(originalColor);
            if (originalAntiAlias != null) {
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, originalAntiAlias);
            }
        }
    }
    
    /**
     * Optimized line drawing
     */
    public static void drawOptimizedLine(Graphics2D g, int x1, int y1, int x2, int y2, 
                                       Color color, BasicStroke stroke, boolean antiAlias) {
        // Store original settings
        Color originalColor = g.getColor();
        Stroke originalStroke = g.getStroke();
        Object originalAntiAlias = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        
        try {
            // Set new settings
            g.setColor(color);
            g.setStroke(stroke);
            
            if (antiAlias) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                                 RenderingHints.VALUE_ANTIALIAS_ON);
            }
            
            // Draw line
            g.drawLine(x1, y1, x2, y2);
            
        } finally {
            // Restore original settings
            g.setColor(originalColor);
            g.setStroke(originalStroke);
            if (originalAntiAlias != null) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, originalAntiAlias);
            }
        }
    }
    
    /**
     * Create optimized buffered image
     */
    public static BufferedImage createOptimizedImage(int width, int height, boolean hasAlpha) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        
        int imageType = hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        
        // Create compatible image for better performance
        BufferedImage image = gc.createCompatibleImage(width, height, 
            hasAlpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE);
        
        return image != null ? image : new BufferedImage(width, height, imageType);
    }
    
    /**
     * Batch color operations for better performance
     */
    public static Color[] createColorGradient(Color startColor, Color endColor, int steps) {
        if (steps <= 0) return new Color[0];
        if (steps == 1) return new Color[]{startColor};
        
        Color[] gradient = new Color[steps];
        
        int startR = startColor.getRed();
        int startG = startColor.getGreen();
        int startB = startColor.getBlue();
        int startA = startColor.getAlpha();
        
        int endR = endColor.getRed();
        int endG = endColor.getGreen();
        int endB = endColor.getBlue();
        int endA = endColor.getAlpha();
        
        for (int i = 0; i < steps; i++) {
            float ratio = (float) i / (steps - 1);
            
            int r = (int) (startR + ratio * (endR - startR));
            int g = (int) (startG + ratio * (endG - startG));
            int b = (int) (startB + ratio * (endB - startB));
            int a = (int) (startA + ratio * (endA - startA));
            
            gradient[i] = getCachedColor(r, g, b, a);
        }
        
        return gradient;
    }
    
    /**
     * Get UI optimization statistics
     */
    public static String getUIStats() {
        return String.format(
            "UI Cache - Colors: %d, Fonts: %d, FontMetrics: %d",
            colorCache.size(),
            fontCache.size(),
            fontMetricsCache.size()
        );
    }
    
    /**
     * Clear UI caches
     */
    public static void clearCaches() {
        colorCache.clear();
        fontCache.clear();
        fontMetricsCache.clear();
    }
}