package com.biotak.ui;

import com.biotak.config.BiotakConfig;
import com.motivewave.platform.sdk.common.DrawContext;
import java.awt.Color;

/**
 * Central theme management system for all UI components
 * Ensures consistent colors across panels, rulers, and other elements
 */
public class ThemeManager {
    
    /**
     * Unified color theme system for all UI components
     */
    public static class ColorTheme {
        // Panel colors
        public final Color panelBgTop;
        public final Color panelBgBottom;
        public final Color panelBorder;
        public final Color panelShadow;
        
        // Text colors
        public final Color titleColor;
        public final Color titleShadow;
        public final Color contentColor;
        public final Color highlightLine;
        
        // Button colors
        public final Color buttonBg;
        public final Color buttonBorder;
        public final Color buttonText;
        
        // Separator colors
        public final Color separatorColor;
        
        // Ruler colors
        public final Color rulerBg;
        public final Color rulerBorder;
        public final Color rulerText;
        public final Color rulerLine;
        
        // Chart element colors
        public final Color levelLine;
        public final Color customPrice;
        
        public ColorTheme(Color panelBgTop, Color panelBgBottom, Color panelBorder, Color panelShadow,
                         Color titleColor, Color titleShadow, Color contentColor, Color highlightLine,
                         Color buttonBg, Color buttonBorder, Color buttonText, Color separatorColor,
                         Color rulerBg, Color rulerBorder, Color rulerText, Color rulerLine,
                         Color levelLine, Color customPrice) {
            this.panelBgTop = panelBgTop;
            this.panelBgBottom = panelBgBottom;
            this.panelBorder = panelBorder;
            this.panelShadow = panelShadow;
            this.titleColor = titleColor;
            this.titleShadow = titleShadow;
            this.contentColor = contentColor;
            this.highlightLine = highlightLine;
            this.buttonBg = buttonBg;
            this.buttonBorder = buttonBorder;
            this.buttonText = buttonText;
            this.separatorColor = separatorColor;
            this.rulerBg = rulerBg;
            this.rulerBorder = rulerBorder;
            this.rulerText = rulerText;
            this.rulerLine = rulerLine;
            this.levelLine = levelLine;
            this.customPrice = customPrice;
        }
    }
    
    // Dark theme for dark chart backgrounds
    private static final ColorTheme DARK_THEME = new ColorTheme(
        new Color(60, 60, 60, 220),     // panelBgTop - lighter gray
        new Color(40, 40, 40, 220),     // panelBgBottom - darker gray
        new Color(80, 80, 80, 180),     // panelBorder - medium gray
        new Color(0, 0, 0, 60),         // panelShadow - black shadow
        new Color(220, 220, 220),       // titleColor - light gray text
        new Color(0, 0, 0, 120),        // titleShadow - dark shadow
        new Color(200, 200, 200),       // contentColor - light content text
        new Color(100, 100, 100, 150),  // highlightLine - subtle highlight
        new Color(70, 70, 70, 200),     // buttonBg - button background
        new Color(90, 90, 90, 180),     // buttonBorder - button border
        new Color(240, 240, 240),       // buttonText - white button text
        new Color(100, 100, 100, 200),  // separatorColor - separator line
        new Color(50, 50, 50, 220),     // rulerBg - dark ruler background
        new Color(80, 80, 80, 180),     // rulerBorder - ruler border
        new Color(220, 220, 220),       // rulerText - light ruler text
        new Color(120, 120, 255),       // rulerLine - blue ruler line
        new Color(180, 180, 180, 150),  // levelLine - subtle level lines
        new Color(255, 215, 0, 200)     // customPrice - gold for custom price
    );
    
    // Light theme for light chart backgrounds
    private static final ColorTheme LIGHT_THEME = new ColorTheme(
        new Color(240, 240, 240, 220),  // panelBgTop - very light gray
        new Color(200, 200, 200, 220),  // panelBgBottom - light gray
        new Color(120, 120, 120, 180),  // panelBorder - medium gray
        new Color(0, 0, 0, 40),         // panelShadow - light shadow
        new Color(40, 40, 40),          // titleColor - dark text
        new Color(255, 255, 255, 100),  // titleShadow - light shadow
        new Color(60, 60, 60),          // contentColor - dark content text
        new Color(180, 180, 180, 120),  // highlightLine - light highlight
        new Color(220, 220, 220, 200),  // buttonBg - light button background
        new Color(140, 140, 140, 180),  // buttonBorder - darker border
        new Color(40, 40, 40),          // buttonText - dark button text
        new Color(140, 140, 140, 200),  // separatorColor - darker separator
        new Color(250, 250, 250, 220),  // rulerBg - light ruler background
        new Color(120, 120, 120, 180),  // rulerBorder - ruler border
        new Color(40, 40, 40),          // rulerText - dark ruler text
        new Color(0, 100, 200),         // rulerLine - blue ruler line
        new Color(100, 100, 100, 150),  // levelLine - darker level lines
        new Color(255, 140, 0, 200)     // customPrice - orange for custom price
    );
    
    /**
     * Gets the current theme based on configuration and context
     */
    public static ColorTheme getCurrentTheme(DrawContext ctx, int transparency) {
        String themePreference = BiotakConfig.getInstance().getString("ui.theme", "auto");
        
        if ("dark".equalsIgnoreCase(themePreference)) {
            return DARK_THEME;
        } else if ("light".equalsIgnoreCase(themePreference)) {
            return LIGHT_THEME;
        } else {
            // Enhanced auto detection logic
            return detectThemeFromContext(ctx, transparency);
        }
    }
    
    /**
     * Detects appropriate theme based on context analysis
     */
    private static ColorTheme detectThemeFromContext(DrawContext ctx, int transparency) {
        try {
            // Try to analyze chart background if possible
            // This is a simplified approach - could be enhanced with actual background sampling
            
            // If transparency is very high, likely a dark background
            if (transparency > 200) {
                return DARK_THEME;
            }
            
            // If transparency is very low, likely a light background  
            if (transparency < 100) {
                return LIGHT_THEME;
            }
            
            // For medium transparency, use a more sophisticated approach
            // Check system theme if available
            String osTheme = System.getProperty("sun.desktop");
            if (osTheme != null && osTheme.toLowerCase().contains("dark")) {
                return DARK_THEME;
            }
            
            // Default fallback based on transparency threshold
            return transparency > 150 ? DARK_THEME : LIGHT_THEME;
            
        } catch (Exception e) {
            // Fallback to simple transparency-based detection
            return transparency > 150 ? DARK_THEME : LIGHT_THEME;
        }
    }
    
    /**
     * Gets themed color with custom transparency
     */
    public static Color getThemedColor(Color baseColor, int alpha) {
        return new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 
                        Math.min(alpha, baseColor.getAlpha()));
    }
    
    /**
     * Checks if adaptive colors are enabled
     */
    public static boolean isAdaptiveColorsEnabled() {
        return BiotakConfig.getInstance().getBoolean("ui.adaptive.colors", true);
    }
    
    /**
     * Gets chart background compatibility factor
     * Returns 1.0 for dark backgrounds, -1.0 for light backgrounds
     */
    public static double getBackgroundCompatibility(DrawContext ctx) {
        // This could be enhanced to actually analyze the chart background
        // For now, we use the theme preference as a proxy
        String themePreference = BiotakConfig.getInstance().getString("ui.theme", "auto");
        
        if ("dark".equalsIgnoreCase(themePreference)) {
            return 1.0;
        } else if ("light".equalsIgnoreCase(themePreference)) {
            return -1.0;
        } else {
            // Auto detection - could be enhanced with actual background analysis
            return 0.0;
        }
    }
}
