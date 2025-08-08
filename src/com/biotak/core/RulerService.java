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
        int maxIterations = 50;
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
          if (Math.abs(mPips - legPip) <= 0.05) break;
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
    double bestATRAboveDiff = Double.MAX_VALUE, bestATRBelowDiff = Double.MAX_VALUE;
    String bestATRAboveLabel = null, bestATRBelowLabel = null;
    double bestATRAbovePips = 0, bestATRBelowPips = 0;

    if (atrMapLocal != null && !atrMapLocal.isEmpty()) {
      for (var entry : atrMapLocal.entrySet()) {
        String lbl = entry.getKey();
        double atrPrice = entry.getValue();
        if (atrPrice <= 0) continue;
        double atrPips = Math.round((atrPrice / tick) * 10.0) / 10.0; // preserve original mid-step behavior
        if (atrPips >= legPip) {
          double diff = atrPips - legPip;
          if (diff < bestATRAboveDiff) { bestATRAboveDiff = diff; bestATRAboveLabel = lbl; bestATRAbovePips = atrPips; }
        } else {
          double diff = legPipsDiff(legPip, atrPips);
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
      bestATRLabel = (bestATRBelowLabel != null ? bestATRBelowLabel : "-");
      bestATRBasePips = bestATRBelowPips; bestATRDiff = bestATRBelowDiff;
    }

    // Refine with binary search using continuous scaling
    if (bestATRDiff > 0.05 && bestATRAboveLabel != null && bestATRBelowLabel != null) {
      int lowMin = TimeframeUtil.parseCompoundTimeframe(bestATRBelowLabel);
      int highMin = TimeframeUtil.parseCompoundTimeframe(bestATRAboveLabel);
      int baseMin = atrStructureMin;
      double baseAtrPrice = atrStructurePrice; // 1× ATR price

      if (lowMin > 0 && highMin > lowMin && baseMin > 0 && baseAtrPrice > 0) {
        int maxIterations = 50;
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
          double atrPriceMid = com.biotak.util.Constants.ATR_FACTOR * baseAtrPrice * Math.sqrt((double) mid / baseMin);
          double atrPipsMid = Math.round((atrPriceMid / tick) * 10.0) / 10.0; // preserve mid-step behavior

          if (atrPipsMid >= legPip) {
            highMin = mid;
            bestAboveDiffRef = atrPipsMid - legPip;
            bestAbovePipsRef = atrPipsMid;
            bestAboveLabelRef = compoundTimeframe(mid);
          } else {
            lowMin = mid;
            bestBelowDiffRef = legPipsDiff(legPip, atrPipsMid);
            bestBelowPipsRef = atrPipsMid;
            bestBelowLabelRef = compoundTimeframe(mid);
          }

          if (Math.abs(atrPipsMid - legPip) <= 0.05) break; // stop when ≤0.05 pip
        }

        if (bestAboveDiffRef < bestBelowDiffRef) {
          bestATRLabel = bestAboveLabelRef; bestATRBasePips = bestAbovePipsRef; bestATRDiff = bestAboveDiffRef;
        } else {
          bestATRLabel = bestBelowLabelRef; bestATRBasePips = bestBelowPipsRef; bestATRDiff = bestBelowDiffRef;
        }
      }
    }

    return new ATRResult(bestATRLabel, bestATRBasePips, bestATRDiff);
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

