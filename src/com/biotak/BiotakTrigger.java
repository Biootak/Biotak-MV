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
    
    private void logRulerInfo(String method, String message) {
        AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, method, message);
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
        grp.addRow(new BooleanDescriptor(Constants.S_LOCK_SSLS_LEVELS, "Lock SS/LS Levels", false));
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
                            Constants.S_LOCK_SSLS_LEVELS,
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
        AdvancedLogger.debug("BiotakTrigger", "onClick", "Processing click at %s with flags %d", loc, flags);
        Settings settings = getSettings();
        DrawContext ctx = getDrawContext();
        if (ctx == null) {
            // In SDK 7, onMouseDown might not be called before onClick
            // So we can't rely on lastDrawContext being set
            AdvancedLogger.info("BiotakTrigger", "onClick", "onClick called but no DrawContext available");
            
            // Even without DrawContext, we can still handle panel button clicks
            // by checking if the click is within the button areas
            if (infoPanel != null) {
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
            }
            return true; // no context available for other actions
        }
        
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
                return false; // prevent default behavior
            }
            // Check if click is on ruler button
            else if (infoPanel.isInRulerButton(loc.x, loc.y)) {
                handleRulerButtonClick(settings, ctx);
                return false; // prevent default behavior
            }
            // Detect double-click inside panel to reset Custom Price quickly
            long nowClick = System.currentTimeMillis();
            if (nowClick - lastClickTime < 350) {
                DataSeries series = ctx.getDataContext().getDataSeries();
                if (series.size() > 0) {
                    double lc = series.getClose(series.size() - 1);
                    settings.setDouble(S_CUSTOM_PRICE, lc);
                    lastCustomMoveTime = nowClick;
                    drawFigures(series.size() - 1, ctx.getDataContext());
                }
            }
            lastClickTime = nowClick;
            return false; // prevent default behavior when clicking panel
        }
        
        // Handle ruler state clicks for point selection
        if (rulerState == RulerState.WAITING_FOR_START) {
            AdvancedLogger.debug("BiotakTrigger", "onClick", "Ruler state is WAITING_FOR_START");
            // Set start point and wait for end point
            AdvancedLogger.debug("BiotakTrigger", "onClick", "Start point selection in progress");
            DataSeries series = ctx.getDataContext().getDataSeries();
                    long time = ctx.translate2Time(loc.getX());
                    double value = ctx.translate2Value(loc.getY());
                    Coordinate coord = new Coordinate(time, value);
            
            tempStartPoint = new Point(loc.x, loc.y);
            
            // Initialize ruler points if needed
            if (rulerStartResize == null) {
                rulerStartResize = new ResizePoint(ResizeType.ALL, true);
                rulerStartResize.setSnapToLocation(true);
            }
            
            rulerStartResize.setLocation(coord.getTime(), coord.getValue());
            settings.setString(S_RULER_START, coord.getValue() + "|" + coord.getTime());
            
            rulerState = RulerState.WAITING_FOR_END;
            AdvancedLogger.info("BiotakTrigger", "onClick", "Start point selected, waiting for end point");
            return false; // prevent default behavior
        }
        else if (rulerState == RulerState.WAITING_FOR_END) {
            // Set end point and complete ruler
            AdvancedLogger.debug("BiotakTrigger", "onClick", "End point selection in progress");
            DataSeries series = ctx.getDataContext().getDataSeries();
                    long time = ctx.translate2Time(loc.getX());
                    double value = ctx.translate2Value(loc.getY());
                    Coordinate coord = new Coordinate(time, value);
            
            tempEndPoint = new Point(loc.x, loc.y);
            
            // Initialize ruler points if needed
            if (rulerEndResize == null) {
                rulerEndResize = new ResizePoint(ResizeType.ALL, true);
                rulerEndResize.setSnapToLocation(true);
            }
            
            rulerEndResize.setLocation(coord.getTime(), coord.getValue());
            settings.setString(S_RULER_END, coord.getValue() + "|" + coord.getTime());
            
            rulerState = RulerState.ACTIVE;
            
            // Create and layout ruler figure
            if (rulerFigure == null) {
                rulerFigure = new RulerFigure();
            }
            
            AdvancedLogger.info("BiotakTrigger", "onClick", "End point selected, ruler completed");
            
            // Redraw to show completed ruler
            DataContext dc = ctx.getDataContext();
            int lastIdx = dc.getDataSeries().size() - 1;
            drawFigures(lastIdx, dc);
            
            return false; // prevent default behavior
        }
        
        return true; // allow default behavior for clicks outside panel
    }
    
    // Keep old method for backward compatibility but make it call onClick
    public void onMouseDown(Point loc, DrawContext ctx) {
        // Store the DrawContext for use in onClick method
        this.lastDrawContext = ctx;
        onClick(loc, 0); // Call the standard SDK method with no modifier flags
    }
    
    /**
     * Handle mouse movement for dynamic ruler tracking
     */
    public void onMouseMove(Point loc, DrawContext ctx) {
        // Store DrawContext for SDK 7 compatibility
        this.lastDrawContext = ctx;
        
        // Only track mouse movement when waiting for end point
        if (rulerState == RulerState.WAITING_FOR_END && rulerStartResize != null) {
            try {
                // Convert mouse location to chart coordinates
                long time = ctx.translate2Time(loc.getX());
                double value = ctx.translate2Value(loc.getY());
                Coordinate coord = new Coordinate(time, value);
                
                // Initialize ruler end point if needed
                if (rulerEndResize == null) {
                    rulerEndResize = new ResizePoint(ResizeType.ALL, true);
                    rulerEndResize.setSnapToLocation(true);
                }
                
                // Update end point position dynamically
                rulerEndResize.setLocation(coord.getTime(), coord.getValue());
                
                // Initialize ruler figure if needed
                if (rulerFigure == null) {
                    rulerFigure = new RulerFigure();
                }
                
                // Force immediate redraw to show the dynamic ruler
                DataContext dc = ctx.getDataContext();
                int lastIdx = dc.getDataSeries().size() - 1;
                drawFigures(lastIdx, dc);
                
            } catch (Exception e) {
                // Silently ignore any coordinate conversion errors
                AdvancedLogger.debug("BiotakTrigger", "onMouseMove", "Mouse move coordinate conversion failed: %s", e.getMessage());
            }
        }
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

            // برای محاسبه TH از 200 کندل آخر استفاده می‌کنیم
            int totalBars    = series.size();
            int lookback     = Math.min(200, totalBars);
            double thBasePrice = series.getClose(totalBars - 2); // Use previous bar's close for TH calculation.
            
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

                // Draw/update custom price horizontal line (now draggable)
                PathInfo customPricePath = getSettings().getPath(S_CUSTOM_PRICE_PATH);
                customPriceLine = new CustomPriceLine(startTime, endTime, savedPrice, customPricePath);
                addFigure(customPriceLine);
                // Logger.debug("CustomPriceLine created and added at price: " + savedPrice);
                
                // Add the invisible ResizePoint for line dragging
                ResizePoint lineResizePoint = customPriceLine.getLineResizePoint();
                if (lineResizePoint != null) {
                    addFigure(lineResizePoint);
                    // Logger.debug("LineResizePoint added for line dragging");
                } else {
                    // Logger.debug("ERROR: LineResizePoint is null!");
                }

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
                                    if (LS_MULTIPLIER * candidate <= allowedRange) { baseTHCalc = candidate; break; }
                                }
                            }
                            case STRUCTURE -> baseTHCalc = structureValue;
                            default -> baseTHCalc = structureValue;
                        }
                        baseTHForSession = baseTHCalc;
                        if (lockLevels) lockedBaseTH = baseTHForSession;
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

                    List<Figure> controlFigures = LevelDrawer.drawControlLevels(getSettings(), series, midpointPrice, finalHigh, finalLow, patternValue, structureValue, shortStep, controlValue, longStep, startTime, endTime);
                    for (Figure f : controlFigures) addFigure(f);
                }
                case M_STEP -> {
                    double controlValue = (shortStep + longStep) / 2.0;
                    double mDistance = controlValue * ATR_FACTOR;
                    com.biotak.enums.MStepBasisType basis = com.biotak.util.EnumUtil.safeEnum(com.biotak.enums.MStepBasisType.class,
                            getSettings().getString(Constants.S_MSTEP_BASIS, com.biotak.enums.MStepBasisType.C_BASED.name()),
                            com.biotak.enums.MStepBasisType.C_BASED);
                    if (basis == null) basis = com.biotak.enums.MStepBasisType.C_BASED;
                    java.util.List<Figure> mFigures;
                    if (basis == com.biotak.enums.MStepBasisType.C_BASED) {
                        mFigures = LevelDrawer.drawMLevels(getSettings(), series, midpointPrice, finalHigh, finalLow, controlValue, startTime, endTime);
                    } else {
                        mFigures = LevelDrawer.drawMEqualLevels(getSettings(), midpointPrice, finalHigh, finalLow, mDistance, startTime, endTime);
                    }
                    for (Figure f : mFigures) addFigure(f);
                }
            }

            // ------------------- LEG RULER -------------------
            if (settings.getBoolean(S_SHOW_RULER, false)) {
                // Draw ruler if fully configured (ACTIVE) or when dynamically tracking (WAITING_FOR_END)
                if ((rulerState == RulerState.ACTIVE || rulerState == RulerState.WAITING_FOR_END) && 
                    rulerStartResize != null && rulerEndResize != null) {
                    // Initialize ruler figure if needed
                    if (rulerFigure == null) rulerFigure = new RulerFigure();
                    
                    // Add the ruler to the chart
                    addFigure(rulerFigure);
                    addFigure(rulerStartResize);
                    addFigure(rulerEndResize);
                }
                // If ruler is WAITING_FOR_START, don't draw anything yet - wait for start point click
            }
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
            
            // Update the custom price line position
            if (customPriceLine != null) {
                customPriceLine.updatePrice(newPrice);
                // Logger.debug("onResize: CustomPriceLine.updatePrice() called");
            }
            
            // Update ResizePoint location
            rp.setLocation(rp.getTime(), newPrice);
            // Logger.debug("onResize: ResizePoint location updated to: (" + rp.getTime() + ", " + newPrice + ")");
            
            // Sync the visible ResizePoint position if it exists
            if (customPricePoint != null) {
                customPricePoint.setLocation(customPricePoint.getTime(), newPrice);
                // Logger.debug("onResize: CustomPricePoint synchronized to: (" + customPricePoint.getTime() + ", " + newPrice + ")");
            }
            
            // Force layout update for smoother dragging
            if (lastDrawContext != null) {
                customPriceLine.layout(lastDrawContext);
                // Logger.debug("onResize: CustomPriceLine.layout() called");
            }
            
            // Note: Redraw will happen automatically through layout updates
            // Logger.debug("onResize: Layout and synchronization completed");
            // Logger.debug("=== LINE DRAG EVENT END ===");
            
            // Light update during drag - full recalculation happens in onEndResize
        }
    }



    @Override
    public int getMinBars() {
        return getSettings().getInteger(S_HISTORICAL_BARS, 100000);
    }

    // Enable live updates so drawing persists on chart updates
    @Override
    public void onBarUpdate(DataContext ctx) {
        // Logger.debug("BiotakTrigger: onBarUpdate called");
        // Call calculate on the latest bar index for live rendering
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
            // Log live bid price and calculated TH steps for verification
            // Logger.debug(String.format("LiveBid=%.5f | PatternTH=%.1f | TriggerTH=%.1f | StructureTH=%.1f | HigherPatternTH=%.1f", basePrice, patternTH, triggerTH, structureTH, higherPatternTH));
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
                                 double perc   = TimeframeUtil.getTimeframePercentage(mid);
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

                 // Arrange lines in requested grouped order with separators
                 String sep = "-------------";
                 String[] lines = {
                     pipsStr,
                     sep,
                     matchStr1,
                     matchStr2,
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
                    
                    // Use ruler font setting if available, otherwise use default font
                    FontInfo rulerFontInfo = getSettings().getFont(S_RULER_FONT);
                    Font rulerFont = rulerFontInfo != null ? rulerFontInfo.getFont() : getSettings().getFont(S_FONT).getFont();
                    gc.setFont(rulerFont);
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
     */
    private void handleRulerButtonClickWithoutContext(Settings settings) {
        final String methodName = "handleRulerButtonClickWithoutContext";
        boolean showRuler = settings.getBoolean(S_SHOW_RULER, false);
        
        logRulerInfo(methodName, "Method called - current showRuler=" + showRuler + ", rulerState=" + rulerState);
        logRulerDebug(methodName, "InfoPanel exists: " + (infoPanel != null));
        logRulerDebug(methodName, "RulerStartResize exists: " + (rulerStartResize != null));
        logRulerDebug(methodName, "RulerEndResize exists: " + (rulerEndResize != null));
        logRulerDebug(methodName, "RulerFigure exists: " + (rulerFigure != null));
        
        if (!showRuler) {
            // Enable ruler mode - set to WAITING_FOR_START (not ACTIVE)
            RulerState oldState = rulerState;
            rulerState = RulerState.WAITING_FOR_START;
            settings.setBoolean(S_SHOW_RULER, true);
            
            logRulerStateTransition(methodName, oldState, rulerState, "Enabling ruler mode (no context)");
            
            // Clear any previously saved ruler points to force new selection
            settings.setString(S_RULER_START, null);
            settings.setString(S_RULER_END, null);
            logRulerDebug(methodName, "Cleared saved ruler points from settings");
            
            // Clear in-memory ruler points
            if (rulerStartResize != null) {
                rulerStartResize = null;
                logRulerDebug(methodName, "Cleared ruler start point from memory");
            }
            if (rulerEndResize != null) {
                rulerEndResize = null;
                logRulerDebug(methodName, "Cleared ruler end point from memory");
            }
            if (rulerFigure != null) {
                rulerFigure = null;
                logRulerDebug(methodName, "Cleared ruler figure from memory");
            }
            
            if (infoPanel != null) {
                infoPanel.setRulerActive(true);
                logRulerDebug(methodName, "Set InfoPanel ruler active to true");
            } else {
                logRulerError(methodName, "InfoPanel is null, cannot update ruler active state");
            }
            
            logRulerInfo(methodName, "Ruler enabled, waiting for start point selection");
        } else {
            // Disable ruler mode - regardless of current state
            RulerState oldState = rulerState;
            rulerState = RulerState.INACTIVE;
            settings.setBoolean(S_SHOW_RULER, false);
            
            logRulerStateTransition(methodName, oldState, rulerState, "Disabling ruler mode (no context)");
            
            if (infoPanel != null) {
                infoPanel.setRulerActive(false);
                logRulerDebug(methodName, "Set InfoPanel ruler active to false");
            } else {
                logRulerError(methodName, "InfoPanel is null, cannot update ruler active state");
            }
            
            // Clear ruler points when disabling
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
            
            logRulerInfo(methodName, "Ruler disabled");
        }
        
        logRulerInfo(methodName, "Method completed - final rulerState=" + rulerState);
    }
    
    /**
     * Create a default ruler when DrawContext is not available
     */
    private void createDefaultRuler(Settings settings) {
        // Try to get saved ruler points first
        String startStr = settings.getString(S_RULER_START, null);
        String endStr = settings.getString(S_RULER_END, null);
        
        double startPrice, endPrice;
        long startTime, endTime;
        
        if (startStr != null && endStr != null) {
            // Use saved ruler points
            try {
                String[] startParts = startStr.split("\\|");
                String[] endParts = endStr.split("\\|");
                
                startPrice = Double.parseDouble(startParts[0]);
                startTime = Long.parseLong(startParts[1]);
                endPrice = Double.parseDouble(endParts[0]);
                endTime = Long.parseLong(endParts[1]);
                
                AdvancedLogger.info("BiotakTrigger", "createDefaultRuler", "Using saved ruler points: start=%.5f@%d, end=%.5f@%d", startPrice, startTime, endPrice, endTime);
            } catch (Exception e) {
                AdvancedLogger.warn("BiotakTrigger", "createDefaultRuler", "Failed to parse saved ruler points, using defaults");
                // Fall back to default values
                startPrice = 3300.0;
                endPrice = 3350.0;
                startTime = System.currentTimeMillis() - 3600000; // 1 hour ago
                endTime = System.currentTimeMillis();
            }
        } else {
            // Create default ruler points
            AdvancedLogger.info("BiotakTrigger", "createDefaultRuler", "No saved ruler points, creating default ruler");
            startPrice = 3300.0;
            endPrice = 3350.0;
            startTime = System.currentTimeMillis() - 3600000; // 1 hour ago
            endTime = System.currentTimeMillis();
            
            // Save default points
            settings.setString(S_RULER_START, startPrice + "|" + startTime);
            settings.setString(S_RULER_END, endPrice + "|" + endTime);
        }
        
        // Create ruler resize points
        if (rulerStartResize == null) {
            rulerStartResize = new ResizePoint(ResizeType.ALL, true);
            rulerStartResize.setSnapToLocation(true);
        }
        rulerStartResize.setLocation(startTime, startPrice);
        
        if (rulerEndResize == null) {
            rulerEndResize = new ResizePoint(ResizeType.ALL, true);
            rulerEndResize.setSnapToLocation(true);
        }
        rulerEndResize.setLocation(endTime, endPrice);
        
        // Create ruler figure
        if (rulerFigure == null) {
            rulerFigure = new RulerFigure();
        }
        
        rulerState = RulerState.ACTIVE;
        AdvancedLogger.info("BiotakTrigger", "createDefaultRuler", "Default ruler created successfully, state set to ACTIVE");
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
            // Enable ruler mode - set state to waiting for start point
            RulerState oldState = rulerState;
            rulerState = RulerState.WAITING_FOR_START;
            settings.setBoolean(S_SHOW_RULER, true);
            
            logRulerStateTransition(methodName, oldState, rulerState, "Enabling ruler mode");
            
            // Clear any previously saved ruler points to force new selection
            settings.setString(S_RULER_START, null);
            settings.setString(S_RULER_END, null);
            logRulerDebug(methodName, "Cleared saved ruler points from settings");
            
            // Clear in-memory ruler points
            if (rulerStartResize != null) {
                rulerStartResize = null;
                logRulerDebug(methodName, "Cleared ruler start point from memory");
            }
            if (rulerEndResize != null) {
                rulerEndResize = null;
                logRulerDebug(methodName, "Cleared ruler end point from memory");
            }
            if (rulerFigure != null) {
                rulerFigure = null;
                logRulerDebug(methodName, "Cleared ruler figure from memory");
            }
            
            if (infoPanel != null) {
                infoPanel.setRulerActive(true);
                logRulerDebug(methodName, "Set InfoPanel ruler active to true");
            } else {
                logRulerError(methodName, "InfoPanel is null, cannot update ruler active state");
            }
            
            logRulerInfo(methodName, "Ruler enabled, waiting for start point selection");
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
            
            // Clear ruler points when disabling
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
            
            logRulerInfo(methodName, "Ruler disabled");
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
