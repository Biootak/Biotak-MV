package com.biotak.ui;

import com.biotak.enums.THStartPointType;
import com.biotak.util.Logger;
import com.biotak.util.THCalculator;
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

public class LevelDrawer {

    /**
     * Draws the historical high and low lines on the chart if they are enabled in the settings.
     */
    public static List<Figure> drawHistoricalLines(Settings settings, long startTime, long endTime, double high, double low) {
        Logger.debug("BiotakTrigger: drawHistoricalLines called. High: " + high + ", Low: " + low);

        boolean showHigh = settings.getBoolean(S_SHOW_HIGH_LINE, true);
        Logger.debug("BiotakTrigger: Show High Line setting is " + showHigh);
        List<Figure> figures = new ArrayList<>();
        if (showHigh) {
            PathInfo highPath = settings.getPath(S_HIGH_LINE_PATH);
            Logger.debug("BiotakTrigger: Drawing High Line at " + high);
            figures.add(new Line(new Coordinate(startTime, high), new Coordinate(endTime, high), highPath));
        }

        boolean showLow = settings.getBoolean(S_SHOW_LOW_LINE, true);
        Logger.debug("BiotakTrigger: Show Low Line setting is " + showLow);
        if (showLow) {
            PathInfo lowPath = settings.getPath(S_LOW_LINE_PATH);
            Logger.debug("BiotakTrigger: Drawing Low Line at " + low);
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
    public static List<Figure> drawTHLevels(Settings settings, DataSeries series, double midpointPrice, double highestHigh, double lowestLow, double thBasePrice, long startTime, long endTime) {
        double timeframePercentage = TimeframeUtil.getTimeframePercentage(series.getBarSize());
        Logger.debug("BiotakTrigger: Timeframe percentage: " + timeframePercentage);
        
        double thStepInPoints = THCalculator.calculateTHPoints(series.getInstrument(), thBasePrice, timeframePercentage);
        Logger.debug("BiotakTrigger: TH step in points: " + thStepInPoints);

        if (thStepInPoints <= 0) {
            Logger.warn("BiotakTrigger: Invalid TH step value (<=0). Cannot draw TH levels.");
            return new ArrayList<>();
        }

        double pointValue = series.getInstrument().getTickSize();
        // Apply pip multiplier so that spacing matches MT4 (price to pips conversion)
        double pipMultiplier = FractalCalculator.getPipMultiplier(series.getInstrument());
        Logger.debug("BiotakTrigger: Point value (tick size): " + pointValue + ", Pip multiplier: " + pipMultiplier);

        // Final price distance between consecutive TH levels
        double stepPrice = thStepInPoints * pointValue * pipMultiplier;
        int maxLevelsAbove = settings.getInteger(S_MAX_LEVELS_ABOVE);
        int maxLevelsBelow = settings.getInteger(S_MAX_LEVELS_BELOW);

        List<Figure> figures = new ArrayList<>();
        // Draw levels above midpoint
        int stepCountAbove = 1;
        int levelCountAbove = 0;
        double priceLevelAbove = midpointPrice + stepPrice;
        while (priceLevelAbove <= highestHigh && levelCountAbove < maxLevelsAbove) {
            PathInfo path = getPathForLevel(settings, stepCountAbove);
            if (path != null) {
                Logger.debug("BiotakTrigger: Drawing level above at " + priceLevelAbove + " (step " + stepCountAbove + ")");
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
                Logger.debug("BiotakTrigger: Drawing level below at " + priceLevelBelow + " (step " + stepCountBelow + ")");
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
            Logger.warn("BiotakTrigger: Invalid SS/LS values – cannot draw levels.");
            return new ArrayList<>();
        }

        // Convert to "pip" distance similar to TH logic
        double pipMultiplier = FractalCalculator.getPipMultiplier(series.getInstrument());
        double stepSS = ssValue * pipMultiplier;
        double stepLS = lsValue * pipMultiplier;
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
            PathInfo path = getPathForLevel(settings, logicalStep);
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
            PathInfo path = getPathForLevel(settings, logicalStep);
            if (path != null) {
                figures.add(new Line(new Coordinate(startTime, priceLevel), new Coordinate(endTime, priceLevel), path));
                drawnBelow++;
            }
        }
        return figures;
    }

    /**
     * Draws levels based on the harmonic T–P–S–SS–C–LS sequence described by user.
     * The distance pattern (multiples of T) between consecutive levels is: 1,1,2,2,1,1 (then repeats).
     *
     * @param series        Data series for instrument info
     * @param midpointPrice Reference price (anchor)
     * @param highestHigh   Upper bound to stop drawing
     * @param lowestLow     Lower bound to stop drawing
     * @param tValue        Trigger value (T) expressed in price units
     * @param startTime     Start timestamp for lines
     * @param endTime       End timestamp for lines
     */
    public static List<Figure> drawControlLevels(
            Settings settings,
            DataSeries series,
            double midpointPrice,
            double highestHigh,
            double lowestLow,
            double patternValue,
            double structureValue,
            double shortStepValue,
            double controlValue,
            double longStepValue,
            long startTime,
            long endTime) {

        // Validate inputs
        if (patternValue <= 0 || structureValue <= 0 || shortStepValue <= 0 || controlValue <= 0 || longStepValue <= 0) {
            Logger.warn("BiotakTrigger: Invalid fractal values – cannot draw Control-Based levels.");
            return new ArrayList<>();
        }

        // Convert distances to 'pip-scaled' price distances (to match TH & SS/LS rendering)
        double pipMultiplier = FractalCalculator.getPipMultiplier(series.getInstrument());

        double[] valueSequence  = new double[]{patternValue, structureValue, shortStepValue, controlValue, longStepValue};
        String[] labelSequence  = new String[]{"P", "S", "SS", "C", "LS"};

        boolean showLabels = settings.getBoolean(S_SHOW_LEVEL_LABELS, true);

        List<Figure> figures = new ArrayList<>();
        for (int i = 0; i < valueSequence.length; i++) {
            double distPrice = valueSequence[i] * pipMultiplier;
            double priceAbove = midpointPrice + distPrice;
            double priceBelow = midpointPrice - distPrice;

            int stepCount = i + 1; // Re-use for getPathForLevel()
            PathInfo path = getPathForLevel(settings, stepCount);

            if (path != null) {
                // Draw above midpoint
                if (priceAbove <= highestHigh) {
                    figures.add(new Line(new Coordinate(startTime, priceAbove), new Coordinate(endTime, priceAbove), path));
                    if (showLabels) figures.add(new LevelLabel(endTime, priceAbove, labelSequence[i]));
                }

                // Draw below midpoint
                if (priceBelow >= lowestLow) {
                    figures.add(new Line(new Coordinate(startTime, priceBelow), new Coordinate(endTime, priceBelow), path));
                    if (showLabels) figures.add(new LevelLabel(endTime, priceBelow, labelSequence[i]));
                }
            }
        }
        return figures;
    }

    /**
     * Returns the path (color/width) configured for a Control-Step label.
     */
    public static PathInfo getControlLevelPath(Settings settings, String lbl) {
        return switch (lbl) {
            case "P"  -> settings.getPath(S_P_LEVEL_PATH);
            case "S"  -> settings.getPath(S_S_LEVEL_PATH);
            case "SS" -> settings.getPath(S_SS_LEVEL_PATH);
            case "C"  -> settings.getPath(S_C_LEVEL_PATH);
            case "LS" -> settings.getPath(S_LS_LEVEL_PATH);
            default    -> null;
        };
    }
} 