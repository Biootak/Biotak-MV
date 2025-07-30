package com.biotak.core;

import com.biotak.debug.AdvancedLogger;
import com.biotak.util.TimeframeUtil;
import com.biotak.util.OptimizedCalculations;
import com.motivewave.platform.sdk.common.BarSize;
import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.Instrument;

import java.util.Locale;

public class FractalCalculator {

    /**
     * Calculates the Structure (S), Pattern (P), and Trigger (T) values based on the current timeframe.
     * 
     * @param barSize The current chart's bar size
     * @param thValue The base TH value
     * @return An array containing [Structure value, Pattern value, Trigger value]
     */
    public static double[] calculateFractalValues(BarSize barSize, double thValue) {
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
    public static double calculateShortStep(double structureValue, double patternValue) {
        return (2 * structureValue) - patternValue;
    }
    
    /**
     * Calculates the Long Step (LS) value using the formula: LS = (3 * S) - (2 * P)
     * 
     * @param structureValue The Structure (S) value
     * @param patternValue The Pattern (P) value
     * @return The calculated Long Step value
     */
    public static double calculateLongStep(double structureValue, double patternValue) {
        return (3 * structureValue) - (2 * patternValue);
    }

    /**
     * Calculates the Average True Range (ATR) for the current timeframe.
     * 
     * @param series The data series
     * @return The ATR value
     */
    public static double calculateATR(DataSeries series) {
        // Get the appropriate ATR period for this timeframe
        int period = TimeframeUtil.getAtrPeriod(series.getBarSize());
        
        // Calculate ATR using the standard formula
        int size = series.size();
        if (size <= period) {
            AdvancedLogger.warn("FractalCalculator", "calculateATR", "Not enough data to calculate ATR. Need %d bars, but only have %d", period, size);
            return 0.0;
        }
        
        // Use optimized ATR calculation
        return OptimizedCalculations.calculateATROptimized(series, period);
    }
    
    /**
     * Calculates the "Live ATR" - the true range of the current bar
     */
    public static double calculateLiveATR(DataSeries series) {
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
     * Determines the appropriate pip multiplier for a given instrument.
     * 
     * @param instrument The trading instrument
     * @return The multiplier to convert from price to pips
     */
    public static double getPipMultiplier(Instrument instrument) {
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
     * Gets the pattern timeframe string representation (one fractal level down)
     * for the given timeframe.
     */
    public static String getPatternTimeframeString(BarSize barSize) {
        return TimeframeUtil.getPatternTimeframeString(barSize);
    }
    
    /**
     * Gets the trigger timeframe string representation (two fractal levels down)
     * for the given timeframe.
     */
    public static String getTriggerTimeframeString(BarSize barSize) {
        return TimeframeUtil.getTriggerTimeframeString(barSize);
    }
    
    /**
     * Gets the ATR period for the pattern timeframe (one level below current)
     */
    public static int getPatternAtrPeriod(BarSize barSize) {
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
    public static int getTriggerAtrPeriod(BarSize barSize) {
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
     * Logs a detailed table showing all calculations across different fractal timeframes
     */
    public static void logCalculationTable(DataSeries series, double thValue, double structureValue, 
                                    double patternValue, double triggerValue, double shortStep, 
                                    double longStep, double atrValue, double liveAtrValue,
                                    double pipMultiplier, long lastCalcTableLogTime, long LOG_INTERVAL_MS) {
        // Force log level to INFO for this method
        AdvancedLogger.LogLevel originalLevel = com.biotak.config.LoggingConfiguration.getCurrentLogLevel();
        AdvancedLogger.setLogLevel(AdvancedLogger.LogLevel.INFO);
        
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
            
            // Calculate TH values (in price) based on these percentages using live bid price
            double basePrice = series.getBidClose(series.size() - 1);
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
            AdvancedLogger.info("FractalCalculator", "logCalculationTable", sb.toString());
        } finally {
            // Restore the previous log level
            AdvancedLogger.setLogLevel(originalLevel);
        }
    }

    /**
     * Helper method to format timeframe strings with proper notation for seconds-based timeframes
     */
    public static String formatTimeframeString(BarSize barSize) {
        if (barSize == null) return "N/A";
        
        // Convert barSize to total minutes first
        int interval = barSize.getInterval();
        long totalMinutes;
        switch (barSize.getIntervalType()) {
            case SECOND:
                // keep seconds separately if less than 60
                if (interval < 60) return interval + "s";
                totalMinutes = interval / 60;
                break;
            case MINUTE: totalMinutes = interval; break;
            case HOUR:   totalMinutes = interval * 60L; break;
            case DAY:    totalMinutes = interval * 24L * 60L; break;
            case WEEK:   totalMinutes = interval * 7L * 24L * 60L; break;
            default:     totalMinutes = interval; break;
        }

        // Build human-readable composite string in y w d h m
        long minutes = totalMinutes;
        long years  = minutes / (365L*24*60); minutes %= 365L*24*60;
        long months = minutes / (30L*24*60); minutes %= 30L*24*60;
        long weeks  = minutes / (7L*24*60);  minutes %= 7L*24*60;
        long days   = minutes / (24*60);     minutes %= 24*60;
        long hours  = minutes / 60;          minutes %= 60;

        StringBuilder sb = new StringBuilder();
        if (years  > 0) sb.append(years ).append("Y ");
        if (months > 0) sb.append(months).append("M ");
        if (weeks  > 0) sb.append(weeks ).append("W ");
        if (days   > 0) sb.append(days  ).append("D ");
        if (hours  > 0) sb.append(hours ).append("H ");
        if (minutes> 0 || sb.length()==0) sb.append(minutes).append("m");
        return sb.toString().trim();
    }
} 