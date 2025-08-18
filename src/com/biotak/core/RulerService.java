package com.biotak.core;

import com.biotak.util.TimeframeUtil;
import com.biotak.util.UnitConverter;
import com.biotak.util.OptimizedCalculations;
import com.motivewave.platform.sdk.common.Instrument;
import java.util.Map;

/**
 * Service for ruler-related computations (behavior-preserving extraction).
 * Contains matching logic for M values and ATR×3 against a measured leg in pips.
 */
public final class RulerService {
  private RulerService() {}

  public static record MResult(String bestLabel, double bestBasePips, double bestDiff) {}
  public static record ATRResult(String bestLabel, double bestBasePips, double bestDiff) {}
  public static record StepResult(String bestLabel, double bestBasePips, double bestDiff) {}

  public static MResult matchM(
      Instrument instrument,
      double legPip,
      double tick,
      double liveBidPrice,
      Map<String, Double> mMapLocal,
      double thToMFactor
  ) {
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
        double basePip = Math.round(UnitConverter.priceToPip(baseMove, instrument) * 10.0) / 10.0;
        if (basePip >= legPip) {
          double diff = basePip - legPip;
          if (diff < bestAboveDiff) {
            bestAboveDiff = diff;
            bestAboveLabel = label;
            bestAbovePips = basePip;
          }
        } else {
          double diff = legPip - basePip;
          if (diff < bestBelowDiff) {
            bestBelowDiff = diff;
            bestBelowLabel = label;
            bestBelowPips = basePip;
          }
        }
      }
    }

    String bestLabel;
    double bestBasePips;
    double bestDiff;

    if (bestAboveLabel != null) {
      bestLabel = bestAboveLabel;
      bestBasePips = bestAbovePips;
      bestDiff = bestAboveDiff;
    } else {
      bestLabel = (bestBelowLabel != null ? bestBelowLabel : "-");
      bestBasePips = bestBelowPips;
      bestDiff = bestBelowDiff;
    }

    // Binary refine if necessary (mirror original logic)
    if (bestDiff > 0.1 && bestAboveLabel != null && bestBelowLabel != null) {
      int lowMin = TimeframeUtil.parseCompoundTimeframe(bestBelowLabel);
      int highMin = TimeframeUtil.parseCompoundTimeframe(bestAboveLabel);
      if (lowMin > 0 && highMin > lowMin) {
        int maxIterations = 100;
        int iteration = 0;
        double bestAboveDiffRef = bestAboveDiff;
        double bestBelowDiffRef = bestBelowDiff;
        String bestAboveLabelRef = bestAboveLabel;
        String bestBelowLabelRef = bestBelowLabel;
        double bestAbovePipsRef = bestAbovePips;
        double bestBelowPipsRef = bestBelowPips;

        while (highMin - lowMin > 1 && iteration < maxIterations) {
          iteration++;
          int mid = (lowMin + highMin) / 2;
          double perc = TimeframeUtil.getTimeframePercentageFromMinutes(mid);
          double thPts = OptimizedCalculations.calculateTHPoints(instrument, liveBidPrice, perc) * tick;
          double mVal = thToMFactor * thPts;
          double mPips = Math.round(UnitConverter.priceToPip(mVal, instrument) * 10.0) / 10.0;
          if (mPips >= legPip) {
            highMin = mid;
            bestAboveDiffRef = mPips - legPip;
            bestAbovePipsRef = mPips;
            bestAboveLabelRef = compoundTimeframe(mid);
          } else {
            lowMin = mid;
            bestBelowDiffRef = legPipsDiff(legPip, mPips);
            bestBelowPipsRef = mPips;
            bestBelowLabelRef = compoundTimeframe(mid);
          }
          if (Math.abs(mPips - legPip) <= 0.01) break;
        }

        // Choose closest after refine
        if (bestAboveDiffRef < bestBelowDiffRef) {
          bestLabel = bestAboveLabelRef;
          bestBasePips = bestAbovePipsRef;
          bestDiff = bestAboveDiffRef;
        } else {
          bestLabel = bestBelowLabelRef;
          bestBasePips = bestBelowPipsRef;
          bestDiff = bestBelowDiffRef;
        }
      }
    }

    return new MResult(bestLabel, bestBasePips, bestDiff);
  }

  public static ATRResult matchATR(
      double legPip,
      double tick,
      Map<String, Double> atrMapLocal,
      int atrStructureMin,
      double atrStructurePrice
  ) {
    return matchATRWithInstrument(legPip, tick, atrMapLocal, atrStructureMin, atrStructurePrice, null);
  }
  
  public static ATRResult matchATRWithInstrument(
      double legPip,
      double tick,
      Map<String, Double> atrMapLocal,
      int atrStructureMin,
      double atrStructurePrice,
      Instrument instrument
  ) {
    // Debug logging for ATR matching
    com.biotak.debug.AdvancedLogger.debug("RulerService", "matchATRWithInstrument", 
        "Starting ATR matching: legPip=%.2f, structureMin=%d, structurePrice=%.5f, mapSize=%d", 
        legPip, atrStructureMin, atrStructurePrice, atrMapLocal != null ? atrMapLocal.size() : 0);
    
    double bestATRAboveDiff = Double.MAX_VALUE, bestATRBelowDiff = Double.MAX_VALUE;
    String bestATRAboveLabel = null, bestATRBelowLabel = null;
    double bestATRAbovePips = 0, bestATRBelowPips = 0;

    if (atrMapLocal != null && !atrMapLocal.isEmpty()) {
        com.biotak.debug.AdvancedLogger.debug("RulerService", "matchATRWithInstrument", 
            "ATR Map contains 3×ATR values. Looking for ATR that when ×3 equals %.2f pips", legPip);
        int count = 0;
        for (var entry : atrMapLocal.entrySet()) {
            String lbl = entry.getKey();
            double atr3xPrice = entry.getValue(); // This is already 3×ATR price
            if (atr3xPrice <= 0) continue;
            
            // Convert 3×ATR price to pips
            double atr3xPips;
            if (instrument != null) {
                atr3xPips = Math.round(UnitConverter.priceToPip(atr3xPrice, instrument) * 100.0) / 100.0;
            } else {
                // Fallback to old calculation if instrument is null
                atr3xPips = Math.round((atr3xPrice / tick) * 100.0) / 100.0;
            }
            
            // The base ATR (before 3x multiplication) in pips
            double baseATRPips = atr3xPips / 3.0;
            
            // Log first few entries for debugging
            if (count < 5) {
                com.biotak.debug.AdvancedLogger.debug("RulerService", "matchATRWithInstrument", 
                    "  %s: 3×ATR=%.2f pips, base ATR=%.2f pips, leg=%.2f pips, diff when ×3=%.2f", 
                    lbl, atr3xPips, baseATRPips, legPip, Math.abs(atr3xPips - legPip));
                count++;
            }
            
            // Compare 3×ATR against leg size (since we want ATR×3 to match leg)
            if (atr3xPips >= legPip) {
                double diff = atr3xPips - legPip;
                if (diff < bestATRAboveDiff) { 
                    bestATRAboveDiff = diff; 
                    bestATRAboveLabel = lbl; 
                    bestATRAbovePips = baseATRPips; // Store base ATR, not 3×ATR
                    com.biotak.debug.AdvancedLogger.debug("RulerService", "matchATRWithInstrument", 
                        "New best above: %s (base ATR=%.2f pips, 3×ATR=%.2f pips, diff=%.2f)", 
                        lbl, baseATRPips, atr3xPips, diff);
                }
            } else {
                double diff = legPipsDiff(legPip, atr3xPips);
                if (diff < bestATRBelowDiff) { 
                    bestATRBelowDiff = diff; 
                    bestATRBelowLabel = lbl; 
                    bestATRBelowPips = baseATRPips; // Store base ATR, not 3×ATR
                    com.biotak.debug.AdvancedLogger.debug("RulerService", "matchATRWithInstrument", 
                        "New best below: %s (base ATR=%.2f pips, 3×ATR=%.2f pips, diff=%.2f)", 
                        lbl, baseATRPips, atr3xPips, diff);
                }
            }
        }
    }

    String bestATRLabel;
    double bestATRBasePips;
    double bestATRDiff;

    // Choose the best match based on smallest absolute difference
    boolean useAbove = false;
    if (bestATRAboveLabel != null && bestATRBelowLabel != null) {
        // Both candidates exist - choose the one with smaller diff
        useAbove = bestATRAboveDiff <= bestATRBelowDiff;
        com.biotak.debug.AdvancedLogger.debug("RulerService", "matchATRWithInstrument", 
            "Both above and below candidates exist. Choosing %s (above diff=%.2f, below diff=%.2f)", 
            useAbove ? "above" : "below", bestATRAboveDiff, bestATRBelowDiff);
    } else if (bestATRAboveLabel != null) {
        useAbove = true;
        com.biotak.debug.AdvancedLogger.debug("RulerService", "matchATRWithInstrument", 
            "Only above candidate exists: %s (diff=%.2f)", bestATRAboveLabel, bestATRAboveDiff);
    } else {
        useAbove = false;
        com.biotak.debug.AdvancedLogger.debug("RulerService", "matchATRWithInstrument", 
            "Only below candidate exists: %s (diff=%.2f)", 
            bestATRBelowLabel != null ? bestATRBelowLabel : "null", bestATRBelowDiff);
    }

    if (useAbove) {
        bestATRLabel = bestATRAboveLabel; 
        bestATRBasePips = bestATRAbovePips; 
        bestATRDiff = bestATRAboveDiff;
    } else {
        bestATRLabel = (bestATRBelowLabel != null ? bestATRBelowLabel : "-");
        bestATRBasePips = bestATRBelowPips; 
        bestATRDiff = bestATRBelowDiff;
    }

    // Refine with binary search using continuous scaling
    if (bestATRDiff > 0.01 && bestATRAboveLabel != null && bestATRBelowLabel != null) {
      int lowMin = TimeframeUtil.parseCompoundTimeframe(bestATRBelowLabel);
      int highMin = TimeframeUtil.parseCompoundTimeframe(bestATRAboveLabel);
      int baseMin = atrStructureMin;
      double baseAtrPrice = atrStructurePrice; // 1× ATR price

      if (lowMin > 0 && highMin > lowMin && baseMin > 0 && baseAtrPrice > 0) {
        int maxIterations = 100;
        int iteration = 0;
        double bestAboveDiffRef = bestATRAboveDiff;
        double bestBelowDiffRef = bestATRBelowDiff;
        String bestAboveLabelRef = bestATRAboveLabel;
        String bestBelowLabelRef = bestATRBelowLabel;
        double bestAbovePipsRef = bestATRAbovePips;
        double bestBelowPipsRef = bestATRBelowPips;

        while (highMin - lowMin > 1 && iteration < maxIterations) {
          iteration++;
          int mid = (lowMin + highMin) / 2;
          
          // Calculate base ATR (1×ATR) for this timeframe
          double baseATRPriceMid = baseAtrPrice * Math.sqrt((double) mid / baseMin);
          
          // Calculate 3×ATR price (what we actually compare against leg)
          double atr3xPriceMid = 3.0 * baseATRPriceMid;
          
          // Convert 3×ATR to pips
          double atr3xPipsMid;
          if (instrument != null) {
            atr3xPipsMid = Math.round(UnitConverter.priceToPip(atr3xPriceMid, instrument) * 100.0) / 100.0;
          } else {
            atr3xPipsMid = Math.round((atr3xPriceMid / tick) * 100.0) / 100.0; // fallback
          }
          
          // Base ATR (1×) in pips for return value
          double baseATRPipsMid = atr3xPipsMid / 3.0;
          
          double diffMid = Math.abs(atr3xPipsMid - legPip);

          // Compare 3×ATR against leg (since we want ATR×3 to match leg)
          if (atr3xPipsMid >= legPip) {
            highMin = mid;
            bestAboveDiffRef = atr3xPipsMid - legPip;
            bestAbovePipsRef = baseATRPipsMid; // Store base ATR, not 3×ATR
            bestAboveLabelRef = compoundTimeframe(mid);
          } else {
            lowMin = mid;
            bestBelowDiffRef = legPipsDiff(legPip, atr3xPipsMid);
            bestBelowPipsRef = baseATRPipsMid; // Store base ATR, not 3×ATR
            bestBelowLabelRef = compoundTimeframe(mid);
          }

          if (diffMid <= 0.01) {
            break; // stop when ≤0.01 pip
          }
        }

        if (bestAboveDiffRef < bestBelowDiffRef) {
          bestATRLabel = bestAboveLabelRef; bestATRBasePips = bestAbovePipsRef; bestATRDiff = bestAboveDiffRef;
        } else {
          bestATRLabel = bestBelowLabelRef; bestATRBasePips = bestBelowPipsRef; bestATRDiff = bestBelowDiffRef;
        }
      }
    }

    // Final result logging
    com.biotak.debug.AdvancedLogger.debug("RulerService", "matchATRWithInstrument", 
        "ATR matching complete: bestLabel=%s, bestPips=%.2f, bestDiff=%.2f (above: %s/%.2f/%.2f, below: %s/%.2f/%.2f)", 
        bestATRLabel, bestATRBasePips, bestATRDiff,
        bestATRAboveLabel != null ? bestATRAboveLabel : "null", bestATRAbovePips, bestATRAboveDiff,
        bestATRBelowLabel != null ? bestATRBelowLabel : "null", bestATRBelowPips, bestATRBelowDiff);
    
    return new ATRResult(bestATRLabel, bestATRBasePips, bestATRDiff);
  }

  /**
   * Generic method to match against any step value type (E, TP, TH, SS, LS)
   */
  public static StepResult matchStepValues(
      Instrument instrument,
      double legPip,
      double tick,
      double liveBidPrice,
      Map<String, Double> stepValuesMap,
      String stepTypeName
  ) {
    double bestAboveDiff = Double.MAX_VALUE;
    String bestAboveLabel = null;
    double bestAbovePips = 0;

    double bestBelowDiff = Double.MAX_VALUE;
    String bestBelowLabel = null;
    double bestBelowPips = 0;

    if (stepValuesMap != null && !stepValuesMap.isEmpty()) {
      for (var entry : stepValuesMap.entrySet()) {
        String label = entry.getKey();
        double baseMove = entry.getValue();
        if (baseMove <= 0) continue;
        double basePip = Math.round(UnitConverter.priceToPip(baseMove, instrument) * 10.0) / 10.0;
        if (basePip >= legPip) {
          double diff = basePip - legPip;
          if (diff < bestAboveDiff) {
            bestAboveDiff = diff;
            bestAboveLabel = label;
            bestAbovePips = basePip;
          }
        } else {
          double diff = legPip - basePip;
          if (diff < bestBelowDiff) {
            bestBelowDiff = diff;
            bestBelowLabel = label;
            bestBelowPips = basePip;
          }
        }
      }
    }

    String bestLabel;
    double bestBasePips;
    double bestDiff;

    if (bestAboveLabel != null) {
      bestLabel = bestAboveLabel;
      bestBasePips = bestAbovePips;
      bestDiff = bestAboveDiff;
    } else {
      bestLabel = (bestBelowLabel != null ? bestBelowLabel : "-");
      bestBasePips = bestBelowPips;
      bestDiff = bestBelowDiff;
    }

    // Binary refine if necessary (similar to M matching logic)
    if (bestDiff > 0.1 && bestAboveLabel != null && bestBelowLabel != null) {
      int lowMin = TimeframeUtil.parseCompoundTimeframe(bestBelowLabel);
      int highMin = TimeframeUtil.parseCompoundTimeframe(bestAboveLabel);
      if (lowMin > 0 && highMin > lowMin) {
        int maxIterations = 100;
        int iteration = 0;
        double bestAboveDiffRef = bestAboveDiff;
        double bestBelowDiffRef = bestBelowDiff;
        String bestAboveLabelRef = bestAboveLabel;
        String bestBelowLabelRef = bestBelowLabel;
        double bestAbovePipsRef = bestAbovePips;
        double bestBelowPipsRef = bestBelowPips;

        while (highMin - lowMin > 1 && iteration < maxIterations) {
          iteration++;
          int mid = (lowMin + highMin) / 2;
          double perc = TimeframeUtil.getTimeframePercentageFromMinutes(mid);
          double thPts = OptimizedCalculations.calculateTHPoints(instrument, liveBidPrice, perc) * tick;
          
          // Calculate step value based on type
          double stepVal = calculateStepValueForTimeframe(thPts, stepTypeName);
          double stepPips = Math.round(UnitConverter.priceToPip(stepVal, instrument) * 10.0) / 10.0;
          
          if (stepPips >= legPip) {
            highMin = mid;
            bestAboveDiffRef = stepPips - legPip;
            bestAbovePipsRef = stepPips;
            bestAboveLabelRef = compoundTimeframe(mid);
          } else {
            lowMin = mid;
            bestBelowDiffRef = legPipsDiff(legPip, stepPips);
            bestBelowPipsRef = stepPips;
            bestBelowLabelRef = compoundTimeframe(mid);
          }
          if (Math.abs(stepPips - legPip) <= 0.01) break;
        }

        // Choose closest after refine
        if (bestAboveDiffRef < bestBelowDiffRef) {
          bestLabel = bestAboveLabelRef;
          bestBasePips = bestAbovePipsRef;
          bestDiff = bestAboveDiffRef;
        } else {
          bestLabel = bestBelowLabelRef;
          bestBasePips = bestBelowPipsRef;
          bestDiff = bestBelowDiffRef;
        }
      }
    }

    return new StepResult(bestLabel, bestBasePips, bestDiff);
  }

  /**
   * Calculate step value based on TH and step type
   */
  private static double calculateStepValueForTimeframe(double thValue, String stepType) {
    return switch (stepType.toUpperCase()) {
      case "E" -> thValue * 0.75; // E = 0.75 * TH
      case "TP" -> thValue * 0.75 * 3.0; // TP = 3 * E = 3 * 0.75 * TH
      case "TH" -> thValue; // TH = TH
      case "SS" -> thValue * 1.5; // SS = 1.5 * TH  
      case "LS" -> thValue * 2.0; // LS = 2.0 * TH
      default -> thValue; // Default to TH
    };
  }

  private static double legPipsDiff(double leg, double value) { return leg - value; }

  private static String compoundTimeframe(long minutes) {
    long hrs = minutes / 60;
    long rem = minutes % 60;
    if (hrs > 0) {
      if (rem > 0) return hrs + "H" + rem + "m";
      else return hrs + "H";
    } else {
      return minutes + "m";
    }
  }
}

