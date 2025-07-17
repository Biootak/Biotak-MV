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

    // Setting Keys
    // General
    public final static String S_OBJ_PREFIX = "objectPrefix";
    // Visual
    public final static String S_FONT = "font";
    // Historical Lines
    public final static String S_SHOW_HIGH_LINE = "showHighLine";
    public final static String S_HIGH_LINE_PATH = "highLinePath";
    public final static String S_SHOW_LOW_LINE = "showLowLine";
    public final static String S_LOW_LINE_PATH = "lowLinePath";
    public final static String S_HISTORICAL_HIGH = "historicalHigh";
    public final static String S_HISTORICAL_LOW = "historicalLow";
    // Manual Override
    public final static String S_MANUAL_HL_ENABLE = "manualHLEnable";
    public static final String S_MANUAL_HIGH = "manualHigh";
    public static final String S_MANUAL_LOW = "manualLow";
    // Info Panel
    public static final String S_PANEL_POSITION = "panelPosition";
    public static final String S_SHOW_INFO_PANEL = "showInfoPanel";
    public static final String S_PANEL_MARGIN_X = "panelMarginX";
    public static final String S_PANEL_MARGIN_Y = "panelMarginY";
    public static final String S_PANEL_TRANSPARENCY = "panelTransparency";
    public static final String S_TITLE_FONT = "titleFont";
    public static final String S_CONTENT_FONT = "contentFont";
    // TH Levels
    public final static String S_SHOW_TH_LEVELS = "showTHLevels";
    public final static String S_SHOW_TRIGGER_LEVELS = "showTriggerLevels";
    public final static String S_TRIGGER_PATH = "triggerPath";
    public final static String S_MAX_LEVELS_ABOVE = "maxLevelsAbove";
    public final static String S_MAX_LEVELS_BELOW = "maxLevelsBelow";
    public final static String S_START_POINT = "startPointType";
    // Structure Lines
    public final static String S_SHOW_STRUCTURE_LINES = "showStructureLines";
    // L1
    public final static String S_SHOW_STRUCT_L1 = "showStructL1";
    public final static String S_STRUCT_L1_PATH = "structL1Path";
    // L2
    public final static String S_SHOW_STRUCT_L2 = "showStructL2";
    public final static String S_STRUCT_L2_PATH = "structL2Path";
    // L3
    public final static String S_SHOW_STRUCT_L3 = "showStructL3";
    public final static String S_STRUCT_L3_PATH = "structL3Path";
    // L4
    public final static String S_SHOW_STRUCT_L4 = "showStructL4";
    public final static String S_STRUCT_L4_PATH = "structL4Path";
    // L5
    public final static String S_SHOW_STRUCT_L5 = "showStructL5";
    public final static String S_STRUCT_L5_PATH = "structL5Path";
    // Display
    public final static String S_SHOW_MIDPOINT = "showMidpoint";
    // Custom Price Line
    public final static String S_CUSTOM_PRICE_PATH = "customPricePath";
    // ------------------  SS / LS Step Mode ------------------
    public final static String S_STEP_MODE = "stepMode";          // TH or SS/LS
    public final static String S_LS_FIRST = "lsFirst";            // If true → draw LS before SS
    public final static String S_SSLS_BASIS = "sslsBasis";        // Structure / Pattern / Trigger / Auto / Higher
    public final static String S_LOCK_SSLS_LEVELS = "lockSsLsLevels"; // If true, keep SS/LS levels fixed when switching timeframes
} 