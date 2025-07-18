package com.biotak;

import com.biotak.enums.THStartPointType;
import com.biotak.enums.PanelPosition;
import com.biotak.util.THCalculator;
import com.biotak.util.TimeframeUtil;
import com.biotak.util.Logger;
import com.biotak.util.Constants;
import com.biotak.util.Logger.LogLevel;
import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.*;
import com.motivewave.platform.sdk.common.menu.MenuDescriptor;
import com.motivewave.platform.sdk.common.menu.MenuItem;
import com.motivewave.platform.sdk.common.menu.MenuSeparator;
import com.motivewave.platform.sdk.draw.Line;
import com.motivewave.platform.sdk.draw.Figure;
import com.motivewave.platform.sdk.draw.ResizePoint;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;

import java.awt.Font;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import com.motivewave.platform.sdk.common.Enums.ResizeType;
import static com.biotak.util.Constants.*;
import com.biotak.enums.StepCalculationMode;
import com.biotak.enums.SSLSBasisType;
import com.biotak.ui.InfoPanel;
import com.biotak.ui.PriceLabel;
import com.biotak.core.FractalCalculator;
import com.biotak.ui.LevelDrawer;

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
    public static final String S_CUSTOM_PRICE   = "customPrice"; // stores user-defined custom price
    public final static String S_HISTORICAL_BARS = "historicalBars";

    // ---------------- New Constants ----------------
    private static final String S_P_LEVEL_PATH  = "pLevelPath";
    private static final String S_S_LEVEL_PATH  = "sLevelPath";
    private static final String S_SS_LEVEL_PATH = "ssLevelPath";
    private static final String S_C_LEVEL_PATH  = "cLevelPath";
    private static final String S_LS_LEVEL_PATH = "lsLevelPath";
    // Add constants for Leg Ruler
    // (Leg Ruler constants removed)
    public static final String S_SHOW_RULER = "showRuler";
    public static final String S_RULER_PATH = "rulerPath";
    public static final String S_RULER_EXT_RIGHT = "rulerExtRight";
    public static final String S_RULER_EXT_LEFT = "rulerExtLeft";
    public static final String S_RULER_START = "rulerStart";
    public static final String S_RULER_END = "rulerEnd";

    private long lastClickTime = 0;              // for double-click detection
    private long lastCustomMoveTime = 0;         // for fade-in highlight
   
    private InfoPanel infoPanel;
    private ResizePoint customPricePoint; // draggable point for custom price
    private PriceLabel customPriceLabel; // displays the numeric value next to the custom price line
    private Line customPriceLine; // horizontal line for custom price anchor
    // Label was not added due to SDK limitations; using only ResizePoint for visual feedback
    // Added caches to avoid full-series scans on each redraw
    private double cachedHigh = Double.NEGATIVE_INFINITY;
    private double cachedLow  = Double.POSITIVE_INFINITY;
    private boolean extremesInitialized = false; // ensures we load stored extremes once

    // Stores SS/LS base TH when lock option is enabled
    private double lockedBaseTH = Double.NaN;
    private double thValue;
    private double patternTH;
    private double triggerTH;
    private double structureTH;
    private double higherPatternTH;

    // Human-readable labels for each TH value (Current, Pattern, Trigger, Structure, Higher)
    private String[] tfLabels = {"", "", "", "", ""};

    private static final long LOG_INTERVAL_MS = 60_000;      // 1 minute
    private static long lastCalcTableLogTime = 0;             // Tracks last time the calc table was printed
    private static long lastHighLowLogTime = 0;             // Tracks last time historical high/low was logged

    // (Leg Ruler fields removed)
    private ResizePoint rulerStartResize, rulerEndResize;
    private RulerFigure rulerFigure; // Custom inner class

    // Add fields at class level
    // (Leg Ruler fields removed)

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
        grp.addRow(new PathDescriptor(S_CUSTOM_PRICE_PATH, "Custom Price Line", X11Colors.GOLD, 2.0f, new float[]{2f,2f}, true, false, false));
        // Add Leg Ruler options
       

        grp = tab.addGroup("Historical Lines");
        grp.addRow(new BooleanDescriptor(S_SHOW_HIGH_LINE, "Show High Line", true));
        grp.addRow(new PathDescriptor(S_HIGH_LINE_PATH, "High Line", defaults.getRed(), 1.0f, null, true, false, false));
        grp.addRow(new BooleanDescriptor(S_SHOW_LOW_LINE, "Show Low Line", true));
        grp.addRow(new PathDescriptor(S_LOW_LINE_PATH, "Low Line", defaults.getBlue(), 1.0f, null, true, false, false));

        grp = tab.addGroup("Manual Override");
        grp.addRow(new BooleanDescriptor(S_MANUAL_HL_ENABLE, "Enable Manual High/Low", false));
        grp.addRow(new DoubleDescriptor(S_MANUAL_HIGH, "Manual High", 0, 0, Double.MAX_VALUE, 0.0001));
        grp.addRow(new DoubleDescriptor(S_MANUAL_LOW, "Manual Low", 0, 0, Double.MAX_VALUE, 0.0001));
        // Historical Data Loading
        grp.addRow(new IntegerDescriptor(S_HISTORICAL_BARS, "Historical Bars to Load", 100000, 1000, Integer.MAX_VALUE, 1000));
        
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
        grp.addRow(new BooleanDescriptor(Constants.S_SHOW_LEVEL_LABELS, "Show Level Labels", true));
        
        // -----------------  NEW SS/LS STEP OPTIONS -----------------
        tab = sd.addTab("Step Mode");
        grp = tab.addGroup("Step Calculation");
        java.util.List<NVP> modeOptions = new java.util.ArrayList<>();
        for (StepCalculationMode m : StepCalculationMode.values()) {
            modeOptions.add(new NVP(m.toString(), m.name()));
        }
        grp.addRow(new DiscreteDescriptor(S_STEP_MODE, "Step Mode", StepCalculationMode.TH_STEP.name(), modeOptions));
        grp.addRow(new BooleanDescriptor(S_LS_FIRST, "Draw LS First", true));
        java.util.List<NVP> basisOptions = new java.util.ArrayList<>();
        for (SSLSBasisType b : SSLSBasisType.values()) {
            basisOptions.add(new NVP(b.toString(), b.name()));
        }
        grp.addRow(new DiscreteDescriptor(S_SSLS_BASIS, "SS/LS Timeframe", SSLSBasisType.STRUCTURE.name(), basisOptions));
        grp.addRow(new BooleanDescriptor(Constants.S_LOCK_SSLS_LEVELS, "Lock SS/LS Levels", false));
        // Quick settings could be added later if desired
        
        // Quick Settings: Allow rapid toggling of TH Start Point and Leg Ruler visibility from toolbar / popup editor
        sd.addQuickSettings(S_START_POINT);
        
        // Quick Settings toolbar icons for rapid access
        sd.addQuickSettings(S_STEP_MODE, S_SSLS_BASIS, S_LS_FIRST, Constants.S_LOCK_SSLS_LEVELS);

        // ------------------ Control Level Style -------------------
        grp = tab.addGroup("Control Level Paths");
        grp.addRow(new PathDescriptor(S_P_LEVEL_PATH , "P Level Path" , X11Colors.GOLD          , 1.5f, null, true, false, false));
        grp.addRow(new PathDescriptor(S_S_LEVEL_PATH , "S Level Path" , X11Colors.CADET_BLUE    , 1.5f, null, true, false, false));
        grp.addRow(new PathDescriptor(S_SS_LEVEL_PATH, "SS Level Path", X11Colors.MEDIUM_VIOLET_RED, 1.5f, null, true, false, false));
        grp.addRow(new PathDescriptor(S_C_LEVEL_PATH , "C Level Path" , X11Colors.ORANGE        , 1.5f, null, true, false, false));
        grp.addRow(new PathDescriptor(S_LS_LEVEL_PATH, "LS Level Path", X11Colors.LIGHT_GRAY     , 1.5f, null, true, false, false));
        grp.addRow(new BooleanDescriptor(S_SHOW_RULER, "Show Ruler", false));
        grp.addRow(new PathDescriptor(S_RULER_PATH, "Ruler", X11Colors.GREEN, 1.0f, null, true, false, false));
        grp.addRow(new BooleanDescriptor(S_RULER_EXT_RIGHT, "Extend Right", false));
        grp.addRow(new BooleanDescriptor(S_RULER_EXT_LEFT, "Extend Left", false));
    }

    @Override
    public MenuDescriptor onMenu(String plotName, Point loc, DrawContext ctx) {
        if (infoPanel != null && infoPanel.contains(loc.x, loc.y, ctx)) {
            if (infoPanel.isInMinimizeButton(loc.x, loc.y)) {
                boolean newState = !getSettings().getBoolean(S_PANEL_MINIMIZED, false);
                getSettings().setBoolean(S_PANEL_MINIMIZED, newState);
                infoPanel.setMinimized(newState);
                // Redraw all figures to prevent levels from disappearing
                DataContext dc = ctx.getDataContext();
                int lastIdx = dc.getDataSeries().size() - 1;
                drawFigures(lastIdx, dc);
            }
            // Suppress any context menu inside panel completely
            return new MenuDescriptor(null, true);
        }

        // Outside info panel → provide "Reset Custom Price" option in context menu
        java.util.List<MenuItem> items = new java.util.ArrayList<>();
        items.add(new MenuSeparator());
        items.add(new MenuItem("Reset Custom Price", false, () -> {
            DataSeries ds = ctx.getDataContext().getDataSeries();
            if (ds.size() == 0) return;
            double lastClose = ds.getClose(ds.size() - 1);
            getSettings().setDouble(S_CUSTOM_PRICE, lastClose);
            lastCustomMoveTime = System.currentTimeMillis();
            drawFigures(ds.size() - 1, ctx.getDataContext());
        }));
        // (Reset Leg Ruler menu item removed)
        return new MenuDescriptor(items, true);
    }

    // Toggle panel on any left-click (generic mouse down) within its bounds
    public void onMouseDown(Point loc, DrawContext ctx) {
        if (infoPanel != null && infoPanel.contains(loc.x, loc.y, ctx)) {
            // Detect double-click inside panel to reset Custom Price quickly
            long nowClick = System.currentTimeMillis();
            if (nowClick - lastClickTime < 350) {
                DataSeries series = ctx.getDataContext().getDataSeries();
                if (series.size() > 0) {
                    double lc = series.getClose(series.size() - 1);
                    getSettings().setDouble(S_CUSTOM_PRICE, lc);
                    lastCustomMoveTime = nowClick;
                    drawFigures(series.size() - 1, ctx.getDataContext());
                }
            }
            lastClickTime = nowClick;
            boolean newState = !getSettings().getBoolean(S_PANEL_MINIMIZED, false);
            getSettings().setBoolean(S_PANEL_MINIMIZED, newState);
            infoPanel.setMinimized(newState);
            DataContext dc = ctx.getDataContext();
            int lastIdx = dc.getDataSeries().size() - 1;
            drawFigures(lastIdx, dc);
            return; // suppress other handling when clicking panel
        }
    }

    @Override
    public void calculate(int index, DataContext ctx) {
        DataSeries series = ctx.getDataSeries();
        if (!series.isBarComplete(index)) return;

        // Initialize cached extremes from settings on first invocation
        if (!extremesInitialized) {
            Settings s = getSettings();
            double storedHigh = s.getDouble(Constants.S_HISTORICAL_HIGH, Double.NaN);
            double storedLow  = s.getDouble(Constants.S_HISTORICAL_LOW, Double.NaN);
            if (!Double.isNaN(storedHigh) && storedHigh != 0) cachedHigh = storedHigh;
            if (!Double.isNaN(storedLow)  && storedLow  != 0) cachedLow  = storedLow;
            extremesInitialized = true;
        }
        
        double barHigh = series.getHigh(index);
        double barLow  = series.getLow(index);

        boolean hadValidHigh = cachedHigh != Double.NEGATIVE_INFINITY && cachedHigh != Double.POSITIVE_INFINITY;
        boolean hadValidLow  = cachedLow  != Double.NEGATIVE_INFINITY && cachedLow  != Double.POSITIVE_INFINITY;

        if (barHigh > cachedHigh) {
            cachedHigh = barHigh;
            if (hadValidHigh) {
                getSettings().setDouble(Constants.S_HISTORICAL_HIGH, cachedHigh);
            }
        }
        if (barLow < cachedLow) {
            cachedLow = barLow;
            if (hadValidLow) {
                getSettings().setDouble(Constants.S_HISTORICAL_LOW, cachedLow);
            }
        }
        
        // Only log and draw figures for the first and last bars to reduce excessive logging
        boolean isFirstBar = (index == 0);
        boolean isLastBar = (index == series.size() - 1);
        
        if (isFirstBar) {
            Logger.info("BiotakTrigger: First bar detected. Drawing initial figures...");
            drawFigures(index, ctx);
        }
        else if (isLastBar) {
            long nowHL = System.currentTimeMillis();
            if (nowHL - lastHighLowLogTime > LOG_INTERVAL_MS) {
                Logger.info("BiotakTrigger: Historical High/Low calculated from " + series.getBarSize() + " timeframe (merged). High: " + cachedHigh + ", Low: " + cachedLow);
                lastHighLowLogTime = nowHL;
            }
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
                if (index == 0) {
                    // Calculate high/low on the fly from the entire loaded series (only once on very first bar)
                    double computedHigh = Double.NEGATIVE_INFINITY;
                    double computedLow  = Double.POSITIVE_INFINITY;
                    for (int i = 0; i < series.size(); i++) {
                        computedHigh = Math.max(computedHigh, series.getHigh(i));
                        computedLow  = Math.min(computedLow,  series.getLow(i));
                    }

                    // Merge with stored extremes so we never shrink the range
                    finalHigh = Math.max(cachedHigh, computedHigh);
                    finalLow  = Math.min(cachedLow,  computedLow);

                    // Persist only if new extremes discovered
                    if (finalHigh > cachedHigh) getSettings().setDouble(S_HISTORICAL_HIGH, finalHigh);
                    if (finalLow  < cachedLow)  getSettings().setDouble(S_HISTORICAL_LOW,  finalLow);

                    // Update in-memory cache
                    cachedHigh = finalHigh;
                    cachedLow  = finalLow;

                    long nowHL2 = System.currentTimeMillis();
                    if (nowHL2 - lastHighLowLogTime > LOG_INTERVAL_MS) {
                        Logger.info("BiotakTrigger: Historical High/Low calculated from " + series.getBarSize() + " timeframe (merged). High: " + cachedHigh + ", Low: " + cachedLow);
                        lastHighLowLogTime = nowHL2;
                    }
                } else {
                    // Use cached values, already updated incrementally in calculate()
                    finalHigh = cachedHigh;
                    finalLow  = cachedLow;
                    Logger.debug("BiotakTrigger: Using cached High/Low. High: " + finalHigh + ", Low: " + finalLow);
                }
            }

            // برای محاسبه TH از 200 کندل آخر استفاده می‌کنیم
            int totalBars    = series.size();
            int lookback     = Math.min(200, totalBars);
            double thBasePrice = series.getClose(totalBars - 2); // Use previous bar's close for TH calculation.
            
            // Use the first and last bar times directly for line drawing
            long startTime = series.getStartTime(0);
            long endTime = series.getStartTime(totalBars - 1);
            
            Logger.debug("BiotakTrigger: Start time: " + startTime + ", End time: " + endTime);
    
            // Determine selected step mode once
            StepCalculationMode currentMode = StepCalculationMode.valueOf(getSettings().getString(S_STEP_MODE, StepCalculationMode.TH_STEP.name()));
    
            // Draw the components of the indicator.
            if (currentMode == StepCalculationMode.TH_STEP) {
                List<Figure> histFigures = LevelDrawer.drawHistoricalLines(getSettings(), startTime, endTime, finalHigh, finalLow);
                for (Figure f : histFigures) addFigure(f);
            }
            double midpointPrice;
            if (currentMode == StepCalculationMode.SS_LS_STEP || currentMode == StepCalculationMode.CONTROL_STEP) {
                // Force use of custom price as anchor; if not set, default to last close
                double cp = getSettings().getDouble(S_CUSTOM_PRICE, Double.NaN);
                if (Double.isNaN(cp) || cp == 0) {
                    cp = series.getClose(totalBars - 1);
                    getSettings().setDouble(S_CUSTOM_PRICE, cp);
                }
                midpointPrice = cp;
            } else {
                midpointPrice = LevelDrawer.determineMidpointPrice(getSettings(), finalHigh, finalLow);
            }
            Logger.debug("BiotakTrigger: Midpoint price calculated: " + midpointPrice);
            // Handle interactive custom price baseline
            String startTypeStr = getSettings().getString(S_START_POINT, THStartPointType.MIDPOINT.name());
            THStartPointType spType = THStartPointType.valueOf(startTypeStr);

            // Always need custom-price anchor when mode is SS_LS_STEP or CONTROL_STEP (baseline)
            boolean needCustomAnchor = (currentMode == StepCalculationMode.SS_LS_STEP ||
                                        currentMode == StepCalculationMode.CONTROL_STEP ||
                                        spType == THStartPointType.CUSTOM_PRICE);

            if (needCustomAnchor) {
                long anchorTime = endTime; // stick to last bar's time so point on right edge

                // --- draggable point ---
                if (customPricePoint == null) {
                    customPricePoint = new ResizePoint(ResizeType.VERTICAL, true);
                    // Enable MotiveWave's native magnet snapping
                    customPricePoint.setSnapToLocation(true);
                }
                // Determine price to place the point: use saved custom price if available, otherwise last close price
                double savedPrice = settings.getDouble(S_CUSTOM_PRICE, Double.NaN);
                if (Double.isNaN(savedPrice) || savedPrice == 0) {
                    savedPrice = series.getClose(totalBars - 1);
                }
                // No manual snapping here; rely on MotiveWave's native magnet when the user drags the point.

                customPricePoint.setLocation(anchorTime, savedPrice);
                addFigure(customPricePoint);

                // --- numeric label ---
                if (customPriceLabel == null) customPriceLabel = new PriceLabel();
                String priceText = series.getInstrument().format(savedPrice);
                customPriceLabel.setData(anchorTime, savedPrice, priceText);
                addFigure(customPriceLabel);

                // Draw/update custom price horizontal line
                PathInfo cpPath = getSettings().getPath(S_CUSTOM_PRICE_PATH);
                customPriceLine = new Line(new Coordinate(startTime, savedPrice), new Coordinate(endTime, savedPrice), cpPath);
                addFigure(customPriceLine);

                // Additional visual labeling can be explored later if needed.
            }
            else {
                customPricePoint = null; // not needed
                // clear label if any
            }
            
            // Draw midpoint line only if not in SS/LS mode (where custom price acts as anchor)
            if (currentMode == StepCalculationMode.TH_STEP) {
                List<Figure> midFigures = LevelDrawer.drawMidpointLine(getSettings(), startTime, endTime, midpointPrice);
                for (Figure f : midFigures) addFigure(f);
            }
    
            // Step lines (TH or SS/LS) will be drawn below once all required values are calculated.
            
            // Calculate TH value for the info panel
            double timeframePercentage = TimeframeUtil.getTimeframePercentage(series.getBarSize());
            double thStepInPoints = THCalculator.calculateTHPoints(series.getInstrument(), thBasePrice, timeframePercentage);
            double pointValue = series.getInstrument().getTickSize();
            double thValue = thStepInPoints * pointValue;
            // Calculate fractal TH for matching
            BarSize patternBarSize = TimeframeUtil.getPatternBarSize(series.getBarSize());
            double patternTFPercent = TimeframeUtil.getTimeframePercentage(patternBarSize);
            double patternTH = THCalculator.calculateTHPoints(series.getInstrument(), thBasePrice, patternTFPercent) * series.getInstrument().getTickSize();
            BarSize triggerBarSize = TimeframeUtil.getTriggerBarSize(series.getBarSize());
            double triggerTFPercent = TimeframeUtil.getTimeframePercentage(triggerBarSize);
            double triggerTH = THCalculator.calculateTHPoints(series.getInstrument(), thBasePrice, triggerTFPercent) * series.getInstrument().getTickSize();
            BarSize structureBarSize = TimeframeUtil.getStructureBarSize(series.getBarSize());
            double structureTFPercent = TimeframeUtil.getTimeframePercentage(structureBarSize);
            double structureTH = THCalculator.calculateTHPoints(series.getInstrument(), thBasePrice, structureTFPercent) * series.getInstrument().getTickSize();
            BarSize higherPatternBarSize = TimeframeUtil.getPatternBarSize(structureBarSize);
            double higherPatternPercent = TimeframeUtil.getTimeframePercentage(higherPatternBarSize);
            double higherPatternTH = THCalculator.calculateTHPoints(series.getInstrument(), thBasePrice, higherPatternPercent) * series.getInstrument().getTickSize();
            // Set instance fields
            this.thValue = thValue;
            this.patternTH = patternTH;
            this.triggerTH = triggerTH;
            this.structureTH = structureTH;
            this.higherPatternTH = higherPatternTH;
            double[] fractalValues = FractalCalculator.calculateFractalValues(series.getBarSize(), thValue);
            double structureValue = fractalValues[0]; // S value
            double patternValue = fractalValues[1];   // P value
            double triggerValue = fractalValues[2];   // T value
            
            // Calculate the long and short steps
            double shortStep = FractalCalculator.calculateShortStep(structureValue, patternValue);
            double longStep = FractalCalculator.calculateLongStep(structureValue, patternValue);
            
            // Calculate ATR value for the full period
            double atrValue = FractalCalculator.calculateATR(series);
            
            // Calculate current bar's ATR (live ATR)
            double liveAtrValue = FractalCalculator.calculateLiveATR(series);
            
            // Calculate pip multiplier for display purposes
            double pipMultiplier = FractalCalculator.getPipMultiplier(series.getInstrument());

            // Log detailed calculation table
            long now = System.currentTimeMillis();
            if (now - lastCalcTableLogTime > LOG_INTERVAL_MS) {
                FractalCalculator.logCalculationTable(series, thValue, structureValue, patternValue, triggerValue, 
                                   shortStep, longStep, atrValue, liveAtrValue, pipMultiplier, lastCalcTableLogTime, LOG_INTERVAL_MS);
                lastCalcTableLogTime = now;
            }
            
            // Draw the information panel with the new values
            drawInfoPanel(series, thValue, startTime, shortStep, longStep, atrValue, liveAtrValue);
            
            // ------------------------------------------------------------------
            // Draw horizontal levels according to selected Step Mode
            // ------------------------------------------------------------------
            switch (currentMode) {
                case TH_STEP -> {
                    if (getSettings().getBoolean(S_SHOW_TH_LEVELS, true)) {
                        List<Figure> thFigures = LevelDrawer.drawTHLevels(getSettings(), series, midpointPrice, finalHigh, finalLow, thBasePrice, startTime, endTime);
                        for (Figure f : thFigures) addFigure(f);
                    }
                }
                case SS_LS_STEP -> {
                    // انتخاب مبنای TH بر اساس تنظیم کاربر
                    String basisStr = getSettings().getString(S_SSLS_BASIS, SSLSBasisType.STRUCTURE.name());
                    SSLSBasisType basis;
                    try {
                        basis = SSLSBasisType.valueOf(basisStr);
                    } catch (IllegalArgumentException e) {
                        // Legacy value (e.g., HIGHER_STRUCTURE) or corrupt entry – fall back
                        basis = SSLSBasisType.STRUCTURE;
                        getSettings().setString(S_SSLS_BASIS, SSLSBasisType.STRUCTURE.name());
                    }

                    // Lock behaviour: if user enabled lock, we calculate baseTH only once per study session
                    boolean lockLevels = getSettings().getBoolean(Constants.S_LOCK_SSLS_LEVELS, false);

                    double baseTHForSession;

                    if (lockLevels && !Double.isNaN(lockedBaseTH)) {
                        // Already locked, use stored value
                        baseTHForSession = lockedBaseTH;
                    } else {
                        double baseTHCalc;
                        switch (basis) {
                            case PATTERN -> baseTHCalc = patternValue;
                            case TRIGGER -> baseTHCalc = triggerValue;
                            case AUTO -> {
                                // حالت Auto (هوشمند)
                                double rangeAbove  = finalHigh   - midpointPrice;
                                double rangeBelow  = midpointPrice - finalLow;
                                double allowedRange = Math.min(rangeAbove, rangeBelow);
                                double[] candidates = new double[] { structureValue, patternValue, triggerValue };
                                baseTHCalc = triggerValue;
                                for (double candidate : candidates) {
                                    if (2.0 * candidate <= allowedRange) { baseTHCalc = candidate; break; }
                                }
                            }
                            case STRUCTURE -> baseTHCalc = structureValue;
                            default -> baseTHCalc = structureValue;
                        }
                        baseTHForSession = baseTHCalc;
                        if (lockLevels) lockedBaseTH = baseTHForSession;
                    }

                    double ssValue = baseTHForSession * 1.5;
                    double lsValue = baseTHForSession * 2.0;
                    boolean drawLsFirst = getSettings().getBoolean(S_LS_FIRST, true);

                    List<Figure> sslsFigures = LevelDrawer.drawSSLSLevels(getSettings(), series, midpointPrice, finalHigh, finalLow, ssValue, lsValue, drawLsFirst, startTime, endTime);
                    for (Figure f : sslsFigures) addFigure(f);
                }
                case CONTROL_STEP -> {
                    // Draw levels based on the new fractal sequence  P → S → SS → C → LS
                    // (LS corresponds to the movement capacity of the next higher fractal timeframe)

                    // Calculate Control (C) value as the midpoint between SS and LS (7 T for the current TF)
                    double controlValue = (shortStep + longStep) / 2.0;

                    List<Figure> controlFigures = LevelDrawer.drawControlLevels(getSettings(), series, midpointPrice, finalHigh, finalLow, patternValue, structureValue, shortStep, controlValue, longStep, startTime, endTime);
                    for (Figure f : controlFigures) addFigure(f);
                }
            }

            // ------------------- LEG RULER -------------------
            // (Leg Ruler logic removed)
            if (settings.getBoolean(S_SHOW_RULER, false)) {
                // Initialize resize points and line if null
                if (rulerStartResize == null) {
                    rulerStartResize = new ResizePoint(ResizeType.ALL, true);
                    rulerStartResize.setSnapToLocation(true);
                }
                if (rulerEndResize == null) {
                    rulerEndResize = new ResizePoint(ResizeType.ALL, true);
                    rulerEndResize.setSnapToLocation(true);
                }
                if (rulerFigure == null) rulerFigure = new RulerFigure();

                // Load or set default positions
                String startStr = settings.getString(S_RULER_START);
                String endStr = settings.getString(S_RULER_END);
                long rulerStartTime = series.getStartTime(series.size() - 41);
                double rulerStartPrice = series.getDouble(series.size() - 41, Enums.BarInput.MIDPOINT);
                long rulerEndTime = series.getStartTime(series.size() - 1);
                double rulerEndPrice = series.getDouble(series.size() - 1, Enums.BarInput.MIDPOINT);
                if (startStr != null) {
                    String[] parts = startStr.split("\\|");
                    rulerStartPrice = Double.parseDouble(parts[0]);
                    rulerStartTime = Long.parseLong(parts[1]);
                }
                if (endStr != null) {
                    String[] parts = endStr.split("\\|");
                    rulerEndPrice = Double.parseDouble(parts[0]);
                    rulerEndTime = Long.parseLong(parts[1]);
                }
                rulerStartResize.setLocation(rulerStartTime, rulerStartPrice);
                rulerEndResize.setLocation(rulerEndTime, rulerEndPrice);

                addFigure(rulerFigure);
                addFigure(rulerStartResize);
                addFigure(rulerEndResize);
            }
        } finally {
            // Restore previous log level
            Logger.setLogLevel(LogLevel.WARN);
        }
    }

    @Override
    public void onEndResize(ResizePoint rp, DrawContext ctx) {
        super.onEndResize(rp, ctx);
        if (rp == rulerStartResize) {
            getSettings().setString(S_RULER_START, rp.getValue() + "|" + rp.getTime());
        } else if (rp == rulerEndResize) {
            getSettings().setString(S_RULER_END, rp.getValue() + "|" + rp.getTime());
        }
    }

    // Provide live feedback while dragging the custom price point
    @Override
    public void onResize(ResizePoint rp, DrawContext ctx) {
        super.onResize(rp, ctx);
        if (rp == rulerStartResize || rp == rulerEndResize) {
            rulerFigure.layout(ctx);
        }
    }

    @Override
    public int getMinBars() {
        return getSettings().getInteger(S_HISTORICAL_BARS, 100000);
    }

    private void drawInfoPanel(DataSeries series, double thValue, long startTime, double shortStep, double longStep, double atrValue, double liveAtrValue) {
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
        String timeframe = FractalCalculator.formatTimeframeString(barSize);
        boolean isSecondsBased = TimeframeUtil.isSecondsBasedTimeframe(barSize);
        double pipMultiplier = FractalCalculator.getPipMultiplier(instrument);
        // Create the info panel and add it as a figure
        this.infoPanel = new InfoPanel(timeframe, thValue, pipMultiplier, contentFont, titleFont, panelPos, marginX, marginY, transparency, shortStep, longStep, atrValue, liveAtrValue, isSecondsBased, isMinimized);
        // Calculate fractal TH values for hierarchy display
        double basePrice = series.getClose(series.size() - 2);
        // Pattern timeframe (one level down)
        BarSize patternBarSize = TimeframeUtil.getPatternBarSize(barSize);
        double patternTFPercent = TimeframeUtil.getTimeframePercentage(patternBarSize);
        double patternTH = THCalculator.calculateTHPoints(instrument, basePrice, patternTFPercent) * instrument.getTickSize();
        // Trigger timeframe (two levels down)
        BarSize triggerBarSize = TimeframeUtil.getTriggerBarSize(barSize);
        double triggerTFPercent = TimeframeUtil.getTimeframePercentage(triggerBarSize);
        double triggerTH = THCalculator.calculateTHPoints(instrument, basePrice, triggerTFPercent) * instrument.getTickSize();
        infoPanel.setDownwardFractalInfo(FractalCalculator.formatTimeframeString(patternBarSize), FractalCalculator.formatTimeframeString(triggerBarSize), patternTH, triggerTH);
        // Structure timeframe (one level up) and its pattern
        BarSize structureBarSize = TimeframeUtil.getStructureBarSize(barSize);
        double structureTFPercent = TimeframeUtil.getTimeframePercentage(structureBarSize);
        double structureTH = THCalculator.calculateTHPoints(instrument, basePrice, structureTFPercent) * instrument.getTickSize();
        BarSize higherPatternBarSize = TimeframeUtil.getPatternBarSize(structureBarSize);
        double higherPatternPercent = TimeframeUtil.getTimeframePercentage(higherPatternBarSize);
        double higherPatternTH = THCalculator.calculateTHPoints(instrument, basePrice, higherPatternPercent) * instrument.getTickSize();
        infoPanel.setUpwardFractalInfo(FractalCalculator.formatTimeframeString(higherPatternBarSize), FractalCalculator.formatTimeframeString(structureBarSize), higherPatternTH, structureTH);
        // Set instance fields
        this.thValue = thValue;
        this.patternTH = patternTH;
        this.triggerTH = triggerTH;
        this.structureTH = structureTH;
        this.higherPatternTH = higherPatternTH;
        addFigure(this.infoPanel);
    }

    private class RulerFigure extends Figure {
        private java.awt.geom.Line2D line;

        @Override
        public boolean contains(double x, double y, DrawContext ctx) {
            return line != null && Util.distanceFromLine(x, y, line) < 6;
        }

        @Override
        public void layout(DrawContext ctx) {
            var start = ctx.translate(rulerStartResize.getLocation());
            var end = ctx.translate(rulerEndResize.getLocation());
            if (start.getX() > end.getX()) {
                var tmp = end;
                end = start;
                start = tmp;
            }
            var gb = ctx.getBounds();
            double m = Util.slope(start, end);
            if (getSettings().getBoolean(S_RULER_EXT_LEFT)) start = calcPoint(m, end, gb.getX(), gb);
            if (getSettings().getBoolean(S_RULER_EXT_RIGHT)) end = calcPoint(m, start, gb.getMaxX(), gb);
            line = new java.awt.geom.Line2D.Double(start, end);
        }

        private java.awt.geom.Point2D calcPoint(double m, java.awt.geom.Point2D p, double x, java.awt.Rectangle gb) {
            double y = 0;
            if (m == Double.POSITIVE_INFINITY) {
                y = gb.getMaxY();
                x = p.getX();
            } else if (m == Double.NEGATIVE_INFINITY) {
                y = gb.getMinY();
                x = p.getX();
            } else {
                double b = p.getY() - (m * p.getX());
                y = m * x + b;
            }
            return new java.awt.geom.Point2D.Double(x, y);
        }

        @Override
        public void draw(java.awt.Graphics2D gc, DrawContext ctx) {
            var path = getSettings().getPath(S_RULER_PATH);
            gc.setStroke(ctx.isSelected() ? path.getSelectedStroke() : path.getStroke());
            gc.setColor(path.getColor());
            gc.draw(line);

            // Add label in the middle
            if (line != null) {
                double midX = (line.getX1() + line.getX2()) / 2;
                double midY = (line.getY1() + line.getY2()) / 2;

                // Get real values for info
                DataSeries series = ctx.getDataContext().getDataSeries();
                double startPrice = rulerStartResize.getValue();
                double endPrice = rulerEndResize.getValue();
                long startTime = rulerStartResize.getTime();
                long endTime = rulerEndResize.getTime();

                // Swap if start > end
                boolean swapped = false;
                if (startTime > endTime) {
                    swapped = true;
                    long tmpTime = startTime;
                    startTime = endTime;
                    endTime = tmpTime;
                    double tmpPrice = startPrice;
                    startPrice = endPrice;
                    endPrice = tmpPrice;
                }

                double priceDiff = endPrice - startPrice;
                double pctChange = (priceDiff / startPrice) * 100;
                double timeDiffMs = Math.abs(endTime - startTime);
                int startIdx = series.findIndex(startTime);
                int endIdx = series.findIndex(endTime);
                double bars = Math.abs(endIdx - startIdx) + 1;
                // Pixel-based angle
                double pixelDX = line.getX2() - line.getX1();
                double pixelDY = line.getY2() - line.getY1();
                double angle = Math.toDegrees(Math.atan2(-pixelDY, pixelDX));
                if (swapped) angle += 180; // Adjust for direction

                // Split info into lines
                String pctStr = String.format("%.2f%%", pctChange);
                String barsStr = String.format("%.0f bars", bars);
                String angleStr = String.format("%.1f°", Math.abs(angle));
                long diffMs = Math.abs(endTime - startTime);
                long minutes = (diffMs / (1000 * 60)) % 60;
                double pips = Math.abs(endPrice - startPrice) / series.getInstrument().getTickSize();
                String pipsStr = String.format("Pips: %.1f", pips);
                 // --- Determine best matching MOVE across ALL timeframes ---
                 double tick = series.getInstrument().getTickSize();
                 long legPips = Math.round(pips);

                 // Candidates: current, pattern, trigger, structure, higher
                 double[] thValues = { thValue, patternTH, triggerTH, structureTH, higherPatternTH };
                 String[] labels    = tfLabels; // same length 5
                 long bestDiff = Long.MAX_VALUE;
                 String bestLabel = "-";

                 for (int i = 0; i < thValues.length; i++) {
                     double mvP = thValues[i];
                     if (mvP <= 0) continue;
                     long movePip = Math.round(mvP / tick);
                     if (movePip == 0) continue;

                     for (int k = 1; k <= 12; k++) {
                         long candidate = k * movePip;
                         long diff = Math.abs(legPips - candidate);
                         if (diff < bestDiff) {
                             bestDiff = diff;
                             String base = (labels[i]==null || labels[i].isBlank()) ? ("TF"+i) : labels[i];
                             bestLabel = (k > 1 ? k + "×" + base : base);
                             if (diff == 0) break;
                         }
                     }
                     if (bestDiff == 0) break;
                 }

                 String matchStr = bestLabel;
                 Logger.debug("Ruler Match -> legPips=" + legPips + ", bestLabel=" + bestLabel + ", diff=" + bestDiff);
                long hours = (diffMs / (1000 * 60 * 60)) % 24;
                long days = (diffMs / (1000 * 60 * 60 * 24)) % 30; // Approximate months
                long months = (diffMs / (1000L * 60 * 60 * 24 * 30)) % 12;
                long years = (diffMs / (1000L * 60 * 60 * 24 * 365));
                StringBuilder timeSB = new StringBuilder("Time: ");
                if (years > 0) timeSB.append(years).append("y ");
                if (months > 0) timeSB.append(months).append("m ");
                if (days > 0) timeSB.append(days).append("d ");
                if (hours > 0) timeSB.append(hours).append("h ");
                if (minutes > 0 || timeSB.length() == 6) timeSB.append(minutes).append("min");
                String timeStr = timeSB.toString();
                String[] lines = { pctStr, pipsStr, barsStr, angleStr, timeStr, matchStr };

                // Calculate max width and total height
                java.awt.font.FontRenderContext frc = gc.getFontRenderContext();
                double maxWidth = 0;
                double lineHeight = gc.getFont().getLineMetrics("A", frc).getHeight();
                for (String ln : lines) {
                    double w = gc.getFont().getStringBounds(ln, frc).getWidth();
                    if (w > maxWidth) maxWidth = w;
                }
                int padding = 5;
                int boxWidth = (int) maxWidth + 2 * padding;
                int boxHeight = (int) (lineHeight * lines.length) + 2 * padding;

                int boxX = (int) (midX - boxWidth / 2);
                // Position box touching the line at midpoint
                int boxY;
                if (pixelDY > 0) { // Descending: box above, bottom touches line
                    boxY = (int) midY - boxHeight;
                } else { // Ascending: box below, top touches line
                    boxY = (int) midY;
                }

                // Draw semi-transparent white background box
                gc.setColor(new java.awt.Color(255, 255, 255, 200)); // Semi-transparent white
                gc.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 8, 8);

                // Optional: draw border
                gc.setColor(java.awt.Color.GRAY);
                gc.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 8, 8);

                gc.setColor(java.awt.Color.BLACK); // Black text for contrast
                gc.setFont(getSettings().getFont(S_FONT).getFont()); // Use existing font setting
                // Draw each line centered
                java.awt.font.LineMetrics lm = gc.getFont().getLineMetrics("A", frc);
                int y = boxY + (int) lm.getAscent() + padding;
                for (String ln : lines) {
                    java.awt.geom.Rectangle2D bounds = gc.getFont().getStringBounds(ln, frc);
                    int x = boxX + (boxWidth - (int) bounds.getWidth()) / 2;
                    gc.drawString(ln, x, y);
                    y += lineHeight;
                }
            }
        }
    }
} 