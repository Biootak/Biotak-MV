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
    
    // 🌌 DARK THEME - Futuristic Cyber 2030 (Neo-Matrix Evolution) - Ultra High Contrast
    private static final ColorTheme DARK_THEME = new ColorTheme(
        new Color(12, 15, 25, 250),     // panelBgTop - Deep void gradient (higher opacity)
        new Color(8, 12, 20, 255),      // panelBgBottom - Absolute cosmic depth
        new Color(0, 255, 200, 90),     // panelBorder - Refined neon teal glow (reduced opacity)
        new Color(0, 0, 0, 120),        // panelShadow - Enhanced depth shadow
        new Color(255, 255, 255, 255),  // titleColor - PURE WHITE for maximum readability
        new Color(0, 255, 200, 150),    // titleShadow - Bright teal glow shadow for neon effect
        new Color(255, 255, 255, 255),  // contentColor - PURE WHITE for maximum contrast
        new Color(64, 255, 180, 85),    // highlightLine - Aqua accent (reduced opacity)
        new Color(18, 22, 35, 245),     // buttonBg - Ultra-dark button (higher opacity)
        new Color(0, 255, 200, 110),    // buttonBorder - Subtle teal glow
        new Color(255, 255, 255, 255),  // buttonText - Pure white (full opacity)
        new Color(64, 255, 180, 70),    // separatorColor - Soft aqua line
        new Color(10, 14, 22, 250),     // rulerBg - Deep space ruler (higher opacity)
        new Color(0, 255, 200, 130),    // rulerBorder - Elegant teal border
        new Color(255, 255, 255, 255),  // rulerText - Maximum contrast white
        new Color(255, 20, 147, 180),   // rulerLine - Vibrant magenta accent
        new Color(147, 0, 255, 120),    // levelLine - Electric purple levels
        new Color(255, 215, 0, 240)     // customPrice - Premium gold (higher opacity)
    );
    
    // ☀️ LIGHT THEME - Futuristic 2030 Modern Sophistication
    private static final ColorTheme LIGHT_THEME = new ColorTheme(
        new Color(240, 242, 247, 245),  // panelBgTop - Ultra-clean pearl white with subtle tech feel
        new Color(232, 236, 243, 255),  // panelBgBottom - Soft arctic gradient for depth
        new Color(45, 85, 165, 180),    // panelBorder - Refined quantum blue for sophistication
        new Color(180, 185, 195, 90),   // panelShadow - Subtle modern shadow
        new Color(15, 25, 45, 255),     // titleColor - Deep space black for ultra-sharp contrast
        new Color(45, 85, 165, 120),    // titleShadow - Quantum blue glow for tech elegance
        new Color(25, 35, 55, 255),     // contentColor - Dark neural blue for premium readability
        new Color(0, 180, 255, 140),    // highlightLine - Electric cyan accent for 2030 feel
        new Color(248, 250, 253, 240),  // buttonBg - Pristine white button surface
        new Color(45, 85, 165, 150),    // buttonBorder - Quantum blue interactive border
        new Color(15, 25, 45, 255),     // buttonText - Ultra-dark for maximum accessibility
        new Color(0, 180, 255, 100),    // separatorColor - Subtle electric cyan separator
        new Color(245, 247, 251, 250),  // rulerBg - Clean ruler background with tech precision
        new Color(45, 85, 165, 160),    // rulerBorder - Strong quantum blue ruler definition
        new Color(15, 25, 45, 255),     // rulerText - Ultra-contrast dark text
        new Color(255, 60, 120, 200),   // rulerLine - Vibrant electric magenta accent
        new Color(120, 50, 255, 160),   // levelLine - Future purple levels
        new Color(255, 165, 0, 250)     // customPrice - Premium golden amber
    );
    
    /**
     * Gets the current theme based on configuration and context
     */
    public static ColorTheme getCurrentTheme(DrawContext ctx, int transparency) {
        String themePreference = BiotakConfig.getInstance().getString("ui.theme", "auto");
        
        // Debug logging
        boolean debug = BiotakConfig.getInstance().getBoolean("ui.show.debug", false);
        if (debug) {
            System.out.println("[ThemeManager] Theme preference: " + themePreference + ", Transparency: " + transparency);
        }
        
        if ("dark".equalsIgnoreCase(themePreference)) {
            if (debug) System.out.println("[ThemeManager] Using DARK_THEME (forced)");
            return DARK_THEME;
        } else if ("light".equalsIgnoreCase(themePreference)) {
            if (debug) System.out.println("[ThemeManager] Using LIGHT_THEME (forced)");
            return LIGHT_THEME;
        } else {
            // Enhanced auto detection logic
            ColorTheme detected = detectThemeFromContext(ctx, transparency);
            if (debug) {
                System.out.println("[ThemeManager] Auto-detected theme: " + 
                    (detected == DARK_THEME ? "DARK" : "LIGHT"));
            }
            return detected;
        }
    }
    
    /**
     * Detects appropriate theme based on context analysis
     */
    private static ColorTheme detectThemeFromContext(DrawContext ctx, int transparency) {
        try {
            // Enhanced auto detection using multiple heuristics
            
            // Method 1: Try to analyze chart background color if possible
            if (ctx != null) {
                try {
                    // Get chart bounds and try to sample background
                    java.awt.Rectangle bounds = ctx.getBounds();
                    if (bounds != null && bounds.width > 0 && bounds.height > 0) {
                        // This is a heuristic approach - we can't directly sample pixels
                        // but we can use other indicators
                        
                        // Check if we're in a typical dark environment
                        // Dark themes usually have high transparency values
                        if (transparency > 200) {
                            return DARK_THEME;
                        }
                        
                        // Light themes usually have lower transparency
                        if (transparency < 120) {
                            return LIGHT_THEME;
                        }
                    }
                } catch (Exception e) {
                    // Continue with other methods
                }
            }
            
            // Method 2: Check system properties for theme hints
            try {
                // Check Windows theme
                String osName = System.getProperty("os.name", "").toLowerCase();
                if (osName.contains("windows")) {
                    // Try to detect Windows dark mode
                    String userTheme = System.getProperty("user.theme");
                    if (userTheme != null && userTheme.toLowerCase().contains("dark")) {
                        return DARK_THEME;
                    }
                }
                
                // Check other system properties
                String lookAndFeel = System.getProperty("swing.defaultlaf");
                if (lookAndFeel != null && lookAndFeel.toLowerCase().contains("dark")) {
                    return DARK_THEME;
                }
            } catch (Exception e) {
                // Continue with fallback
            }
            
            // Method 3: Use time-based heuristic (dark theme more likely at night)
            try {
                java.time.LocalTime now = java.time.LocalTime.now();
                int hour = now.getHour();
                
                // If it's night time (6 PM to 6 AM), bias towards dark theme
                if (hour >= 18 || hour <= 6) {
                    // Night time + medium/high transparency = probably dark theme
                    if (transparency >= 150) {
                        return DARK_THEME;
                    }
                } else {
                    // Day time + low/medium transparency = probably light theme
                    if (transparency <= 180) {
                        return LIGHT_THEME;
                    }
                }
            } catch (Exception e) {
                // Continue with final fallback
            }
            
            // Method 4: Enhanced transparency-based detection with better thresholds
            if (transparency > 190) {
                return DARK_THEME;  // Very high transparency suggests dark background
            } else if (transparency < 140) {
                return LIGHT_THEME; // Low transparency suggests light background
            } else {
                // Medium transparency: use additional heuristics
                // Default to dark theme for better contrast in uncertain cases
                return DARK_THEME;
            }
            
        } catch (Exception e) {
            // Ultimate fallback - use transparency only
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
