package com.biotak.util;

import com.motivewave.platform.sdk.common.BarSize;

/**
 * Display-only utilities that format values without changing behavior.
 * Extracted to keep BiotakTrigger smaller while preserving exact output.
 */
public final class DisplayUtil {
  private DisplayUtil() {}

  /**
   * Formats a BarSize to a short, user-facing label used in the ruler info box.
   * Behavior preserved from BiotakTrigger.formatTimeframeForDisplay:
   * - seconds: "Xs"
   * - minutes: "Mm"
   * - hours/days: keep standard format from TimeframeUtil (e.g., H1, D1)
   * - fallback: TimeframeUtil standard string
   */
  public static String formatTimeframeForDisplay(BarSize barSize) {
    if (barSize == null) return "-";

    if (TimeframeUtil.isSecondsBasedTimeframe(barSize)) {
      int seconds = barSize.getInterval();
      return seconds + "s";
    }

    String standardFormat = TimeframeUtil.getStandardTimeframeString(barSize);

    if (standardFormat.startsWith("M")) {
      String number = standardFormat.substring(1);
      return number + "m";
    } else if (standardFormat.startsWith("H")) {
      return standardFormat;
    } else if (standardFormat.startsWith("D")) {
      return standardFormat;
    }

    return standardFormat;
  }
}

