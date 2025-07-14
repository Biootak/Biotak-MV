package com.biotak;

import com.motivewave.platform.sdk.common.DataContext;
import com.motivewave.platform.sdk.common.Defaults;
import com.motivewave.platform.sdk.common.NVP;
import com.motivewave.platform.sdk.common.desc.DiscreteDescriptor;
import com.motivewave.platform.sdk.common.desc.BooleanDescriptor;
import com.motivewave.platform.sdk.common.desc.ColorDescriptor;
import com.motivewave.platform.sdk.common.desc.FontDescriptor;
import com.motivewave.platform.sdk.common.desc.IntegerDescriptor;
import com.motivewave.platform.sdk.common.desc.DoubleDescriptor;
import com.motivewave.platform.sdk.common.desc.StringDescriptor;
import com.motivewave.platform.sdk.common.desc.PathDescriptor;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;
import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.Instrument;
import com.motivewave.platform.sdk.common.Enums;
import com.motivewave.platform.sdk.common.Coordinate;
import com.motivewave.platform.sdk.draw.Line;
import com.motivewave.platform.sdk.common.PathInfo;
import com.motivewave.platform.sdk.common.LineInfo;
import java.awt.Color;
import com.motivewave.platform.sdk.common.X11Colors;
import java.util.ArrayList;
import java.util.List;
import com.motivewave.platform.sdk.common.BarSize;
import java.util.Map;
import java.util.HashMap;

/**
 * Biotak Trigger TH3 Indicator for MotiveWave.
 * This indicator calculates and displays key horizontal price levels based on historical volatility and price action.
 */
@StudyHeader(
    namespace = "com.biotak",
    id = "BIOTAK_TRIGGER_TH3",
    name = "Biotak Trigger TH3",
    label = "Biotak TH3",
    desc = "Replicates the Biotak Trigger TH3 indicator from MT4.",
    menu = "Biotak",
    overlay = true,
    studyOverlay = true
)
public class BiotakTrigger extends Study {

    // Timeframe to Percentage Mapping
    private static final Map<String, Double> FRACTAL_PERCENTAGES = new HashMap<>();
    static {
        FRACTAL_PERCENTAGES.put("M1", 0.02);
        FRACTAL_PERCENTAGES.put("M4", 0.04);
        FRACTAL_PERCENTAGES.put("M16", 0.08);
        FRACTAL_PERCENTAGES.put("H1+M4", 0.16);
        FRACTAL_PERCENTAGES.put("H4+M16", 0.32);
        FRACTAL_PERCENTAGES.put("H17+M4", 0.64);
        FRACTAL_PERCENTAGES.put("D2+H20+M16", 1.28);
        FRACTAL_PERCENTAGES.put("D11+H9+M4", 2.56);
        FRACTAL_PERCENTAGES.put("D45+H12+M16", 5.12);
    }

    // Define enums for settings, similar to MT4
    enum THStartPointType {
        MIDPOINT("Midpoint"),
        HISTORICAL_HIGH("Historical High"),
        HISTORICAL_LOW("Historical Low"),
        CUSTOM_PRICE("Custom Price");

        private final String value;
        THStartPointType(String value) { this.value = value; }
        @Override
        public String toString() { return value; }
    }

    // Setting Keys
    // General
    final static String S_OBJ_PREFIX = "objectPrefix";
    // Visual
    final static String S_FONT = "font";
    // Historical Lines
    final static String S_HIGH_LINE_PATH = "highLinePath";
    final static String S_LOW_LINE_PATH = "lowLinePath";
    // TH Levels
    final static String S_SHOW_TH_LEVELS = "showTHLevels";
    final static String S_SHOW_TRIGGER_LEVELS = "showTriggerLevels";
    final static String S_TRIGGER_PATH = "triggerPath";
    final static String S_MAX_LEVELS_ABOVE = "maxLevelsAbove";
    final static String S_MAX_LEVELS_BELOW = "maxLevelsBelow";
    // Structure Lines
    final static String S_SHOW_STRUCTURE_LINES = "showStructureLines";
    // L1
    final static String S_SHOW_STRUCT_L1 = "showStructL1";
    final static String S_STRUCT_L1_PATH = "structL1Path";
    // L2
    final static String S_SHOW_STRUCT_L2 = "showStructL2";
    final static String S_STRUCT_L2_PATH = "structL2Path";
    // L3
    final static String S_SHOW_STRUCT_L3 = "showStructL3";
    final static String S_STRUCT_L3_PATH = "structL3Path";
    // L4
    final static String S_SHOW_STRUCT_L4 = "showStructL4";
    final static String S_STRUCT_L4_PATH = "structL4Path";
    // L5
    final static String S_SHOW_STRUCT_L5 = "showStructL5";
    final static String S_STRUCT_L5_PATH = "structL5Path";
    // Display
    final static String S_SHOW_MIDPOINT = "showMidpoint";
    // ... many more setting keys to be added ...


    private static final String S_START_POINT = "startPointType";

    @Override
    public void initialize(Defaults defaults) {
        // Define settings to be configured by the user
        var sd = createSD();
        var tab = sd.addTab("General");

        var grp = tab.addGroup("General");
        grp.addRow(new StringDescriptor(S_OBJ_PREFIX, "Object Prefix", "BiotakTH3"));
        
        grp = tab.addGroup("Visual");
        grp.addRow(new FontDescriptor(S_FONT, "Label Font", defaults.getFont()));

        grp = tab.addGroup("Historical Lines");
        grp.addRow(new PathDescriptor(S_HIGH_LINE_PATH, "High Line", defaults.getRed(), 1.0f, null, true, false, false));
        grp.addRow(new PathDescriptor(S_LOW_LINE_PATH, "Low Line", defaults.getBlue(), 1.0f, null, true, false, false));
        
        tab = sd.addTab("Levels");
        grp = tab.addGroup("TH Levels");
        grp.addRow(new BooleanDescriptor(S_SHOW_TH_LEVELS, "Show TH Levels", true));
        grp.addRow(new BooleanDescriptor(S_SHOW_TRIGGER_LEVELS, "Show Trigger Levels", false));
        grp.addRow(new PathDescriptor(S_TRIGGER_PATH, "Trigger Line", X11Colors.DIM_GRAY, 1.0f, new float[] {3f, 3f} , true, false, false));
        grp.addRow(new IntegerDescriptor(S_MAX_LEVELS_ABOVE, "Max Levels Above", 100, 1, 10000, 1));
        grp.addRow(new IntegerDescriptor(S_MAX_LEVELS_BELOW, "Max Levels Below", 100, 1, 10000, 1));


        grp = tab.addGroup("Start Point");
        List<NVP> startPointOptions = new ArrayList<>();
        for(THStartPointType type : THStartPointType.values()) {
            startPointOptions.add(new NVP(type.toString(), type.name()));
        }
        grp.addRow(new DiscreteDescriptor(S_START_POINT, "TH Start Point", THStartPointType.MIDPOINT.name(), startPointOptions));
        
        tab = sd.addTab("Structure Lines");
        grp = tab.addGroup("Structure Lines");
        grp.addRow(new BooleanDescriptor(S_SHOW_STRUCTURE_LINES, "Show Structure Lines", true));

        // Level 1
        grp = tab.addGroup("Level 1");
        grp.addRow(new BooleanDescriptor(S_SHOW_STRUCT_L1, "Show", true));
        grp.addRow(new PathDescriptor(S_STRUCT_L1_PATH, "Path", defaults.getBlue(), 2.0f, null, true, false, false));

        // Level 2
        grp = tab.addGroup("Level 2");
        grp.addRow(new BooleanDescriptor(S_SHOW_STRUCT_L2, "Show", true));
        grp.addRow(new PathDescriptor(S_STRUCT_L2_PATH, "Path", X11Colors.DARK_GREEN, 2.0f, null, true, false, false));

        // Level 3
        grp = tab.addGroup("Level 3");
        grp.addRow(new BooleanDescriptor(S_SHOW_STRUCT_L3, "Show", true));
        grp.addRow(new PathDescriptor(S_STRUCT_L3_PATH, "Path", X11Colors.DARK_VIOLET, 2.0f, null, true, false, false));

        // Level 4
        grp = tab.addGroup("Level 4");
        grp.addRow(new BooleanDescriptor(S_SHOW_STRUCT_L4, "Show", true));
        grp.addRow(new PathDescriptor(S_STRUCT_L4_PATH, "Path", X11Colors.DARK_ORANGE, 2.0f, null, true, false, false));

        // Level 5
        grp = tab.addGroup("Level 5");
        grp.addRow(new BooleanDescriptor(S_SHOW_STRUCT_L5, "Show", true));
        grp.addRow(new PathDescriptor(S_STRUCT_L5_PATH, "Path", X11Colors.MAROON, 2.0f, null, true, false, false));
        
        tab = sd.addTab("Display");
        grp = tab.addGroup("Display Options");
        grp.addRow(new BooleanDescriptor(S_SHOW_MIDPOINT, "Show Midpoint Line", true));
    }

    private String getFractalTimeframe(BarSize barSize) {
        if (barSize.getType() == Enums.BarSizeType.LINEAR) {
            var intervalType = barSize.getIntervalType();
            int interval = barSize.getInterval();

            if (intervalType == Enums.IntervalType.MINUTE) {
                if (interval == 1) return "M1";
                if (interval <= 5) return "M4";
                if (interval <= 30) return "M16";
                if (interval <= 60) return "H1+M4";
                if (interval <= 240) return "H4+M16";
            }
            if (intervalType == Enums.IntervalType.DAY) return "H17+M4";
            if (intervalType == Enums.IntervalType.WEEK) return "D11+H9+M4";
            if (intervalType == Enums.IntervalType.MONTH) return "D45+H12+M16";
        }
        
        // Fallback for non-linear charts or unexpected types
        return "H4+M16"; 
    }

    private double getTimeframePercentage(BarSize barSize) {
        String fractalTf = getFractalTimeframe(barSize);
        return FRACTAL_PERCENTAGES.getOrDefault(fractalTf, 0.32); // Default to H4+M16's percentage
    }
    
    // Calculation Helper Methods - Ported from MQL4
    
    /**
     * Calculates the TH value. This is a core calculation from the original MQL4 code.
     * The logic with 'digits' is specific to how MT4 handles price normalization.
     */
    private double calculateTH(double price, int digits, double percentage) {
        if (price <= 0 || percentage <= 0) return 0;

        double baseValue = price;
        switch (digits) {
            case 0: baseValue /= 100.0; break;
            case 1: baseValue /= 10.0; break;
            case 2: break; // Default
            case 3: baseValue /= 10.0; break;
            case 4:
            case 5: baseValue *= 100.0; break;
            case 6: baseValue *= 1000.0; break;
            case 7:
            case 8: baseValue *= 10000.0; break;
        }
        return (baseValue * percentage) / 10.0;
    }

    /**
     * Calculates the step size in points for the TH levels.
     */
    private double calculateTHPoints(Instrument instrument, double price, double percentage) {
        if (price <= 0 || percentage <= 0) return 0;
        double point = instrument.getTickSize();
        if (point <= 0) return 0;
        
        // Determine digits from tick size
        int digits = 0;
        if (instrument.getTickSize() > 0) {
            String tickStr = String.valueOf(instrument.getTickSize());
            if (tickStr.contains(".")) {
                digits = tickStr.length() - tickStr.indexOf('.') - 1;
            }
        }

        double thValue = calculateTH(price, digits, percentage);
        double thStepPriceUnits = thValue / 10.0; // Another normalization from the original code
        
        return thStepPriceUnits / point;
    }


    @Override
    public void calculate(int index, DataContext ctx) {
        DataSeries series = ctx.getDataSeries();
        if (!series.isBarComplete(index)) return;

        // On the latest bar, clear all previously drawn figures
        if(index == series.size() - 1) {
            clearFigures();
        } else { // On historical bars, don't calculate to avoid clutter
            return;
        }

        int lookback = 200; 
        if (index < lookback) return;

        // Use previous bar's close for TH calculation base price
        double thBasePrice = series.getClose(index - 1);

        double highestHigh = series.highest(index, lookback, Enums.BarInput.HIGH);
        double lowestLow = series.lowest(index, lookback, Enums.BarInput.LOW);

        // Get the start point type from settings
        String startPointStr = getSettings().getString(S_START_POINT, THStartPointType.MIDPOINT.name());
        THStartPointType startPointType = THStartPointType.valueOf(startPointStr);
        
        double midpointPrice;
        switch(startPointType) {
            case HISTORICAL_HIGH: midpointPrice = highestHigh; break;
            case HISTORICAL_LOW: midpointPrice = lowestLow; break;
            case CUSTOM_PRICE:
            case MIDPOINT:
            default:
                midpointPrice = (highestHigh + lowestLow) / 2.0;
                break;
        }

        if (getSettings().getBoolean(S_SHOW_MIDPOINT)) {
            PathInfo path = getSettings().getPath(S_TRIGGER_PATH);
            long startTime = series.getStartTime(series.getStartIndex());
            long endTime = series.getStartTime(series.getEndIndex());
            addFigure(new Line(new Coordinate(startTime, midpointPrice), new Coordinate(endTime, midpointPrice), path));
        }

        if (!getSettings().getBoolean(S_SHOW_TH_LEVELS)) return;

        // This part is simplified for now. Will add timeframe-based percentage later.
        double timeframePercentage = getTimeframePercentage(series.getBarSize()); 
        double thStepInPoints = calculateTHPoints(series.getInstrument(), thBasePrice, timeframePercentage);

        if (thStepInPoints <= 0) return;

        double pointValue = series.getInstrument().getTickSize();
        int maxLevelsAbove = getSettings().getInteger(S_MAX_LEVELS_ABOVE);
        int maxLevelsBelow = getSettings().getInteger(S_MAX_LEVELS_BELOW);

        PathInfo triggerPath = getSettings().getPath(S_TRIGGER_PATH);
        long startTime = series.getStartTime(series.getStartIndex());
        long endTime = series.getStartTime(series.getEndIndex());
        
        // Draw levels above midpoint
        int stepCountAbove = 1;
        int levelCountAbove = 0;
        double priceLevelAbove = midpointPrice + (thStepInPoints * pointValue);
        while (priceLevelAbove <= highestHigh && levelCountAbove < maxLevelsAbove) {
            PathInfo path = getPathForLevel(stepCountAbove, true);
            if (path != null) {
                addFigure(new Line(new Coordinate(startTime, priceLevelAbove), new Coordinate(endTime, priceLevelAbove), path));
                levelCountAbove++;
            }
            priceLevelAbove += (thStepInPoints * pointValue);
            stepCountAbove++;
        }

        // Draw levels below midpoint
        int stepCountBelow = 1;
        int levelCountBelow = 0;
        double priceLevelBelow = midpointPrice - (thStepInPoints * pointValue);
        while (priceLevelBelow >= lowestLow && levelCountBelow < maxLevelsBelow) {
            PathInfo path = getPathForLevel(stepCountBelow, false);
            if (path != null) {
                addFigure(new Line(new Coordinate(startTime, priceLevelBelow), new Coordinate(endTime, priceLevelBelow), path));
                levelCountBelow++;
            }
            priceLevelBelow -= (thStepInPoints * pointValue);
            stepCountBelow++;
        }
    }

    /**
     * Determines which PathInfo to use based on the step count.
     * This replicates the logic of showing different lines for structure levels.
     * @param stepCount The current step/level number from the midpoint.
     * @param isAbove Is the level above the midpoint? (not used yet, but for future)
     * @return PathInfo for the correct line type, or null if this level should not be drawn.
     */
    private PathInfo getPathForLevel(int stepCount, boolean isAbove) {
        if (getSettings().getBoolean(S_SHOW_STRUCTURE_LINES)) {
            if (stepCount % 128 == 0 && getSettings().getBoolean(S_SHOW_STRUCT_L5)) return getSettings().getPath(S_STRUCT_L5_PATH);
            if (stepCount % 64 == 0 && getSettings().getBoolean(S_SHOW_STRUCT_L4)) return getSettings().getPath(S_STRUCT_L4_PATH);
            if (stepCount % 32 == 0 && getSettings().getBoolean(S_SHOW_STRUCT_L3)) return getSettings().getPath(S_STRUCT_L3_PATH);
            if (stepCount % 16 == 0 && getSettings().getBoolean(S_SHOW_STRUCT_L2)) return getSettings().getPath(S_STRUCT_L2_PATH);
            if (stepCount % 4 == 0 && getSettings().getBoolean(S_SHOW_STRUCT_L1)) return getSettings().getPath(S_STRUCT_L1_PATH);
        }

        if (getSettings().getBoolean(S_SHOW_TRIGGER_LEVELS)) {
            return getSettings().getPath(S_TRIGGER_PATH);
        }

        return null; // Don't draw anything if neither structure nor trigger lines are enabled for this step
    }
} 