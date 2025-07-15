package com.biotak;

import com.biotak.enums.THStartPointType;
import com.biotak.enums.PanelPosition;
import com.biotak.util.THCalculator;
import com.biotak.util.TimeframeUtil;
import com.biotak.util.Logger;
import com.biotak.util.Logger.LogLevel;
import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.*;
import com.motivewave.platform.sdk.common.Enums;
import com.motivewave.platform.sdk.common.menu.MenuDescriptor;
import com.motivewave.platform.sdk.common.menu.MenuItem;
import com.motivewave.platform.sdk.draw.Line;
import com.motivewave.platform.sdk.draw.Figure;
import com.motivewave.platform.sdk.draw.Label;
import com.motivewave.platform.sdk.draw.ResizePoint;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.FontMetrics;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;

import static com.biotak.util.Constants.*;

/**
 * Biotak Trigger TH3 Indicator for MotiveWave.
 * This indicator calculates and displays key horizontal price levels based on historical volatility and price action.
 */
@StudyHeader(
    namespace = "Biotak",
    id = "BiotakTriggerTH3",
    name = "Biotak Trigger TH3",
    label = "Biotak TH3",
    desc = "Replicates the Biotak Trigger TH3 indicator from MT4.",
    menu = "Biotak",
    overlay = true,
    studyOverlay = true
)
public class BiotakTrigger extends Study {

    public static final String S_PANEL_MINIMIZED = "panelMinimized";

    private InfoPanel infoPanel;

    public BiotakTrigger() {
        super();
        Logger.info("BiotakTrigger: Constructor called. The study is being instantiated by MotiveWave.");
    }

    @Override
    public void initialize(Defaults defaults) {
        Logger.info("BiotakTrigger: initialize() called. Settings are being configured.");
        var sd = createSD();
        var tab = sd.addTab("General");

        var grp = tab.addGroup("General");
        grp.addRow(new StringDescriptor(S_OBJ_PREFIX, "Object Prefix", "BiotakTH3"));
        
        grp = tab.addGroup("Visual");
        grp.addRow(new FontDescriptor(S_FONT, "Label Font", defaults.getFont()));

        grp = tab.addGroup("Historical Lines");
        grp.addRow(new BooleanDescriptor(S_SHOW_HIGH_LINE, "Show High Line", true));
        grp.addRow(new PathDescriptor(S_HIGH_LINE_PATH, "High Line", defaults.getRed(), 1.0f, null, true, false, false));
        grp.addRow(new BooleanDescriptor(S_SHOW_LOW_LINE, "Show Low Line", true));
        grp.addRow(new PathDescriptor(S_LOW_LINE_PATH, "Low Line", defaults.getBlue(), 1.0f, null, true, false, false));

        grp = tab.addGroup("Manual Override");
        grp.addRow(new BooleanDescriptor(S_MANUAL_HL_ENABLE, "Enable Manual High/Low", false));
        grp.addRow(new DoubleDescriptor(S_MANUAL_HIGH, "Manual High", 0, 0, Double.MAX_VALUE, 0.0001));
        grp.addRow(new DoubleDescriptor(S_MANUAL_LOW, "Manual Low", 0, 0, Double.MAX_VALUE, 0.0001));
        
        grp = tab.addGroup("Info Panel");
        grp.addRow(new BooleanDescriptor(S_SHOW_INFO_PANEL, "Show Info Panel", true));
        
        // Add panel position dropdown with the four corner options
        List<NVP> positionOptions = new ArrayList<>();
        for(PanelPosition pos : PanelPosition.values()) {
            positionOptions.add(new NVP(pos.toString(), pos.name()));
        }
        grp.addRow(new DiscreteDescriptor(S_PANEL_POSITION, "Panel Position", PanelPosition.BOTTOM_RIGHT.name(), positionOptions));
        
        // Add panel margin settings
        grp.addRow(new IntegerDescriptor(S_PANEL_MARGIN_X, "Panel Margin X", 10, 0, 100, 1));
        grp.addRow(new IntegerDescriptor(S_PANEL_MARGIN_Y, "Panel Margin Y", 10, 0, 100, 1));
        
        // Add panel transparency setting - remove the description parameter
        grp.addRow(new IntegerDescriptor(S_PANEL_TRANSPARENCY, "Panel Transparency", 230, 0, 255, 1));
        grp.addRow(new BooleanDescriptor(S_PANEL_MINIMIZED, "Start Minimized", false));
        
        // Add title font and content font settings
        grp.addRow(new FontDescriptor(S_TITLE_FONT, "Title Font", new Font("Arial", Font.BOLD, 12)));
        grp.addRow(new FontDescriptor(S_CONTENT_FONT, "Content Font", new Font("Arial", Font.PLAIN, 11)));

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

    @Override
    public MenuDescriptor onMenu(String plotName, Point loc, DrawContext ctx) {
        if (infoPanel != null && infoPanel.contains(loc.x, loc.y, ctx)) {
            boolean isMinimized = getSettings().getBoolean(S_PANEL_MINIMIZED, false);
            String menuItemText = isMinimized ? "Maximize Panel" : "Minimize Panel";
            
            MenuItem item = new MenuItem(menuItemText, () -> {
                getSettings().setBoolean(S_PANEL_MINIMIZED, !isMinimized);
            });
            
            return new MenuDescriptor(List.of(item), true);
        }
        return null;
    }

    @Override
    public void calculate(int index, DataContext ctx) {
        DataSeries series = ctx.getDataSeries();
        if (!series.isBarComplete(index)) return;
        
        // Only log and draw figures for the first and last bars to reduce excessive logging
        boolean isFirstBar = (index == 0);
        boolean isLastBar = (index == series.size() - 1);
        
        if (isFirstBar) {
            Logger.info("BiotakTrigger: First bar detected. Drawing initial figures...");
            drawFigures(index, ctx);
        }
        else if (isLastBar) {
            Logger.info("BiotakTrigger: Last bar detected. Updating figures...");
            drawFigures(index, ctx);
        }
    }

    /**
     * Main method to orchestrate the drawing of all indicator figures.
     * It's called only on the first and last bars.
     */
    private void drawFigures(int index, DataContext ctx) {
        Logger.debug("BiotakTrigger: drawFigures() called for index: " + index);
        clearFigures(); // Clear all previously drawn figures for a clean redraw.

        DataSeries series = ctx.getDataSeries();
        Settings settings = getSettings();
        
        // Need at least one previous bar
        if (series.size() < 2) {
            Logger.warn("BiotakTrigger: Not enough bars to calculate. Series size: " + series.size());
            return;
        }
        
        // Temporarily set log level to INFO for important high/low calculations
        Logger.setLogLevel(LogLevel.INFO);

        try {
            double finalHigh, finalLow;
            boolean manualMode = settings.getBoolean(S_MANUAL_HL_ENABLE, false);

            if (manualMode) {
                // Use manual values if enabled
                finalHigh = settings.getDouble(S_MANUAL_HIGH, 0);
                finalLow = settings.getDouble(S_MANUAL_LOW, 0);
                Logger.info("BiotakTrigger: Using manual high/low values. High: " + finalHigh + ", Low: " + finalLow);
            } else {
                // More efficient automatic detection:
                // We only need to check the last bar's high and low against the stored historical values.
                double historicalHigh = settings.getDouble(S_HISTORICAL_HIGH, Double.MIN_VALUE);
                double historicalLow = settings.getDouble(S_HISTORICAL_LOW, Double.MAX_VALUE);

                double currentBarHigh = series.getHigh(index);
                double currentBarLow = series.getLow(index);

                boolean updated = false;
                if (currentBarHigh > historicalHigh) {
                    settings.setDouble(S_HISTORICAL_HIGH, currentBarHigh);
                    historicalHigh = currentBarHigh;
                    updated = true;
                    Logger.info("BiotakTrigger: New historical high found and saved: " + historicalHigh);
                }
                if (currentBarLow < historicalLow) {
                    settings.setDouble(S_HISTORICAL_LOW, currentBarLow);
                    historicalLow = currentBarLow;
                    updated = true;
                    Logger.info("BiotakTrigger: New historical low found and saved: " + historicalLow);
                }

                if (!updated) {
                    Logger.info("BiotakTrigger: Historical high/low remain unchanged.");
                }
                
                finalHigh = historicalHigh;
                finalLow = historicalLow;
                Logger.info("BiotakTrigger: Using automatic historical values. High: " + finalHigh + ", Low: " + finalLow);
            }
            
            // برای محاسبه TH از 200 کندل آخر استفاده می‌کنیم
            int totalBars = series.size();
            int lookback = Math.min(200, totalBars);
            double thBasePrice = series.getClose(totalBars - 2); // Use previous bar's close for TH calculation.
            
            // Use the first and last bar times directly for line drawing
            long startTime = series.getStartTime(0);
            long endTime = series.getStartTime(totalBars - 1);
            
            Logger.debug("BiotakTrigger: Start time: " + startTime + ", End time: " + endTime);
    
            // Draw the components of the indicator.
            drawHistoricalLines(startTime, endTime, finalHigh, finalLow);
            double midpointPrice = determineMidpointPrice(finalHigh, finalLow);
            Logger.debug("BiotakTrigger: Midpoint price calculated: " + midpointPrice);
            
            drawMidpointLine(startTime, endTime, midpointPrice);
    
            if (getSettings().getBoolean(S_SHOW_TH_LEVELS, true)) {
                Logger.debug("BiotakTrigger: Drawing TH levels");
                drawTHLevels(series, midpointPrice, finalHigh, finalLow, thBasePrice, startTime, endTime);
            }
            
            // Calculate TH value for the info panel
            double timeframePercentage = TimeframeUtil.getTimeframePercentage(series.getBarSize());
            double thStepInPoints = THCalculator.calculateTHPoints(series.getInstrument(), thBasePrice, timeframePercentage);
            double pointValue = series.getInstrument().getTickSize();
            double thValue = thStepInPoints * pointValue;
            
            // Calculate LS (Long Step) and SS (Short Step) values based on the fractal bit model
            double[] fractalValues = calculateFractalValues(series.getBarSize(), thValue);
            double structureValue = fractalValues[0]; // S value
            double patternValue = fractalValues[1];   // P value
            double triggerValue = fractalValues[2];   // T value
            
            // Calculate the long and short steps
            double shortStep = calculateShortStep(structureValue, patternValue);
            double longStep = calculateLongStep(structureValue, patternValue);
            
            // Calculate ATR value for the full period
            double atrValue = calculateATR(series);
            
            // Calculate current bar's ATR (live ATR)
            double liveAtrValue = calculateLiveATR(series);
            
            // Calculate pip multiplier for display purposes
            double pipMultiplier = getPipMultiplier(series.getInstrument());

            // Log detailed calculation table
            logCalculationTable(series, thValue, structureValue, patternValue, triggerValue, 
                               shortStep, longStep, atrValue, liveAtrValue, pipMultiplier);
            
            // Draw the information panel with the new values
            drawInfoPanel(series, thValue, startTime, shortStep, longStep, atrValue, liveAtrValue);
            
        } finally {
            // Restore previous log level
            Logger.setLogLevel(LogLevel.WARN);
        }
    }

    /**
     * Logs a detailed table showing all calculations across different fractal timeframes
     */
    private void logCalculationTable(DataSeries series, double thValue, double structureValue, 
                                    double patternValue, double triggerValue, double shortStep, 
                                    double longStep, double atrValue, double liveAtrValue,
                                    double pipMultiplier) {
        // Force log level to INFO for this method
        Logger.setLogLevel(LogLevel.INFO);
        
        try {
            StringBuilder sb = new StringBuilder();
            String currentTimeframe = series.getBarSize().toString();
            String structureTimeframe = currentTimeframe; // Current timeframe is Structure
            String patternTimeframe = getPatternTimeframeString(series.getBarSize());
            String triggerTimeframe = getTriggerTimeframeString(series.getBarSize());
            
            // Get ATR periods for each level
            int structureAtrPeriod = TimeframeUtil.getAtrPeriod(series.getBarSize());
            int patternAtrPeriod = getPatternAtrPeriod(series.getBarSize());
            int triggerAtrPeriod = getTriggerAtrPeriod(series.getBarSize());
            
            // Calculate ATR values for each timeframe level
            double structureAtr = atrValue; // Current timeframe ATR
            
            // Get pattern and trigger timeframes as BarSize objects
            BarSize patternBarSize = TimeframeUtil.getPatternBarSize(series.getBarSize());
            BarSize triggerBarSize = TimeframeUtil.getTriggerBarSize(series.getBarSize());
            
            // Calculate pattern ATR (approximate using the ratio of timeframes)
            double patternAtr = atrValue / Math.sqrt(4.0);
            
            // Calculate trigger ATR (approximate using the ratio of timeframes)
            double triggerAtr = atrValue / Math.sqrt(16.0);
            
            // Get TH percentages for each level
            double structureTFPercentage = TimeframeUtil.getTimeframePercentage(series.getBarSize());
            double patternTFPercentage = TimeframeUtil.getTimeframePercentage(patternBarSize);
            double triggerTFPercentage = TimeframeUtil.getTimeframePercentage(triggerBarSize);
            
            // Calculate TH values (in price) based on these percentages
            double basePrice = series.getClose(series.size() - 2);
            double structureTHValue = (basePrice * structureTFPercentage) / 100.0;
            double patternTHValue = (basePrice * patternTFPercentage) / 100.0;
            double triggerTHValue = (basePrice * triggerTFPercentage) / 100.0;
            
            // Format header for log table
            sb.append("\n+----------------------------------------------------------------------------------------+\n");
            sb.append("| BIOTAK TRIGGER CALCULATION TABLE                                                       |\n");
            sb.append("+----------------------------------------------------------------------------------------+\n");
            sb.append(String.format("| Base Price: %.5f | Point Value: %.5f | Pip Multiplier: %.1f                     |\n", 
                    basePrice, series.getInstrument().getTickSize(), pipMultiplier));
            sb.append("+----------------------------------------------------------------------------------------+\n");
            sb.append(String.format("| %-12s | %-12s | %-12s | %-12s | %-12s |\n", 
                    "Timeframe", "Type", "Value", "Value (pips)", "ATR Period"));
            sb.append("+----------------------------------------------------------------------------------------+\n");
            
            // Add Structure row (current timeframe)
            sb.append(String.format("| %-12s | %-12s | %12.5f | %12.1f | %12d |\n", 
                    structureTimeframe, "Structure (S)", structureValue, structureValue * pipMultiplier, structureAtrPeriod));
            
            // Add Pattern row (one level down)
            sb.append(String.format("| %-12s | %-12s | %12.5f | %12.1f | %12d |\n", 
                    patternTimeframe, "Pattern (P)", patternValue, patternValue * pipMultiplier, patternAtrPeriod));
            
            // Add Trigger row (two levels down)
            sb.append(String.format("| %-12s | %-12s | %12.5f | %12.1f | %12d |\n", 
                    triggerTimeframe, "Trigger (T)", triggerValue, triggerValue * pipMultiplier, triggerAtrPeriod));
            
            // Add TH, Short Step, Long Step, ATR rows
            sb.append("+----------------------------------------------------------------------------------------+\n");
            sb.append("| TH CALCULATIONS FROM FRACTAL TIMEFRAME PERCENTAGES                                     |\n");
            sb.append("+----------------------------------------------------------------------------------------+\n");
            sb.append(String.format("| %-12s | %-12s | %12.5f | %12.1f | %12.2f%% |\n", 
                    structureTimeframe, "Structure TH", structureTHValue, structureTHValue * pipMultiplier, structureTFPercentage));
            sb.append(String.format("| %-12s | %-12s | %12.5f | %12.1f | %12.2f%% |\n", 
                    patternTimeframe, "Pattern TH", patternTHValue, patternTHValue * pipMultiplier, patternTFPercentage));
            sb.append(String.format("| %-12s | %-12s | %12.5f | %12.1f | %12.2f%% |\n", 
                    triggerTimeframe, "Trigger TH", triggerTHValue, triggerTHValue * pipMultiplier, triggerTFPercentage));
                    
            sb.append("+----------------------------------------------------------------------------------------+\n");
            sb.append(String.format("| %-12s | %-12s | %12.5f | %12.1f | %12s |\n", 
                    currentTimeframe, "TH", thValue, thValue * pipMultiplier, "-"));
            sb.append(String.format("| %-12s | %-12s | %12.5f | %12.1f | %12s |\n", 
                    currentTimeframe, "Short Step (SS)", shortStep, shortStep * pipMultiplier, "-"));
            sb.append(String.format("| %-12s | %-12s | %12.5f | %12.1f | %12s |\n", 
                    currentTimeframe, "Long Step (LS)", longStep, longStep * pipMultiplier, "-"));
            
            // Add ATR values for each level
            sb.append("+----------------------------------------------------------------------------------------+\n");
            sb.append("| ATR VALUES BY TIMEFRAME LEVEL                                                          |\n");
            sb.append("+----------------------------------------------------------------------------------------+\n");
            sb.append(String.format("| %-12s | %-12s | %12.5f | %12.1f | %12d |\n", 
                    structureTimeframe, "Structure ATR", structureAtr, structureAtr * pipMultiplier, structureAtrPeriod));
            sb.append(String.format("| %-12s | %-12s | %12.5f | %12.1f | %12d |\n", 
                    patternTimeframe, "Pattern ATR", patternAtr, patternAtr * pipMultiplier, patternAtrPeriod));
            sb.append(String.format("| %-12s | %-12s | %12.5f | %12.1f | %12d |\n", 
                    triggerTimeframe, "Trigger ATR", triggerAtr, triggerAtr * pipMultiplier, triggerAtrPeriod));
            sb.append(String.format("| %-12s | %-12s | %12.5f | %12.1f | %12s |\n", 
                    currentTimeframe, "Live ATR", liveAtrValue, liveAtrValue * pipMultiplier, "-"));
            
            sb.append("+----------------------------------------------------------------------------------------+\n");
            
            // Formula verification
            sb.append("| Formula Verification:                                                                  |\n");
            sb.append(String.format("| SS = (2 * S) - P = (2 * %.1f) - %.1f = %.1f                                         |\n", 
                    structureValue * pipMultiplier, patternValue * pipMultiplier, shortStep * pipMultiplier));
            sb.append(String.format("| LS = (3 * S) - (2 * P) = (3 * %.1f) - (2 * %.1f) = %.1f                             |\n", 
                    structureValue * pipMultiplier, patternValue * pipMultiplier, longStep * pipMultiplier));
            sb.append(String.format("| Control = (LS + SS) / 2 / 7 ≈ T = (%.1f + %.1f) / 2 / 7 = %.1f ≈ %.1f                      |\n",
                    longStep * pipMultiplier, shortStep * pipMultiplier, 
                    ((longStep + shortStep) / 2 / 7) * pipMultiplier, triggerValue * pipMultiplier));
            
            // ATR verification
            sb.append(String.format("| ATR Relation: Structure:Pattern:Trigger = 1:1/√4:1/√16 = 1:%.2f:%.2f                      |\n",
                    1.0/Math.sqrt(4.0), 1.0/Math.sqrt(16.0)));
            
            // Add timeframe mapping info for debugging
            sb.append("+----------------------------------------------------------------------------------------+\n");
            sb.append("| TIMEFRAME MAPPING DEBUG INFO:                                                          |\n");
            sb.append(String.format("| Current: %-59s |\n", currentTimeframe));
            sb.append(String.format("| Pattern: %-59s |\n", 
                    patternTimeframe + " (via " + TimeframeUtil.getPatternTimeframeString(series.getBarSize()) + ")"));
            sb.append(String.format("| Trigger: %-59s |\n", 
                    triggerTimeframe + " (via " + TimeframeUtil.getTriggerTimeframeString(series.getBarSize()) + ")"));
            
            // Calculate and display timeframe percentages for extra verification
            sb.append(String.format("| TF Percentages - Structure: %.2f%% | Pattern: %.2f%% | Trigger: %.2f%%            |\n", 
                    structureTFPercentage, patternTFPercentage, triggerTFPercentage));
            
            sb.append("+----------------------------------------------------------------------------------------+\n");
            
            // Log the entire table
            Logger.info(sb.toString());
        } finally {
            // Restore the previous log level
            Logger.setLogLevel(LogLevel.WARN);
        }
    }
    
    /**
     * Gets the pattern timeframe string representation (one fractal level down)
     * for the given timeframe.
     */
    private String getPatternTimeframeString(BarSize barSize) {
        return TimeframeUtil.getPatternTimeframeString(barSize);
    }
    
    /**
     * Gets the trigger timeframe string representation (two fractal levels down)
     * for the given timeframe.
     */
    private String getTriggerTimeframeString(BarSize barSize) {
        return TimeframeUtil.getTriggerTimeframeString(barSize);
    }
    
    /**
     * Gets the ATR period for the pattern timeframe (one level below current)
     */
    private int getPatternAtrPeriod(BarSize barSize) {
        // Get the pattern timeframe as a string
        String patternTimeframe = TimeframeUtil.getPatternTimeframeString(barSize);
        
        // Convert this to a standard format and get the ATR period
        if (patternTimeframe.startsWith("M")) {
            // Simple minute timeframes
            try {
                int minutes = Integer.parseInt(patternTimeframe.substring(1));
                if (minutes <= 1) return 24; // M1
                else if (minutes <= 5) return 24; // M5 
                else if (minutes <= 15) return 24; // M15
                else if (minutes <= 30) return 24; // M30
                else return 24; // M45, etc.
            } catch (NumberFormatException e) {
                return 24; // Default to minute timeframe ATR period
            }
        }
        else if (patternTimeframe.startsWith("H")) {
            try {
                int hours = Integer.parseInt(patternTimeframe.substring(1));
                if (hours <= 1) return 24; // H1
                else if (hours <= 4) return 30; // H4
                else return 30; // H8, etc.
            } catch (NumberFormatException e) {
                return 24; // Default
            }
        }
        else if (patternTimeframe.equals("D1")) return 22;
        else if (patternTimeframe.equals("W1")) return 52;
        else if (patternTimeframe.equals("MN")) return 12;
        
        // For complex fractal timeframes or unrecognized formats, use the original period
        return TimeframeUtil.getAtrPeriod(barSize);
    }
    
    /**
     * Gets the ATR period for the trigger timeframe (two levels below current)
     */
    private int getTriggerAtrPeriod(BarSize barSize) {
        // Get the trigger timeframe as a string
        String triggerTimeframe = TimeframeUtil.getTriggerTimeframeString(barSize);
        
        // Convert this to a standard format and get the ATR period
        if (triggerTimeframe.startsWith("M")) {
            // Simple minute timeframes
            try {
                int minutes = Integer.parseInt(triggerTimeframe.substring(1));
                if (minutes <= 1) return 24; // M1
                else if (minutes <= 5) return 24; // M5 
                else if (minutes <= 15) return 24; // M15
                else if (minutes <= 30) return 24; // M30
                else return 24; // M45, etc.
            } catch (NumberFormatException e) {
                return 24; // Default to minute timeframe ATR period
            }
        }
        else if (triggerTimeframe.startsWith("H")) {
            try {
                int hours = Integer.parseInt(triggerTimeframe.substring(1));
                if (hours <= 1) return 24; // H1
                else if (hours <= 4) return 30; // H4
                else return 30; // H8, etc.
            } catch (NumberFormatException e) {
                return 24; // Default
            }
        }
        else if (triggerTimeframe.equals("D1")) return 22;
        else if (triggerTimeframe.equals("W1")) return 52;
        else if (triggerTimeframe.equals("MN")) return 12;
        
        // For complex fractal timeframes or unrecognized formats, use the original period
        return TimeframeUtil.getAtrPeriod(barSize);
    }

    /**
     * Calculates the Structure (S), Pattern (P), and Trigger (T) values based on the current timeframe.
     * 
     * @param barSize The current chart's bar size
     * @param thValue The base TH value
     * @return An array containing [Structure value, Pattern value, Trigger value]
     */
    private double[] calculateFractalValues(BarSize barSize, double thValue) {
        // Get the current timeframe percentage
        double currentPercentage = TimeframeUtil.getTimeframePercentage(barSize);
        
        // Structure value is based on the current timeframe
        double structureValue = thValue;
        
        // Pattern value is typically half of the structure value (one fractal level down)
        double patternValue = structureValue / 2.0;
        
        // Trigger value is half of the pattern value (two fractal levels down from structure)
        double triggerValue = patternValue / 2.0;
        
        return new double[] {structureValue, patternValue, triggerValue};
    }
    
    /**
     * Calculates the Short Step (SS) value using the formula: SS = (2 * S) - P
     * 
     * @param structureValue The Structure (S) value
     * @param patternValue The Pattern (P) value
     * @return The calculated Short Step value
     */
    private double calculateShortStep(double structureValue, double patternValue) {
        return (2 * structureValue) - patternValue;
    }
    
    /**
     * Calculates the Long Step (LS) value using the formula: LS = (3 * S) - (2 * P)
     * 
     * @param structureValue The Structure (S) value
     * @param patternValue The Pattern (P) value
     * @return The calculated Long Step value
     */
    private double calculateLongStep(double structureValue, double patternValue) {
        return (3 * structureValue) - (2 * patternValue);
    }

    /**
     * Calculates the Average True Range (ATR) for the current timeframe.
     * 
     * @param series The data series
     * @return The ATR value
     */
    private double calculateATR(DataSeries series) {
        // Get the appropriate ATR period for this timeframe
        int period = TimeframeUtil.getAtrPeriod(series.getBarSize());
        Logger.debug("BiotakTrigger: Using ATR period " + period + " for timeframe " + series.getBarSize().toString());
        
        // Calculate ATR using the standard formula
        int size = series.size();
        if (size <= period) {
            Logger.warn("BiotakTrigger: Not enough data to calculate ATR. Need " + period + " bars, but only have " + size);
            return 0.0;
        }
        
        // Calculate first TR
        double sumTR = 0;
        for (int i = size - period; i < size; i++) {
            double high = series.getHigh(i);
            double low = series.getLow(i);
            double prevClose = (i > 0) ? series.getClose(i-1) : series.getOpen(i);
            
            // True Range = max(high - low, abs(high - prevClose), abs(low - prevClose))
            double tr = Math.max(high - low, Math.max(
                Math.abs(high - prevClose),
                Math.abs(low - prevClose)
            ));
            
            sumTR += tr;
        }
        
        return sumTR / period;
    }
    
    /**
     * Calculates the "Live ATR" - the true range of the current bar
     */
    private double calculateLiveATR(DataSeries series) {
        int lastIndex = series.size() - 1;
        if (lastIndex < 0) return 0.0;
        
        double high = series.getHigh(lastIndex);
        double low = series.getLow(lastIndex);
        double prevClose = (lastIndex > 0) ? series.getClose(lastIndex - 1) : series.getOpen(lastIndex);
        
        // True Range = max(high - low, abs(high - prevClose), abs(low - prevClose))
        return Math.max(high - low, Math.max(
            Math.abs(high - prevClose),
            Math.abs(low - prevClose)
        ));
    }

    /**
     * Draws the historical high and low lines on the chart if they are enabled in the settings.
     */
    private void drawHistoricalLines(long startTime, long endTime, double high, double low) {
        Logger.debug("BiotakTrigger: drawHistoricalLines called. High: " + high + ", Low: " + low);

        boolean showHigh = getSettings().getBoolean(S_SHOW_HIGH_LINE, true);
        Logger.debug("BiotakTrigger: Show High Line setting is " + showHigh);
        if (showHigh) {
            PathInfo highPath = getSettings().getPath(S_HIGH_LINE_PATH);
            Logger.debug("BiotakTrigger: Drawing High Line at " + high);
            addFigure(new Line(new Coordinate(startTime, high), new Coordinate(endTime, high), highPath));
        }

        boolean showLow = getSettings().getBoolean(S_SHOW_LOW_LINE, true);
        Logger.debug("BiotakTrigger: Show Low Line setting is " + showLow);
        if (showLow) {
            PathInfo lowPath = getSettings().getPath(S_LOW_LINE_PATH);
            Logger.debug("BiotakTrigger: Drawing Low Line at " + low);
            addFigure(new Line(new Coordinate(startTime, low), new Coordinate(endTime, low), lowPath));
        }
    }

    /**
     * Custom figure class to draw the information panel
     */
    private class InfoPanel extends Figure {
        // General Info
        private String timeframe;
        private double pipMultiplier;
        private Font contentFont;
        private Font titleFont;
        private PanelPosition position;
        private int marginX, marginY, transparency;
        private boolean isSecondsBased; // آیا تایم‌فریم ثانیه‌ای است

        // Main Values
        private double thValue, shortStep, longStep, atrValue, liveAtrValue;

        // Fractal Hierarchy Values
        private String lowerPatternTF, lowerTriggerTF;
        private double lowerPatternTH, lowerTriggerTH;

        private String higherPatternTF, higherStructureTF;
        private double higherPatternTH, higherStructureTH;
        private boolean isMinimized;
        private Rectangle panelBounds;
        
        public InfoPanel(String timeframe, double thValue, double pipMultiplier, 
                        Font contentFont, Font titleFont, PanelPosition position, 
                        int marginX, int marginY, int transparency, 
                        double shortStep, double longStep, double atrValue, double liveAtrValue, boolean isSecondsBased, boolean isMinimized) {
            this.timeframe = timeframe;
            this.thValue = thValue;
            this.pipMultiplier = pipMultiplier;
            this.contentFont = contentFont;
            this.titleFont = titleFont;
            this.position = position;
            this.marginX = marginX;
            this.marginY = marginY;
            this.transparency = Math.max(0, Math.min(255, transparency));
            this.shortStep = shortStep;
            this.longStep = longStep;
            this.atrValue = atrValue;
            this.liveAtrValue = liveAtrValue;
            this.isSecondsBased = isSecondsBased;
            this.isMinimized = isMinimized;
        }
        
        public void setDownwardFractalInfo(String pattern, String trigger, double patternTH, double triggerTH) {
            this.lowerPatternTF = pattern;
            this.lowerTriggerTF = trigger;
            this.lowerPatternTH = patternTH;
            this.lowerTriggerTH = triggerTH;
        }
        
        public void setUpwardFractalInfo(String pattern, String structure, double patternTH, double structureTH) {
            this.higherPatternTF = pattern;
            this.higherStructureTF = structure;
            this.higherPatternTH = patternTH;
            this.higherStructureTH = structureTH;
        }
        
        @Override
        public void draw(Graphics2D gc, DrawContext ctx) {
            // Save original settings
            Color origColor = gc.getColor();
            Font origFont = gc.getFont();
            Stroke origStroke = gc.getStroke();
            
            // Get chart bounds
            Rectangle bounds = ctx.getBounds();
            
            // Prepare content for each section
            List<String> coreLines = new ArrayList<>();
            coreLines.add("TH: " + String.format("%.1f", thValue * pipMultiplier));
            coreLines.add("ATR: " + String.format("%.1f", atrValue * pipMultiplier));
            coreLines.add("SS: " + String.format("%.1f", shortStep * pipMultiplier));
            coreLines.add("LS: " + String.format("%.1f", longStep * pipMultiplier));
            coreLines.add("Live: " + String.format("%.1f", liveAtrValue * pipMultiplier));

            List<String> hierarchyLines = new ArrayList<>();
            if (higherStructureTF != null && !higherStructureTF.isEmpty()) {
                hierarchyLines.add("▲ S [" + higherStructureTF + "]: " + String.format("%.1f", higherStructureTH * pipMultiplier));
            }
            if (higherPatternTF != null && !higherPatternTF.isEmpty()) {
                hierarchyLines.add("▲ P [" + higherPatternTF + "]: " + String.format("%.1f", higherPatternTH * pipMultiplier));
            }
            
            // Current timeframe display - mark with a star and highlight if seconds-based
            hierarchyLines.add("■ [" + timeframe + "]: " + String.format("%.1f", thValue * pipMultiplier) + " *");
            
            if (lowerPatternTF != null && !lowerPatternTF.isEmpty()) {
                hierarchyLines.add("▼ P [" + lowerPatternTF + "]: " + String.format("%.1f", lowerPatternTH * pipMultiplier));
            }
            if (lowerTriggerTF != null && !lowerTriggerTF.isEmpty()) {
                hierarchyLines.add("▼ T [" + lowerTriggerTF + "]: " + String.format("%.1f", lowerTriggerTH * pipMultiplier));
            }

            // Calculate panel dimensions
            gc.setFont(titleFont);
            FontMetrics titleMetrics = gc.getFontMetrics();
            int titleHeight = titleMetrics.getHeight();
            int titleWidth = titleMetrics.stringWidth(timeframe);
            
            gc.setFont(contentFont);
            FontMetrics contentMetrics = gc.getFontMetrics();
            int contentLineHeight = contentMetrics.getHeight();
            int lineSpacing = 7; // افزایش فاصله بین خطوط برای خوانایی بهتر
            
            int coreWidth = contentMetrics.stringWidth(coreLines.get(0)) + contentMetrics.stringWidth(coreLines.get(1)) + 50; // افزایش فاصله افقی
            int hierarchyWidth = 0;
            for(String line : hierarchyLines) hierarchyWidth = Math.max(hierarchyWidth, contentMetrics.stringWidth(line));
            int panelWidth = Math.max(coreWidth, hierarchyWidth) + 40; // افزایش عرض پنل

            int coreSectionHeight = (3 * (contentLineHeight + lineSpacing)) + 15; // 3 rows of content
            int hierarchySectionHeight = isMinimized ? 0 : (hierarchyLines.size() * (contentLineHeight + lineSpacing)) + 15;
            int panelHeight = (titleHeight + 15) + coreSectionHeight + hierarchySectionHeight;

            // Calculate panel position
            int x, y;
            switch (position) {
                case TOP_LEFT: x = bounds.x + marginX; y = bounds.y + marginY; break;
                case TOP_RIGHT: x = bounds.x + bounds.width - panelWidth - marginX; y = bounds.y + marginY; break;
                case BOTTOM_LEFT: x = bounds.x + marginX; y = bounds.y + bounds.height - panelHeight - marginY; break;
                default: x = bounds.x + bounds.width - panelWidth - marginX; y = bounds.y + bounds.height - panelHeight - marginY; break;
            }
            
            this.panelBounds = new Rectangle(x, y, panelWidth, panelHeight);

            // Draw panel background and border
            int alpha = 255 - transparency;
            gc.setColor(new Color(25, 25, 30, alpha));
            gc.fillRoundRect(x, y, panelWidth, panelHeight, 10, 10);
            gc.setColor(new Color(80, 80, 90, alpha));
            gc.setStroke(new BasicStroke(1.5f));
            gc.drawRoundRect(x, y, panelWidth, panelHeight, 10, 10);

            // Draw title (centered)
            gc.setFont(titleFont);
            gc.setColor(Color.WHITE);
            
            int currentY = y + titleHeight + 5;
            gc.drawString(timeframe, x + (panelWidth - titleWidth) / 2, currentY);
            
            currentY += 10; // Add space below the title before the separator
            
            // Draw sections without section titles
            drawSection(gc, x, currentY, panelWidth, "", coreLines, contentMetrics, contentFont, lineSpacing, true);
            if (!isMinimized) {
                currentY += coreSectionHeight;
                drawHierarchySection(gc, x, currentY, panelWidth, hierarchyLines, contentMetrics, contentFont, lineSpacing);
            }
            
            // Restore graphics settings
            gc.setColor(origColor);
            gc.setFont(origFont);
            gc.setStroke(origStroke);
        }

        private void drawHierarchySection(Graphics2D gc, int x, int y, int panelWidth, List<String> lines, 
                                         FontMetrics fm, Font font, int spacing) {
            int currentY = y;
            
            // Separator line
            gc.setColor(new Color(60, 60, 70, 200));
            gc.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f}, 0.0f));
            gc.drawLine(x + 10, currentY, x + panelWidth - 10, currentY);
            currentY += 15; // Add some padding after the line
            
            // Section Content - Centered single-column layout with special handling for current timeframe
            gc.setFont(font);
            for (String line : lines) {
                // Extract the timeframe from the string
                String timeframeStr = extractTimeframeFromLine(line);
                
                // Check if this is the current timeframe (contains the * marker)
                boolean isCurrentTimeframe = line.contains("*");
                
                int lineWidth = fm.stringWidth(line);
                int textX = x + (panelWidth - lineWidth) / 2;
                
                // Only highlight the current timeframe in yellow
                if (isCurrentTimeframe) {
                    // Use a very distinct background color for current timeframe
                    int padding = 6;
                    
                    // Use yellow for current timeframe
                    gc.setColor(new Color(120, 100, 0, 180)); // Dark gold background
                    gc.fillRoundRect(textX - padding, currentY - fm.getAscent(), lineWidth + (padding * 2), fm.getHeight(), 8, 8);
                    
                    // Draw the text with a bright yellow color
                    gc.setColor(new Color(255, 255, 0)); // Bright yellow for maximum visibility
                    gc.drawString(line, textX, currentY);
                } else {
                    // Check if the line contains an arrow symbol
                    if (line.contains("▲")) {
                        // Draw the arrow in light green and the rest of the text in white
                        String beforeArrow = line.substring(0, line.indexOf("▲"));
                        String arrow = "▲";
                        String afterArrow = line.substring(line.indexOf("▲") + 1);
                        
                        int beforeWidth = fm.stringWidth(beforeArrow);
                        int arrowWidth = fm.stringWidth(arrow);
                        
                        // Draw the text before the arrow in white
                        gc.setColor(Color.WHITE);
                        gc.drawString(beforeArrow, textX, currentY);
                        
                        // Draw the arrow in light green
                        gc.setColor(new Color(144, 238, 144)); // Light green
                        gc.drawString(arrow, textX + beforeWidth, currentY);
                        
                        // Draw the text after the arrow in white
                        gc.setColor(Color.WHITE);
                        gc.drawString(afterArrow, textX + beforeWidth + arrowWidth, currentY);
                    } 
                    else if (line.contains("▼")) {
                        // Draw the arrow in red and the rest of the text in white
                        String beforeArrow = line.substring(0, line.indexOf("▼"));
                        String arrow = "▼";
                        String afterArrow = line.substring(line.indexOf("▼") + 1);
                        
                        int beforeWidth = fm.stringWidth(beforeArrow);
                        int arrowWidth = fm.stringWidth(arrow);
                        
                        // Draw the text before the arrow in white
                        gc.setColor(Color.WHITE);
                        gc.drawString(beforeArrow, textX, currentY);
                        
                        // Draw the arrow in red
                        gc.setColor(new Color(255, 99, 71)); // Tomato red
                        gc.drawString(arrow, textX + beforeWidth, currentY);
                        
                        // Draw the text after the arrow in white
                        gc.setColor(Color.WHITE);
                        gc.drawString(afterArrow, textX + beforeWidth + arrowWidth, currentY);
                    }
                    else {
                        // No arrows, draw the entire line in white
                        gc.setColor(Color.WHITE);
                        gc.drawString(line, textX, currentY);
                    }
                }
                
                currentY += fm.getHeight() + spacing;
            }
        }
        
        /**
         * Extracts timeframe string from a line in the hierarchy panel
         */
        private String extractTimeframeFromLine(String line) {
            try {
                // Lines are formatted like: "▲ S [M1]: 6.7" or "■ [S16]: 1.0 *"
                int startBracket = line.indexOf('[');
                int endBracket = line.indexOf(']');
                
                if (startBracket >= 0 && endBracket > startBracket) {
                    return line.substring(startBracket + 1, endBracket);
                }
            } catch (Exception e) {
                // In case of any parsing error, return null
            }
            return null;
        }

        private void drawSection(Graphics2D gc, int x, int y, int panelWidth, String title, List<String> lines, FontMetrics fm, Font font, int spacing, boolean isTwoColumn) {
            int currentY = y;
            
            // Separator line
            gc.setColor(new Color(60, 60, 70, 200));
            gc.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f}, 0.0f));
            gc.drawLine(x + 10, currentY, x + panelWidth - 10, currentY);
            currentY += 15; // Add some padding after the line
            
            // Section Content
            gc.setColor(Color.WHITE);
            gc.setFont(font);
            if (isTwoColumn) {
                // Two-column layout
                gc.drawString(lines.get(0), x + 15, currentY);
                gc.drawString(lines.get(1), x + panelWidth / 2, currentY);
                currentY += fm.getHeight() + spacing;

                gc.drawString(lines.get(2), x + 15, currentY);
                gc.drawString(lines.get(3), x + panelWidth / 2, currentY);
                currentY += fm.getHeight() + spacing;

                int liveWidth = fm.stringWidth(lines.get(4));
                gc.drawString(lines.get(4), x + (panelWidth - liveWidth) / 2, currentY);

            } else {
                // Centered single-column layout
                for(String line : lines) {
                    int lineWidth = fm.stringWidth(line);
                    gc.drawString(line, x + (panelWidth - lineWidth) / 2, currentY);
                    currentY += fm.getHeight() + spacing;
                }
            }
        }
        
        @Override
        public boolean contains(double x, double y, DrawContext ctx) { 
            return panelBounds != null && panelBounds.contains(x, y);
        }
    }
    
    /**
     * Draws an information panel on the chart showing key metrics and values.
     */
    private void drawInfoPanel(DataSeries series, double thValue, long startTime, 
                             double shortStep, double longStep, double atrValue, double liveAtrValue) {
        if (!getSettings().getBoolean(S_SHOW_INFO_PANEL, true)) return;
        
        Instrument instrument = series.getInstrument();
        if (instrument == null) return;

        // Get settings for panel display
        FontInfo contentFontInfo = getSettings().getFont(S_CONTENT_FONT);
        Font contentFont = contentFontInfo != null ? contentFontInfo.getFont() : new Font("Arial", Font.PLAIN, 11);
        FontInfo titleFontInfo = getSettings().getFont(S_TITLE_FONT);
        Font titleFont = titleFontInfo != null ? titleFontInfo.getFont() : new Font("Arial", Font.BOLD, 12);
        PanelPosition panelPos = PanelPosition.valueOf(getSettings().getString(S_PANEL_POSITION, PanelPosition.BOTTOM_RIGHT.name()));
        int marginX = getSettings().getInteger(S_PANEL_MARGIN_X, 10);
        int marginY = getSettings().getInteger(S_PANEL_MARGIN_Y, 10);
        int transparency = getSettings().getInteger(S_PANEL_TRANSPARENCY, 230);
        boolean isMinimized = getSettings().getBoolean(S_PANEL_MINIMIZED, false);

        // Get timeframe info
        BarSize barSize = series.getBarSize();
        String timeframe = formatTimeframeString(barSize);
        boolean isSecondsBased = TimeframeUtil.isSecondsBasedTimeframe(barSize);
        
        double pipMultiplier = getPipMultiplier(instrument);

        // Create the info panel and add it as a figure
        this.infoPanel = new InfoPanel(timeframe, thValue, pipMultiplier, 
                        contentFont, titleFont, panelPos, 
                        marginX, marginY, transparency, 
                        shortStep, longStep, atrValue, liveAtrValue, isSecondsBased, isMinimized);

        // Get fractal info
        BarSize patternBarSize = TimeframeUtil.getPatternBarSize(barSize);
        BarSize triggerBarSize = TimeframeUtil.getTriggerBarSize(barSize);
        double patternTH = TimeframeUtil.getTimeframePercentage(patternBarSize) * (instrument.getHigh() - instrument.getLow());
        double triggerTH = TimeframeUtil.getTimeframePercentage(triggerBarSize) * (instrument.getHigh() - instrument.getLow());

        infoPanel.setDownwardFractalInfo(formatTimeframeString(patternBarSize), formatTimeframeString(triggerBarSize), patternTH, triggerTH);

        BarSize structureBarSize = TimeframeUtil.getStructureBarSize(barSize);
        double structureTH = TimeframeUtil.getTimeframePercentage(structureBarSize) * (instrument.getHigh() - instrument.getLow());

        infoPanel.setUpwardFractalInfo(formatTimeframeString(patternBarSize), formatTimeframeString(structureBarSize), patternTH, structureTH);

        addFigure(this.infoPanel);
    }
    
    /**
     * Helper method to format timeframe strings with proper notation for seconds-based timeframes
     */
    private String formatTimeframeString(BarSize barSize) {
        if (barSize == null) return "N/A";
        
        // For seconds-based timeframes, directly use the interval to avoid parsing errors.
        if (barSize.getIntervalType() == Enums.IntervalType.SECOND) {
            return "S" + barSize.getInterval();
        }
        
        // For all other timeframes, use the standard string representation.
        return TimeframeUtil.getStandardTimeframeString(barSize);
    }
    
    /**
     * Determines the appropriate pip multiplier for a given instrument.
     * 
     * @param instrument The trading instrument
     * @return The multiplier to convert from price to pips
     */
    private double getPipMultiplier(Instrument instrument) {
        if (instrument == null) return 10.0; // Default multiplier
        
        String symbol = instrument.getSymbol();
        double tickSize = instrument.getTickSize();
        
        // Determine number of decimal places in tick size
        int decimalPlaces = 0;
        if (tickSize > 0) {
            String tickStr = String.valueOf(tickSize);
            if (tickStr.contains(".")) {
                decimalPlaces = tickStr.length() - tickStr.indexOf('.') - 1;
            }
        }
        
        // For forex pairs
        if (symbol != null && 
            (symbol.contains("/") || 
             (symbol.length() >= 6 && !symbol.contains(".")))) {
            
            // JPY pairs typically have 2 decimal places
            if (symbol.contains("JPY") || symbol.contains("jpy")) {
                return 100.0;
            }
            
            // Most other forex pairs have 4 decimal places, with pip being the 4th decimal
            if (decimalPlaces >= 4) {
                return 10.0;
            }
        }
        
        // For indices, stocks, etc. - use a multiplier based on decimal places
        switch (decimalPlaces) {
            case 0: return 1.0;    // No decimal places
            case 1: return 10.0;   // 1 decimal place
            case 2: return 100.0;  // 2 decimal places
            case 3: return 10.0;   // 3 decimal places (unusual)
            case 4: return 10.0;   // 4 decimal places (standard forex)
            case 5: return 10.0;   // 5 decimal places (some brokers)
            default: return 10.0;  // Default
        }
    }

    /**
     * Determines the starting price for TH levels based on user settings.
     * @return The calculated midpoint price.
     */
    private double determineMidpointPrice(double high, double low) {
        String startPointStr = getSettings().getString(S_START_POINT, THStartPointType.MIDPOINT.name());
        THStartPointType startPointType = THStartPointType.valueOf(startPointStr);

        switch (startPointType) {
            case HISTORICAL_HIGH: return high;
            case HISTORICAL_LOW: return low;
            case CUSTOM_PRICE: // NOTE: Custom price input is not implemented yet.
            case MIDPOINT:
            default:
                return (high + low) / 2.0;
        }
    }

    /**
     * Draws the midpoint line on the chart if it is enabled in the settings.
     */
    private void drawMidpointLine(long startTime, long endTime, double midpointPrice) {
        if (getSettings().getBoolean(S_SHOW_MIDPOINT, true)) {
            PathInfo path = getSettings().getPath(S_TRIGGER_PATH);
            addFigure(new Line(new Coordinate(startTime, midpointPrice), new Coordinate(endTime, midpointPrice), path));
        }
    }

    /**
     * Calculates and draws all the "TH" (Trigger and Structure) levels above and below the midpoint.
     */
    private void drawTHLevels(DataSeries series, double midpointPrice, double highestHigh, double lowestLow, double thBasePrice, long startTime, long endTime) {
        double timeframePercentage = TimeframeUtil.getTimeframePercentage(series.getBarSize());
        Logger.debug("BiotakTrigger: Timeframe percentage: " + timeframePercentage);
        
        double thStepInPoints = THCalculator.calculateTHPoints(series.getInstrument(), thBasePrice, timeframePercentage);
        Logger.debug("BiotakTrigger: TH step in points: " + thStepInPoints);

        if (thStepInPoints <= 0) {
            Logger.warn("BiotakTrigger: Invalid TH step value (<=0). Cannot draw TH levels.");
            return;
        }

        double pointValue = series.getInstrument().getTickSize();
        Logger.debug("BiotakTrigger: Point value (tick size): " + pointValue);
        
        int maxLevelsAbove = getSettings().getInteger(S_MAX_LEVELS_ABOVE);
        int maxLevelsBelow = getSettings().getInteger(S_MAX_LEVELS_BELOW);

        // Draw levels above midpoint
        int stepCountAbove = 1;
        int levelCountAbove = 0;
        double priceLevelAbove = midpointPrice + (thStepInPoints * pointValue);
        while (priceLevelAbove <= highestHigh && levelCountAbove < maxLevelsAbove) {
            PathInfo path = getPathForLevel(stepCountAbove);
            if (path != null) {
                Logger.debug("BiotakTrigger: Drawing level above at " + priceLevelAbove + " (step " + stepCountAbove + ")");
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
            PathInfo path = getPathForLevel(stepCountBelow);
            if (path != null) {
                Logger.debug("BiotakTrigger: Drawing level below at " + priceLevelBelow + " (step " + stepCountBelow + ")");
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
     * @return PathInfo for the correct line type, or null if this level should not be drawn.
     */
    private PathInfo getPathForLevel(int stepCount) {
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