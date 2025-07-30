package com.biotak.util;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

/**
 * Contains all the constants used in the BiotakTrigger study.
 * This includes setting keys and other static values.
 */
public final class Constants {

    private Constants() {
        // Private constructor to prevent instantiation
    }

    // Timeframe to Percentage Mapping
    public static final Map<String, Double> FRACTAL_PERCENTAGES;
    static {
        Map<String, Double> map = new HashMap<>();
        // Second-based timeframes
        map.put("S16", 0.01);     // 16 seconds - 1% of current price
        
        // Minute-based timeframes
        map.put("M1", 0.02);
        map.put("M4", 0.04);
        map.put("M16", 0.08);
        map.put("H1+M4", 0.16);
        map.put("H4+M16", 0.32);
        map.put("H17+M4", 0.64);
        map.put("D2+H20+M16", 1.28);
        map.put("D11+H9+M4", 2.56);
        map.put("D45+H12+M16", 5.12);
        // Extended fractal timeframes (continuing the pattern)
        map.put("D182+H2+M4", 10.24);   // ~6 months
        map.put("D365+H1", 20.48);      // ~1 year
        map.put("D730+H2", 40.96);      // ~2 years
        map.put("D1460+H4", 81.92);     // ~4 years
        map.put("D2920+H8", 163.84);    // ~8 years
        FRACTAL_PERCENTAGES = Collections.unmodifiableMap(map);
    }

    public static final String S_MSTEP_BASIS = "mstepBasis";
    public static final double TH_TO_M_FACTOR = 5.25; // Multiplier to convert TH to M distance
    public static final double SS_MULTIPLIER = 1.5;   // Short-Step is 1.5 × TH
    public static final double LS_MULTIPLIER = 2.0;   // Long-Step is 2   × TH
    public static final double ATR_FACTOR    = 3.0;   // Default factor for 3×ATR calculations
    
    // Theme System Constants
    public static final String S_UI_THEME = "ui.theme";             // Theme selection: auto, light, dark
    public static final String S_ADAPTIVE_COLORS = "ui.adaptive.colors"; // Enable adaptive color system
    public static final String S_PANEL_COLOR_THEME = "panel.color.theme"; // Panel-specific theme
    public static final String S_LEVEL_COLOR_THEME = "level.color.theme"; // Level-specific theme
}
