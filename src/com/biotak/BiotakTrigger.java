package com.biotak;

import com.biotak.enums.THStartPointType;
import com.biotak.enums.PanelPosition;
import com.biotak.enums.RulerState;
import com.biotak.util.TimeframeUtil;
import com.biotak.debug.AdvancedLogger;
import com.biotak.util.Constants;
import com.biotak.config.LoggingConfiguration;
import com.biotak.ui.ThemeManager;
import com.biotak.config.SettingsService;
import com.biotak.config.BiotakConfig;
import static com.biotak.config.SettingsRepository.*;
import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.menu.MenuDescriptor;
import com.motivewave.platform.sdk.common.menu.MenuItem;
import com.motivewave.platform.sdk.common.menu.MenuSeparator;
import com.motivewave.platform.sdk.draw.Figure;
import com.motivewave.platform.sdk.draw.ResizePoint;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;

import java.awt.Font;
import java.awt.Rectangle;

import java.awt.Point;
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
 * بایوتک تریگر TH3 - نسخه حرفه‌ای برای MotiveWave
 * Biotak Trigger TH3 - Professional Edition for MotiveWave
 * 
 * این اندیکاتور تخصصی سطوح کلیدی قیمت افقی را بر اساس نوسانات تاریخی
 * و عمل قیمت محاسبه و نمایش می‌دهد
 * 
 * This indicator calculates and displays key horizontal price levels 
 * based on historical volatility and price action
 * 
 * ویژگی‌های کلیدی / Key Features:
 * - محاسبه سطوح فراکتال چندگانه / Multi-fractal level calculations
 * - سیستم لاگ‌گذاری پیشرفته / Advanced logging system  
 * - رابط کاربری تطبیقی / Adaptive user interface
 * - خط‌کش تعاملی / Interactive ruler
 * - پیکربندی انعطاف‌پذیر / Flexible configuration
 * 
 * @author Biotak Development Team
 * @version 3.0.0
 * @since 2024
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

    private long lastClickTime = 0;              // for double-click detection        // for fade-in highlight
   
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
    private static volatile boolean logLevelInitialized = false; // prevents repeated log level initialization

    // Stores locked values for all level types when lock all option is enabled
    private double lockedCustomPrice = Double.NaN;
    
    // Human-readable labels for each TH value (Current, Pattern, Trigger, Structure, Higher)
    private String[] tfLabels = {"", "", "", "", ""};

    // Holds comprehensive M values for ruler matching built during drawFigures() - optimized with size limit
    private final java.util.Map<String, Double> fullMValues = new java.util.concurrent.ConcurrentHashMap<>(16, 0.75f, 1);
    // Holds 3×ATR values (price) for ruler comparison - optimized with size limit
    private final java.util.Map<String, Double> fullATRValues = new java.util.concurrent.ConcurrentHashMap<>(16, 0.75f, 1);
    
    // New step value maps for different step types
    private final java.util.Map<String, Double> fullEValues = new java.util.concurrent.ConcurrentHashMap<>(16, 0.75f, 1);
    private final java.util.Map<String, Double> fullTPValues = new java.util.concurrent.ConcurrentHashMap<>(16, 0.75f, 1);
    private final java.util.Map<String, Double> fullTHValues = new java.util.concurrent.ConcurrentHashMap<>(16, 0.75f, 1);
    private final java.util.Map<String, Double> fullSSValues = new java.util.concurrent.ConcurrentHashMap<>(16, 0.75f, 1);
    private final java.util.Map<String, Double> fullLSValues = new java.util.concurrent.ConcurrentHashMap<>(16, 0.75f, 1);
    
    // Maximum size limits to prevent OutOfMemoryError
    private static final int MAX_MAP_SIZE = 1000;
    private static final int CLEANUP_THRESHOLD = 800; // Start cleanup when reaching this size

    // Base values for ATR scaling (current timeframe)
    private int atrStructureMin = 0;          // minutes of current structure timeframe
    private double atrStructurePrice = Double.NaN; // 1× ATR price (not multiplied by 3)

    private static final long LOG_INTERVAL_MS = 60_000;      // 1 minute
    private static long lastCalcTableLogTime = 0;             // Tracks last time the calc table was printed
    private static long lastHighLowLogTime = 0;             // Tracks last time historical high/low was logged

    // Throttle ruler INFO logs
    private static long lastRulerInfoLog = 0;
    private static final long RULER_LOG_INTERVAL_MS = 60000; // Log ruler status at most once per minute

    // (Leg Ruler fields removed)
    private ResizePoint rulerStartResize, rulerEndResize;
    private RulerFigure rulerFigure; // Custom inner class
    
    private RulerState rulerState = RulerState.INACTIVE;

    // Add fields at class level
    // (Leg Ruler fields removed)

    // Keep last DataContext for quick redraws triggered by key events
    private DrawContext lastDrawContext;
    

    /**
     * سازنده کلاس - مقداردهی اولیه سیستم لاگ‌گذاری و اجزای اصلی
     * Constructor - Initialize logging system and core components
     * 
     * این متد توسط MotiveWave هنگام بارگذاری اندیکاتور فراخوانی می‌شود
     * This method is called by MotiveWave when loading the indicator
     */
    public BiotakTrigger() {
        super();
        // مقداردهی اولیه پیکربندی لاگ‌گذاری / Initialize logging configuration
        LoggingConfiguration.initialize();
        AdvancedLogger.info("BiotakTrigger", "constructor", "Constructor called. The study is being instantiated by MotiveWave.");
    }

    @Override
    public void initialize(Defaults defaults) {
        AdvancedLogger.debug("BiotakTrigger", "initialize", "initialize() called. Settings are being configured.");
        var sd = createSD();
        SettingsService.initializeSettings(sd, defaults);
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
        Settings settings = getSettings();
        DrawContext ctx = this.lastDrawContext;
        
        // Handle panel button clicks
        if (infoPanel != null && infoPanel.contains(loc.x, loc.y, ctx)) {
            // Check if click is on minimize button
            if (infoPanel.isInMinimizeButton(loc.x, loc.y)) {
                boolean newState = !settings.getBoolean(S_PANEL_MINIMIZED, false);
                settings.setBoolean(S_PANEL_MINIMIZED, newState);
                infoPanel.setMinimized(newState);
                return false;
            }
            // Check if click is on ruler button
            else if (infoPanel.isInRulerButton(loc.x, loc.y)) {
                toggleRuler(settings, ctx);
                return false;
            }
            // Any other click inside panel - handle double-click for custom price reset
            else {
                long nowClick = System.currentTimeMillis();
                if (nowClick - lastClickTime < 350 && ctx != null) {
                    DataSeries series = ctx.getDataContext().getDataSeries();
                    if (series.size() > 0) {
                        double lc = series.getClose(series.size() - 1);
                        settings.setDouble(S_CUSTOM_PRICE, lc);
                        drawFigures(series.size() - 1, ctx.getDataContext());
                    }
                }
                lastClickTime = nowClick;
                return false;
            }
        }
        
        // Check if click is on ruler figure or custom price line - suppress to prevent settings dialog
        if (ctx != null) {
            // Check if click is on ruler figure
            if (rulerFigure != null && rulerFigure.contains(loc.x, loc.y, ctx)) {
                return false; // Suppress click to prevent settings dialog
            }
            
            // Check if click is on custom price line
            if (customPriceLine != null && customPriceLine.contains(loc.x, loc.y, ctx)) {
                return false; // Suppress click to prevent settings dialog
            }
        }
        
        return true; // allow default behavior for clicks outside panel
    }
    
    /**
     * Override onDoubleClick to suppress settings dialog for ruler and custom price line
     */
    @Override
    public boolean onDoubleClick(Point loc, int flags) {
        DrawContext ctx = this.lastDrawContext;
        
        if (ctx != null) {
            // Check if double-click is on ruler figure - suppress settings dialog
            if (rulerFigure != null && rulerFigure.contains(loc.x, loc.y, ctx)) {
                AdvancedLogger.debug("BiotakTrigger", "onDoubleClick", "Double-click on ruler figure suppressed");
                return false; // Suppress double-click to prevent settings dialog
            }
            
            // Check if double-click is on custom price line - suppress settings dialog
            if (customPriceLine != null && customPriceLine.contains(loc.x, loc.y, ctx)) {
                AdvancedLogger.debug("BiotakTrigger", "onDoubleClick", "Double-click on custom price line suppressed");
                return false; // Suppress double-click to prevent settings dialog
            }
        }
        
        // For any other location, allow default behavior (which may open settings dialog)
        return super.onDoubleClick(loc, flags);
    }
    
    /**
     * Toggle ruler on/off with immediate display in center screen
     */
    private void toggleRuler(Settings settings, DrawContext ctx) {
        boolean showRuler = settings.getBoolean(S_SHOW_RULER, false);
        
        if (!showRuler || rulerState == RulerState.INACTIVE) {
            // Enable ruler - create immediately in center screen
            settings.setBoolean(S_SHOW_RULER, true);
            
            if (ctx != null) {
                createDefaultRulerWithContext(settings, ctx);
            } else {
                createDefaultRulerImmediate(settings);
            }
            
            if (infoPanel != null) {
                infoPanel.setRulerActive(true);
            }
        } else {
            // Disable ruler
            rulerState = RulerState.INACTIVE;
            settings.setBoolean(S_SHOW_RULER, false);
            
            if (infoPanel != null) {
                infoPanel.setRulerActive(false);
            }
            
            clearRulerComponents();
        }
        
        // Redraw to update ruler visibility
        if (ctx != null) {
            DataContext dc = ctx.getDataContext();
            int lastIdx = dc.getDataSeries().size() - 1;
            drawFigures(lastIdx, dc);
        }
    }

    /**
     * محاسبات اصلی اندیکاتور - اجرا برای هر کندل
     * Main indicator calculations - executed for each candle
     * 
     * این متد توسط MotiveWave برای هر کندل جدید فراخوانی می‌شود
     * This method is called by MotiveWave for each new candle
     * 
     * @param index ایندکس کندل فعلی / Index of current candle
     * @param ctx زمینه داده شامل سری داده‌ها / Data context containing data series
     */
    @Override
    public void calculate(int index, DataContext ctx) {
        // توجه: در SDK 7، DrawContext معمولاً از طریق onDraw() ارسال می‌شود
        // Note: In SDK 7, DrawContext is typically passed through onDraw() method
        // We cannot directly get DrawContext from DataContext
        
        // هماهنگ‌سازی سطح لاگ‌گذاری در اولین کندل / Sync logger level once per session only
        if (index == 0 && !logLevelInitialized) {
            AdvancedLogger.LogLevel lvl = com.biotak.util.EnumUtil.safeEnum(AdvancedLogger.LogLevel.class,
                    getSettings().getString(S_LOG_LEVEL, AdvancedLogger.LogLevel.INFO.name()), AdvancedLogger.LogLevel.INFO);
            AdvancedLogger.setLogLevel(lvl);
            logLevelInitialized = true; // Prevent further calls
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
            double storedHigh = s.getDouble(S_HISTORICAL_HIGH, Double.NaN);
            double storedLow  = s.getDouble(S_HISTORICAL_LOW, Double.NaN);
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
                getSettings().setDouble(S_HISTORICAL_HIGH, cachedHigh);
            }
        }
        if (barLow < cachedLow) {
            cachedLow = barLow;
            if (hadValidLow) {
                getSettings().setDouble(S_HISTORICAL_LOW, cachedLow);
            }
        }
        
    // Only draw figures when really necessary to reduce excessive calls
        boolean isFirstBar = (index == 0);
        boolean shouldRedraw = false;
        
        if (isFirstBar && !firstBarDrawn) {
            shouldRedraw = true;
            firstBarDrawn = true;
        }
        else if (isLastBar) {
            // Only redraw if significant price movement or time interval passed
            long nowHL = System.currentTimeMillis();
            if (nowHL - lastHighLowLogTime > LOG_INTERVAL_MS) {
                AdvancedLogger.info("BiotakTrigger", "calculate", "Historical High/Low: %.5f/%.5f [%s]", cachedHigh, cachedLow, series.getBarSize());
                lastHighLowLogTime = nowHL;
                shouldRedraw = true;
            }
            // Also redraw if extremes changed significantly
            else if (hadValidHigh && Math.abs(barHigh - cachedHigh) > 0 || 
                     hadValidLow && Math.abs(barLow - cachedLow) > 0) {
                shouldRedraw = true;
            }
        }
        
        if (shouldRedraw) {
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
        
        // Remove temporary log level changes to prevent excessive logging
        // The log level should be managed globally, not per method call
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
            if (currentMode == StepCalculationMode.SS_LS_STEP) {
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

            // Always need custom-price anchor when mode is SS_LS_STEP (baseline)
            boolean needCustomAnchor = (currentMode == StepCalculationMode.SS_LS_STEP ||
                                        spType == THStartPointType.CUSTOM_PRICE);

            if (needCustomAnchor) {
                long anchorTime = endTime; // stick to last bar's time so point on right edge

                // Check if Lock All Levels is enabled to determine custom price behavior
                boolean lockAllLevels = getSettings().getBoolean(S_LOCK_ALL_LEVELS, false);
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
            double thValue = thBundle.th();
            // Note: Other TH values (pattern, trigger, structure, higherPattern) are available from thBundle
            // but are not used in this method - they are calculated separately in drawInfoPanel when needed

            BarSize patternBarSize     = TimeframeUtil.getPatternBarSize(series.getBarSize());
            BarSize triggerBarSize     = TimeframeUtil.getTriggerBarSize(series.getBarSize());
            BarSize structureBarSize   = TimeframeUtil.getStructureBarSize(series.getBarSize());
            BarSize higherPatternBarSize = TimeframeUtil.getPatternBarSize(structureBarSize);
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

            // 3) Human-readable timeframe labels for ruler pop-up
            BarSize currBarSize = series.getBarSize();
            this.tfLabels[0] = FractalCalculator.formatTimeframeString(currBarSize);
            this.tfLabels[1] = FractalCalculator.formatTimeframeString(patternBarSize);
            this.tfLabels[2] = FractalCalculator.formatTimeframeString(triggerBarSize);
            this.tfLabels[3] = FractalCalculator.formatTimeframeString(structureBarSize);
            this.tfLabels[4] = FractalCalculator.formatTimeframeString(higherPatternBarSize);

            // -----------------------------  BUILD COMPREHENSIVE M MAP  -----------------------------
            this.fullMValues.clear();
            java.util.Map<String, Double> newMValues = com.biotak.util.FractalUtil.buildMMap(series, thBasePrice, mScale);
            
            // Check size before adding to prevent OutOfMemoryError
            if (newMValues.size() > MAX_MAP_SIZE) {
                AdvancedLogger.warn("BiotakTrigger", "drawFigures", "M map size exceeds limit (%d), truncating to %d entries", 
                    newMValues.size(), CLEANUP_THRESHOLD);
                // Keep only the most recent entries
                java.util.List<java.util.Map.Entry<String, Double>> entries = new java.util.ArrayList<>(newMValues.entrySet());
                entries = entries.subList(0, CLEANUP_THRESHOLD);
                newMValues = entries.stream().collect(java.util.stream.Collectors.toMap(
                    java.util.Map.Entry::getKey, java.util.Map.Entry::getValue));
            }
            
            this.fullMValues.putAll(newMValues);
 
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

            // Build comprehensive ATR map from all major timeframes once
            // This ensures consistent results regardless of current timeframe
            // Clear cache if timeframe has changed to ensure proper recalculation
            boolean shouldRebuildATR = this.fullATRValues.isEmpty() || this.atrStructureMin != structureMin;
            if (shouldRebuildATR) {
                com.biotak.debug.AdvancedLogger.info("BiotakTrigger", "drawFigures", 
                    "Building comprehensive ATR map from all timeframes for consistent matching");
                    
                this.fullATRValues.clear();
                
                // Build ATR map using same method as M for timeframe independence
                // Use price and instrument only (like M method) to ensure consistent results
                java.util.Map<String, Double> comprehensiveATRValues = com.biotak.util.FractalUtil.buildComprehensiveATRMap(thBasePrice, series.getInstrument());
                
                this.fullATRValues.putAll(comprehensiveATRValues);
                this.atrStructureMin   = structureMin;
                this.atrStructurePrice = atrValue;
                
                // Log comprehensive ATR map creation
                com.biotak.debug.AdvancedLogger.info("BiotakTrigger", "drawFigures", 
                    "Comprehensive ATR Map built with %d entries covering all major timeframes", comprehensiveATRValues.size());
                    
                // Log a sample of the comprehensive coverage
                String sampleEntries = comprehensiveATRValues.entrySet().stream().limit(5)
                    .map(e -> e.getKey() + "=" + String.format("%.2f", com.biotak.util.UnitConverter.priceToPip(e.getValue(), series.getInstrument())) + "p")
                    .collect(java.util.stream.Collectors.joining(", "));
                com.biotak.debug.AdvancedLogger.info("BiotakTrigger", "drawFigures", 
                    "Sample ATR coverage: %s", sampleEntries);
            }
            
            // --------------------- BUILD ALL STEP VALUE MAPS ---------------------
            // Build maps for E, TP, TH, SS, LS step values
            this.fullEValues.clear();
            this.fullEValues.putAll(com.biotak.util.FractalUtil.buildStepValuesMap(series, thBasePrice, "E"));
            
            this.fullTPValues.clear();
            this.fullTPValues.putAll(com.biotak.util.FractalUtil.buildStepValuesMap(series, thBasePrice, "TP"));
            
            this.fullTHValues.clear();
            this.fullTHValues.putAll(com.biotak.util.FractalUtil.buildStepValuesMap(series, thBasePrice, "TH"));
            
            this.fullSSValues.clear();
            this.fullSSValues.putAll(com.biotak.util.FractalUtil.buildStepValuesMap(series, thBasePrice, "SS"));
            
            this.fullLSValues.clear();
            this.fullLSValues.putAll(com.biotak.util.FractalUtil.buildStepValuesMap(series, thBasePrice, "LS"));

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
                    boolean lockAllLevels = getSettings().getBoolean(S_LOCK_ALL_LEVELS, false);
                    double finalThStepInPoints;

                    if (lockAllLevels) {
                        // Try to get the globally-locked TH value
                        double globallyLockedTH = getSettings().getDouble(S_LOCKED_TH_VALUE, Double.NaN);

                        if (!Double.isNaN(globallyLockedTH)) {
                            // A globally-locked value exists; use it for all timeframes
                            finalThStepInPoints = globallyLockedTH;
                            AdvancedLogger.info("BiotakTrigger", "drawFigures", "Using globally-locked TH value: %.5f", finalThStepInPoints);
                        } else {
                            // No globally-locked value exists; this is the first time locking
                            // Calculate TH step for the current timeframe
                            double timeframePercentage = TimeframeUtil.getTimeframePercentage(series.getBarSize());
                            finalThStepInPoints = com.biotak.util.OptimizedCalculations.calculateTHPoints(series.getInstrument(), thBasePrice, timeframePercentage);

                            // Store this value as the new global lock
                            getSettings().setDouble(S_LOCKED_TH_VALUE, finalThStepInPoints);
                            getSettings().setString(S_LOCKED_TH_ORIGIN_TIMEFRAME, series.getBarSize().toString());

                            AdvancedLogger.info("BiotakTrigger", "drawFigures", "Locking global TH value to %.5f from timeframe %s", 
                                finalThStepInPoints, series.getBarSize().toString());
                        }
                    } else {
                        // Lock is disabled; calculate TH step normally for the current timeframe
                        double timeframePercentage = TimeframeUtil.getTimeframePercentage(series.getBarSize());
                        finalThStepInPoints = com.biotak.util.OptimizedCalculations.calculateTHPoints(series.getInstrument(), thBasePrice, timeframePercentage);
                        
                        // Also, ensure any old global lock values are cleared
                        getSettings().setDouble(S_LOCKED_TH_VALUE, Double.NaN);
                        getSettings().setString(S_LOCKED_TH_ORIGIN_TIMEFRAME, null);
                    }

                    if (getSettings().getBoolean(S_SHOW_TH_LEVELS, true)) {
                        List<Figure> thFigures = LevelDrawer.drawTHLevels(getSettings(), series, midpointPrice, finalHigh, finalLow, finalThStepInPoints, startTime, endTime);
                        for (Figure f : thFigures) addFigure(f);
                    }
                }
                case SS_LS_STEP -> {
                    boolean lockAllLevels = getSettings().getBoolean(S_LOCK_ALL_LEVELS, false);
                    double baseTHForSession;

                    if (lockAllLevels) {
                        double globallyLockedBaseTH = getSettings().getDouble(S_LOCKED_BASE_TH_VALUE, Double.NaN);
                        if (!Double.isNaN(globallyLockedBaseTH)) {
                            baseTHForSession = globallyLockedBaseTH;
                        } else {
                            SSLSBasisType basis = com.biotak.util.EnumUtil.safeEnum(SSLSBasisType.class, getSettings().getString(S_SSLS_BASIS, SSLSBasisType.STRUCTURE.name()), SSLSBasisType.STRUCTURE);
                            if (basis == null) basis = SSLSBasisType.STRUCTURE;
                            switch (basis) {
                                case PATTERN -> baseTHForSession = patternValue;
                                case TRIGGER -> baseTHForSession = triggerValue;
                                default -> baseTHForSession = structureValue;
                            }
                            getSettings().setDouble(S_LOCKED_BASE_TH_VALUE, baseTHForSession);
                            getSettings().setString(S_LOCKED_BASE_TH_ORIGIN_TIMEFRAME, series.getBarSize().toString());
                        }
                    } else {
                        SSLSBasisType basis = com.biotak.util.EnumUtil.safeEnum(SSLSBasisType.class, getSettings().getString(S_SSLS_BASIS, SSLSBasisType.STRUCTURE.name()), SSLSBasisType.STRUCTURE);
                        if (basis == null) basis = SSLSBasisType.STRUCTURE;
                        switch (basis) {
                            case PATTERN -> baseTHForSession = patternValue;
                            case TRIGGER -> baseTHForSession = triggerValue;
                            default -> baseTHForSession = structureValue;
                        }
                        getSettings().setDouble(S_LOCKED_BASE_TH_VALUE, Double.NaN);
                        getSettings().setString(S_LOCKED_BASE_TH_ORIGIN_TIMEFRAME, null);
                    }

                    double ssValue = baseTHForSession * SS_MULTIPLIER;
                    double lsValue = baseTHForSession * LS_MULTIPLIER;
                    boolean drawLsFirst = getSettings().getBoolean(S_LS_FIRST, true);

                    List<Figure> sslsFigures = LevelDrawer.drawSSLSLevels(getSettings(), series, midpointPrice, finalHigh, finalLow, ssValue, lsValue, drawLsFirst, startTime, endTime);
                    for (Figure f : sslsFigures) addFigure(f);
                }
                case M_STEP -> {
                    boolean lockAllLevels = getSettings().getBoolean(S_LOCK_ALL_LEVELS, false);
                    double controlValue = (shortStep + longStep) / 2.0;
                    double finalControlValue;

                    if (lockAllLevels) {
                        double globallyLockedControlValue = getSettings().getDouble(S_LOCKED_CONTROL_VALUE, Double.NaN);
                        if (!Double.isNaN(globallyLockedControlValue)) {
                            finalControlValue = globallyLockedControlValue;
                        } else {
                            finalControlValue = controlValue;
                            getSettings().setDouble(S_LOCKED_CONTROL_VALUE, finalControlValue);
                            getSettings().setString(S_LOCKED_CONTROL_ORIGIN_TIMEFRAME, series.getBarSize().toString());
                        }
                    } else {
                        finalControlValue = controlValue;
                        getSettings().setDouble(S_LOCKED_CONTROL_VALUE, Double.NaN);
                        getSettings().setString(S_LOCKED_CONTROL_ORIGIN_TIMEFRAME, null);
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
                case E_STEP -> {
                    boolean lockAllLevels = getSettings().getBoolean(S_LOCK_ALL_LEVELS, false);
                    boolean useTpForEStep = getSettings().getBoolean(S_USE_TP_FOR_E_STEP, false);
                    double finalEThStepInPoints;

                    if (lockAllLevels) {
                        double globallyLockedTH = getSettings().getDouble(S_LOCKED_TH_VALUE, Double.NaN);
                        if (!Double.isNaN(globallyLockedTH)) {
                            finalEThStepInPoints = globallyLockedTH;
                        } else {
                            // Calculate TH step for E mode - same as TH_STEP but with 0.75 factor
                            double timeframePercentage = TimeframeUtil.getTimeframePercentage(series.getBarSize());
                            double thStepInPoints = com.biotak.util.OptimizedCalculations.calculateTHPoints(series.getInstrument(), thBasePrice, timeframePercentage);
                            double eDistance = thStepInPoints * 0.75; // E = TH * 0.75
                            finalEThStepInPoints = useTpForEStep ? (eDistance * 3.0) : eDistance; // TP = E * 3
                            getSettings().setDouble(S_LOCKED_TH_VALUE, finalEThStepInPoints);
                            getSettings().setString(S_LOCKED_TH_ORIGIN_TIMEFRAME, series.getBarSize().toString());
                        }
                    } else {
                        // Calculate TH step for E mode - same as TH_STEP but with 0.75 factor
                        double timeframePercentage = TimeframeUtil.getTimeframePercentage(series.getBarSize());
                        double thStepInPoints = com.biotak.util.OptimizedCalculations.calculateTHPoints(series.getInstrument(), thBasePrice, timeframePercentage);
                        double eDistance = thStepInPoints * 0.75; // E = TH * 0.75
                        finalEThStepInPoints = useTpForEStep ? (eDistance * 3.0) : eDistance; // TP = E * 3
                        getSettings().setDouble(S_LOCKED_TH_VALUE, Double.NaN);
                        getSettings().setString(S_LOCKED_TH_ORIGIN_TIMEFRAME, null);
                    }
                    
                    // Only log E_STEP calculation once per minute to reduce spam
                    long nowEStep = System.currentTimeMillis();
                    if (nowEStep - lastCalcTableLogTime > LOG_INTERVAL_MS) {
                        String modeDescription = useTpForEStep ? "TP (3×E)" : "E (0.75×TH)";
                        AdvancedLogger.info("BiotakTrigger", "drawFigures", "E_STEP calc: thValue=%.3f, finalPoints=%.3f (%s)", 
                            thValue, finalEThStepInPoints, modeDescription);
                        lastCalcTableLogTime = nowEStep;
                    }
                    
                    // Use the same drawing method as TH_STEP but with E or TP distance
                    List<Figure> eFigures = LevelDrawer.drawTHLevels(getSettings(), series, midpointPrice, finalHigh, finalLow, finalEThStepInPoints, startTime, endTime);
                    for (Figure f : eFigures) addFigure(f);
                }
                case TP_STEP -> {
                    // TP_STEP implementation - placeholder for now
                    boolean lockAllLevels = getSettings().getBoolean(S_LOCK_ALL_LEVELS, false);
                    double finalTpThStepInPoints;

                    if (lockAllLevels) {
                        double globallyLockedTH = getSettings().getDouble(S_LOCKED_TH_VALUE, Double.NaN);
                        if (!Double.isNaN(globallyLockedTH)) {
                            finalTpThStepInPoints = globallyLockedTH;
                        } else {
                            // Calculate TH step for TP mode - E * 3
                            double timeframePercentage = TimeframeUtil.getTimeframePercentage(series.getBarSize());
                            double thStepInPoints = com.biotak.util.OptimizedCalculations.calculateTHPoints(series.getInstrument(), thBasePrice, timeframePercentage);
                            double eDistance = thStepInPoints * 0.75; // E = TH * 0.75
                            finalTpThStepInPoints = eDistance * 3.0; // TP = E * 3
                            getSettings().setDouble(S_LOCKED_TH_VALUE, finalTpThStepInPoints);
                            getSettings().setString(S_LOCKED_TH_ORIGIN_TIMEFRAME, series.getBarSize().toString());
                        }
                    } else {
                        // Calculate TH step for TP mode - E * 3
                        double timeframePercentage = TimeframeUtil.getTimeframePercentage(series.getBarSize());
                        double thStepInPoints = com.biotak.util.OptimizedCalculations.calculateTHPoints(series.getInstrument(), thBasePrice, timeframePercentage);
                        double eDistance = thStepInPoints * 0.75; // E = TH * 0.75
                        finalTpThStepInPoints = eDistance * 3.0; // TP = E * 3
                        getSettings().setDouble(S_LOCKED_TH_VALUE, Double.NaN);
                        getSettings().setString(S_LOCKED_TH_ORIGIN_TIMEFRAME, null);
                    }
                    
                    // Use the same drawing method as TH_STEP but with TP distance
                    List<Figure> tpFigures = LevelDrawer.drawTHLevels(getSettings(), series, midpointPrice, finalHigh, finalLow, finalTpThStepInPoints, startTime, endTime);
                    for (Figure f : tpFigures) addFigure(f);
                }
            }

            // ------------------- LEG RULER -------------------
            boolean showRuler = settings.getBoolean(S_SHOW_RULER, false);
            
            // Throttle ruler logging to prevent spam
            long nowRuler = System.currentTimeMillis();
            if (nowRuler - lastRulerInfoLog > RULER_LOG_INTERVAL_MS) {
                AdvancedLogger.info("BiotakTrigger", "drawFigures", "Ruler status: show=%s, state=%s, components=[%s,%s,%s]", 
                    showRuler, rulerState, 
                    (rulerStartResize != null), (rulerEndResize != null), (rulerFigure != null));
                lastRulerInfoLog = nowRuler;
            }
            
            if (showRuler && rulerState == RulerState.ACTIVE && rulerStartResize != null && rulerEndResize != null) {
                // Initialize ruler figure if needed
                if (rulerFigure == null) {
                    rulerFigure = new RulerFigure();
                    AdvancedLogger.debug("BiotakTrigger", "drawFigures", "Created new RulerFigure instance");
                }
                
                // Add the ruler to the chart
                addFigure(rulerFigure);
                addFigure(rulerStartResize);
                addFigure(rulerEndResize);
            }
        } finally {
            // Log level management removed to prevent spam
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
            boolean lockAllLevels = getSettings().getBoolean(S_LOCK_ALL_LEVELS, false);
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
            
            drawFigures(ctx.getDataContext().getDataSeries().size() - 1, ctx.getDataContext());
        } else if (customPriceLine != null && rp == customPriceLine.getLineResizePoint()) {
            // Check if levels are locked before allowing line drag
            boolean lockAllLevels = getSettings().getBoolean(S_LOCK_ALL_LEVELS, false);
            if (lockAllLevels) {
                // Don't allow custom price line changes when locked
                return;
            }
            
            // User finished dragging the invisible line ResizePoint (line itself)
            double newPrice = rp.getValue();
            
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
            boolean lockAllLevels = getSettings().getBoolean(S_LOCK_ALL_LEVELS, false);
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
            boolean lockAllLevels = getSettings().getBoolean(S_LOCK_ALL_LEVELS, false);
            if (lockAllLevels) {
                // Don't allow custom price line changes when locked
                return;
            }
            
            // User is dragging the invisible line ResizePoint (dragging the line itself)
            double newPrice = rp.getValue();
            
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
        BarSize triggerBarSize = TimeframeUtil.getTriggerBarSize(barSize);
        infoPanel.setDownwardFractalInfo(
            FractalCalculator.formatTimeframeString(patternBarSize), 
            FractalCalculator.formatTimeframeString(triggerBarSize), 
            com.biotak.util.OptimizedCalculations.calculateTHPoints(instrument, basePrice, TimeframeUtil.getTimeframePercentage(patternBarSize)) * instrument.getTickSize(),
            com.biotak.util.OptimizedCalculations.calculateTHPoints(instrument, basePrice, TimeframeUtil.getTimeframePercentage(triggerBarSize)) * instrument.getTickSize()
        );
        // Structure timeframe (one level up) and its pattern
        BarSize structureBarSize = TimeframeUtil.getStructureBarSize(barSize);
        BarSize higherPatternBarSize = TimeframeUtil.getPatternBarSize(structureBarSize);
        infoPanel.setUpwardFractalInfo(
            FractalCalculator.formatTimeframeString(higherPatternBarSize), 
            FractalCalculator.formatTimeframeString(structureBarSize), 
            com.biotak.util.OptimizedCalculations.calculateTHPoints(instrument, basePrice, TimeframeUtil.getTimeframePercentage(higherPatternBarSize)) * instrument.getTickSize(),
            com.biotak.util.OptimizedCalculations.calculateTHPoints(instrument, basePrice, TimeframeUtil.getTimeframePercentage(structureBarSize)) * instrument.getTickSize()
        );
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
        // Cache for ATR values (always calculated)
        private String cachedBestATRLabel = null;
        private double cachedBestATRBasePips = 0;
        private double cachedBestATRDiff = 0;

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
            // Remove log level changes to prevent excessive logging
            
            // CRITICAL FIX: Call layout first to ensure line coordinates are set
            if (line == null || rulerStartResize == null || rulerEndResize == null) {
                AdvancedLogger.error("BiotakTrigger", "RulerFigure.draw", "Missing ruler components - calling layout");
                layout(ctx);
            }
            
            // Re-check after layout
            if (line == null) {
                AdvancedLogger.error("BiotakTrigger", "RulerFigure.draw", "Line is still null after layout - cannot draw ruler");
                return;
            }
            
            // Draw the actual line
            var path = getSettings().getPath(S_RULER_PATH);
            gc.setStroke(ctx.isSelected() ? path.getSelectedStroke() : path.getStroke());
            gc.setColor(path.getColor());
            
            // IMPORTANT: Actually draw the line!
            gc.draw(line);
            
            // Throttle ruler draw logging
            long nowDraw = System.currentTimeMillis();
            if (nowDraw - lastRulerInfoLog > RULER_LOG_INTERVAL_MS) {
                AdvancedLogger.debug("BiotakTrigger", "RulerFigure.draw", "Ruler drawn: (%.0f,%.0f)-(%.0f,%.0f)", 
                    line.getX1(), line.getY1(), line.getX2(), line.getY2());
                lastRulerInfoLog = nowDraw;
            }

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
                if (startTime > endTime) {
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

                // Pixel-based angle for positioning info box
                double pixelDY = line.getY2() - line.getY1();

                long diffMs = Math.abs(endTime - startTime);
                long minutes = (diffMs / (1000 * 60)) % 60;

                double pips = com.biotak.util.UnitConverter.priceToPip(Math.abs(priceDiff), series.getInstrument());
                String pipsStr = String.format("Pips: %.1f", pips);
                String barsStr = String.format("Bars: %.0f", bars);
                 // --- Determine best matching MOVE based on selected comparison type ---
                 double tick = series.getInstrument().getTickSize();
                 // Round leg length to 0.1-pip precision for matching
                 double legPip = Math.round(pips * 10.0) / 10.0;

                 // Get the selected ruler comparison type
                 com.biotak.enums.RulerComparisonType comparisonType = com.biotak.util.EnumUtil.safeEnum(
                     com.biotak.enums.RulerComparisonType.class,
                     getSettings().getString(Constants.S_RULER_COMPARISON_TYPE, com.biotak.enums.RulerComparisonType.M.name()),
                     com.biotak.enums.RulerComparisonType.M
                 );

                 // Variables for primary comparison result
                 String bestLabel = "-";
                 double bestBasePips = 0;
                 double bestDiff = Double.MAX_VALUE;
                 
                 // Variables for ATR comparison (always calculated)
                 String bestATRLabel = "-";
                 double bestATRBasePips = 0;
                 double bestATRDiff = Double.MAX_VALUE;

                 // Cache check - only use if leg hasn't changed significantly
                 if (!Double.isNaN(cachedLegPip) && Math.abs(cachedLegPip - legPip) < 0.05) {
                     bestLabel      = cachedBestLabel;
                     bestBasePips   = cachedBestBasePips;
                     bestDiff       = cachedBestDiff;
                     // Use cached ATR values as well
                     bestATRLabel   = cachedBestATRLabel;
                     bestATRBasePips = cachedBestATRBasePips;
                     bestATRDiff    = cachedBestATRDiff;
                 } else {
                         // Calculate fresh ATR comparison using comprehensive map (no persistent cache needed)
                         {
                     // Calculate ATR comparison first (always needed for display)
                     var atrRes = com.biotak.core.RulerService.matchATRWithInstrument(
                         legPip, tick,
                         BiotakTrigger.this.fullATRValues,
                         BiotakTrigger.this.atrStructureMin,
                         BiotakTrigger.this.atrStructurePrice,
                         series.getInstrument()
                     );
                     bestATRLabel = atrRes.bestLabel();
                     bestATRBasePips = atrRes.bestBasePips();
                     bestATRDiff = atrRes.bestDiff();
                     
                     // Perform comparison based on selected type
                     switch (comparisonType) {
                         case M -> {
                             var mRes = com.biotak.core.RulerService.matchM(
                                 series.getInstrument(), legPip, tick,
                                 series.getBidClose(series.size()-1),
                                 BiotakTrigger.this.fullMValues, TH_TO_M_FACTOR
                             );
                             bestLabel = mRes.bestLabel();
                             bestBasePips = mRes.bestBasePips();
                             bestDiff = mRes.bestDiff();
                         }
                         case E -> {
                             var eRes = com.biotak.core.RulerService.matchStepValues(
                                 series.getInstrument(), legPip, tick,
                                 series.getBidClose(series.size()-1),
                                 BiotakTrigger.this.fullEValues, "E"
                             );
                             bestLabel = eRes.bestLabel();
                             bestBasePips = eRes.bestBasePips();
                             bestDiff = eRes.bestDiff();
                         }
                         case TP -> {
                             var tpRes = com.biotak.core.RulerService.matchStepValues(
                                 series.getInstrument(), legPip, tick,
                                 series.getBidClose(series.size()-1),
                                 BiotakTrigger.this.fullTPValues, "TP"
                             );
                             bestLabel = tpRes.bestLabel();
                             bestBasePips = tpRes.bestBasePips();
                             bestDiff = tpRes.bestDiff();
                         }
                         case TH -> {
                             var thRes = com.biotak.core.RulerService.matchStepValues(
                                 series.getInstrument(), legPip, tick,
                                 series.getBidClose(series.size()-1),
                                 BiotakTrigger.this.fullTHValues, "TH"
                             );
                             bestLabel = thRes.bestLabel();
                             bestBasePips = thRes.bestBasePips();
                             bestDiff = thRes.bestDiff();
                         }
                         case SS -> {
                             var ssRes = com.biotak.core.RulerService.matchStepValues(
                                 series.getInstrument(), legPip, tick,
                                 series.getBidClose(series.size()-1),
                                 BiotakTrigger.this.fullSSValues, "SS"
                             );
                             bestLabel = ssRes.bestLabel();
                             bestBasePips = ssRes.bestBasePips();
                             bestDiff = ssRes.bestDiff();
                         }
                         case LS -> {
                             var lsRes = com.biotak.core.RulerService.matchStepValues(
                                 series.getInstrument(), legPip, tick,
                                 series.getBidClose(series.size()-1),
                                 BiotakTrigger.this.fullLSValues, "LS"
                             );
                             bestLabel = lsRes.bestLabel();
                             bestBasePips = lsRes.bestBasePips();
                             bestDiff = lsRes.bestDiff();
                         }
                         case ATR -> {
                             // Use the already calculated ATR results as primary comparison
                             bestLabel = bestATRLabel;
                             bestBasePips = bestATRBasePips;
                             bestDiff = bestATRDiff;
                         }
                         case BOTH -> {
                             // Legacy mode - show both M and ATR results
                             var mRes = com.biotak.core.RulerService.matchM(
                                 series.getInstrument(), legPip, tick,
                                 series.getBidClose(series.size()-1),
                                 BiotakTrigger.this.fullMValues, TH_TO_M_FACTOR
                             );
                             bestLabel = mRes.bestLabel();
                             bestBasePips = mRes.bestBasePips();
                             bestDiff = mRes.bestDiff();
                             // ATR values are already calculated above
                         }
                     }
                     
                     // Cache the results (including ATR)
                     cachedLegPip         = legPip;
                     cachedBestLabel      = bestLabel;
                     cachedBestBasePips   = bestBasePips;
                     cachedBestDiff       = bestDiff;
                     cachedBestATRLabel   = bestATRLabel;
                     cachedBestATRBasePips= bestATRBasePips;
                     cachedBestATRDiff    = bestATRDiff;
                     
                     // Note: Since we now use a comprehensive ATR map built from all timeframes,
                     // we don't need persistent caching - results are stable across timeframe changes
                 }
                 }

                 // Log comparison results
                 long nowInfo = System.currentTimeMillis();
                 if (nowInfo - lastRulerInfoLog > RULER_LOG_INTERVAL_MS) {
                     if (comparisonType == com.biotak.enums.RulerComparisonType.BOTH) {
                         AdvancedLogger.info("BiotakTrigger", "RulerFigure.draw", "[Ruler] Leg=%.1f pips, M→%s (%.1f pips, d=%.1f) | ATR→%s (base=%.1f pips, ×3=%.1f pips, d=%.1f)",
                             legPip, bestLabel, bestBasePips, bestDiff, bestATRLabel, bestATRBasePips, bestATRBasePips * 3.0, bestATRDiff);
                     } else {
                         AdvancedLogger.info("BiotakTrigger", "RulerFigure.draw", "[Ruler] Leg=%.1f pips, %s→%s (%.1f pips, d=%.1f)",
                             legPip, comparisonType.name(), bestLabel, bestBasePips, bestDiff);
                     }
                     lastRulerInfoLog = nowInfo;
                 }

                 long totalMinutes = -1;
                 if (!bestLabel.equals("-")) {
                     int baseMinutes = TimeframeUtil.parseCompoundTimeframe(bestLabel);
                     if (baseMinutes > 0) {
                         totalMinutes = baseMinutes;
                     }
                 }

                 String matchMinutes;
                 if (totalMinutes > 0) {
                     matchMinutes = totalMinutes + "m";
                     // Logger.debug("[Ruler] Match compound=" + compoundTimeframe(totalMinutes) + ", minutes=" + matchMinutes);
                 } else {
                     matchMinutes = "-";
                 }

                 // Build display strings based on comparison type
                 String matchStr1, matchStr2;
                 
                 // Show ATR timeframe match (leg ≈ 3×ATR of this timeframe)
                 String atrStr1 = String.format("ATR : %s", bestATRLabel);
                 int atrMinVal = TimeframeUtil.parseCompoundTimeframe(bestATRLabel);
                 String atrStr2 = (atrMinVal > 0 ? atrMinVal + "m" : "-");
                 
                 if (comparisonType == com.biotak.enums.RulerComparisonType.BOTH) {
                     // Legacy mode - show both M and ATR
                     matchStr1 = "M : " + bestLabel;
                     matchStr2 = (totalMinutes > 0 ? matchMinutes : "-");
                 } else {
                     // Single comparison mode - show selected type
                     matchStr1 = comparisonType.name() + " : " + bestLabel;
                     matchStr2 = (totalMinutes > 0 ? matchMinutes : "-");
                 }
                 
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
                 
                // Add Time Pattern and Time Trigger based on the best M match (not current timeframe)
                // First, determine the base timeframe from the best M match
                BarSize baseTimeframeForM = null;
                if (!bestLabel.equals("-")) {
                    int baseMinutes = TimeframeUtil.parseCompoundTimeframe(bestLabel);
                    if (baseMinutes > 0) {
                        // Create a BarSize from the M match timeframe
                        baseTimeframeForM = BarSize.getBarSize(baseMinutes);
                    }
                }
                
                String timePatternStr1, timeTriggerStr1;
                if (baseTimeframeForM != null) {
                    // Calculate pattern and trigger based on the M match timeframe
                    BarSize patternBarSize = TimeframeUtil.getPatternBarSize(baseTimeframeForM);
                    BarSize triggerBarSize = TimeframeUtil.getTriggerBarSize(baseTimeframeForM);
                    
                    timePatternStr1 = "Time Pattern: " + com.biotak.util.DisplayUtil.formatTimeframeForDisplay(patternBarSize);
                    timeTriggerStr1 = "Time Trigger: " + com.biotak.util.DisplayUtil.formatTimeframeForDisplay(triggerBarSize);
                } else {
                    // Fallback to current timeframe if no M match found
                    BarSize currentBarSize = series.getBarSize();
                    BarSize patternBarSize = TimeframeUtil.getPatternBarSize(currentBarSize);
                    BarSize triggerBarSize = TimeframeUtil.getTriggerBarSize(currentBarSize);
                    
                    timePatternStr1 = "Time Pattern: " + com.biotak.util.DisplayUtil.formatTimeframeForDisplay(patternBarSize);
                    timeTriggerStr1 = "Time Trigger: " + com.biotak.util.DisplayUtil.formatTimeframeForDisplay(triggerBarSize);
                }
                 
                // Add nearest fractal timeframe info (only timeframe, no minutes)
                String nearestLabel = TimeframeUtil.getNearestFractalTimeframe(bestLabel);
                String nearestStr1 = "Near F: " + nearestLabel;

                // Arrange lines in requested grouped order with separators
                String sep = "-------------";
                java.util.List<String> linesList = new java.util.ArrayList<>();
                
                // Always include basic measurements
                linesList.add(pipsStr);
                linesList.add(sep);
                
                // Primary comparison result
                linesList.add(matchStr1);
                linesList.add(matchStr2);
                linesList.add(nearestStr1);
                linesList.add(sep);
                
                // Time pattern and trigger info
                linesList.add(timePatternStr1);
                linesList.add(timeTriggerStr1);
                linesList.add(sep);
                
                // Time and bar information
                linesList.add(barsStr);
                linesList.add(timeStr);
                
                // Always display ATR lines (separate from primary comparison)
                if (atrStr1 != null && atrStr2 != null) {
                    linesList.add(sep);
                    linesList.add(atrStr1);
                    linesList.add(atrStr2);
                }
                
                String[] lines = linesList.toArray(new String[0]);

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
     * ایجاد خط‌کش پیش‌فرض با استفاده از DrawContext - همیشه در وسط صفحه
     * این متد خط‌کش را در وسط صفحه نمایشی رسم می‌کند، بدون وابستگی به موقعیت چارت
     */
    private void createDefaultRulerWithContext(Settings settings, DrawContext ctx) {
        final String methodName = "createDefaultRulerWithContext";
        
        try {
            DataSeries series = ctx.getDataContext().getDataSeries();
            if (series.size() < 2) {
                AdvancedLogger.error("BiotakTrigger", methodName, "Not enough data points, falling back to immediate creation");
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
            
            AdvancedLogger.info("BiotakTrigger", methodName, "Creating screen-centered ruler: start=%.5f@%d, end=%.5f@%d", startPrice, startTime, endPrice, endTime);
            AdvancedLogger.info("BiotakTrigger", methodName, "Screen center: (%.1f, %.1f), Chart bounds: %s", screenCenterX, screenCenterY, chartBounds);
            
            // Create ruler resize points
            if (rulerStartResize == null) {
                rulerStartResize = new ResizePoint(ResizeType.ALL, true);
                rulerStartResize.setSnapToLocation(true);
                AdvancedLogger.debug("BiotakTrigger", methodName, "Created new ruler start resize point at screen center");
            }
            rulerStartResize.setLocation(startTime, startPrice);
            
            if (rulerEndResize == null) {
                rulerEndResize = new ResizePoint(ResizeType.ALL, true);
                rulerEndResize.setSnapToLocation(true);
                AdvancedLogger.debug("BiotakTrigger", methodName, "Created new ruler end resize point at screen center");
            }
            rulerEndResize.setLocation(endTime, endPrice);
            
            // Create ruler figure
            if (rulerFigure == null) {
                rulerFigure = new RulerFigure();
                AdvancedLogger.debug("BiotakTrigger", methodName, "Created new ruler figure for screen center");
            }
            
            // Save points to settings
            settings.setString(S_RULER_START, startPrice + "|" + startTime);
            settings.setString(S_RULER_END, endPrice + "|" + endTime);
            AdvancedLogger.debug("BiotakTrigger", methodName, "Saved screen-centered ruler points to settings");
            
            rulerState = RulerState.ACTIVE;
            AdvancedLogger.info("BiotakTrigger", methodName, "Screen-centered ruler created successfully and set to ACTIVE state");
            
            // Calculate and log initial measurements
            double priceDiff = endPrice - startPrice;
            double pips = com.biotak.util.UnitConverter.priceToPip(Math.abs(priceDiff), series.getInstrument());
            long timeDiff = Math.abs(endTime - startTime);
            AdvancedLogger.info("BiotakTrigger", methodName, "Initial ruler measurement: %.1f pips, price diff: %.5f, time span: %d ms", pips, priceDiff, timeDiff);
            
        } catch (Exception e) {
            AdvancedLogger.error("BiotakTrigger", methodName, "Error creating screen-centered ruler: %s", e.getMessage());
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
                AdvancedLogger.debug("BiotakTrigger", methodName, "Using custom price as current price: %s", currentPrice);
            }
            
            // اگر تاریخ بالا یا پایین در تنظیمات موجود باشد، قیمت را بر اساس آن تنظیم کن
            double historicalHigh = settings.getDouble(S_HISTORICAL_HIGH, Double.NaN);
            double historicalLow = settings.getDouble(S_HISTORICAL_LOW, Double.NaN);
            
            if (!Double.isNaN(historicalHigh) && !Double.isNaN(historicalLow) && historicalHigh > historicalLow) {
                // استفاده از نقطه میانی بین بالاترین و پایین‌ترین قیمت تاریخی
                currentPrice = (historicalHigh + historicalLow) / 2.0;
                AdvancedLogger.debug("BiotakTrigger", methodName, "Using midpoint of historical range as current price: %s", currentPrice);
            }
        } catch (Exception e) {
            AdvancedLogger.debug("BiotakTrigger", methodName, "Could not get current price, using default: %s", currentPrice);
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
        
        AdvancedLogger.info("BiotakTrigger", methodName, "Creating centered default ruler: start=%.5f@%d, end=%.5f@%d", startPrice, startTime, endPrice, endTime);
        AdvancedLogger.info("BiotakTrigger", methodName, "Ruler spans %.1f hours with price range of %.2f (%.3f%% of current price)", 
            timeSpan / (60.0 * 60.0 * 1000.0), priceRange, (priceRange / currentPrice) * 100);
        
        // Create ruler resize points
        if (rulerStartResize == null) {
            rulerStartResize = new ResizePoint(ResizeType.ALL, true);
            rulerStartResize.setSnapToLocation(true);
            AdvancedLogger.debug("BiotakTrigger", methodName, "Created new ruler start resize point");
        }
        rulerStartResize.setLocation(startTime, startPrice);
        
        if (rulerEndResize == null) {
            rulerEndResize = new ResizePoint(ResizeType.ALL, true);
            rulerEndResize.setSnapToLocation(true);
            AdvancedLogger.debug("BiotakTrigger", methodName, "Created new ruler end resize point");
        }
        rulerEndResize.setLocation(endTime, endPrice);
        
        // Create ruler figure
        if (rulerFigure == null) {
            rulerFigure = new RulerFigure();
            AdvancedLogger.debug("BiotakTrigger", methodName, "Created new ruler figure");
        }
        
        // Save points to settings
        settings.setString(S_RULER_START, startPrice + "|" + startTime);
        settings.setString(S_RULER_END, endPrice + "|" + endTime);
        AdvancedLogger.debug("BiotakTrigger", methodName, "Saved ruler points to settings");
        
        // CRITICAL: Set ruler state to ACTIVE after creating all components
        rulerState = RulerState.ACTIVE;
        AdvancedLogger.info("BiotakTrigger", methodName, "Centered default ruler created successfully and set to ACTIVE state");
        
        // Calculate and log initial measurements
        double priceDiff = endPrice - startPrice;
        AdvancedLogger.info("BiotakTrigger", methodName, "Initial ruler measurement: price diff: %.5f, time span: %.1f hours", 
            priceDiff, timeSpan / (60.0 * 60.0 * 1000.0));
        
        // IMPORTANT: Double-check that the state was actually set
        if (rulerState != RulerState.ACTIVE) {
            AdvancedLogger.error("BiotakTrigger", methodName, "CRITICAL ERROR: rulerState failed to set to ACTIVE!");
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
            AdvancedLogger.debug("BiotakTrigger", methodName, "Cleared ruler start point");
        }
        if (rulerEndResize != null) {
            rulerEndResize = null;
            AdvancedLogger.debug("BiotakTrigger", methodName, "Cleared ruler end point");
        }
        if (rulerFigure != null) {
            rulerFigure = null;
            AdvancedLogger.debug("BiotakTrigger", methodName, "Cleared ruler figure");
        }
        
        // Clear saved settings
        Settings settings = getSettings();
        settings.setString(S_RULER_START, null);
        settings.setString(S_RULER_END, null);
        AdvancedLogger.debug("BiotakTrigger", methodName, "Cleared ruler settings");
    }
    
    
    /**
     * Reset all locked values when lock all levels is disabled
     */
    
    
}
