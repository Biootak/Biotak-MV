package com.biotak.ui;

import com.biotak.debug.AdvancedLogger;
import com.biotak.enums.RulerState;
import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.draw.ResizePoint;
import com.motivewave.platform.sdk.common.Enums.ResizeType;

/**
 * کلاس اتصال برای مدیریت خط کش در BiotakTrigger
 * 
 * این کلاس مشکل نمایش خط کش در Live به جای موقعیت فعلی کاربر را حل می‌کند
 */
public class RulerIntegrationManager {
    
    private static final String CLASS_NAME = "RulerIntegrationManager";
    
    private final Study study;
    private RulerPositionFix rulerPositionFix;
    // Note: RulerCurrentPositionManager removed to fix compilation
    // private RulerCurrentPositionManager currentPositionManager;
    
    // Ruler components from BiotakTrigger
    private ResizePoint rulerStartResize;
    private ResizePoint rulerEndResize; 
    private RulerState rulerState = RulerState.INACTIVE;
    
    public RulerIntegrationManager(Study study) {
        this.study = study;
        AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
            "🔧 RulerIntegrationManager initialized");
    }
    
    /**
     * تنظیم context ها برای کار با MotiveWave API
     */
    public void setupContext(DrawContext drawContext, DataContext dataContext) {
        try {
            // Initialize position fix with current contexts
            rulerPositionFix = new RulerPositionFix(study, drawContext, dataContext);
            
            // Initialize position manager (temporarily disabled)
            // currentPositionManager = new RulerCurrentPositionManager();
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "✅ Contexts setup successfully");
                
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Failed to setup contexts: %s", e.getMessage());
        }
    }
    
    /**
     * فعال‌سازی خط کش در موقعیت فعلی کاربر (حل مشکل اصلی)
     */
    public boolean activateRulerAtCurrentPosition() {
        try {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "🎯 Activating ruler at current user position...");
            
            if (rulerPositionFix == null) {
                AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                    "❌ RulerPositionFix not initialized. Call setupContext() first.");
                return false;
            }
            
            // Use position fix to activate ruler at current position
            boolean success = rulerPositionFix.activateRulerHere();
            
            if (success) {
                rulerState = RulerState.WAITING_FOR_START;
                AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                    "✅ Ruler activated at current position successfully");
            } else {
                AdvancedLogger.ruler(AdvancedLogger.LogLevel.WARN, CLASS_NAME,
                    "⚠️ Failed to activate ruler at current position");
            }
            
            return success;
            
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Error activating ruler: %s", e.getMessage());
            return false;
        }
    }
    
    /**
     * شروع نظارت بر موقعیت خط کش (جلوگیری از jump به Live)
     */
    public void startPositionMonitoring() {
        try {
            // Position monitoring temporarily disabled
            // if (currentPositionManager != null) {
            //     currentPositionManager.startPositionMonitoring();
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "👁️ Position monitoring started (placeholder)");
            // }
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Error starting position monitoring: %s", e.getMessage());
        }
    }
    
    /**
     * تنظیم نقاط resize برای خط کش
     */
    public void setupRulerPoints(ResizePoint startResize, ResizePoint endResize) {
        this.rulerStartResize = startResize;
        this.rulerEndResize = endResize;
        
        AdvancedLogger.ruler(AdvancedLogger.LogLevel.DEBUG, CLASS_NAME,
            "🔧 Ruler resize points setup");
    }
    
    /**
     * مدیریت کلیک برای انتخاب نقاط خط کش
     */
    public boolean handleRulerClick(double x, double y, DrawContext drawContext) {
        try {
            if (rulerPositionFix == null) {
                return false;
            }
            
            RulerState currentState = rulerPositionFix.getRulerState();
            
            if (currentState == RulerState.WAITING_FOR_START) {
                // Set start point
                return handleStartPointSelection(x, y, drawContext);
                
            } else if (currentState == RulerState.WAITING_FOR_END) {
                // Set end point  
                return handleEndPointSelection(x, y, drawContext);
            }
            
            return false;
            
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Error handling ruler click: %s", e.getMessage());
            return false;
        }
    }
    
    private boolean handleStartPointSelection(double x, double y, DrawContext drawContext) {
        try {
            // Convert screen coordinates to chart coordinates
            long time = drawContext.translate2Time(x);
            double value = drawContext.translate2Value(y);
            
            // Initialize start resize point if needed
            if (rulerStartResize == null) {
                rulerStartResize = new ResizePoint(ResizeType.ALL, true);
                rulerStartResize.setSnapToLocation(true);
            }
            
            // Set the location
            rulerStartResize.setLocation(time, value);
            
            // Update state
            rulerState = RulerState.WAITING_FOR_END;
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "🎯 Ruler start point set at time: %d, value: %.5f", time, value);
                
            return true;
            
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Error setting start point: %s", e.getMessage());
            return false;
        }
    }
    
    private boolean handleEndPointSelection(double x, double y, DrawContext drawContext) {
        try {
            // Convert screen coordinates to chart coordinates
            long time = drawContext.translate2Time(x);
            double value = drawContext.translate2Value(y);
            
            // Initialize end resize point if needed
            if (rulerEndResize == null) {
                rulerEndResize = new ResizePoint(ResizeType.ALL, true);
                rulerEndResize.setSnapToLocation(true);
            }
            
            // Set the location
            rulerEndResize.setLocation(time, value);
            
            // Update state
            rulerState = RulerState.ACTIVE;
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "🏁 Ruler end point set at time: %d, value: %.5f", time, value);
                
            return true;
            
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Error setting end point: %s", e.getMessage());
            return false;
        }
    }
    
    /**
     * غیرفعال‌سازی خط کش
     */
    public void deactivateRuler() {
        try {
            if (rulerPositionFix != null) {
                rulerPositionFix.deactivateRuler();
            }
            
            // Position manager temporarily disabled
            // if (currentPositionManager != null) {
            //     currentPositionManager.deactivateRuler();
            // }
            
            rulerState = RulerState.INACTIVE;
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "⏹️ Ruler deactivated");
                
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Error deactivating ruler: %s", e.getMessage());
        }
    }
    
    /**
     * دریافت وضعیت فعلی خط کش
     */
    public RulerState getRulerState() {
        if (rulerPositionFix != null) {
            return rulerPositionFix.getRulerState();
        }
        return rulerState;
    }
    
    /**
     * بررسی اینکه آیا خط کش فعال است
     */
    public boolean isRulerActive() {
        return getRulerState() != RulerState.INACTIVE;
    }
    
    /**
     * دریافت نقاط خط کش
     */
    public ResizePoint getRulerStartPoint() {
        return rulerStartResize;
    }
    
    public ResizePoint getRulerEndPoint() {
        return rulerEndResize;
    }
    
    /**
     * بروزرسانی context ها (برای تغییرات viewport)
     */
    public void updateContext(DrawContext drawContext, DataContext dataContext) {
        try {
            if (rulerPositionFix != null) {
                // Create new instance with updated contexts
                rulerPositionFix = new RulerPositionFix(study, drawContext, dataContext);
            }
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.DEBUG, CLASS_NAME,
                "🔄 Contexts updated");
                
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Error updating contexts: %s", e.getMessage());
        }
    }
    
    /**
     * فرجام‌کردن و تمیز کردن منابع
     */
    public void cleanup() {
        try {
            deactivateRuler();
            
            rulerPositionFix = null;
            // currentPositionManager = null; // Commented out
            rulerStartResize = null;
            rulerEndResize = null;
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "🧹 Resources cleaned up");
                
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Error during cleanup: %s", e.getMessage());
        }
    }
}
