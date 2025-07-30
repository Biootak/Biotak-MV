package com.biotak;

import com.biotak.enums.THStartPointType;
import com.biotak.enums.PanelPosition;
import com.biotak.enums.RulerState;
import com.biotak.util.THCalculator;
import com.biotak.util.TimeframeUtil;
import com.biotak.debug.AdvancedLogger;
import com.biotak.util.Constants;
import com.biotak.config.LoggingConfiguration;
import com.biotak.ui.ThemeManager;
import com.biotak.config.BiotakConfig;
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
import java.awt.Cursor;
import java.awt.Rectangle;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import com.motivewave.platform.sdk.common.Enums.ResizeType;
import static com.biotak.util.Constants.*;
import com.biotak.enums.StepCalculationMode;
import com.biotak.enums.SSLSBasisType;
import com.biotak.ui.InfoPanel;
import com.biotak.ui.PriceLabel;
import com.biotak.ui.CustomPriceLine;
import com.biotak.ui.LineResizePoint;
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
    studyOverlay = true,
    requiresBarUpdates = true
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
    private static final String S_M_LEVEL_PATH  = "mLevelPath";
    // Add constants for Leg Ruler
    // (Leg Ruler constants removed)
    public static final String S_SHOW_RULER = "showRuler";
    public static final String S_ALWAYS_SHOW_RULER_INFO = "alwaysShowRulerInfo";
    public static final String S_RULER_PATH = "rulerPath";
    public static final String S_RULER_TEXT_COLOR = "rulerTextColor";
    public static final String S_RULER_BG_COLOR   = "rulerBgColor";
    public static final String S_RULER_BORDER_COLOR = "rulerBorderColor";
    public static final String S_RULER_FONT = "rulerFont";
    public static final String S_LOG_LEVEL = "logLevel";
    public static final String S_RULER_EXT_RIGHT = "rulerExtRight";
    public static final String S_RULER_EXT_LEFT = "rulerExtLeft";
    public static final String S_RULER_START = "rulerStart";
    public static final String S_RULER_END = "rulerEnd";

    private long lastClickTime = 0;              // for double-click detection
    private long lastCustomMoveTime = 0;         // for fade-in highlight
   
    private InfoPanel infoPanel;
    private ResizePoint customPricePoint; // draggable point for custom price
    private PriceLabel customPriceLabel; // displays the numeric value next to the custom price line
    private CustomPriceLine customPriceLine; // horizontal line for custom price anchor (now draggable)
    // Label was not added due to SDK limitations; using only ResizePoint for visual feedback
    // Added caches to avoid full-series scans on each redraw
    private double cachedHigh = Double.NEGATIVE_INFINITY;
    private double cachedLow  = Double.POSITIVE_INFINITY;
    private boolean extremesInitialized = false; // ensures we load stored extremes once
    private boolean firstBarDrawn = false; // prevents repeated first-bar drawing/logging

    // Stores SS/LS base TH when lock option is enabled
    private double lockedBaseTH = Double.NaN;
    
    // Stores locked values for all level types when lock all option is enabled
    private double lockedTHValue = Double.NaN;
    private double lockedPatternValue = Double.NaN;
    private double lockedTriggerValue = Double.NaN;
    private double lockedStructureValue = Double.NaN;
    private double lockedControlValue = Double.NaN;
    private double lockedMValue = Double.NaN;
    private double lockedCustomPrice = Double.NaN;
    private double thValue;
    private double patternTH;
    private double triggerTH;
    private double structureTH;
    private double higherPatternTH;

    // --- M values (SS + C + LS) for each fractal level ---
    private double mValue;           // current timeframe
    private double patternM;
    private double triggerM;
    private double structureM;
    private double higherPatternM;

    // Human-readable labels for each TH value (Current, Pattern, Trigger, Structure, Higher)
    private String[] tfLabels = {"", "", "", "", ""};

    // Holds comprehensive M values for ruler matching built during drawFigures() - optimized
    private final java.util.Map<String, Double> fullMValues = new java.util.concurrent.ConcurrentHashMap<>(16, 0.75f, 1);
    // Holds 3×ATR values (price) for ruler comparison - optimized
    private final java.util.Map<String, Double> fullATRValues = new java.util.concurrent.ConcurrentHashMap<>(16, 0.75f, 1);

    // Base values for ATR scaling (current timeframe)
    private int atrStructureMin = 0;          // minutes of current structure timeframe
    private double atrStructurePrice = Double.NaN; // 1× ATR price (not multiplied by 3)

    private static final long LOG_INTERVAL_MS = 60_000;      // 1 minute
    private static long lastCalcTableLogTime = 0;             // Tracks last time the calc table was printed
    private static long lastHighLowLogTime = 0;             // Tracks last time historical high/low was logged

    // Throttle ruler INFO logs
    private static long lastRulerInfoLog = 0;
    private static final long RULER_LOG_INTERVAL_MS = 1000;

    // (Leg Ruler fields removed)
    private ResizePoint rulerStartResize, rulerEndResize;
    private RulerFigure rulerFigure; // Custom inner class
    
    // Ruler state management
    private RulerState rulerState = RulerState.INACTIVE;
    private Point tempStartPoint;
    private Point tempEndPoint;

    // Add fields at class level
    // (Leg Ruler fields removed)

    // Keep last DataContext for quick redraws triggered by key events
    private DrawContext lastDrawContext;
    
    // Enhanced logging methods for ruler debugging
    private void logRulerStateTransition(String method, RulerState oldState, RulerState newState, String additionalInfo) {
        AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, method, "Ruler state transition: %s → %s | %s", 
            oldState, newState, additionalInfo);
    }
    
    private void logRulerDebug(String method, String message) {
        AdvancedLogger.ruler(AdvancedLogger.LogLevel.DEBUG, method, message);
    }
    
    private void logRulerInfo(String method, String message, Object... args) {
        AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, method, message, args);
    }
    
    private void logRulerError(String method, String message) {
        AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, method, "ERROR: %s", message);
    }

    public BiotakTrigger() {
        super();
        // Initialize logging configuration
        LoggingConfiguration.initialize();
        AdvancedLogger.info("BiotakTrigger", "constructor", "Constructor called. The study is being instantiated by MotiveWave.");
    }

    @Override
    public void initialize(Defaults defaults) {
        AdvancedLogger.debug("BiotakTrigger", "initialize", "initialize() called. Settings are being configured.");
        var sd = createSD();
        // (Bar updates are now requested via StudyHeader.requiresBarUpdates=true)

        // ---------- NEW QUICK SETUP TAB (beginner-friendly) ----------
        // This tab collects the most essential options so that new users can configure the study without browsing all tabs.
        var quick = sd.addTab("Quick Setup");

        var qBasic = quick.addGroup("Basic");
        qBasic.addRow(new DiscreteDescriptor(S_START_POINT, "Anchor Point", THStartPointType.MIDPOINT.name(), java.util.Arrays.asList(
                new NVP("Midpoint (Default)", THStartPointType.MIDPOINT.name()),
                new NVP("Historical High", THStartPointType.HISTORICAL_HIGH.name()),
                new NVP("Historical Low", THStartPointType.HISTORICAL_LOW.name()),
                new NVP("Custom Price", THStartPointType.CUSTOM_PRICE.name()))));

        qBasic.addRow(new DiscreteDescriptor(S_STEP_MODE, "Step Mode", StepCalculationMode.TH_STEP.name(), java.util.Arrays.asList(
                new NVP("Equal TH Steps", StepCalculationMode.TH_STEP.name()),
                new NVP("SS / LS Steps", StepCalculationMode.SS_LS_STEP.name()),
                new NVP("TPC / Control", StepCalculationMode.CONTROL_STEP.name()),
                new NVP("M (C×3) Steps", StepCalculationMode.M_STEP.name()))));

        var qLevels = quick.addGroup("Show / Hide Levels");
        qLevels.addRow(new BooleanDescriptor(S_SHOW_TH_LEVELS, "TH Ladder", true));
        qLevels.addRow(new BooleanDescriptor(S_SHOW_STRUCTURE_LINES, "Structure Highlights", true));
        qLevels.addRow(new BooleanDescriptor(S_SHOW_TRIGGER_LEVELS, "Trigger Sub-levels", false));

        var qExtras = quick.addGroup("Extras");
        qExtras.addRow(new BooleanDescriptor(S_SHOW_HIGH_LINE, "Historical High", true));
        qExtras.addRow(new BooleanDescriptor(S_SHOW_LOW_LINE, "Historical Low", true));
        qExtras.addRow(new BooleanDescriptor(S_SHOW_INFO_PANEL, "Info Panel", true));
        qExtras.addRow(new BooleanDescriptor(S_SHOW_RULER, "Leg Ruler", false));
        
        var qTheme = quick.addGroup("Theme");
        qTheme.addRow(new DiscreteDescriptor(Constants.S_UI_THEME, "Color Theme", "auto", java.util.Arrays.asList(
                new NVP("Auto (Smart Detection)", "auto"),
                new NVP("Light Theme", "light"),
                new NVP("Dark Theme", "dark"))));
        qTheme.addRow(new BooleanDescriptor(Constants.S_ADAPTIVE_COLORS, "Adaptive Colors", true));

        // Theme initialization will happen in draw methods

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
        
        // Add ruler font setting
        grp.addRow(new FontDescriptor(S_RULER_FONT, "Ruler Info Font", new Font("Arial", Font.PLAIN, 11)));
        
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
        grp.addRow(new BooleanDescriptor(Constants.S_LOCK_ALL_LEVELS, "Lock All Levels", false));
        // Quick settings could be added later if desired
        
        // ----------------- QUICK SETTINGS (Toolbar/Panels) -----------------
        // Provide the most commonly toggled options for new users so they don't have to open all tabs.
        // 1) Anchor & Mode
        sd.addQuickSettings(S_START_POINT,                 // Midpoint / High / Low / Custom
                            S_STEP_MODE);                  // TH-Step / SS-LS / TPC

        // 2) Level Visibility
        sd.addQuickSettings(S_SHOW_TH_LEVELS,              // Classic TH ladder
                            S_SHOW_STRUCTURE_LINES,        // Structure fractal highlights
                            S_SHOW_TRIGGER_LEVELS);        // Trigger sub-levels

        // 3) Historical extremes and ruler helpers
        sd.addQuickSettings(S_SHOW_HIGH_LINE, S_SHOW_LOW_LINE, // Historical high/low toggle
                            S_SHOW_RULER, S_RULER_EXT_LEFT, S_RULER_EXT_RIGHT); // Ruler toggles

        // 4) SS/LS options when users switch mode
        sd.addQuickSettings(S_SSLS_BASIS,  // Basis for SS/LS (Structure/Pattern/Trigger)
                            S_LS_FIRST,    // Draw LS before SS?
                            Constants.S_LOCK_ALL_LEVELS,
                            Constants.S_MSTEP_BASIS,
                            Constants.S_M_LEVEL_PATH);

        // ------------------ Control Level Style -------------------
        // Control-Level paths now reuse Trigger/Structure styles – no separate color settings needed.

        // --------------------------- RULER TAB ---------------------------
        var tabR = sd.addTab("Ruler");
        var grpR = tabR.addGroup("Ruler Settings");
        grpR.addRow(new BooleanDescriptor(S_SHOW_RULER, "Show Ruler", false));
        grpR.addRow(new BooleanDescriptor(S_ALWAYS_SHOW_RULER_INFO, "Always Show Ruler Info", false));
        grpR.addRow(new PathDescriptor(S_RULER_PATH, "Ruler Line Path", X11Colors.GREEN, 1.0f, null, true, false, false));
        grpR.addRow(new BooleanDescriptor(S_RULER_EXT_LEFT, "Extend Left", false));
        grpR.addRow(new BooleanDescriptor(S_RULER_EXT_RIGHT, "Extend Right", false));
        grpR.addRow(new ColorDescriptor(S_RULER_TEXT_COLOR, "Text Color", java.awt.Color.BLACK));
        grpR.addRow(new ColorDescriptor(S_RULER_BG_COLOR,   "Background Color", new java.awt.Color(255,255,255)));
        grpR.addRow(new ColorDescriptor(S_RULER_BORDER_COLOR, "Border Color", java.awt.Color.GRAY));
        // Logging
        java.util.List<NVP> levelOpts = new java.util.ArrayList<>();
        for (AdvancedLogger.LogLevel lv : AdvancedLogger.LogLevel.values()) levelOpts.add(new NVP(lv.name(), lv.name()));
        grp.addRow(new DiscreteDescriptor(S_LOG_LEVEL, "Log Level", AdvancedLogger.LogLevel.INFO.name(), levelOpts));

        // -------------------  M-Step Options  -------------------
        java.util.List<NVP> mbasisOptions = new java.util.ArrayList<>();
        for (com.biotak.enums.MStepBasisType b : com.biotak.enums.MStepBasisType.values()) {
            mbasisOptions.add(new NVP(b.toString(), b.name()));
        }
        grp = tab.addGroup("M-Step Basis");
        grp.addRow(new DiscreteDescriptor(Constants.S_MSTEP_BASIS, "Distance By", com.biotak.enums.MStepBasisType.C_BASED.name(), mbasisOptions));
    }

    @Override
    public MenuDescriptor onMenu(String plotName, Point loc, DrawContext ctx) {
        // Store DrawContext for SDK 7 compatibility
        this.lastDrawContext = ctx;
        
        // If click is inside info panel, suppress context menu completely
        // The minimize and ruler button functionality has been moved to onClick method
        if (infoPanel != null && infoPanel.contains(loc.x, loc.y, ctx)) {
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
        // ---- Ruler context toggles ----
        boolean showRuler = getSettings().getBoolean(S_SHOW_RULER, false);
        items.add(new MenuItem(showRuler ? "Hide Ruler" : "Show Ruler", false, () -> {
            boolean newRulerState = !showRuler;
            getSettings().setBoolean(S_SHOW_RULER, newRulerState);
            if (infoPanel != null) {
                infoPanel.setRulerActive(newRulerState);
            }
            DataSeries ds = ctx.getDataContext().getDataSeries();
            drawFigures(ds.size() - 1, ctx.getDataContext());
        }));
        
        boolean alwaysShowRulerInfo = getSettings().getBoolean(S_ALWAYS_SHOW_RULER_INFO, false);
        items.add(new MenuItem("Always Show Ruler Info", alwaysShowRulerInfo, () -> {
            getSettings().setBoolean(S_ALWAYS_SHOW_RULER_INFO, !alwaysShowRulerInfo);
            DataSeries ds = ctx.getDataContext().getDataSeries();
            drawFigures(ds.size() - 1, ctx.getDataContext());
        }));

        boolean extLeft = getSettings().getBoolean(S_RULER_EXT_LEFT, false);
        items.add(new MenuItem("Extend Left", extLeft, () -> {
            getSettings().setBoolean(S_RULER_EXT_LEFT, !extLeft);
            DataSeries ds = ctx.getDataContext().getDataSeries();
            drawFigures(ds.size() - 1, ctx.getDataContext());
        }));

        boolean extRight = getSettings().getBoolean(S_RULER_EXT_RIGHT, false);
        items.add(new MenuItem("Extend Right", extRight, () -> {
            getSettings().setBoolean(S_RULER_EXT_RIGHT, !extRight);
            DataSeries ds = ctx.getDataContext().getDataSeries();
            drawFigures(ds.size() - 1, ctx.getDataContext());
        }));
        
        // Add Reset Ruler to Last Leg option
        items.add(new MenuItem("Reset Ruler to Last Leg", false, () -> {
            DataSeries series = ctx.getDataContext().getDataSeries();
            if (series.size() < 2) return; // Need at least 2 bars
            
            // Set ruler end to last bar
            int lastIdx = series.size() - 1;
            long endTime = series.getStartTime(lastIdx);
            double endPrice = series.getDouble(lastIdx, Enums.BarInput.MIDPOINT);
            
            // Set ruler start to a reasonable distance back (e.g., 40 bars)
            int startIdx = Math.max(0, lastIdx - 40);
            long startTime = series.getStartTime(startIdx);
            double startPrice = series.getDouble(startIdx, Enums.BarInput.MIDPOINT);
            
            // Update resize points
            rulerStartResize.setLocation(startTime, startPrice);
            rulerEndResize.setLocation(endTime, endPrice);
            
            // Save settings
            getSettings().setString(S_RULER_START, startPrice + "|" + startTime);
            getSettings().setString(S_RULER_END, endPrice + "|" + endTime);
            
            // Make sure ruler is visible
            if (!getSettings().getBoolean(S_SHOW_RULER, false)) {
                getSettings().setBoolean(S_SHOW_RULER, true);
            }
            
            // Redraw
            rulerFigure.layout(ctx);
            drawFigures(lastIdx, ctx.getDataContext());
        }));
        
        return new MenuDescriptor(items, true);
    }

    
    /**
     * Handle left-click events using the standard SDK onClick method
     * This replaces the custom onMouseDown implementation
     */
    @Override
    public boolean onClick(Point loc, int flags) {
        AdvancedLogger.info("BiotakTrigger", "onClick", "=== CLICK EVENT START === Processing click at %s with flags %d", loc, flags);
        AdvancedLogger.info("BiotakTrigger", "onClick", "Current ruler state: %s", rulerState);
        Settings settings = getSettings();
        DrawContext ctx = this.lastDrawContext; // Use stored context first
        
        // Try to get DrawContext if not available
        if (ctx == null) {
            // In MotiveWave SDK, we need to use the context from the last draw call
            AdvancedLogger.info("BiotakTrigger", "onClick", "No DrawContext available, trying alternative methods");
            
            // Handle panel button clicks even without DrawContext
            if (infoPanel != null) {
                // First check if click is anywhere inside the panel area
                if (infoPanel.contains(loc.x, loc.y, null)) {
                    // Check if click is on minimize button
                    if (infoPanel.isInMinimizeButton(loc.x, loc.y)) {
                        boolean newState = !settings.getBoolean(S_PANEL_MINIMIZED, false);
                        settings.setBoolean(S_PANEL_MINIMIZED, newState);
                        infoPanel.setMinimized(newState);
                        AdvancedLogger.info("BiotakTrigger", "onClick", "Minimize button clicked, new state: %s", newState);
                        return false; // prevent default behavior
                    }
                    // Check if click is on ruler button
                    else if (infoPanel.isInRulerButton(loc.x, loc.y)) {
                        // Handle ruler button click even without DrawContext
                        handleRulerButtonClickWithoutContext(settings);
                        return false; // prevent default behavior
                    }
                    // Click is inside panel but not on buttons - prevent default behavior
                    else {
                        AdvancedLogger.info("BiotakTrigger", "onClick", "Click inside panel area, preventing default behavior");
                        return false; // prevent default behavior for any panel click
                    }
                }
            }
            
            // For ruler point selection, we need DrawContext - return true to allow default behavior
            if (rulerState == RulerState.WAITING_FOR_START || rulerState == RulerState.WAITING_FOR_END) {
                AdvancedLogger.warn("BiotakTrigger", "onClick", "Ruler waiting for point selection but no DrawContext available");
                return true;
            }
            
            return true; // no context available for other actions
        }
        
        AdvancedLogger.info("BiotakTrigger", "onClick", "DrawContext available: %s", (ctx != null));
        
        // Handle clicks inside info panel
        if (infoPanel != null && infoPanel.contains(loc.x, loc.y, ctx)) {
            // Check if click is on minimize button
            if (infoPanel.isInMinimizeButton(loc.x, loc.y)) {
                boolean newState = !settings.getBoolean(S_PANEL_MINIMIZED, false);
                settings.setBoolean(S_PANEL_MINIMIZED, newState);
                infoPanel.setMinimized(newState);
                // Redraw all figures to prevent levels from disappearing
                DataContext dc = ctx.getDataContext();
                int lastIdx = dc.getDataSeries().size() - 1;
                drawFigures(lastIdx, dc);
                AdvancedLogger.info("BiotakTrigger", "onClick", "Minimize button handled, preventing default behavior");
                return false; // prevent default behavior
            }
            // Check if click is on ruler button
            else if (infoPanel.isInRulerButton(loc.x, loc.y)) {
                handleRulerButtonClick(settings, ctx);
                AdvancedLogger.info("BiotakTrigger", "onClick", "Ruler button handled, preventing default behavior");
                return false; // prevent default behavior
            }
            // Any other click inside panel - handle double-click for custom price reset
            else {
                long nowClick = System.currentTimeMillis();
                if (nowClick - lastClickTime < 350) {
                    DataSeries series = ctx.getDataContext().getDataSeries();
                    if (series.size() > 0) {
                        double lc = series.getClose(series.size() - 1);
                        settings.setDouble(S_CUSTOM_PRICE, lc);
                        lastCustomMoveTime = nowClick;
                        drawFigures(series.size() - 1, ctx.getDataContext());
                        AdvancedLogger.info("BiotakTrigger", "onClick", "Double-click custom price reset handled");
                    }
                }
                lastClickTime = nowClick;
                AdvancedLogger.info("BiotakTrigger", "onClick", "Click inside panel handled, preventing default behavior");
                return false; // prevent default behavior for any panel click
            }
        }
        
        // CRITICAL: Handle ruler state clicks for point selection OUTSIDE the panel
        if (rulerState == RulerState.WAITING_FOR_START) {
            AdvancedLogger.info("BiotakTrigger", "onClick", "=== RULER START POINT SELECTION ===");
            AdvancedLogger.info("BiotakTrigger", "onClick", "Ruler state is WAITING_FOR_START");
            AdvancedLogger.info("BiotakTrigger", "onClick", "Start point selection in progress at pixel location: x=%d, y=%d", loc.x, loc.y);
            
            DataSeries series = ctx.getDataContext().getDataSeries();
            try {
                long time = ctx.translate2Time(loc.getX());
                double value = ctx.translate2Value(loc.getY());
                AdvancedLogger.info("BiotakTrigger", "onClick", "Translated coordinates - Time: %d, Value: %.5f", time, value);
                Coordinate coord = new Coordinate(time, value);
                
                tempStartPoint = new Point(loc.x, loc.y);
                
                // Initialize ruler points if needed
                if (rulerStartResize == null) {
                    rulerStartResize = new ResizePoint(ResizeType.ALL, true);
                    rulerStartResize.setSnapToLocation(true);
                }
                
                rulerStartResize.setLocation(coord.getTime(), coord.getValue());
                AdvancedLogger.info("BiotakTrigger", "onClick", "Start resize point created and positioned at Time: %d, Value: %.5f", coord.getTime(), coord.getValue());
                
                String startPointData = coord.getValue() + "|" + coord.getTime();
                settings.setString(S_RULER_START, startPointData);
                AdvancedLogger.info("BiotakTrigger", "onClick", "Start point data saved to settings: %s", startPointData);
                
                RulerState oldState = rulerState;
                rulerState = RulerState.WAITING_FOR_END;
                logRulerStateTransition("onClick", oldState, rulerState, "Start point selected successfully");
                
                AdvancedLogger.info("BiotakTrigger", "onClick", "=== START POINT SELECTION COMPLETED ===");
                AdvancedLogger.info("BiotakTrigger", "onClick", "Ruler start point handled, preventing default behavior");
                return false; // prevent default behavior
            } catch (Exception e) {
                logRulerError("onClick", "Failed to set start point: " + e.getMessage());
                return true;
            }
        }
        else if (rulerState == RulerState.WAITING_FOR_END) {
            AdvancedLogger.info("BiotakTrigger", "onClick", "=== RULER END POINT SELECTION ===");
            AdvancedLogger.info("BiotakTrigger", "onClick", "End point selection in progress at pixel location: x=%d, y=%d", loc.x, loc.y);
            
            // CRITICAL: Add extra validation to ensure we're really in the right state
            if (rulerStartResize == null) {
                AdvancedLogger.error("BiotakTrigger", "onClick", "CRITICAL ERROR: WAITING_FOR_END but rulerStartResize is null!");
                rulerState = RulerState.INACTIVE;
                return true; // Allow default behavior due to invalid state
            }
            
            DataSeries series = ctx.getDataContext().getDataSeries();
            try {
                long time = ctx.translate2Time(loc.getX());
                double value = ctx.translate2Value(loc.getY());
                AdvancedLogger.info("BiotakTrigger", "onClick", "Translated coordinates - Time: %d, Value: %.5f", time, value);
                Coordinate coord = new Coordinate(time, value);
                
                tempEndPoint = new Point(loc.x, loc.y);
                
                // Initialize ruler points if needed
                if (rulerEndResize == null) {
                    rulerEndResize = new ResizePoint(ResizeType.ALL, true);
                    rulerEndResize.setSnapToLocation(true);
                    AdvancedLogger.info("BiotakTrigger", "onClick", "Created new rulerEndResize for end point");
                } else {
                    AdvancedLogger.info("BiotakTrigger", "onClick", "Using existing rulerEndResize for end point");
                }
                
                rulerEndResize.setLocation(coord.getTime(), coord.getValue());
                AdvancedLogger.info("BiotakTrigger", "onClick", "End resize point created and positioned at Time: %d, Value: %.5f", coord.getTime(), coord.getValue());
                
                String endPointData = coord.getValue() + "|" + coord.getTime();
                settings.setString(S_RULER_END, endPointData);
                AdvancedLogger.info("BiotakTrigger", "onClick", "End point data saved to settings: %s", endPointData);
                
                RulerState oldState = rulerState;
                rulerState = RulerState.ACTIVE;
                logRulerStateTransition("onClick", oldState, rulerState, "End point selected, ruler completed");
                
                // Create and layout ruler figure
                if (rulerFigure == null) {
                    rulerFigure = new RulerFigure();
                    AdvancedLogger.info("BiotakTrigger", "onClick", "New RulerFigure created");
                } else {
                    AdvancedLogger.info("BiotakTrigger", "onClick", "Using existing RulerFigure");
                }
                
                // CRITICAL: Add delay before redraw to prevent event conflicts
                AdvancedLogger.info("BiotakTrigger", "onClick", "CRITICAL: About to call drawFigures - this should complete ruler setup");
                
                // Redraw to show completed ruler
                DataContext dc = ctx.getDataContext();
                int lastIdx = dc.getDataSeries().size() - 1;
                AdvancedLogger.info("BiotakTrigger", "onClick", "Calling drawFigures to render ruler with lastIdx: %d", lastIdx);
                drawFigures(lastIdx, dc);
                
                AdvancedLogger.info("BiotakTrigger", "onClick", "=== END POINT SELECTION COMPLETED ===");
                AdvancedLogger.info("BiotakTrigger", "onClick", "CRITICAL: Ruler end point handled, FORCING false return to prevent settings dialog");
                
                // CRITICAL: Force immediate return to prevent any further processing
                return false; // prevent default behavior - MUST NOT open settings
            } catch (Exception e) {
                AdvancedLogger.error("BiotakTrigger", "onClick", "CRITICAL ERROR in end point selection: %s", e.getMessage());
                AdvancedLogger.error("BiotakTrigger", "onClick", "Exception stack: %s", java.util.Arrays.toString(e.getStackTrace()));
                logRulerError("onClick", "Failed to set end point: " + e.getMessage());
                // Reset ruler state on error
                rulerState = RulerState.INACTIVE;
                return false; // Still prevent default behavior even on error
            }
        }
        AdvancedLogger.info("BiotakTrigger", "onClick", "=== CLICK EVENT END === No special handling needed, allowing default behavior");
        return true; // allow default behavior for clicks outside panel and ruler states
    }
    
    // Store the latest DrawContext for later use by onClick/other handlers
public void onMouseDown(Point loc, DrawContext ctx) {
    // Persist the DrawContext so that onClick (triggered on mouse release) has access to it
    this.lastDrawContext = ctx;
    // Do NOT call onClick here – MotiveWave will invoke it automatically on mouse release.
}

    /**
     * Handle mouse movement for dynamic ruler tracking
     */
    public void onMouseMove(Point loc, DrawContext ctx) {
// ... (rest of the code remains the same)
        // Store DrawContext for SDK 7 compatibility
        this.lastDrawContext = ctx;
        
        AdvancedLogger.debug("BiotakTrigger", "onMouseMove", "=== MOUSE MOVE EVENT START === Position: %s", loc);
        AdvancedLogger.debug("BiotakTrigger", "onMouseMove", "Current ruler state: %s", rulerState);
        AdvancedLogger.debug("BiotakTrigger", "onMouseMove", "DrawContext available: %s", (ctx != null));
        AdvancedLogger.debug("BiotakTrigger", "onMouseMove", "RulerStartResize exists: %s", (rulerStartResize != null));
        
        // Only track mouse movement when waiting for end point
        if (rulerState == RulerState.WAITING_FOR_END && rulerStartResize != null) {
            AdvancedLogger.info("BiotakTrigger", "onMouseMove", "=== RULER DYNAMIC TRACKING === Mouse at pixel: x=%d, y=%d", loc.x, loc.y);
            try {
                // Convert mouse location to chart coordinates
                long time = ctx.translate2Time(loc.getX());
                double value = ctx.translate2Value(loc.getY());
                AdvancedLogger.debug("BiotakTrigger", "onMouseMove", "Translated coordinates - Time: %d, Value: %.5f", time, value);
                Coordinate coord = new Coordinate(time, value);
                
                // Initialize ruler end point if needed
                if (rulerEndResize == null) {
                    rulerEndResize = new ResizePoint(ResizeType.ALL, true);
                    rulerEndResize.setSnapToLocation(true);
                    AdvancedLogger.debug("BiotakTrigger", "onMouseMove", "Created new rulerEndResize during mouse move");
                } else {
                    AdvancedLogger.debug("BiotakTrigger", "onMouseMove", "Using existing rulerEndResize");
                }
                
                // Update end point position dynamically
                rulerEndResize.setLocation(coord.getTime(), coord.getValue());
                AdvancedLogger.debug("BiotakTrigger", "onMouseMove", "Updated rulerEndResize location to Time: %d, Value: %.5f", coord.getTime(), coord.getValue());
                
                // Initialize ruler figure if needed
                if (rulerFigure == null) {
                    rulerFigure = new RulerFigure();
                    AdvancedLogger.debug("BiotakTrigger", "onMouseMove", "Created new rulerFigure during mouse move");
                } else {
                    AdvancedLogger.debug("BiotakTrigger", "onMouseMove", "Using existing rulerFigure");
                }
                
                // Force immediate redraw to show the dynamic ruler
                DataContext dc = ctx.getDataContext();
                int lastIdx = dc.getDataSeries().size() - 1;
                AdvancedLogger.debug("BiotakTrigger", "onMouseMove", "Calling drawFigures for dynamic update with lastIdx: %d", lastIdx);
                drawFigures(lastIdx, dc);
                AdvancedLogger.debug("BiotakTrigger", "onMouseMove", "Dynamic ruler update completed");
                
            } catch (Exception e) {
                logRulerError("onMouseMove", "Mouse move coordinate conversion failed: " + e.getMessage());
                AdvancedLogger.debug("BiotakTrigger", "onMouseMove", "Exception details: %s", e.toString());
            }
        } else {
            AdvancedLogger.debug("BiotakTrigger", "onMouseMove", "No ruler tracking - rulerState is %s, rulerStartResize exists: %s", rulerState, (rulerStartResize != null));
        }
        
        AdvancedLogger.debug("BiotakTrigger", "onMouseMove", "=== MOUSE MOVE EVENT END ===");
    }
    
    
    /**
     * Get the current DrawContext from the chart
     * This is needed for SDK 7 compatibility
     */
    private DrawContext getDrawContext() {
        // If lastDrawContext is already set, use it
        if (lastDrawContext != null) return lastDrawContext;
        
        // In SDK 7, we can't directly get DrawContext without it being passed to us
        // So we have to return null and handle this case in onClick
        return null;
    }

    @Override
    public void calculate(int index, DataContext ctx) {
        // Note: In SDK 7, DrawContext is typically passed through onDraw() method
        // We cannot directly get DrawContext from DataContext
        
        // Sync logger level once per bar zero
        if (index == 0) {
            AdvancedLogger.LogLevel lvl = com.biotak.util.EnumUtil.safeEnum(AdvancedLogger.LogLevel.class,
                    getSettings().getString(S_LOG_LEVEL, AdvancedLogger.LogLevel.INFO.name()), AdvancedLogger.LogLevel.INFO);
            AdvancedLogger.setLogLevel(lvl);
        }

        DataSeries series = ctx.getDataSeries();
        // Determine if this is the last bar for live updates
        boolean isLastBar = (index == series.size() - 1);
        // Skip incomplete bars except for the last bar, remove logging for performance
        if (!series.isBarComplete(index) && !isLastBar) {
            return;
        }
        
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
        
        if (isFirstBar && !firstBarDrawn) {
            // Remove excessive logging for better performance
            drawFigures(index, ctx);
            firstBarDrawn = true;
        }
        else if (isLastBar) {
            long nowHL = System.currentTimeMillis();
            if (nowHL - lastHighLowLogTime > LOG_INTERVAL_MS) {
                AdvancedLogger.info("BiotakTrigger", "calculate", "Historical High/Low calculated from %s timeframe (merged). High: %.5f, Low: %.5f", series.getBarSize(), cachedHigh, cachedLow);
                lastHighLowLogTime = nowHL;
            }
            // Remove debug logging for better performance
            drawFigures(index, ctx);
        }
    }

    /**
     * Main method to orchestrate the drawing of all indicator figures.
     * It's called only on the first and last bars.
     */
    private void drawFigures(int index, DataContext ctx) {
        // Remove debug logging for better performance
        clearFigures(); // Clear all previously drawn figures for a clean redraw.

        DataSeries series = ctx.getDataSeries();
        Settings settings = getSettings();
        
        // Need at least one previous bar
        if (series.size() < 2) {
            AdvancedLogger.warn("BiotakTrigger", "drawFigures", "Not enough bars to calculate. Series size: %d", series.size());
            return;
        }
        
        // Temporarily set log level to INFO for important high/low calculations
        AdvancedLogger.LogLevel originalLevel = com.biotak.config.LoggingConfiguration.getCurrentLogLevel();
        AdvancedLogger.setLogLevel(AdvancedLogger.LogLevel.INFO);
        try {
            double finalHigh, finalLow;
            boolean manualMode = settings.getBoolean(S_MANUAL_HL_ENABLE, false);

            if (manualMode) {
                finalHigh = settings.getDouble(S_MANUAL_HIGH, 0);
                finalLow  = settings.getDouble(S_MANUAL_LOW, 0);
                // Use throttled logging to prevent spam - only log once per minute
                long nowManual = System.currentTimeMillis();
                if (nowManual - lastHighLowLogTime > LOG_INTERVAL_MS) {
                    AdvancedLogger.info("BiotakTrigger", "drawFigures", "Using manual high/low values. High: %.5f, Low: %.5f", finalHigh, finalLow);
                    lastHighLowLogTime = nowManual;
                }
            } else {
                double[] range = com.biotak.util.FractalUtil.getHistoricalRange(series, settings, cachedHigh, cachedLow, false);
                finalHigh = range[0];
                finalLow  = range[1];

                if (index == 0) {
                    // Persist only if new extremes discovered
                    if (finalHigh > cachedHigh) settings.setDouble(S_HISTORICAL_HIGH, finalHigh);
                    if (finalLow  < cachedLow)  settings.setDouble(S_HISTORICAL_LOW,  finalLow);
                }

                // Update in-memory cache for future bars
                cachedHigh = finalHigh;
                cachedLow  = finalLow;

                if (index == 0 || index == series.size() - 1) {
                    long nowHL2 = System.currentTimeMillis();
                    if (nowHL2 - lastHighLowLogTime > LOG_INTERVAL_MS) {
                        AdvancedLogger.info("BiotakTrigger", "drawFigures", "Historical High/Low calculated from %s timeframe (merged). High: %.5f, Low: %.5f", series.getBarSize(), cachedHigh, cachedLow);
                        lastHighLowLogTime = nowHL2;
                    }
                }
            }

            // برای محاسبه TH از قیمت لایو Bid استفاده می‌کنیم
            int totalBars    = series.size();
            int lookback     = Math.min(200, totalBars);
            double thBasePrice = series.getBidClose(totalBars - 1); // Use current live bid price for TH calculation.
            
            // Use the first and last bar times directly for line drawing
            long startTime = series.getStartTime(0);
            long endTime = series.getStartTime(totalBars - 1);
            
            // Remove debug logging for better performance
    
            // Determine selected step mode once
            StepCalculationMode currentMode = com.biotak.util.EnumUtil.safeEnum(StepCalculationMode.class,
                    getSettings().getString(S_STEP_MODE, StepCalculationMode.TH_STEP.name()), StepCalculationMode.TH_STEP);
    
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
            // Logger.debug("BiotakTrigger: Midpoint price calculated: " + midpointPrice);
            // Handle interactive custom price baseline
            String startTypeStr = getSettings().getString(S_START_POINT, THStartPointType.MIDPOINT.name());
            THStartPointType spType = THStartPointType.valueOf(startTypeStr);

            // Always need custom-price anchor when mode is SS_LS_STEP or CONTROL_STEP (baseline)
            boolean needCustomAnchor = (currentMode == StepCalculationMode.SS_LS_STEP ||
                                        currentMode == StepCalculationMode.CONTROL_STEP ||
                                        spType == THStartPointType.CUSTOM_PRICE);

            if (needCustomAnchor) {
                long anchorTime = endTime; // stick to last bar's time so point on right edge

                // Check if Lock All Levels is enabled to determine custom price behavior
                boolean lockAllLevels = getSettings().getBoolean(Constants.S_LOCK_ALL_LEVELS, false);
                double finalCustomPrice;
                
                // Always get the current saved custom price first
                double savedPrice = settings.getDouble(S_CUSTOM_PRICE, Double.NaN);
                if (Double.isNaN(savedPrice) || savedPrice == 0) {
                    savedPrice = series.getClose(totalBars - 1);
                    settings.setDouble(S_CUSTOM_PRICE, savedPrice);
                }
                
                if (lockAllLevels && !Double.isNaN(lockedCustomPrice)) {
                    // Use locked custom price (previously stored)
                    finalCustomPrice = lockedCustomPrice;
                } else if (lockAllLevels) {
                    // First time locking - store current saved price as locked value
                    lockedCustomPrice = savedPrice;
                    finalCustomPrice = savedPrice;
                } else {
                    // Not locked - use current saved price
                    finalCustomPrice = savedPrice;
                }

                // --- draggable point (only if not locked) ---
                if (!lockAllLevels) {
                    if (customPricePoint == null) {
                        customPricePoint = new ResizePoint(ResizeType.VERTICAL, true);
                        // Enable MotiveWave's native magnet snapping
                        customPricePoint.setSnapToLocation(true);
                    }
                    customPricePoint.setLocation(anchorTime, finalCustomPrice);
                    addFigure(customPricePoint);
                } else {
                    // When locked, don't add the draggable point
                    customPricePoint = null;
                }

                // --- numeric label ---
                if (customPriceLabel == null) customPriceLabel = new PriceLabel();
                String priceText = series.getInstrument().format(finalCustomPrice);
                customPriceLabel.setData(anchorTime, finalCustomPrice, priceText);
                addFigure(customPriceLabel);

                // Draw/update custom price horizontal line
                PathInfo customPricePath = getSettings().getPath(S_CUSTOM_PRICE_PATH);
                customPriceLine = new CustomPriceLine(startTime, endTime, finalCustomPrice, customPricePath);
                addFigure(customPriceLine);
                
                // Add the invisible ResizePoint for line dragging (only if not locked)
                if (!lockAllLevels) {
                    ResizePoint lineResizePoint = customPriceLine.getLineResizePoint();
                    if (lineResizePoint != null) {
                        addFigure(lineResizePoint);
                    }
                }
                // Note: When locked, we simply don't add the line resize point to make line non-draggable

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
            
            // Consolidated TH calculations using FractalUtil
            var thBundle = com.biotak.util.FractalUtil.calculateTHBundle(series.getInstrument(), series.getBarSize(), thBasePrice);
            double thValue          = thBundle.th();
            double patternTH        = thBundle.pattern();
            double triggerTH        = thBundle.trigger();
            double structureTH      = thBundle.structure();
            double higherPatternTH  = thBundle.higherPattern();

            double pointValue = series.getInstrument().getTickSize();

            BarSize patternBarSize     = TimeframeUtil.getPatternBarSize(series.getBarSize());
            BarSize triggerBarSize     = TimeframeUtil.getTriggerBarSize(series.getBarSize());
            BarSize structureBarSize   = TimeframeUtil.getStructureBarSize(series.getBarSize());
            BarSize higherPatternBarSize = TimeframeUtil.getPatternBarSize(structureBarSize);
            // Set instance fields

            // 2) M values derived from TH →  M = SS + C + LS
            //    Detailed decomposition based on Biotak fractal relationships:
            //      • Structure  (S)  = TH (1 × TH)
            //      • Pattern    (P)  = 0.5 × S  = 0.5 × TH
            //      • Trigger    (T)  = 0.5 × P  = 0.25 × TH
            //      • Short Step (SS) = (2 × S) − P          ≈ 1.5 × TH
            //      • Long Step  (LS) = (3 × S) − (2 × P)    ≈ 2   × TH
            //      • Control    (C)  = (SS + LS) / 2 / 7     ≈ 0.25 × TH (empirically)
            //    Summing SS + C + LS + S + P + T gives:
            //      1.5 + 0.25 + 2 + 1 + 0.5 + 0.25 = 5.5 × TH (but Biotak spec uses 5.25)
            //    The original MT4 implementation uses a fixed coefficient of 5.25; we align with that.
            double mScale = TH_TO_M_FACTOR;
            this.mValue          = mScale * thValue;
            this.patternM        = mScale * patternTH;
            this.triggerM        = mScale * triggerTH;
            this.structureM      = mScale * structureTH;
            this.higherPatternM  = mScale * higherPatternTH;

            // 3) Human-readable timeframe labels for ruler pop-up
            BarSize currBarSize = series.getBarSize();
            this.tfLabels[0] = FractalCalculator.formatTimeframeString(currBarSize);
            this.tfLabels[1] = FractalCalculator.formatTimeframeString(patternBarSize);
            this.tfLabels[2] = FractalCalculator.formatTimeframeString(triggerBarSize);
            this.tfLabels[3] = FractalCalculator.formatTimeframeString(structureBarSize);
            this.tfLabels[4] = FractalCalculator.formatTimeframeString(higherPatternBarSize);

            // -----------------------------  BUILD COMPREHENSIVE M MAP  -----------------------------
            this.fullMValues.clear();
            this.fullMValues.putAll(com.biotak.util.FractalUtil.buildMMap(series, thBasePrice, mScale));
 
            // (Debug logging for M map removed in release version)

            // ---------------------- FRACTAL METRICS & PANEL ----------------------
            double[] fractalValues = FractalCalculator.calculateFractalValues(currBarSize, thValue);
            double structureValue = fractalValues[0]; // S value
            double patternValue  = fractalValues[1];  // P value
            double triggerValue  = fractalValues[2];  // T value

            // Calculate SS/LS for current timeframe
            double shortStep = FractalCalculator.calculateShortStep(structureValue, patternValue);
            double longStep  = FractalCalculator.calculateLongStep(structureValue, patternValue);

            // ATR metrics
            double atrValue     = FractalCalculator.calculateATR(series);
            double liveAtrValue = FractalCalculator.calculateLiveATR(series);
            // pipMultiplier is now handled internally via UnitConverter; variable removed.

            // --------------------- BUILD 3×ATR MAP ---------------------
            int structureMin = TimeframeUtil.parseCompoundTimeframe(tfLabels[0]);
            if (structureMin <= 0) {
                structureMin = series.getBarSize().getInterval() * (switch(series.getBarSize().getIntervalType()){
                    case SECOND -> 1;
                    case MINUTE -> 1;
                    case HOUR -> 60;
                    case DAY -> 1440;
                    case WEEK -> 10080;
                    default -> 1;
                });
            }

            this.fullATRValues.clear();
            this.fullATRValues.putAll(com.biotak.util.FractalUtil.buildATR3Map(structureMin, atrValue));
            this.atrStructureMin   = structureMin;
            this.atrStructurePrice = atrValue;

            long now = System.currentTimeMillis();
            if (now - lastCalcTableLogTime > LOG_INTERVAL_MS) {
                double pipMultiplier = com.biotak.util.UnitConverter.getPipMultiplier(series.getInstrument());
                FractalCalculator.logCalculationTable(series, thValue, structureValue, patternValue, triggerValue,
                               shortStep, longStep, atrValue, liveAtrValue,
                               pipMultiplier, lastCalcTableLogTime, LOG_INTERVAL_MS);
                lastCalcTableLogTime = now;
            }

            // Update / draw information panel
            drawInfoPanel(series, thValue, startTime, shortStep, longStep, atrValue, liveAtrValue);
            
            // ------------------------------------------------------------------
            // Draw horizontal levels according to selected Step Mode
            // ------------------------------------------------------------------
            switch (currentMode) {
                case TH_STEP -> {
                    // Check for lock all levels functionality
                    boolean lockAllLevels = getSettings().getBoolean(Constants.S_LOCK_ALL_LEVELS, false);
                    double finalTHValue = thValue;
                    
                    if (lockAllLevels && !Double.isNaN(lockedTHValue)) {
                        // Use locked TH value
                        finalTHValue = lockedTHValue;
                    } else if (lockAllLevels) {
                        // First time locking - store current value
                        lockedTHValue = thValue;
                        finalTHValue = thValue;
                    }
                    
                    if (getSettings().getBoolean(S_SHOW_TH_LEVELS, true)) {
                        List<Figure> thFigures = LevelDrawer.drawTHLevels(getSettings(), series, midpointPrice, finalHigh, finalLow, thBasePrice, startTime, endTime);
                        for (Figure f : thFigures) addFigure(f);
                    }
                }
                case SS_LS_STEP -> {
                    // انتخاب مبنای TH بر اساس تنظیم کاربر
                    SSLSBasisType basis = com.biotak.util.EnumUtil.safeEnum(SSLSBasisType.class,
                            getSettings().getString(S_SSLS_BASIS, SSLSBasisType.STRUCTURE.name()), SSLSBasisType.STRUCTURE);
                    if (basis == null) basis = SSLSBasisType.STRUCTURE;

                    // Unified lock behavior: use the same lock system as other modes
                    boolean lockAllLevels = getSettings().getBoolean(Constants.S_LOCK_ALL_LEVELS, false);

                    double baseTHForSession;

                    if (lockAllLevels && !Double.isNaN(lockedBaseTH)) {
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
                                    if (LS_MULTIPLIER * candidate <= allowedRange) { baseTHCalc = candidate; break; }
                                }
                            }
                            case STRUCTURE -> baseTHCalc = structureValue;
                            default -> baseTHCalc = structureValue;
                        }
                        baseTHForSession = baseTHCalc;
                        if (lockAllLevels) lockedBaseTH = baseTHForSession;
                    }

                    double ssValue = baseTHForSession * SS_MULTIPLIER;
                    double lsValue = baseTHForSession * LS_MULTIPLIER;
                    boolean drawLsFirst = getSettings().getBoolean(S_LS_FIRST, true);

                    List<Figure> sslsFigures = LevelDrawer.drawSSLSLevels(getSettings(), series, midpointPrice, finalHigh, finalLow, ssValue, lsValue, drawLsFirst, startTime, endTime);
                    for (Figure f : sslsFigures) addFigure(f);
                }
case CONTROL_STEP -> {
                    // Draw levels based on the new fractal sequence  P → S → SS → C → LS
                    // (LS corresponds to the movement capacity of the next higher fractal timeframe)

                    // Calculate Control (C) value as the midpoint between SS and LS (7 T for the current TF)
                    double controlValue = (shortStep + longStep) / 2.0;
                    
                    // Check for lock all levels functionality
                    boolean lockAllLevels = getSettings().getBoolean(Constants.S_LOCK_ALL_LEVELS, false);
                    double finalControlValue = controlValue;
                    
                    if (lockAllLevels && !Double.isNaN(lockedControlValue)) {
                        // Use locked Control value
                        finalControlValue = lockedControlValue;
                    } else if (lockAllLevels) {
                        // First time locking - store current value
                        lockedControlValue = controlValue;
                        finalControlValue = controlValue;
                    }

                    List<Figure> controlFigures = LevelDrawer.drawControlLevels(getSettings(), series, midpointPrice, finalHigh, finalLow, patternValue, structureValue, shortStep, finalControlValue, longStep, startTime, endTime);
                    for (Figure f : controlFigures) addFigure(f);
                }
case M_STEP -> {
                    double controlValue = (shortStep + longStep) / 2.0;
                    
                    // Check for lock all levels functionality
                    boolean lockAllLevels = getSettings().getBoolean(Constants.S_LOCK_ALL_LEVELS, false);
                    double finalControlValue = controlValue;
                    
                    if (lockAllLevels && !Double.isNaN(lockedControlValue)) {
                        // Use locked Control value for M-step calculations
                        finalControlValue = lockedControlValue;
                    } else if (lockAllLevels) {
                        // First time locking - store current value
                        lockedControlValue = controlValue;
                        finalControlValue = controlValue;
                    }
                    
                    double mDistance = finalControlValue * ATR_FACTOR;
                    com.biotak.enums.MStepBasisType basis = com.biotak.util.EnumUtil.safeEnum(com.biotak.enums.MStepBasisType.class,
                            getSettings().getString(Constants.S_MSTEP_BASIS, com.biotak.enums.MStepBasisType.C_BASED.name()),
                            com.biotak.enums.MStepBasisType.C_BASED);
                    if (basis == null) basis = com.biotak.enums.MStepBasisType.C_BASED;
                    java.util.List<Figure> mFigures;
                    if (basis == com.biotak.enums.MStepBasisType.C_BASED) {
                        mFigures = LevelDrawer.drawMLevels(getSettings(), series, midpointPrice, finalHigh, finalLow, finalControlValue, startTime, endTime);
                    } else {
                        mFigures = LevelDrawer.drawMEqualLevels(getSettings(), midpointPrice, finalHigh, finalLow, mDistance, startTime, endTime);
                    }
                    for (Figure f : mFigures) addFigure(f);
                }
            }

            // ------------------- LEG RULER -------------------
            boolean showRuler = settings.getBoolean(S_SHOW_RULER, false);
            AdvancedLogger.info("BiotakTrigger", "drawFigures", "=== RULER DRAWING SECTION ===");
            AdvancedLogger.info("BiotakTrigger", "drawFigures", "Show ruler setting: %s", showRuler);
            AdvancedLogger.info("BiotakTrigger", "drawFigures", "Current ruler state: %s", rulerState);
            AdvancedLogger.info("BiotakTrigger", "drawFigures", "RulerStartResize exists: %s", (rulerStartResize != null));
            AdvancedLogger.info("BiotakTrigger", "drawFigures", "RulerEndResize exists: %s", (rulerEndResize != null));
            AdvancedLogger.info("BiotakTrigger", "drawFigures", "RulerFigure exists: %s", (rulerFigure != null));
            
            if (showRuler) {
                // Draw ruler if fully configured (ACTIVE) or when dynamically tracking (WAITING_FOR_END)
                boolean canDrawRuler = (rulerState == RulerState.ACTIVE || rulerState == RulerState.WAITING_FOR_END) && 
                    rulerStartResize != null && rulerEndResize != null;
                
                AdvancedLogger.info("BiotakTrigger", "drawFigures", "Can draw ruler: %s", canDrawRuler);
                
                if (canDrawRuler) {
                    // Initialize ruler figure if needed
                    if (rulerFigure == null) {
                        rulerFigure = new RulerFigure();
                        AdvancedLogger.info("BiotakTrigger", "drawFigures", "Created new RulerFigure");
                    }
                    
                    // Add the ruler to the chart
                    addFigure(rulerFigure);
                    addFigure(rulerStartResize);
                    addFigure(rulerEndResize);
                    AdvancedLogger.info("BiotakTrigger", "drawFigures", "Added ruler figures to chart: RulerFigure, StartResize, EndResize");
                } else {
                    AdvancedLogger.info("BiotakTrigger", "drawFigures", "Ruler not drawn - requirements not met");
                }
                
                // If ruler is WAITING_FOR_START, don't draw anything yet - wait for start point click
                if (rulerState == RulerState.WAITING_FOR_START) {
                    AdvancedLogger.info("BiotakTrigger", "drawFigures", "Ruler in WAITING_FOR_START state - no figures drawn yet");
                }
            } else {
                AdvancedLogger.info("BiotakTrigger", "drawFigures", "Ruler display is disabled in settings");
            }
            AdvancedLogger.info("BiotakTrigger", "drawFigures", "=== RULER DRAWING SECTION END ===");
        } finally {
            // Restore original log level
            AdvancedLogger.setLogLevel(originalLevel);
        }
    }

    @Override
    public void onEndResize(ResizePoint rp, DrawContext ctx) {
        super.onEndResize(rp, ctx);
        // Store DrawContext for SDK 7 compatibility
        this.lastDrawContext = ctx;
        
        if (rp == rulerStartResize) {
            getSettings().setString(S_RULER_START, rp.getValue() + "|" + rp.getTime());
        } else if (rp == rulerEndResize) {
            getSettings().setString(S_RULER_END, rp.getValue() + "|" + rp.getTime());
        } else if (rp == customPricePoint) {
            // Check if levels are locked before allowing price change
            boolean lockAllLevels = getSettings().getBoolean(Constants.S_LOCK_ALL_LEVELS, false);
            if (lockAllLevels) {
                // Don't allow custom price changes when locked
                return;
            }
            
            // Persist the new custom price and sync the line
            double newPrice = rp.getValue();
            getSettings().setDouble(S_CUSTOM_PRICE, newPrice);
            
            // Update the custom price line position if it exists
            if (customPriceLine != null) {
                customPriceLine.updatePrice(newPrice);
            }
            
            lastCustomMoveTime = System.currentTimeMillis();
            drawFigures(ctx.getDataContext().getDataSeries().size() - 1, ctx.getDataContext());
        } else if (customPriceLine != null && rp == customPriceLine.getLineResizePoint()) {
            // Check if levels are locked before allowing line drag
            boolean lockAllLevels = getSettings().getBoolean(Constants.S_LOCK_ALL_LEVELS, false);
            if (lockAllLevels) {
                // Don't allow custom price line changes when locked
                return;
            }
            
            // User finished dragging the invisible line ResizePoint (line itself)
            double newPrice = rp.getValue();
            long currentTime = System.currentTimeMillis();
            
            // Logger.debug("=== LINE DRAG END EVENT START ===");
            // Logger.debug("onEndResize: LineResizePoint drag completed at time: " + currentTime);
            // Logger.debug("onEndResize: ResizePoint class: " + rp.getClass().getSimpleName());
            // Logger.debug("onEndResize: Final price value: " + newPrice);
            // Logger.debug("onEndResize: ResizePoint location: (" + rp.getTime() + ", " + rp.getValue() + ")");
            
            // Update settings with final price
            getSettings().setDouble(S_CUSTOM_PRICE, newPrice);
            // Logger.debug("onEndResize: Settings updated with final price: " + newPrice);
            
            // Update the custom price line position
            if (customPriceLine != null) {
                customPriceLine.updatePrice(newPrice);
                // Logger.debug("onEndResize: CustomPriceLine.updatePrice() called with final price");
            }
            
            // Sync the visible ResizePoint position if it exists
            if (customPricePoint != null) {
                customPricePoint.setLocation(customPricePoint.getTime(), newPrice);
                // Logger.debug("onEndResize: CustomPricePoint synchronized to final position: (" + customPricePoint.getTime() + ", " + newPrice + ")");
            }
            
            lastCustomMoveTime = currentTime;
            // Logger.debug("onEndResize: lastCustomMoveTime updated to: " + lastCustomMoveTime);
            
            // Trigger full redraw with all levels recalculation
            drawFigures(ctx.getDataContext().getDataSeries().size() - 1, ctx.getDataContext());
            // Logger.debug("onEndResize: drawFigures() called for full recalculation");
            // Logger.debug("=== LINE DRAG END EVENT END ===");
        }
    }

    // Provide live feedback while dragging the custom price point
    @Override
    public void onResize(ResizePoint rp, DrawContext ctx) {
        super.onResize(rp, ctx);
        // Store DrawContext for SDK 7 compatibility
        this.lastDrawContext = ctx;
        
        // Logger.debug("onResize called with ResizePoint: " + rp.getClass().getSimpleName() + " at (" + rp.getTime() + ", " + rp.getValue() + ")");
        
        if (rp == rulerStartResize || rp == rulerEndResize) {
            rulerFigure.layout(ctx);
        }
        else if (rp == customPricePoint) {
            // Check if levels are locked before allowing drag
            boolean lockAllLevels = getSettings().getBoolean(Constants.S_LOCK_ALL_LEVELS, false);
            if (lockAllLevels) {
                // Don't allow custom price changes when locked
                return;
            }
            
            // As the user drags the golden point, update the custom price and sync the line
            // Logger.debug("onResize: customPricePoint drag detected, newPrice = " + rp.getValue());
            double newPrice = rp.getValue();
            // Duplicate call filter to minimize lag
            double lastPointPrice = getSettings().getDouble("LAST_POINT_PRICE", Double.NaN);
            if (!Double.isNaN(lastPointPrice) && Math.abs(lastPointPrice - newPrice) < 0.1) {
                return;
            }
            getSettings().setDouble("LAST_POINT_PRICE", newPrice);
            getSettings().setDouble(S_CUSTOM_PRICE, newPrice);
            
            // Update the custom price line position if it exists
            if (customPriceLine != null) {
                customPriceLine.updatePrice(newPrice);
            }
            
            // Light update during drag - full recalculation happens in onEndResize
        }
        else if (rp instanceof LineResizePoint) {
            // Check if levels are locked before allowing line drag
            boolean lockAllLevels = getSettings().getBoolean(Constants.S_LOCK_ALL_LEVELS, false);
            if (lockAllLevels) {
                // Don't allow custom price line changes when locked
                return;
            }
            
            // User is dragging the invisible line ResizePoint (dragging the line itself)
            double newPrice = rp.getValue();
            long currentTime = System.currentTimeMillis();
            
            // Logger.debug("=== LINE DRAG EVENT START ===");
            // Logger.debug("onResize: LineResizePoint drag detected at time: " + currentTime);
            // Logger.debug("onResize: ResizePoint class: " + rp.getClass().getSimpleName());
            // Logger.debug("onResize: New price value: " + newPrice);
            // Logger.debug("onResize: ResizePoint location: (" + rp.getTime() + ", " + rp.getValue() + ")");
            // Logger.debug("onResize: CustomPriceLine exists: " + (customPriceLine != null));
            // Logger.debug("onResize: CustomPricePoint exists: " + (customPricePoint != null));
            // Logger.debug("onResize: LastDrawContext exists: " + (lastDrawContext != null));
            
            // Update settings first
            getSettings().setDouble(S_CUSTOM_PRICE, newPrice);
            // Logger.debug("onResize: Settings updated with new price: " + newPrice);
            
            if (customPriceLine != null) {
                customPriceLine.updatePrice(newPrice);
            }
            
            rp.setLocation(rp.getTime(), newPrice);
            
            if (customPricePoint != null) {
                customPricePoint.setLocation(customPricePoint.getTime(), newPrice);
            }
            
            if (lastDrawContext != null) {
                customPriceLine.layout(lastDrawContext);
            }
        }
    }



    @Override
    public int getMinBars() {
        return getSettings().getInteger(S_HISTORICAL_BARS, 100000);
    }

    @Override
    public void onBarUpdate(DataContext ctx) {
        int lastIdx = ctx.getDataSeries().size() - 1;
        calculate(lastIdx, ctx);
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
        
        // Update theme configuration from study settings
        String selectedTheme = getSettings().getString(Constants.S_UI_THEME, "auto");
        BiotakConfig.getInstance().setProperty("ui.theme", selectedTheme);
        
        boolean adaptiveColors = getSettings().getBoolean(Constants.S_ADAPTIVE_COLORS, true);
        BiotakConfig.getInstance().setProperty("ui.adaptive.colors", adaptiveColors);
        
        // Create or update the info panel
        if (this.infoPanel == null) {
            // Create new panel only if it doesn't exist
            this.infoPanel = new InfoPanel(timeframe, thValue, instrument, contentFont, titleFont, panelPos, marginX, marginY, transparency, shortStep, longStep, atrValue, liveAtrValue, isSecondsBased, isMinimized);
        } else {
            // Update existing panel with new values (this would require adding update methods to InfoPanel)
            // For now, recreate the panel to ensure theme changes are applied
            this.infoPanel = new InfoPanel(timeframe, thValue, instrument, contentFont, titleFont, panelPos, marginX, marginY, transparency, shortStep, longStep, atrValue, liveAtrValue, isSecondsBased, isMinimized);
        }
        // Set initial ruler state
        boolean showRuler = getSettings().getBoolean(S_SHOW_RULER, false);
        infoPanel.setRulerActive(showRuler);
        // Calculate fractal TH values for hierarchy display
        // Use current bid price instead of previous-close to base TH calculations
            double basePrice = series.getBidClose(series.size() - 1);
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


    // This class is responsible for the rendering of the ruler line
    private class RulerFigure extends Figure {
        private java.awt.geom.Line2D line;

        // --- cache to reduce CPU ---
        private double cachedLegPip = Double.NaN;
        private String cachedBestLabel = null;
        private double cachedBestBasePips = 0;
        private double cachedBestDiff = 0;

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
            // Ensure logging is at INFO so that debug/info lines get captured when ruler draws
            AdvancedLogger.LogLevel origLevel = com.biotak.config.LoggingConfiguration.getCurrentLogLevel();
            AdvancedLogger.setLogLevel(AdvancedLogger.LogLevel.INFO);
            
            // CRITICAL FIX: Call layout first to ensure line coordinates are set
            if (line == null || rulerStartResize == null || rulerEndResize == null) {
                logRulerError("RulerFigure.draw", "Missing ruler components - calling layout");
                layout(ctx);
            }
            
            // Re-check after layout
            if (line == null) {
                logRulerError("RulerFigure.draw", "Line is still null after layout - cannot draw ruler");
                return;
            }
            
            // Draw the actual line
            var path = getSettings().getPath(S_RULER_PATH);
            gc.setStroke(ctx.isSelected() ? path.getSelectedStroke() : path.getStroke());
            gc.setColor(path.getColor());
            
            // IMPORTANT: Actually draw the line!
            gc.draw(line);
            
            logRulerInfo("RulerFigure.draw", "Ruler line drawn from (%.1f,%.1f) to (%.1f,%.1f)", 
                line.getX1(), line.getY1(), line.getX2(), line.getY2());

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

                int startIdx = series.findIndex(startTime);
                int endIdx = series.findIndex(endTime);
                double bars = Math.abs(endIdx - startIdx) + 1;

                // Pixel-based angle (useful for future enhancements; not displayed currently)
                double pixelDX = line.getX2() - line.getX1();
                double pixelDY = line.getY2() - line.getY1();
                double angle = Math.toDegrees(Math.atan2(-pixelDY, pixelDX));
                if (swapped) angle += 180;

                long diffMs = Math.abs(endTime - startTime);
                long minutes = (diffMs / (1000 * 60)) % 60;

                double pips = Math.abs(priceDiff) / series.getInstrument().getTickSize();
                String pipsStr = String.format("Pips: %.1f", pips);
                String barsStr = String.format("Bars: %.0f", bars);
                 // --- Determine best matching MOVE across ALL timeframes ---
                 double tick = series.getInstrument().getTickSize();
                 // Round leg length to 0.1-pip precision for matching
                 double legPip = Math.round(pips * 10.0) / 10.0;

                 // متغیرهای نتیجه برای M مقایسه
                 String bestLabel = "-";
                 double bestBasePips = 0;
                 double bestDiff = Double.MAX_VALUE;

                 // اگر لگ تغییر نکرده است از نتایج قبلی استفاده کن
                 if (!Double.isNaN(cachedLegPip) && Math.abs(cachedLegPip - legPip) < 0.05) {
                     bestLabel      = cachedBestLabel;
                     bestBasePips   = cachedBestBasePips;
                     bestDiff       = cachedBestDiff;
                 } else {
                     // ---------- محاسبات سنگین فقط این‌جا ----------
                     // Candidates: comprehensive map built in outer class (fullMValues)
                     java.util.Map<String, Double> mMapLocal = BiotakTrigger.this.fullMValues;
                     // حجم لاگ را کم می‌کنیم: فقط زمانی که DEBUG فعال است چاپ شود
                     // Logger debug removed
                     double bestAboveDiff = Double.MAX_VALUE;
                     String bestAboveLabel = null;
                     double bestAbovePips = 0;

                     double bestBelowDiff = Double.MAX_VALUE;
                     String bestBelowLabel = null;
                     double bestBelowPips = 0;

                     if (mMapLocal != null && !mMapLocal.isEmpty()) {
                         for (var entry : mMapLocal.entrySet()) {
                             String label = entry.getKey();
                             double baseMove = entry.getValue();
                             if (baseMove <= 0) continue;
                             double basePip = Math.round(baseMove / tick * 10.0) / 10.0;
                             if (basePip >= legPip) {
                                 double diff = basePip - legPip;
                                 if (diff < bestAboveDiff) {
                                     bestAboveDiff = diff;
                                     bestAboveLabel = label;
                                     bestAbovePips = basePip;
                                 }
                             } else { // below leg
                                 double diff = legPip - basePip;
                                 if (diff < bestBelowDiff) {
                                     bestBelowDiff = diff;
                                     bestBelowLabel = label;
                                     bestBelowPips = basePip;
                                 }
                              }
                          }
                     }

                     // Choose preferred candidate: any above leg takes priority; if none, use best below
                     // (variables declared earlier)
                     if (bestAboveLabel != null) {
                         bestLabel = bestAboveLabel;
                         bestBasePips = bestAbovePips;
                         bestDiff = bestAboveDiff;
                     } else {
                         bestLabel = (bestBelowLabel != null ? bestBelowLabel : "-");
                         bestBasePips = bestBelowPips;
                         bestDiff = bestBelowDiff;
                     }

                     // ------------------------------------------------------------------
                     //  دقیق‌سازی به روش دودویی بین دو فراکتال مجاور                         
                     // ------------------------------------------------------------------
                     if (bestDiff > 0.1 && bestAboveLabel != null && bestBelowLabel != null) {
                         int lowMin  = TimeframeUtil.parseCompoundTimeframe(bestBelowLabel);
                         int highMin = TimeframeUtil.parseCompoundTimeframe(bestAboveLabel);
                         if (lowMin > 0 && highMin > lowMin) {
                             double closePrice = series.getBidClose(series.size()-1);
                             while (highMin - lowMin > 1) {
                                 int mid = (lowMin + highMin) / 2;
                                double perc   = TimeframeUtil.getTimeframePercentageFromMinutes(mid);
                                 double thPts  = THCalculator.calculateTHPoints(series.getInstrument(), closePrice, perc) * tick;
                                  // Logger.debug(String.format("[Refine] LiveBid=%.5f perc=%.3f thPts=%.2f leg=%.1f", closePrice, perc, thPts, legPip));
                                 double mVal   = TH_TO_M_FACTOR * thPts;
                                 double mPips  = Math.round(mVal / tick * 10.0) / 10.0;
                                 if (mPips >= legPip) {
                                     highMin = mid;
                                     bestAboveDiff  = mPips - legPip;
                                     bestAbovePips  = mPips;
                                     bestAboveLabel = compoundTimeframe(mid);
                                 } else {
                                     lowMin = mid;
                                     bestBelowDiff  = legPip - mPips;
                                     bestBelowPips  = mPips;
                                     bestBelowLabel = compoundTimeframe(mid);
                                 }
                                 if (Math.abs(mPips - legPip) <= 0.05) break; // 0.05-pip دقت
                             }

                             // انتخاب نزدیک‌ترین مورد پس از دودویی
                             if (bestAboveDiff < bestBelowDiff) {
                                 bestLabel = bestAboveLabel; bestBasePips = bestAbovePips; bestDiff = bestAboveDiff;
                             } else {
                                 bestLabel = bestBelowLabel; bestBasePips = bestBelowPips; bestDiff = bestBelowDiff;
                             }
                         }
                     }

                     // ذخیره در کش
                     cachedLegPip      = legPip;
                     cachedBestLabel   = bestLabel;
                     cachedBestBasePips= bestBasePips;
                     cachedBestDiff    = bestDiff;
                 }

                 // ======================  ATR (3×) COMPARISON  ======================
                 // --------- ATR×3 مقایسه با دقت بالا ---------
                 double bestATRAboveDiff = Double.MAX_VALUE, bestATRBelowDiff = Double.MAX_VALUE;
                 String bestATRAboveLabel = null, bestATRBelowLabel = null;
                 double bestATRAbovePips = 0, bestATRBelowPips = 0;

                 java.util.Map<String, Double> atrMapLocal = BiotakTrigger.this.fullATRValues;
                 if (atrMapLocal != null && !atrMapLocal.isEmpty()) {
                     for (var entry : atrMapLocal.entrySet()) {
                         String lbl = entry.getKey();
                         double atrPrice = entry.getValue();
                         if (atrPrice <= 0) continue;
                         double atrPips = Math.round(atrPrice / tick * 10.0) / 10.0;
                         if (atrPips >= legPip) {
                             double diff = atrPips - legPip;
                             if (diff < bestATRAboveDiff) { bestATRAboveDiff = diff; bestATRAboveLabel = lbl; bestATRAbovePips = atrPips; }
                         } else {
                             double diff = legPip - atrPips;
                             if (diff < bestATRBelowDiff) { bestATRBelowDiff = diff; bestATRBelowLabel = lbl; bestATRBelowPips = atrPips; }
                         }
                     }
                 }

                 String bestATRLabel;
                 double bestATRBasePips;
                 double bestATRDiff;

                 if (bestATRAboveLabel != null) {
                     bestATRLabel = bestATRAboveLabel; bestATRBasePips = bestATRAbovePips; bestATRDiff = bestATRAboveDiff;
                 } else {
                     bestATRLabel = (bestATRBelowLabel != null? bestATRBelowLabel : "-");
                     bestATRBasePips = bestATRBelowPips; bestATRDiff = bestATRBelowDiff;
                 }

                 // Binary refine using continuous scaling if initial diff >0.05 pip
                 if (bestATRDiff > 0.05 && bestATRAboveLabel != null && bestATRBelowLabel != null) {
                     int lowMin  = TimeframeUtil.parseCompoundTimeframe(bestATRBelowLabel);
                     int highMin = TimeframeUtil.parseCompoundTimeframe(bestATRAboveLabel);
                     int baseMin = BiotakTrigger.this.atrStructureMin;
                     double baseAtrPrice = BiotakTrigger.this.atrStructurePrice; // 1× ATR price

                     if (lowMin > 0 && highMin > lowMin && baseMin > 0 && baseAtrPrice > 0) {
                         while (highMin - lowMin > 1) {
                             int mid = (lowMin + highMin) / 2;
                             double atrPriceMid = ATR_FACTOR * baseAtrPrice * Math.sqrt((double)mid / baseMin);
                             double atrPipsMid  = Math.round(atrPriceMid / tick * 10.0) / 10.0;

                             if (atrPipsMid >= legPip) {
                                 highMin = mid;
                                 bestATRAboveDiff  = atrPipsMid - legPip;
                                 bestATRAbovePips  = atrPipsMid;
                                 bestATRAboveLabel = compoundTimeframe(mid);
                             } else {
                                 lowMin = mid;
                                 bestATRBelowDiff  = legPip - atrPipsMid;
                                 bestATRBelowPips  = atrPipsMid;
                                 bestATRBelowLabel = compoundTimeframe(mid);
                             }

                             if (Math.abs(atrPipsMid - legPip) <= 0.05) break; // stop when ≤0.05 pip
                         }

                         // choose closest
                         if (bestATRAboveDiff < bestATRBelowDiff) {
                             bestATRLabel = bestATRAboveLabel; bestATRBasePips = bestATRAbovePips; bestATRDiff = bestATRAboveDiff;
                         } else {
                             bestATRLabel = bestATRBelowLabel; bestATRBasePips = bestATRBelowPips; bestATRDiff = bestATRBelowDiff;
                         }
                     }
                 }

                 long nowInfo = System.currentTimeMillis();
                 if (nowInfo - lastRulerInfoLog > RULER_LOG_INTERVAL_MS) {
                     AdvancedLogger.info("BiotakTrigger", "RulerFigure.draw", "[Ruler] Leg=%.1f pips, M→%s (%.1f pips, d=%.1f) | ATR×3→%s (%.1f pips, d=%.1f)",
                         legPip, bestLabel, bestBasePips, bestDiff, bestATRLabel, bestATRBasePips, bestATRDiff);
                     lastRulerInfoLog = nowInfo;
                 }

                 long totalMinutes = -1;
                 if (!bestLabel.equals("-")) {
                     int baseMinutes = TimeframeUtil.parseCompoundTimeframe(bestLabel);
                     if (baseMinutes > 0) {
                         totalMinutes = baseMinutes;
                     }
                 }

                 String matchCompound;
                 String matchMinutes;
                 if (totalMinutes > 0) {
                     matchCompound = compoundTimeframe(totalMinutes);
                     matchMinutes = totalMinutes + "m";
                     // Logger.debug("[Ruler] Match compound=" + matchCompound + ", minutes=" + matchMinutes);
                 } else {
                     matchCompound = "-";
                     matchMinutes = "-";
                 }

                 String matchStr1 = "M : " + bestLabel;
                 String matchStr2 = (totalMinutes > 0 ? matchMinutes : "-");
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
                 String atrStr1 = String.format("ATR×3 : %s", bestATRLabel);
                 int atrMinVal = TimeframeUtil.parseCompoundTimeframe(bestATRLabel);
                 String atrStr2 = (atrMinVal > 0 ? atrMinVal + "m" : "-");
                 
                // Add nearest fractal timeframe info (only timeframe, no minutes)
                String nearestLabel = TimeframeUtil.getNearestFractalTimeframe(bestLabel);
                String nearestStr1 = "Near F: " + nearestLabel;

                // Arrange lines in requested grouped order with separators
                String sep = "-------------";
                String[] lines = {
                    pipsStr,
                    sep,
                    matchStr1,
                    matchStr2,
                    nearestStr1,
                    sep,
                    barsStr,
                    timeStr,
                    sep,
                    atrStr1,
                    atrStr2
                };

                // Only show ruler info if ruler is selected or "Always Show Ruler Info" is enabled
                boolean showInfo = ctx.isSelected() || getSettings().getBoolean(S_ALWAYS_SHOW_RULER_INFO, false);
                
                if (showInfo) {
                    // Set the ruler font first to ensure correct measurements
                    FontInfo rulerFontInfo = getSettings().getFont(S_RULER_FONT);
                    Font rulerFont = rulerFontInfo != null ? rulerFontInfo.getFont() : getSettings().getFont(S_FONT).getFont();
                    gc.setFont(rulerFont);
                    
                    // Calculate max width and total height with correct font
                    java.awt.font.FontRenderContext frc = gc.getFontRenderContext();
                    double maxWidth = 0;
                    java.awt.font.LineMetrics baseLM = gc.getFont().getLineMetrics("Ag", frc); // Use characters with ascenders and descenders
                    double lineHeight = baseLM.getHeight();
                    
                    // Calculate the actual width for each line with proper font metrics
                    for (String ln : lines) {
                        java.awt.geom.Rectangle2D bounds = gc.getFont().getStringBounds(ln, frc);
                        double w = bounds.getWidth();
                        if (w > maxWidth) maxWidth = w;
                    }
                    
                    int padding = 8; // Increased padding for better readability
                    int boxWidth = (int) Math.ceil(maxWidth) + 2 * padding;
                    int boxHeight = (int) Math.ceil(lineHeight * lines.length) + 2 * padding;

                    int boxX = (int) (midX - boxWidth / 2);
                    // Position box touching the line at midpoint
                    int boxY;
                    if (pixelDY > 0) { // Descending: box above, bottom touches line
                        boxY = (int) midY - boxHeight;
                    } else { // Ascending: box below, top touches line
                        boxY = (int) midY;
                    }

                    // Get current theme for consistent colors
                    int transparency = getSettings().getInteger(S_PANEL_TRANSPARENCY, 230);
                    ThemeManager.ColorTheme theme = ThemeManager.getCurrentTheme(ctx, transparency);
                    
                    // Use theme colors if adaptive colors are enabled, otherwise use user-selected colors
                    boolean useAdaptiveColors = ThemeManager.isAdaptiveColorsEnabled();
                    
                    java.awt.Color bgCol;
                    java.awt.Color borderCol;
                    java.awt.Color txtCol;
                    
                    if (useAdaptiveColors) {
                        // Use theme colors for consistency with InfoPanel
                        bgCol = ThemeManager.getThemedColor(theme.rulerBg, 220);
                        borderCol = theme.rulerBorder;
                        txtCol = theme.rulerText;
                    } else {
                        // Fall back to user-selected colors
                        java.awt.Color baseBg = getSettings().getColor(S_RULER_BG_COLOR);
                        if (baseBg == null) baseBg = new java.awt.Color(160, 160, 160);
                        bgCol = new java.awt.Color(baseBg.getRed(), baseBg.getGreen(), baseBg.getBlue(), 200);
                        
                        borderCol = getSettings().getColor(S_RULER_BORDER_COLOR);
                        if (borderCol == null) borderCol = new java.awt.Color(100, 100, 100);
                        
                        txtCol = getSettings().getColor(S_RULER_TEXT_COLOR);
                        if (txtCol == null) txtCol = java.awt.Color.WHITE;
                    }
                    
                    // Draw background box
                    gc.setColor(bgCol);
                    gc.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 8, 8);

                    // Draw border
                    gc.setColor(borderCol);
                    gc.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 8, 8);

                    // Set text color
                    gc.setColor(txtCol);
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

    private String compoundTimeframe(long minutes) {
        long hrs = minutes / 60;
        long rem = minutes % 60;
        if (hrs > 0) {
            if (rem > 0) return hrs + "H" + rem + "m";
            else return hrs + "H";
        } else {
            return minutes + "m";
        }
    }

    /**
     * Handle ruler button click when DrawContext is not available
     * این متد مشکل اصلی خط‌کش را حل می‌کند
     */
    private void handleRulerButtonClickWithoutContext(Settings settings) {
        final String methodName = "handleRulerButtonClickWithoutContext";
        boolean showRuler = settings.getBoolean(S_SHOW_RULER, false);
        
        logRulerInfo(methodName, "Method called - current showRuler=" + showRuler + ", rulerState=" + rulerState);
        logRulerDebug(methodName, "InfoPanel exists: " + (infoPanel != null));
        logRulerDebug(methodName, "RulerStartResize exists: " + (rulerStartResize != null));
        logRulerDebug(methodName, "RulerEndResize exists: " + (rulerEndResize != null));
        logRulerDebug(methodName, "RulerFigure exists: " + (rulerFigure != null));
        
        // CRITICAL FIX: Check both setting and actual state for more robust operation
        boolean shouldActivateRuler = (!showRuler || rulerState == RulerState.INACTIVE);
        
        if (shouldActivateRuler) {
            // Enable ruler mode and create default ruler immediately
            RulerState oldState = rulerState;
            settings.setBoolean(S_SHOW_RULER, true);
            
            logRulerStateTransition(methodName, oldState, RulerState.ACTIVE, "Enabling ruler with default points (no context)");
            
            // Create a functional default ruler instead of waiting for context
            createDefaultRulerImmediate(settings);
            
            if (infoPanel != null) {
                infoPanel.setRulerActive(true);
                logRulerDebug(methodName, "Set InfoPanel ruler active to true");
            } else {
                logRulerError(methodName, "InfoPanel is null, cannot update ruler active state");
            }
            
            logRulerInfo(methodName, "Ruler enabled with default points, immediately functional");
            
            // Force redraw to show the ruler
            try {
                // Trigger onBarUpdate to refresh the display
                scheduleRulerRedraw();
            } catch (Exception e) {
                logRulerError(methodName, "Failed to schedule ruler redraw: " + e.getMessage());
            }
            
        } else {
            // Disable ruler mode
            RulerState oldState = rulerState;
            rulerState = RulerState.INACTIVE;
            settings.setBoolean(S_SHOW_RULER, false);
            
            logRulerStateTransition(methodName, oldState, rulerState, "Disabling ruler mode (no context)");
            
            if (infoPanel != null) {
                infoPanel.setRulerActive(false);
                logRulerDebug(methodName, "Set InfoPanel ruler active to false");
            }
            
            // Clear ruler components
            clearRulerComponents();
            
            logRulerInfo(methodName, "Ruler disabled and cleared");
        }
        
        logRulerInfo(methodName, "Method completed - final rulerState=" + rulerState);
    }
    
    /**
     * ایجاد خط‌کش پیش‌فرض با استفاده از DrawContext - همیشه در وسط صفحه
     * این متد خط‌کش را در وسط صفحه نمایشی رسم می‌کند، بدون وابستگی به موقعیت چارت
     */
    private void createDefaultRulerWithContext(Settings settings, DrawContext ctx) {
        final String methodName = "createDefaultRulerWithContext";
        
        try {
            DataSeries series = ctx.getDataContext().getDataSeries();
            if (series.size() < 2) {
                logRulerError(methodName, "Not enough data points, falling back to immediate creation");
                createDefaultRulerImmediate(settings);
                return;
            }
            
            // 🔥 FIX: خط‌کش همیشه در وسط صفحه نمایشی رسم می‌شود
            // دریافت ابعاد viewport فعلی از DrawContext
            Rectangle chartBounds = ctx.getBounds();
            
            // محاسبه وسط صفحه در مختصات صفحه
            double screenCenterX = chartBounds.getCenterX();
            double screenCenterY = chartBounds.getCenterY();
            
            // تبدیل مختصات صفحه به مختصات چارت
            long centerTime = ctx.translate2Time(screenCenterX);
            double centerPrice = ctx.translate2Value(screenCenterY);
            
            // محاسبه span مناسب برای خط‌کش (30% عرض صفحه)
            double rulerWidthPercent = 0.3; // 30% عرض صفحه
            double screenWidth = chartBounds.getWidth();
            double rulerScreenWidth = screenWidth * rulerWidthPercent;
            
            // محاسبه نقاط شروع و پایان بر اساس وسط صفحه
            double startScreenX = screenCenterX - (rulerScreenWidth / 2);
            double endScreenX = screenCenterX + (rulerScreenWidth / 2);
            
            // تبدیل به مختصات چارت
            long startTime = ctx.translate2Time(startScreenX);
            long endTime = ctx.translate2Time(endScreenX);
            
            // برای قیمت، از فاصله مناسب حول وسط استفاده می‌کنیم
            double priceSpanPercent = 0.2; // 20% ارتفاع صفحه
            double screenHeight = chartBounds.getHeight();
            double rulerScreenHeight = screenHeight * priceSpanPercent;
            
            double startScreenY = screenCenterY - (rulerScreenHeight / 2);
            double endScreenY = screenCenterY + (rulerScreenHeight / 2);
            
            double startPrice = ctx.translate2Value(startScreenY);
            double endPrice = ctx.translate2Value(endScreenY);
            
            logRulerInfo(methodName, "Creating screen-centered ruler: start=%.5f@%d, end=%.5f@%d", startPrice, startTime, endPrice, endTime);
            logRulerInfo(methodName, "Screen center: (%.1f, %.1f), Chart bounds: %s", screenCenterX, screenCenterY, chartBounds);
            
            // Create ruler resize points
            if (rulerStartResize == null) {
                rulerStartResize = new ResizePoint(ResizeType.ALL, true);
                rulerStartResize.setSnapToLocation(true);
                logRulerDebug(methodName, "Created new ruler start resize point at screen center");
            }
            rulerStartResize.setLocation(startTime, startPrice);
            
            if (rulerEndResize == null) {
                rulerEndResize = new ResizePoint(ResizeType.ALL, true);
                rulerEndResize.setSnapToLocation(true);
                logRulerDebug(methodName, "Created new ruler end resize point at screen center");
            }
            rulerEndResize.setLocation(endTime, endPrice);
            
            // Create ruler figure
            if (rulerFigure == null) {
                rulerFigure = new RulerFigure();
                logRulerDebug(methodName, "Created new ruler figure for screen center");
            }
            
            // Save points to settings
            settings.setString(S_RULER_START, startPrice + "|" + startTime);
            settings.setString(S_RULER_END, endPrice + "|" + endTime);
            logRulerDebug(methodName, "Saved screen-centered ruler points to settings");
            
            rulerState = RulerState.ACTIVE;
            logRulerInfo(methodName, "Screen-centered ruler created successfully and set to ACTIVE state");
            
            // Calculate and log initial measurements
            double priceDiff = endPrice - startPrice;
            double pips = Math.abs(priceDiff) / series.getInstrument().getTickSize();
            long timeDiff = Math.abs(endTime - startTime);
            logRulerInfo(methodName, "Initial ruler measurement: %.1f pips, price diff: %.5f, time span: %d ms", pips, priceDiff, timeDiff);
            
        } catch (Exception e) {
            logRulerError(methodName, "Error creating screen-centered ruler: " + e.getMessage());
            // Fallback to immediate creation
            createDefaultRulerImmediate(settings);
        }
    }
    
    /**
     * ایجاد خط‌کش پیش‌فرض فوری (بدون نیاز به DrawContext) - به شکل معقول و قابل استفاده
     */
    private void createDefaultRulerImmediate(Settings settings) {
        final String methodName = "createDefaultRulerImmediate";
        
        // تلاش برای دریافت قیمت فعلی برای قرارگیری بهتر خط‌کش
        double currentPrice = 3310.0; // Default fallback
        long currentTime = System.currentTimeMillis();
        
        // تلاش برای دریافت قیمت واقعی اگر موجود باشد
        try {
            // احتمال دسترسی به آخرین قیمت از تنظیمات
            double customPrice = settings.getDouble(S_CUSTOM_PRICE, Double.NaN);
            if (!Double.isNaN(customPrice) && customPrice > 0) {
                currentPrice = customPrice;
                logRulerDebug(methodName, "Using custom price as current price: " + currentPrice);
            }
            
            // اگر تاریخ بالا یا پایین در تنظیمات موجود باشد، قیمت را بر اساس آن تنظیم کن
            double historicalHigh = settings.getDouble(Constants.S_HISTORICAL_HIGH, Double.NaN);
            double historicalLow = settings.getDouble(Constants.S_HISTORICAL_LOW, Double.NaN);
            
            if (!Double.isNaN(historicalHigh) && !Double.isNaN(historicalLow) && historicalHigh > historicalLow) {
                // استفاده از نقطه میانی بین بالاترین و پایین‌ترین قیمت تاریخی
                currentPrice = (historicalHigh + historicalLow) / 2.0;
                logRulerDebug(methodName, "Using midpoint of historical range as current price: " + currentPrice);
            }
        } catch (Exception e) {
            logRulerDebug(methodName, "Could not get current price, using default: " + currentPrice);
        }
        
        // ایجاد نقاط قابل استفاده خط‌کش با فاصله مناسب
        // از فاصله متعادل بالا و پایین قیمت فعلی استفاده می‌کنیم
        double priceRange = currentPrice * 0.015; // 1.5% از قیمت فعلی (کمی بیشتر از قبل)
        double startPrice = currentPrice - (priceRange * 0.5); // نیمی پایین‌تر
        double endPrice = currentPrice + (priceRange * 0.5);   // نیمی بالاتر
        
        // بازه زمانی حدود 2 ساعت (برای نمایش بهتر)
        long timeSpan = 2 * 60 * 60 * 1000; // 2 ساعت
        long centerTime = currentTime - (30 * 60 * 1000); // 30 دقیقه قبل (برای نمایش معقول‌تر)
        long startTime = centerTime - (timeSpan / 2); // 1 ساعت قبل
        long endTime = centerTime + (timeSpan / 2);   // 1 ساعت بعد
        
        logRulerInfo(methodName, "Creating centered default ruler: start=%.5f@%d, end=%.5f@%d", startPrice, startTime, endPrice, endTime);
        logRulerInfo(methodName, "Ruler spans %.1f hours with price range of %.2f (%.3f%% of current price)", 
            timeSpan / (60.0 * 60.0 * 1000.0), priceRange, (priceRange / currentPrice) * 100);
        
        // Create ruler resize points
        if (rulerStartResize == null) {
            rulerStartResize = new ResizePoint(ResizeType.ALL, true);
            rulerStartResize.setSnapToLocation(true);
            logRulerDebug(methodName, "Created new ruler start resize point");
        }
        rulerStartResize.setLocation(startTime, startPrice);
        
        if (rulerEndResize == null) {
            rulerEndResize = new ResizePoint(ResizeType.ALL, true);
            rulerEndResize.setSnapToLocation(true);
            logRulerDebug(methodName, "Created new ruler end resize point");
        }
        rulerEndResize.setLocation(endTime, endPrice);
        
        // Create ruler figure
        if (rulerFigure == null) {
            rulerFigure = new RulerFigure();
            logRulerDebug(methodName, "Created new ruler figure");
        }
        
        // Save points to settings
        settings.setString(S_RULER_START, startPrice + "|" + startTime);
        settings.setString(S_RULER_END, endPrice + "|" + endTime);
        logRulerDebug(methodName, "Saved ruler points to settings");
        
        // CRITICAL: Set ruler state to ACTIVE after creating all components
        rulerState = RulerState.ACTIVE;
        logRulerInfo(methodName, "Centered default ruler created successfully and set to ACTIVE state");
        
        // Calculate and log initial measurements
        double priceDiff = endPrice - startPrice;
        double pips = Math.abs(priceDiff) * 10; // Assuming 1 pip = 0.1
        logRulerInfo(methodName, "Initial ruler measurement: %.1f pips, price diff: %.5f, time span: %.1f hours", 
            pips, priceDiff, timeSpan / (60.0 * 60.0 * 1000.0));
        
        // IMPORTANT: Double-check that the state was actually set
        if (rulerState != RulerState.ACTIVE) {
            logRulerError(methodName, "CRITICAL ERROR: rulerState failed to set to ACTIVE!");
            rulerState = RulerState.ACTIVE; // Force it
        }
    }
    
    /**
     * پاک کردن تمام کامپوننت‌های خط‌کش
     */
    private void clearRulerComponents() {
        final String methodName = "clearRulerComponents";
        
        if (rulerStartResize != null) {
            rulerStartResize = null;
            logRulerDebug(methodName, "Cleared ruler start point");
        }
        if (rulerEndResize != null) {
            rulerEndResize = null;
            logRulerDebug(methodName, "Cleared ruler end point");
        }
        if (rulerFigure != null) {
            rulerFigure = null;
            logRulerDebug(methodName, "Cleared ruler figure");
        }
        
        // Clear saved settings
        Settings settings = getSettings();
        settings.setString(S_RULER_START, null);
        settings.setString(S_RULER_END, null);
        logRulerDebug(methodName, "Cleared ruler settings");
    }
    
    /**
     * برنامه‌ریزی مجدد رسم خط‌کش
     */
    private void scheduleRulerRedraw() {
        final String methodName = "scheduleRulerRedraw";
        
        // Use a timer to trigger redraw after a short delay
        java.util.Timer timer = new java.util.Timer();
        timer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                try {
                    // Force a calculation update which will trigger drawing
                    logRulerDebug(methodName, "Triggering scheduled redraw");
                    
                    // This will be called by MotiveWave automatically
                    // We just need to ensure our ruler is ready when it does
                    if (rulerState == RulerState.ACTIVE && rulerFigure != null) {
                        logRulerInfo(methodName, "Ruler is ready for rendering");
                    }
                } catch (Exception e) {
                    logRulerError(methodName, "Error in scheduled redraw: " + e.getMessage());
                }
            }
        }, 100); // 100ms delay
    }
    
    /**
     * Handle ruler button click to toggle ruler state and wait for user selection
     */
    private void handleRulerButtonClick(Settings settings, DrawContext ctx) {
        final String methodName = "handleRulerButtonClick";
        boolean showRuler = settings.getBoolean(S_SHOW_RULER, false);
        
        logRulerInfo(methodName, "Method called - current showRuler=" + showRuler + ", rulerState=" + rulerState);
        logRulerDebug(methodName, "DrawContext available: " + (ctx != null));
        logRulerDebug(methodName, "InfoPanel exists: " + (infoPanel != null));
        logRulerDebug(methodName, "RulerStartResize exists: " + (rulerStartResize != null));
        logRulerDebug(methodName, "RulerEndResize exists: " + (rulerEndResize != null));
        logRulerDebug(methodName, "RulerFigure exists: " + (rulerFigure != null));
        
        if (!showRuler || rulerState == RulerState.INACTIVE) {
            // Enable ruler mode - CREATE IMMEDIATE RULER WITH DEFAULT POINTS
            RulerState oldState = rulerState;
            settings.setBoolean(S_SHOW_RULER, true);
            
            logRulerStateTransition(methodName, oldState, RulerState.ACTIVE, "Enabling ruler mode with immediate default ruler");
            
            // Create default ruler immediately with DrawContext available
            if (ctx != null) {
                createDefaultRulerWithContext(settings, ctx);
            } else {
                createDefaultRulerImmediate(settings);
            }
            
            if (infoPanel != null) {
                infoPanel.setRulerActive(true);
                logRulerDebug(methodName, "Set InfoPanel ruler active to true");
            } else {
                logRulerError(methodName, "InfoPanel is null, cannot update ruler active state");
            }
            
            logRulerInfo(methodName, "Ruler enabled with immediate default ruler");
        } else {
            // Disable ruler mode - regardless of current state (ACTIVE, WAITING_FOR_START, WAITING_FOR_END)
            RulerState oldState = rulerState;
            rulerState = RulerState.INACTIVE;
            settings.setBoolean(S_SHOW_RULER, false);
            
            logRulerStateTransition(methodName, oldState, rulerState, "Disabling ruler mode");
            
            if (infoPanel != null) {
                infoPanel.setRulerActive(false);
                logRulerDebug(methodName, "Set InfoPanel ruler active to false");
            } else {
                logRulerError(methodName, "InfoPanel is null, cannot update ruler active state");
            }
            
            // Clear ruler components
            clearRulerComponents();
            
            logRulerInfo(methodName, "Ruler disabled and cleared");
        }
        
        // Redraw all figures to update ruler visibility
        if (ctx != null) {
            DataContext dc = ctx.getDataContext();
            int lastIdx = dc.getDataSeries().size() - 1;
            logRulerDebug(methodName, "Calling drawFigures to redraw with lastIdx=" + lastIdx);
            drawFigures(lastIdx, dc);
            logRulerDebug(methodName, "drawFigures completed");
        } else {
            logRulerError(methodName, "DrawContext is null, cannot redraw figures");
        }
        
        logRulerInfo(methodName, "Method completed - final rulerState=" + rulerState);
    }
    
    // Note: Cursor management is not available in MotiveWave SDK
    // The visual feedback for ruler state is provided through the button appearance only
    
    // ----------------------- KEYBOARD SHORTCUTS -----------------------
    public void onKey(java.awt.event.KeyEvent e) {
        AdvancedLogger.info("BiotakTrigger", "onKey", "Key pressed code=%d", e.getKeyCode());
        // Keyboard shortcuts have been removed as requested
    }
}
