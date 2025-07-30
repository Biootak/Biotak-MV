package com.biotak.ui;

import com.biotak.enums.THStartPointType;
import com.biotak.debug.AdvancedLogger;
import com.biotak.util.TimeframeUtil;
import com.biotak.core.FractalCalculator;
import com.motivewave.platform.sdk.common.Coordinate;
import com.motivewave.platform.sdk.common.DataContext;
import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.PathInfo;
import com.motivewave.platform.sdk.common.Settings;
import com.motivewave.platform.sdk.common.X11Colors;
import com.motivewave.platform.sdk.draw.Line;
import com.motivewave.platform.sdk.draw.Figure;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static com.biotak.util.Constants.*;
import static com.biotak.config.SettingsRepository.*;

public class LevelDrawer {

    /**
     * Draws the historical high and low lines on the chart if they are enabled in the settings.
     */
    public static List<Figure> drawHistoricalLines(Settings settings, long startTime, long endTime, double high, double low) {
        boolean showHigh = settings.getBoolean(S_SHOW_HIGH_LINE, true);
        boolean showLow = settings.getBoolean(S_SHOW_LOW_LINE, true);
        
        // Pre-allocate list size for better performance
        List<Figure> figures = new ArrayList<>((showHigh ? 1 : 0) + (showLow ? 1 : 0));
        
        if (showHigh) {
            PathInfo highPath = settings.getPath(S_HIGH_LINE_PATH);
            figures.add(new Line(new Coordinate(startTime, high), new Coordinate(endTime, high), highPath));
        }

        if (showLow) {
            PathInfo lowPath = settings.getPath(S_LOW_LINE_PATH);
            figures.add(new Line(new Coordinate(startTime, low), new Coordinate(endTime, low), lowPath));
        }
        return figures;
    }

    /**
     * Determines the starting price for TH levels based on user settings.
     * @return The calculated midpoint price.
     */
    public static double determineMidpointPrice(Settings settings, double high, double low) {
        String startPointStr = settings.getString(S_START_POINT, THStartPointType.MIDPOINT.name());
        THStartPointType startPointType = THStartPointType.valueOf(startPointStr);

        switch (startPointType) {
            case HISTORICAL_HIGH: return high;
            case HISTORICAL_LOW: return low;
            case CUSTOM_PRICE:
                // Use stored custom price, fallback to midpoint if not set
                double cp = settings.getDouble(S_CUSTOM_PRICE, 0);
                return cp != 0 ? cp : (high + low) / 2.0;
            case MIDPOINT:
            default:
                return (high + low) / 2.0;
        }
    }

    /**
     * Draws the midpoint line on the chart if it is enabled in the settings.
     */
    public static List<Figure> drawMidpointLine(Settings settings, long startTime, long endTime, double midpointPrice) {
        List<Figure> figures = new ArrayList<>();
        if (settings.getBoolean(S_SHOW_MIDPOINT, true)) {
            PathInfo path = settings.getPath(S_TRIGGER_PATH);
            figures.add(new Line(new Coordinate(startTime, midpointPrice), new Coordinate(endTime, midpointPrice), path));
        }
        return figures;
    }

    /**
     * Calculates and draws all the "TH" (Trigger and Structure) levels above and below the midpoint.
     */
    public static List<Figure> drawTHLevels(Settings settings, DataSeries series, double midpointPrice, double highestHigh, double lowestLow, double thStepInPoints, long startTime, long endTime) {

        if (thStepInPoints <= 0) {
            AdvancedLogger.warn("LevelDrawer", "drawTHLevels", "Invalid TH step value (<=0). Cannot draw TH levels.");
            return new ArrayList<>();
        }

        double pointValue = series.getInstrument().getTickSize();

        // Final price distance between consecutive TH levels
        // Removed pipMultiplier scaling: keep distance in price units
        double stepPrice = thStepInPoints * pointValue;
        int maxLevelsAbove = settings.getInteger(S_MAX_LEVELS_ABOVE);
        int maxLevelsBelow = settings.getInteger(S_MAX_LEVELS_BELOW);

        List<Figure> figures = new ArrayList<>(maxLevelsAbove + maxLevelsBelow); // Pre-allocate capacity
        // Draw levels above midpoint
        int stepCountAbove = 1;
        int levelCountAbove = 0;
        double priceLevelAbove = midpointPrice + stepPrice;
        while (priceLevelAbove <= highestHigh && levelCountAbove < maxLevelsAbove) {
            PathInfo path = getPathForLevel(settings, stepCountAbove);
            if (path != null) {
                // Remove debug logging to improve performance
                figures.add(new Line(new Coordinate(startTime, priceLevelAbove), new Coordinate(endTime, priceLevelAbove), path));
                levelCountAbove++;
            }
            priceLevelAbove += stepPrice;
            stepCountAbove++;
        }

        // Draw levels below midpoint
        int stepCountBelow = 1;
        int levelCountBelow = 0;
        double priceLevelBelow = midpointPrice - stepPrice;
        while (priceLevelBelow >= lowestLow && levelCountBelow < maxLevelsBelow) {
            PathInfo path = getPathForLevel(settings, stepCountBelow);
            if (path != null) {
                // Remove debug logging to improve performance
                figures.add(new Line(new Coordinate(startTime, priceLevelBelow), new Coordinate(endTime, priceLevelBelow), path));
                levelCountBelow++;
            }
            priceLevelBelow -= stepPrice;
            stepCountBelow++;
        }
        return figures;
    }

    /**
     * Determines which PathInfo to use based on the step count.
     * This replicates the logic of showing different lines for structure levels.
     * @param stepCount The current step/level number from the midpoint.
     * @return PathInfo for the correct line type, or null if this level should not be drawn.
     */
    public static PathInfo getPathForLevel(Settings settings, int stepCount) {
        if (settings.getBoolean(S_SHOW_STRUCTURE_LINES)) {
            if (stepCount % 128 == 0 && settings.getBoolean(S_SHOW_STRUCT_L5)) return settings.getPath(S_STRUCT_L5_PATH);
            if (stepCount % 64 == 0 && settings.getBoolean(S_SHOW_STRUCT_L4)) return settings.getPath(S_STRUCT_L4_PATH);
            if (stepCount % 32 == 0 && settings.getBoolean(S_SHOW_STRUCT_L3)) return settings.getPath(S_STRUCT_L3_PATH);
            if (stepCount % 16 == 0 && settings.getBoolean(S_SHOW_STRUCT_L2)) return settings.getPath(S_STRUCT_L2_PATH);
            if (stepCount % 4 == 0 && settings.getBoolean(S_SHOW_STRUCT_L1)) return settings.getPath(S_STRUCT_L1_PATH);
        }

        if (settings.getBoolean(S_SHOW_TRIGGER_LEVELS)) {
            return settings.getPath(S_TRIGGER_PATH);
        }

        return null; // Don't draw anything if neither structure nor trigger lines are enabled for this step
    }

    /**
     * Draws variable-distance levels alternating between Short Step (SS) and Long Step (LS).
     *
     * @param series         Data series (for instrument/tick size if needed)
     * @param midpointPrice  Center reference price
     * @param highestHigh    Upper bound for drawing
     * @param lowestLow      Lower bound for drawing
     * @param ssValue        Short-Step distance (price units)
     * @param lsValue        Long-Step distance  (price units)
     * @param lsFirst        If true, first step after midpoint uses LS, otherwise SS
     * @param startTime      Start timestamp for horizontal lines
     * @param endTime        End timestamp for horizontal lines
     */
    public static List<Figure> drawSSLSLevels(Settings settings, DataSeries series, double midpointPrice, double highestHigh, double lowestLow,
                                double ssValue, double lsValue, boolean lsFirst,
                                long startTime, long endTime) {
        if (ssValue <= 0 || lsValue <= 0) {
            AdvancedLogger.warn("LevelDrawer", "drawSSLSLevels", "Invalid SS/LS values – cannot draw levels.");
            return new ArrayList<>();
        }

        double pipMultiplier = com.biotak.util.UnitConverter.getPipMultiplier(series.getInstrument()); // retained for potential debug logs
        // Removed pipMultiplier scaling: ssValue/lsValue are already in price units
        double stepSS = ssValue;
        double stepLS = lsValue;
        double[] stepDistances = new double[]{lsFirst ? stepLS : stepSS, lsFirst ? stepSS : stepLS};

        int drawnAbove = 0;
        int logicalStep = 0; // counts every step (SS/LS) processed
        double cumulative = 0;
        int maxLevelsAbove = settings.getInteger(S_MAX_LEVELS_ABOVE);
        List<Figure> figures = new ArrayList<>();
        while (drawnAbove < maxLevelsAbove) {
            double dist = stepDistances[logicalStep % 2];
            cumulative += dist;
            logicalStep++;
            double priceLevel = midpointPrice + cumulative;
            if (priceLevel > highestHigh) break;

            if (!shouldDrawStep(settings, logicalStep)) {
                continue;
            }
            PathInfo path = getPathForLevel(settings, logicalStep);
            if (path == null) {
                // Fall back to specific SS/LS paths when structure/trigger paths are disabled
                boolean isSS = ((logicalStep % 2 == 0) == lsFirst); // logicalStep starts at 1 after ++, so evaluate before use
                path = isSS ? settings.getPath(S_SS_LEVEL_PATH) : settings.getPath(S_LS_LEVEL_PATH);
            }
            if (path != null) {
                figures.add(new Line(new Coordinate(startTime, priceLevel), new Coordinate(endTime, priceLevel), path));
                drawnAbove++;
            }
        }

        int drawnBelow = 0;
        logicalStep = 0;
        cumulative = 0;
        int maxLevelsBelow = settings.getInteger(S_MAX_LEVELS_BELOW);
        while (drawnBelow < maxLevelsBelow) {
            double dist = stepDistances[logicalStep % 2];
            cumulative += dist;
            logicalStep++;
            double priceLevel = midpointPrice - cumulative;
            if (priceLevel < lowestLow) break;

            if (!shouldDrawStep(settings, logicalStep)) {
                continue;
            }
            PathInfo path = getPathForLevel(settings, logicalStep);
            if (path == null) {
                boolean isSS = ((logicalStep % 2 == 0) == lsFirst);
                path = isSS ? settings.getPath(S_SS_LEVEL_PATH) : settings.getPath(S_LS_LEVEL_PATH);
            }
            if (path != null) {
                figures.add(new Line(new Coordinate(startTime, priceLevel), new Coordinate(endTime, priceLevel), path));
                drawnBelow++;
            }
        }
        return figures;
    }

    /**
     * Determines if a step/level should be drawn based on Trigger visibility.
     * If Trigger lines are enabled, all steps are drawn. Otherwise only steps
     * that are multiples of 4 (structure highlights) are rendered.
     */
    private static boolean shouldDrawStep(Settings settings, int step) {
        if (settings.getBoolean(S_SHOW_TRIGGER_LEVELS)) return true;
        return step % 4 == 0;
    }


    /**
     * Draws Control-increment levels (distance = C). Every third level (3C) is
     * labeled/colored as "M" to denote reaching the Structure distance.
     */
    public static List<Figure> drawMLevels(
            Settings settings,
            DataSeries series,
            double midpointPrice,
            double highestHigh,
            double lowestLow,
            double controlDistance,
            long startTime,
            long endTime) {

        if (controlDistance <= 0) {
            AdvancedLogger.warn("LevelDrawer", "drawMLevels", "Invalid C distance – cannot draw M ladder.");
            return new java.util.ArrayList<>();
        }

        boolean showLabels = settings.getBoolean(S_SHOW_LEVEL_LABELS, true);
        int maxAbove = settings.getInteger(S_MAX_LEVELS_ABOVE);
        int maxBelow = settings.getInteger(S_MAX_LEVELS_BELOW);

        java.util.List<Figure> figs = new java.util.ArrayList<>();

        // ------------- ABOVE -------------
        int logicalStep = 1; // 1 => 1×C
        int drawnAbove = 0;
        double priceAbove = midpointPrice + controlDistance;
        while (priceAbove <= highestHigh && drawnAbove < maxAbove) {
            boolean isM = (logicalStep % 3 == 0);
            String lbl = isM ? "M" : "C";

            if (!shouldDrawStep(settings, logicalStep)) {
                logicalStep++;
                priceAbove += controlDistance;
                continue;
            }
            PathInfo path = getPathForLevel(settings, logicalStep);
            if (path == null && settings.getBoolean(S_SHOW_TRIGGER_LEVELS)) {
                path = settings.getPath(S_TRIGGER_PATH);
            }
            if (path != null) {
                figs.add(new Line(new Coordinate(startTime, priceAbove), new Coordinate(endTime, priceAbove), path));
                if (showLabels) figs.add(new LevelLabel(endTime, priceAbove, lbl));
                drawnAbove++;
            }

            logicalStep++;
            priceAbove += controlDistance;
        }

        // ------------- BELOW -------------
        logicalStep = 1;
        int drawnBelow = 0;
        double priceBelow = midpointPrice - controlDistance;
        while (priceBelow >= lowestLow && drawnBelow < maxBelow) {
            boolean isM2 = (logicalStep % 3 == 0);
            String lbl2 = isM2 ? "M" : "C";

            if (!shouldDrawStep(settings, logicalStep)) {
                logicalStep++;
                priceBelow -= controlDistance;
                continue;
            }

            PathInfo path = getPathForLevel(settings, logicalStep);
            if (path == null && settings.getBoolean(S_SHOW_TRIGGER_LEVELS)) {
                path = settings.getPath(S_TRIGGER_PATH);
            }
            if (path != null) {
                figs.add(new Line(new Coordinate(startTime, priceBelow), new Coordinate(endTime, priceBelow), path));
                if (showLabels) figs.add(new LevelLabel(endTime, priceBelow, lbl2));
                drawnBelow++;
            }

            logicalStep++;
            priceBelow -= controlDistance;
        }

        return figs;
    }

    /**
     * Draws levels at equal spacing of mDistance, labelling each as "M".
     */
    public static List<Figure> drawMEqualLevels(
            Settings settings,
            double midpointPrice,
            double highestHigh,
            double lowestLow,
            double mDistance,
            long startTime,
            long endTime) {

        if (mDistance <= 0) {
            AdvancedLogger.warn("LevelDrawer", "drawMEqualLevels", "Invalid M distance – cannot draw levels.");
            return new java.util.ArrayList<>();
        }

        boolean showLabels = settings.getBoolean(S_SHOW_LEVEL_LABELS, true);
        int maxAbove = settings.getInteger(S_MAX_LEVELS_ABOVE);
        int maxBelow = settings.getInteger(S_MAX_LEVELS_BELOW);

        java.util.List<Figure> figs = new java.util.ArrayList<>();

        // Above
        int step = 1;
        double price = midpointPrice + mDistance;
        while (price <= highestHigh && step <= maxAbove) {
            PathInfo path = getPathForLevel(settings, step);
            if (path == null) {
                boolean triggerOn = settings.getBoolean(S_SHOW_TRIGGER_LEVELS);
                path = triggerOn ? settings.getPath(S_TRIGGER_PATH) : settings.getPath(S_STRUCT_L1_PATH);
            }
            if (path != null) {
                figs.add(new Line(new Coordinate(startTime, price), new Coordinate(endTime, price), path));
                if (showLabels) figs.add(new LevelLabel(endTime, price, "M"));
            }
            step++;
            price += mDistance;
        }

        // Below
        step = 1;
        price = midpointPrice - mDistance;
        while (price >= lowestLow && step <= maxBelow) {
            PathInfo path = getPathForLevel(settings, step);
            if (path == null) {
                boolean triggerOn = settings.getBoolean(S_SHOW_TRIGGER_LEVELS);
                path = triggerOn ? settings.getPath(S_TRIGGER_PATH) : settings.getPath(S_STRUCT_L1_PATH);
            }
            if (path != null) {
                figs.add(new Line(new Coordinate(startTime, price), new Coordinate(endTime, price), path));
                if (showLabels) figs.add(new LevelLabel(endTime, price, "M"));
            }
            step++;
            price -= mDistance;
        }

        return figs;
    }

    /**
     * Draws levels at equal spacing of eDistance, labelling each as \"E\".
     */
    public static List<Figure> drawELevels(
            Settings settings,
            double midpointPrice,
            double highestHigh,
            double lowestLow,
            double eDistance,
            long startTime,
            long endTime) {

        if (eDistance <= 0) {
            AdvancedLogger.warn("LevelDrawer", "drawELevels", "Invalid E distance – cannot draw levels.");
            return new java.util.ArrayList<>();
        }

        boolean showLabels = settings.getBoolean(S_SHOW_LEVEL_LABELS, true);
        int maxAbove = settings.getInteger(S_MAX_LEVELS_ABOVE);
        int maxBelow = settings.getInteger(S_MAX_LEVELS_BELOW);

        java.util.List<Figure> figs = new java.util.ArrayList<>();

        // Above
        int step = 1;
        double price = midpointPrice + eDistance;
        while (price <= highestHigh && step <= maxAbove) {
            PathInfo path = getPathForLevel(settings, step);
            if (path == null) {
                boolean triggerOn = settings.getBoolean(S_SHOW_TRIGGER_LEVELS);
                if (triggerOn) {
                    path = settings.getPath(S_TRIGGER_PATH);
                }
            }
            if (path != null) {
                figs.add(new Line(new Coordinate(startTime, price), new Coordinate(endTime, price), path));
                if (showLabels) figs.add(new LevelLabel(endTime, price, "E"));
            }
            step++;
            price += eDistance;
        }

        // Below
        step = 1;
        price = midpointPrice - eDistance;
        while (price >= lowestLow && step <= maxBelow) {
            PathInfo path = getPathForLevel(settings, step);
            if (path == null) {
                boolean triggerOn = settings.getBoolean(S_SHOW_TRIGGER_LEVELS);
                if (triggerOn) {
                    path = settings.getPath(S_TRIGGER_PATH);
                }
            }
            if (path != null) {
                figs.add(new Line(new Coordinate(startTime, price), new Coordinate(endTime, price), path));
                if (showLabels) figs.add(new LevelLabel(endTime, price, "E"));
            }
            step++;
            price -= eDistance;
        }

        return figs;
    }
}
