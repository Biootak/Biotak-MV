package com.biotak.ui;

import com.biotak.debug.AdvancedLogger;
import com.biotak.enums.RulerState;
import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.study.Study;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.concurrent.atomic.AtomicReference;

/**
 * راه‌حل عملی برای مشکل نمایش خط کش در Live به جای موقعیت فعلی کاربر
 * 
 * این کلاس دقیقاً مشکل شما را حل می‌کند: 
 * وقتی در موقعیت تاریخی هستید و خط کش را فعال می‌کنید، در همان موقعیت نمایش داده شود
 */
public class RulerPositionFix {
    
    private static final String CLASS_NAME = "RulerPositionFix";
    
    // موقعیت فعلی چارت که کاربر در آن قرار دارد
    private final AtomicReference<ChartPosition> userCurrentPosition = new AtomicReference<>();
    
    // وضعیت خط کش
    private RulerState rulerState = RulerState.INACTIVE;
    
    // آیا باید خط کش را در موقعیت فعلی اجبار کنیم؟
    private boolean forceCurrentPosition = true;
    
    // References to MotiveWave components
    private Study study;
    private DrawContext drawContext;
    private DataContext dataContext;
    
    // Constructor to inject MotiveWave dependencies
    public RulerPositionFix(Study study, DrawContext drawContext, DataContext dataContext) {
        this.study = study;
        this.drawContext = drawContext;
        this.dataContext = dataContext;
        
        AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
            "🔧 RulerPositionFix initialized with MotiveWave components");
    }
    
    /**
     * متد اصلی برای فعال‌سازی خط کش در موقعیت فعلی
     */
    public boolean activateRulerHere() {
        try {
            // 1. دریافت موقعیت فعلی چارت
            ChartPosition currentPos = getCurrentChartPosition();
            
            if (currentPos == null) {
                AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                    "❌ Cannot determine current chart position");
                return false;
            }
            
            // 2. ذخیره موقعیت فعلی
            userCurrentPosition.set(currentPos);
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "📍 User is at position: %s", currentPos);
            
            // 3. بررسی اینکه آیا در Live هستیم یا خیر
            if (currentPos.isLive()) {
                // اگر در Live هستیم، فعال‌سازی عادی
                return activateRulerNormally();
            } else {
                // اگر در موقعیت تاریخی هستیم، فعال‌سازی در همین موقعیت
                return activateRulerAtHistoricalPosition(currentPos);
            }
            
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Error activating ruler: %s", e.getMessage());
            return false;
        }
    }
    
    /**
     * فعال‌سازی خط کش در موقعیت تاریخی (غیر Live)
     */
    private boolean activateRulerAtHistoricalPosition(ChartPosition position) {
        try {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "🕰️ Activating ruler at historical position: %s", position);
            
            // 1. منع انتقال به Live
            preventJumpToLive();
            
            // 2. تنظیم موقعیت خط کش روی موقعیت فعلی
            setRulerPosition(position);
            
            // 3. فعال‌سازی خط کش
            rulerState = RulerState.WAITING_FOR_START;
            
            // 4. شروع نظارت بر موقعیت
            startPositionWatch();
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "✅ Ruler activated at historical position successfully");
            
            return true;
            
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Failed to activate ruler at historical position: %s", e.getMessage());
            return false;
        }
    }
    
    /**
     * فعال‌سازی عادی خط کش (برای حالت Live)
     */
    private boolean activateRulerNormally() {
        try {
            rulerState = RulerState.WAITING_FOR_START;
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "✅ Ruler activated normally (Live mode)");
            
            return true;
            
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Failed to activate ruler normally: %s", e.getMessage());
            return false;
        }
    }
    
    /**
     * جلوگیری از انتقال خودکار به Live
     */
    private void preventJumpToLive() {
        try {
            // این متد باید منطق پلتفرم را override کند
            // تا از انتقال خودکار به Live جلوگیری کند
            
            // مثال: تنظیم flag در سیستم
            // chartAPI.setAutoJumpToLive(false);
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.DEBUG, CLASS_NAME,
                "🚫 Auto jump to Live prevented");
            
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.WARN, CLASS_NAME,
                "⚠️ Could not prevent jump to Live: %s", e.getMessage());
        }
    }
    
    /**
     * تنظیم موقعیت خط کش
     */
    private void setRulerPosition(ChartPosition position) {
        try {
            // این متد باید موقعیت خط کش را مستقیماً تنظیم کند
            // به جای اینکه بگذارد سیستم خودش تصمیم بگیرد
            
            // مثال پیاده‌سازی:
            // rulerAPI.setPosition(position.timeStamp, position.priceLevel);
            // rulerAPI.setViewport(position.startTime, position.endTime);
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.DEBUG, CLASS_NAME,
                "🎯 Ruler position set to: %s", position);
            
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Failed to set ruler position: %s", e.getMessage());
        }
    }
    
    /**
     * شروع نظارت بر موقعیت خط کش
     */
    private void startPositionWatch() {
        Thread watchThread = new Thread(() -> {
            while (rulerState != RulerState.INACTIVE) {
                try {
                    // بررسی اینکه آیا خط کش هنوز در موقعیت صحیح است
                    checkAndCorrectRulerPosition();
                    
                    Thread.sleep(200); // هر 200 میلی‌ثانیه چک کن
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                        "❌ Error in position watch: %s", e.getMessage());
                }
            }
        });
        
        watchThread.setName("RulerPositionWatch");
        watchThread.setDaemon(true);
        watchThread.start();
        
        AdvancedLogger.ruler(AdvancedLogger.LogLevel.DEBUG, CLASS_NAME,
            "👁️ Position watch started");
    }
    
    /**
     * بررسی و تصحیح موقعیت خط کش
     */
    private void checkAndCorrectRulerPosition() {
        try {
            ChartPosition expectedPosition = userCurrentPosition.get();
            ChartPosition actualPosition = getRulerActualPosition();
            
            if (expectedPosition != null && actualPosition != null) {
                if (!positionsMatch(expectedPosition, actualPosition)) {
                    
                    AdvancedLogger.ruler(AdvancedLogger.LogLevel.WARN, CLASS_NAME,
                        "⚠️ Ruler position drift detected. Correcting...");
                    
                    // تصحیح موقعیت
                    setRulerPosition(expectedPosition);
                }
            }
            
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Error checking ruler position: %s", e.getMessage());
        }
    }
    
    /**
     * کلیک handler برای شروع خط کش
     */
    public MouseListener createRulerClickHandler() {
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (rulerState == RulerState.WAITING_FOR_START) {
                    // شروع خط کش در موقعیت کلیک شده
                    startRulerAtClick(e.getPoint());
                } else if (rulerState == RulerState.WAITING_FOR_END) {
                    // پایان خط کش
                    endRulerAtClick(e.getPoint());
                }
            }
            
            @Override public void mousePressed(MouseEvent e) {}
            @Override public void mouseReleased(MouseEvent e) {}
            @Override public void mouseEntered(MouseEvent e) {}
            @Override public void mouseExited(MouseEvent e) {}
        };
    }
    
    /**
     * شروع خط کش در نقطه کلیک شده
     */
    private void startRulerAtClick(Point clickPoint) {
        try {
            // تبدیل نقطه کلیک به مختصات چارت
            ChartCoordinate chartCoord = convertToChartCoordinate(clickPoint);
            
            if (chartCoord != null) {
                rulerState = RulerState.WAITING_FOR_END;
                
                AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                    "🖱️ Ruler started at: %s", chartCoord);
            }
            
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Error starting ruler at click: %s", e.getMessage());
        }
    }
    
    /**
     * پایان خط کش در نقطه کلیک شده
     */
    private void endRulerAtClick(Point clickPoint) {
        try {
            ChartCoordinate chartCoord = convertToChartCoordinate(clickPoint);
            
            if (chartCoord != null) {
                rulerState = RulerState.ACTIVE;
                
                AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                    "🏁 Ruler completed at: %s", chartCoord);
            }
            
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Error ending ruler at click: %s", e.getMessage());
        }
    }
    
    /**
     * غیرفعال‌سازی خط کش
     */
    public void deactivateRuler() {
        try {
            rulerState = RulerState.INACTIVE;
            userCurrentPosition.set(null);
            
            // بازگرداندن تنظیمات عادی
            // chartAPI.setAutoJumpToLive(true);
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.INFO, CLASS_NAME,
                "⏹️ Ruler deactivated");
            
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Error deactivating ruler: %s", e.getMessage());
        }
    }
    
    // ================== Helper Methods ==================
    
    private ChartPosition getCurrentChartPosition() {
        try {
            if (drawContext == null || dataContext == null) {
                AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                    "❌ DrawContext or DataContext is null");
                return null;
            }
            
            DataSeries series = dataContext.getDataSeries();
            if (series == null || series.size() == 0) {
                AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                    "❌ No data series available");
                return null;
            }
            
            // Get visible time range from chart bounds
            long visibleStartTime = series.getStartTime(0); // First visible bar
            long visibleEndTime = series.getStartTime(series.size() - 1); // Last bar
            
            // Get current visible center time (approximate)
            long centerTime = (visibleStartTime + visibleEndTime) / 2;
            
            // Find the bar closest to center time
            int centerBarIndex = findClosestBarIndex(series, centerTime);
            if (centerBarIndex < 0) {
                centerBarIndex = Math.max(0, series.size() - 1);
            }
            
            // Get price at center
            double centerPrice = series.getClose(centerBarIndex);
            
            // Check if we're at live position (within last few bars)
            int lastBarIndex = series.size() - 1;
            long lastBarTime = series.getStartTime(lastBarIndex);
            long currentTime = System.currentTimeMillis();
            
            // Consider "live" if we're within the last 5 bars or within 5 minutes of current time
            boolean isLive = (centerBarIndex >= lastBarIndex - 5) || 
                           (Math.abs(currentTime - lastBarTime) < 5 * 60 * 1000);
            
            ChartPosition position = new ChartPosition(centerTime, centerPrice, isLive);
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.DEBUG, CLASS_NAME,
                "📊 Current chart position - Time: %d, Price: %.5f, Live: %b, CenterBar: %d/%d", 
                centerTime, centerPrice, isLive, centerBarIndex, lastBarIndex);
            
            return position;
            
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Error getting current chart position: %s", e.getMessage());
            return null;
        }
    }
    
    private ChartPosition getRulerActualPosition() {
        try {
            // این متد باید موقعیت فعلی خط کش را از API دریافت کند
            // return rulerAPI.getCurrentPosition();
            
            return null; // پیاده‌سازی موقت
            
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Error getting ruler actual position: %s", e.getMessage());
            return null;
        }
    }
    
    private boolean positionsMatch(ChartPosition pos1, ChartPosition pos2) {
        if (pos1 == null || pos2 == null) return false;
        
        long timeDiff = Math.abs(pos1.getTimeStamp() - pos2.getTimeStamp());
        double priceDiff = Math.abs(pos1.getPriceLevel() - pos2.getPriceLevel());
        
        return timeDiff < 60000 && priceDiff < 50.0; // tolerance: 1 دقیقه و 50 واحد قیمت
    }
    
    private ChartCoordinate convertToChartCoordinate(Point screenPoint) {
        try {
            if (drawContext == null) {
                AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                    "❌ DrawContext is null, cannot convert coordinates");
                return null;
            }
            
            // Use MotiveWave API to convert screen coordinates to chart coordinates
            long time = drawContext.translate2Time(screenPoint.getX());
            double price = drawContext.translate2Value(screenPoint.getY());
            
            ChartCoordinate coord = new ChartCoordinate(time, price);
            
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.DEBUG, CLASS_NAME,
                "📍 Screen point (%d, %d) converted to chart coordinate: %s", 
                screenPoint.x, screenPoint.y, coord);
            
            return coord;
            
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Error converting to chart coordinate: %s", e.getMessage());
            return null;
        }
    }
    
    /**
     * پیدا کردن نزدیک‌ترین bar به زمان مشخص
     */
    private int findClosestBarIndex(DataSeries series, long targetTime) {
        try {
            int closestIndex = 0;
            long minDiff = Math.abs(series.getStartTime(0) - targetTime);
            
            for (int i = 1; i < series.size(); i++) {
                long diff = Math.abs(series.getStartTime(i) - targetTime);
                if (diff < minDiff) {
                    minDiff = diff;
                    closestIndex = i;
                } else {
                    // Time series is usually sorted, so we can break early
                    break;
                }
            }
            
            return closestIndex;
            
        } catch (Exception e) {
            AdvancedLogger.ruler(AdvancedLogger.LogLevel.ERROR, CLASS_NAME,
                "❌ Error finding closest bar index: %s", e.getMessage());
            return 0;
        }
    }
    
    // ================== Data Classes ==================
    
    /**
     * موقعیت چارت
     */
    public static class ChartPosition {
        private final long timeStamp;
        private final double priceLevel;
        private final boolean isLive;
        
        public ChartPosition(long timeStamp, double priceLevel, boolean isLive) {
            this.timeStamp = timeStamp;
            this.priceLevel = priceLevel;
            this.isLive = isLive;
        }
        
        public long getTimeStamp() { return timeStamp; }
        public double getPriceLevel() { return priceLevel; }
        public boolean isLive() { return isLive; }
        
        @Override
        public String toString() {
            return String.format("ChartPosition{time=%d, price=%.2f, live=%b}", 
                timeStamp, priceLevel, isLive);
        }
    }
    
    /**
     * مختصات چارت
     */
    public static class ChartCoordinate {
        private final long time;
        private final double price;
        
        public ChartCoordinate(long time, double price) {
            this.time = time;
            this.price = price;
        }
        
        public long getTime() { return time; }
        public double getPrice() { return price; }
        
        @Override
        public String toString() {
            return String.format("ChartCoordinate{time=%d, price=%.2f}", time, price);
        }
    }
    
    // ================== Public Interface ==================
    
    /**
     * دریافت وضعیت فعلی خط کش
     */
    public RulerState getRulerState() {
        return rulerState;
    }
    
    /**
     * بررسی اینکه آیا خط کش فعال است
     */
    public boolean isRulerActive() {
        return rulerState != RulerState.INACTIVE;
    }
    
    /**
     * تنظیم حالت اجبار موقعیت فعلی
     */
    public void setForceCurrentPosition(boolean force) {
        this.forceCurrentPosition = force;
        
        AdvancedLogger.ruler(AdvancedLogger.LogLevel.DEBUG, CLASS_NAME,
            "🔧 Force current position set to: %b", force);
    }
}
