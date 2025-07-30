package com.biotak.config;

import com.biotak.enums.MStepBasisType;
import com.biotak.enums.PanelPosition;
import com.biotak.enums.SSLSBasisType;
import com.biotak.enums.StepCalculationMode;
import com.biotak.enums.THStartPointType;
import com.biotak.util.Constants;
import com.motivewave.platform.sdk.common.Defaults;
import com.motivewave.platform.sdk.common.NVP;
import com.motivewave.platform.sdk.common.desc.BooleanDescriptor;
import com.motivewave.platform.sdk.common.desc.ColorDescriptor;
import com.motivewave.platform.sdk.common.desc.DiscreteDescriptor;
import com.motivewave.platform.sdk.common.desc.DoubleDescriptor;
import com.motivewave.platform.sdk.common.desc.FontDescriptor;
import com.motivewave.platform.sdk.common.desc.IntegerDescriptor;
import com.motivewave.platform.sdk.common.desc.PathDescriptor;
import com.motivewave.platform.sdk.common.desc.SettingsDescriptor;
import com.motivewave.platform.sdk.common.desc.StringDescriptor;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import static com.biotak.config.SettingsRepository.*;
import com.motivewave.platform.sdk.common.X11Colors;

public class SettingsService {

    public static void initializeSettings(SettingsDescriptor sd, Defaults defaults) {
        setupQuickSettingsTab(sd, defaults);
        setupLevelsTab(sd, defaults);
        setupCalculationTab(sd);
        setupAppearanceTab(sd, defaults);
        setupRulerTab(sd);
        setupAdvancedTab(sd, defaults);
        setupQuickSettingsToolbar(sd);
    }

    private static void setupQuickSettingsTab(SettingsDescriptor sd, Defaults defaults) {
        var quick = sd.addTab("Quick Setup");
        
        var qBasic = quick.addGroup("📊 Basic Calculation");
        qBasic.addRow(new DiscreteDescriptor(S_START_POINT, "Anchor Point", THStartPointType.MIDPOINT.name(), 
            createStartPointOptions()));
        qBasic.addRow(new DiscreteDescriptor(S_STEP_MODE, "Step Mode", StepCalculationMode.TH_STEP.name(), 
            createStepModeOptions()));
        
        var qLevels = quick.addGroup("👁️ Show / Hide Levels");
        qLevels.addRow(new BooleanDescriptor(S_SHOW_TH_LEVELS, "TH Ladder", true));
        qLevels.addRow(new BooleanDescriptor(S_SHOW_STRUCTURE_LINES, "Structure Highlights", true));
        qLevels.addRow(new BooleanDescriptor(S_SHOW_TRIGGER_LEVELS, "Trigger Sub-levels", false));
        
        var qTools = quick.addGroup("🛠️ Essential Tools");
        qTools.addRow(new BooleanDescriptor(S_SHOW_HIGH_LINE, "Historical High Line", true));
        qTools.addRow(new BooleanDescriptor(S_SHOW_LOW_LINE, "Historical Low Line", true));
        qTools.addRow(new BooleanDescriptor(S_SHOW_INFO_PANEL, "Info Panel", true));
        qTools.addRow(new BooleanDescriptor(S_SHOW_RULER, "Leg Ruler Tool", false));
        
        var qTheme = quick.addGroup("🎨 Appearance");
        qTheme.addRow(new DiscreteDescriptor(Constants.S_UI_THEME, "Color Theme", "auto", 
            createThemeOptions()));
        qTheme.addRow(new BooleanDescriptor(Constants.S_ADAPTIVE_COLORS, "Adaptive Colors", true));
    }

    private static List<NVP> createStartPointOptions() {
        List<NVP> options = new ArrayList<>();
        options.add(new NVP("Midpoint (Default)", THStartPointType.MIDPOINT.name()));
        options.add(new NVP("Historical High", THStartPointType.HISTORICAL_HIGH.name()));
        options.add(new NVP("Historical Low", THStartPointType.HISTORICAL_LOW.name()));
        options.add(new NVP("Custom Price", THStartPointType.CUSTOM_PRICE.name()));
        return options;
    }

    private static List<NVP> createStepModeOptions() {
        List<NVP> options = new ArrayList<>();
        options.add(new NVP("Equal TH Steps", StepCalculationMode.TH_STEP.name()));
        options.add(new NVP("SS / LS Steps", StepCalculationMode.SS_LS_STEP.name()));
        options.add(new NVP("M (C×3) Steps", StepCalculationMode.M_STEP.name()));
        options.add(new NVP("E Steps", StepCalculationMode.E_STEP.name()));
        return options;
    }

    private static List<NVP> createThemeOptions() {
        List<NVP> options = new ArrayList<>();
        options.add(new NVP("Auto (Smart Detection)", "auto"));
        options.add(new NVP("Light Theme", "light"));
        options.add(new NVP("Dark Theme", "dark"));
        return options;
    }

    private static void setupLevelsTab(SettingsDescriptor sd, Defaults defaults) {
        var tab = sd.addTab("Levels");
        var grp = tab.addGroup("TH Levels");
        grp.addRow(new BooleanDescriptor(S_SHOW_TH_LEVELS, "Show TH Levels", true));
        grp.addRow(new BooleanDescriptor(S_SHOW_TRIGGER_LEVELS, "Show Trigger Levels", false));
        grp.addRow(new PathDescriptor(S_TRIGGER_PATH, "Trigger Line", X11Colors.DIM_GRAY, 1.0f, new float[] {3f, 3f} , true, false, false));
        grp.addRow(new IntegerDescriptor(S_MAX_LEVELS_ABOVE, "Max Levels Above", 100, 1, 10000, 1));
        grp.addRow(new IntegerDescriptor(S_MAX_LEVELS_BELOW, "Max Levels Below", 100, 1, 10000, 1));
        
        grp = tab.addGroup("Start Point");
        grp.addRow(new DiscreteDescriptor(S_START_POINT, "TH Start Point", THStartPointType.MIDPOINT.name(), createStartPointOptions()));
    }

    private static void setupCalculationTab(SettingsDescriptor sd) {
        var tab = sd.addTab("Calculation");
        var grp = tab.addGroup("Step Calculation");
        grp.addRow(new DiscreteDescriptor(S_STEP_MODE, "Step Mode", StepCalculationMode.TH_STEP.name(), createStepModeOptions()));
        grp.addRow(new BooleanDescriptor(S_LS_FIRST, "Draw LS First", true));
        
        List<NVP> basisOptions = new ArrayList<>();
        for (SSLSBasisType b : SSLSBasisType.values()) {
            basisOptions.add(new NVP(b.toString(), b.name()));
        }
        grp.addRow(new DiscreteDescriptor(S_SSLS_BASIS, "SS/LS Timeframe", SSLSBasisType.STRUCTURE.name(), basisOptions));
        
        grp = tab.addGroup("M-Step Basis");
        List<NVP> mbasisOptions = new ArrayList<>();
        for (com.biotak.enums.MStepBasisType b : com.biotak.enums.MStepBasisType.values()) {
            mbasisOptions.add(new NVP(b.toString(), b.name()));
        }
        grp.addRow(new DiscreteDescriptor(Constants.S_MSTEP_BASIS, "Distance By", com.biotak.enums.MStepBasisType.C_BASED.name(), mbasisOptions));
        
        grp = tab.addGroup("General");
        grp.addRow(new StringDescriptor(S_OBJ_PREFIX, "Object Prefix", "BiotakTH3"));
        grp.addRow(new BooleanDescriptor(S_LOCK_ALL_LEVELS, "Lock All Levels", false));
    }

    private static void setupAppearanceTab(SettingsDescriptor sd, Defaults defaults) {
        var tab = sd.addTab("Appearance");
        var grp = tab.addGroup("General Style");
        grp.addRow(new FontDescriptor(S_FONT, "Label Font", defaults.getFont()));
        grp.addRow(new PathDescriptor(S_CUSTOM_PRICE_PATH, "Custom Price Line", X11Colors.GOLD, 2.0f, new float[]{2f,2f}, true, false, false));
        
        grp = tab.addGroup("Historical Lines");
        grp.addRow(new BooleanDescriptor(S_SHOW_HIGH_LINE, "Show High Line", true));
        grp.addRow(new PathDescriptor(S_HIGH_LINE_PATH, "High Line", defaults.getRed(), 1.0f, null, true, false, false));
        grp.addRow(new BooleanDescriptor(S_SHOW_LOW_LINE, "Show Low Line", true));
        grp.addRow(new PathDescriptor(S_LOW_LINE_PATH, "Low Line", defaults.getBlue(), 1.0f, null, true, false, false));

        grp = tab.addGroup("Structure Lines");
        grp.addRow(new BooleanDescriptor(S_SHOW_STRUCTURE_LINES, "Show Structure Lines", true));
        grp.addRow(new BooleanDescriptor(S_SHOW_STRUCT_L1, "Show Level 1", true));
        grp.addRow(new PathDescriptor(S_STRUCT_L1_PATH, "Level 1 Path", defaults.getBlue(), 2.0f, null, true, false, false));
        grp.addRow(new BooleanDescriptor(S_SHOW_STRUCT_L2, "Show Level 2", true));
        grp.addRow(new PathDescriptor(S_STRUCT_L2_PATH, "Level 2 Path", X11Colors.DARK_GREEN, 2.0f, null, true, false, false));
        grp.addRow(new BooleanDescriptor(S_SHOW_STRUCT_L3, "Show Level 3", true));
        grp.addRow(new PathDescriptor(S_STRUCT_L3_PATH, "Level 3 Path", X11Colors.DARK_VIOLET, 2.0f, null, true, false, false));
        grp.addRow(new BooleanDescriptor(S_SHOW_STRUCT_L4, "Show Level 4", true));
        grp.addRow(new PathDescriptor(S_STRUCT_L4_PATH, "Level 4 Path", X11Colors.DARK_ORANGE, 2.0f, null, true, false, false));
        grp.addRow(new BooleanDescriptor(S_SHOW_STRUCT_L5, "Show Level 5", true));
        grp.addRow(new PathDescriptor(S_STRUCT_L5_PATH, "Level 5 Path", X11Colors.MAROON, 2.0f, null, true, false, false));

        grp = tab.addGroup("Info Panel");
        grp.addRow(new BooleanDescriptor(S_SHOW_INFO_PANEL, "Show Info Panel", true));
        List<NVP> positionOptions = new ArrayList<>();
        for(PanelPosition pos : PanelPosition.values()) {
            positionOptions.add(new NVP(pos.toString(), pos.name()));
        }
        grp.addRow(new DiscreteDescriptor(S_PANEL_POSITION, "Panel Position", PanelPosition.BOTTOM_RIGHT.name(), positionOptions));
        grp.addRow(new IntegerDescriptor(S_PANEL_MARGIN_X, "Panel Margin X", 10, 0, 100, 1));
        grp.addRow(new IntegerDescriptor(S_PANEL_MARGIN_Y, "Panel Margin Y", 10, 0, 100, 1));
        grp.addRow(new IntegerDescriptor(S_PANEL_TRANSPARENCY, "Panel Transparency", 230, 0, 255, 1));
        grp.addRow(new BooleanDescriptor(S_PANEL_MINIMIZED, "Start Minimized", false));
        grp.addRow(new FontDescriptor(S_TITLE_FONT, "Title Font", new Font("Arial", Font.BOLD, 12)));
        grp.addRow(new FontDescriptor(S_CONTENT_FONT, "Content Font", new Font("Arial", Font.PLAIN, 11)));
    }

    private static void setupRulerTab(SettingsDescriptor sd) {
        var tab = sd.addTab("Ruler");
        var grp = tab.addGroup("Ruler Settings");
        grp.addRow(new BooleanDescriptor(S_SHOW_RULER, "Show Ruler", false));
        grp.addRow(new BooleanDescriptor(S_ALWAYS_SHOW_RULER_INFO, "Always Show Ruler Info", false));
        grp.addRow(new PathDescriptor(S_RULER_PATH, "Ruler Line Path", X11Colors.GREEN, 1.0f, null, true, false, false));
        grp.addRow(new BooleanDescriptor(S_RULER_EXT_LEFT, "Extend Left", false));
        grp.addRow(new BooleanDescriptor(S_RULER_EXT_RIGHT, "Extend Right", false));
        grp.addRow(new ColorDescriptor(S_RULER_TEXT_COLOR, "Text Color", java.awt.Color.BLACK));
        grp.addRow(new ColorDescriptor(S_RULER_BG_COLOR, "Background Color", new java.awt.Color(255,255,255)));
        grp.addRow(new ColorDescriptor(S_RULER_BORDER_COLOR, "Border Color", java.awt.Color.GRAY));
        grp.addRow(new FontDescriptor(S_RULER_FONT, "Ruler Info Font", new Font("Arial", Font.PLAIN, 11)));
    }

    private static void setupAdvancedTab(SettingsDescriptor sd, Defaults defaults) {
        var tab = sd.addTab("Advanced");
        var grp = tab.addGroup("Manual Override");
        grp.addRow(new BooleanDescriptor(S_MANUAL_HL_ENABLE, "Enable Manual High/Low", false));
        grp.addRow(new DoubleDescriptor(S_MANUAL_HIGH, "Manual High", 0, 0, Double.MAX_VALUE, 0.0001));
        grp.addRow(new DoubleDescriptor(S_MANUAL_LOW, "Manual Low", 0, 0, Double.MAX_VALUE, 0.0001));
        
        grp = tab.addGroup("Historical Data");
        grp.addRow(new IntegerDescriptor(S_HISTORICAL_BARS, "Historical Bars to Load", 100000, 1000, Integer.MAX_VALUE, 1000));
        
        grp = tab.addGroup("Logging");
        List<NVP> levelOpts = new ArrayList<>();
        for (com.biotak.debug.AdvancedLogger.LogLevel lv : com.biotak.debug.AdvancedLogger.LogLevel.values()) levelOpts.add(new NVP(lv.name(), lv.name()));
        grp.addRow(new DiscreteDescriptor(S_LOG_LEVEL, "Log Level", com.biotak.debug.AdvancedLogger.LogLevel.INFO.name(), levelOpts));
    }

    private static void setupQuickSettingsToolbar(SettingsDescriptor sd) {
        sd.addQuickSettings(S_START_POINT, S_STEP_MODE);
        sd.addQuickSettings(S_SHOW_TH_LEVELS, S_SHOW_STRUCTURE_LINES, S_SHOW_TRIGGER_LEVELS);
        sd.addQuickSettings(S_SHOW_HIGH_LINE, S_SHOW_LOW_LINE, S_SHOW_RULER, S_RULER_EXT_LEFT, S_RULER_EXT_RIGHT);
        sd.addQuickSettings(S_SSLS_BASIS, S_LS_FIRST, S_LOCK_ALL_LEVELS, Constants.S_MSTEP_BASIS);
    }
}
