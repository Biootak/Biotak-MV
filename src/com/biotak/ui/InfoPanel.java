package com.biotak.ui;

import com.motivewave.platform.sdk.common.Coordinate;
import com.motivewave.platform.sdk.common.DrawContext;
import com.motivewave.platform.sdk.draw.Figure;
import com.motivewave.platform.sdk.common.FontInfo;
import com.motivewave.platform.sdk.common.PathInfo;
import com.motivewave.platform.sdk.common.Util;
import com.motivewave.platform.sdk.common.desc.FontDescriptor;
import com.motivewave.platform.sdk.draw.Line;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import com.biotak.enums.PanelPosition;
import com.biotak.util.PoolManager;
import com.biotak.util.StringUtils;

/**
 * Custom figure class to draw the information panel.
 */
public class InfoPanel extends Figure {
    // General Info
    private String timeframe;
    private com.motivewave.platform.sdk.common.Instrument instrument;
    private Font contentFont;
    private Font titleFont;
    private PanelPosition position;
    private int marginX, marginY, transparency;
    private boolean isSecondsBased; // Is the timeframe in seconds

    // Main Values
    private double thValue, shortStep, longStep, atrValue, liveAtrValue;

    // Fractal Hierarchy Values
    private String lowerPatternTF, lowerTriggerTF;
    private double lowerPatternTH, lowerTriggerTH;

    private String higherPatternTF, higherStructureTF;
    private double higherPatternTH, higherStructureTH;
    private boolean isMinimized;
    private Rectangle panelBounds;
    private Rectangle minimizeButtonRect; // Stores bounds of minimize/restore button
    private Rectangle rulerButtonRect; // Stores bounds of ruler toggle button
    // Added constant to control vertical padding after separator lines inside the panel
    private static final int SEPARATOR_PADDING = 25; // was previously 15 – gives text more breathing room
    
    // Cache for UI elements to avoid repeated creation
    private List<String> cachedCoreLines;
    private List<String> cachedHierarchyLines;
    private long lastCacheTime = 0;
    private static final long CACHE_DURATION_MS = 1000; // 1 second cache
    
    // Object pools removed - using centralized PoolManager instead
    
    public InfoPanel(String timeframe, double thValue, com.motivewave.platform.sdk.common.Instrument instrument, 
                    Font contentFont, Font titleFont, PanelPosition position, 
                    int marginX, int marginY, int transparency, 
                    double shortStep, double longStep, double atrValue, double liveAtrValue, boolean isSecondsBased, boolean isMinimized) {
        this.timeframe = timeframe;
        this.thValue = thValue;
        this.instrument = instrument;
        this.contentFont = contentFont;
        this.titleFont = titleFont;
        this.position = position;
        this.marginX = marginX;
        this.marginY = marginY;
        this.transparency = Math.max(0, Math.min(255, transparency));
        this.shortStep = shortStep;
        this.longStep = longStep;
        this.atrValue = atrValue;
        this.liveAtrValue = liveAtrValue;
        this.isSecondsBased = isSecondsBased;
        this.isMinimized = isMinimized;
    }
    
    public void setDownwardFractalInfo(String pattern, String trigger, double patternTH, double triggerTH) {
        this.lowerPatternTF = pattern;
        this.lowerTriggerTF = trigger;
        this.lowerPatternTH = patternTH;
        this.lowerTriggerTH = triggerTH;
    }
    
    public void setUpwardFractalInfo(String pattern, String structure, double patternTH, double structureTH) {
        this.higherPatternTF = pattern;
        this.higherStructureTF = structure;
        this.higherPatternTH = patternTH;
        this.higherStructureTH = structureTH;
    }

    public void setMinimized(boolean value) { this.isMinimized = value; }
    
    @Override
    public void draw(Graphics2D gc, DrawContext ctx) {
        // Save original settings
        Color origColor = gc.getColor();
        Font origFont = gc.getFont();
        Stroke origStroke = gc.getStroke();
        
        // Get chart bounds
        Rectangle bounds = ctx.getBounds();
        
        // Use cached content if available and recent
        long currentTime = System.currentTimeMillis();
        List<String> coreLines;
        List<String> hierarchyLines;
        
        if (cachedCoreLines != null && cachedHierarchyLines != null && 
            (currentTime - lastCacheTime) < CACHE_DURATION_MS) {
            // Use cached content
            coreLines = cachedCoreLines;
            hierarchyLines = cachedHierarchyLines;
        } else {
            // Generate new content and cache it
            coreLines = generateCoreLines();
            hierarchyLines = generateHierarchyLines();
            
            cachedCoreLines = coreLines;
            cachedHierarchyLines = hierarchyLines;
            lastCacheTime = currentTime;
        }

        // Calculate panel dimensions
        gc.setFont(titleFont);
        FontMetrics titleMetrics = gc.getFontMetrics();
        int titleHeight = titleMetrics.getHeight();
        int titleWidth = titleMetrics.stringWidth(timeframe);
        
        gc.setFont(contentFont);
        FontMetrics contentMetrics = gc.getFontMetrics();
        int contentLineHeight = contentMetrics.getHeight();
        int lineSpacing = 7; // Increased line spacing for better readability
        
        // Dynamically determine maximum width needed for the two-column core section
        int coreWidth = 0;
        for (int i = 0; i + 1 < coreLines.size(); i += 2) {
            int pairWidth = contentMetrics.stringWidth(coreLines.get(i)) + contentMetrics.stringWidth(coreLines.get(i + 1)) + 50;
            if (pairWidth > coreWidth) coreWidth = pairWidth;
        }
        // In case of an odd leftover (should not happen with current list size), include its width
        if (coreLines.size() % 2 == 1) {
            coreWidth = Math.max(coreWidth, contentMetrics.stringWidth(coreLines.get(coreLines.size() - 1)));
        }

        // Determine maximum width of hierarchy section as well
        int hierarchyWidth = 0;
        for (String line : hierarchyLines) {
            hierarchyWidth = Math.max(hierarchyWidth, contentMetrics.stringWidth(line));
        }

        int panelWidth = Math.max(coreWidth, hierarchyWidth) + 40; // Increased panel width

        // Adjusted padding after the separator using constant
        int coreRows = ((coreLines.size() + 1) / 2); // rows = pairs + possible leftover
        int coreSectionHeight = (coreRows * (contentLineHeight + lineSpacing)) + SEPARATOR_PADDING;
        int hierarchySectionHeight = isMinimized ? 0 : (hierarchyLines.size() * (contentLineHeight + lineSpacing)) + SEPARATOR_PADDING;
        int panelHeight = (titleHeight + SEPARATOR_PADDING) + coreSectionHeight + hierarchySectionHeight;

        // Calculate panel position
        int x, y;
        switch (position) {
            case TOP_LEFT: x = bounds.x + marginX; y = bounds.y + marginY; break;
            case TOP_RIGHT: x = bounds.x + bounds.width - panelWidth - marginX; y = bounds.y + marginY; break;
            case BOTTOM_LEFT: x = bounds.x + marginX; y = bounds.y + bounds.height - panelHeight - marginY; break;
            case CENTER: x = bounds.x + (bounds.width - panelWidth) / 2; y = bounds.y + (bounds.height - panelHeight) / 2; break;
            default: x = bounds.x + bounds.width - panelWidth - marginX; y = bounds.y + bounds.height - panelHeight - marginY; break;
        }
        
        this.panelBounds = new Rectangle(x, y, panelWidth, panelHeight);

        // Draw panel background (with transparency)
        Color panelBg = new Color(30, 30, 30, transparency);
        gc.setColor(panelBg);
        gc.fillRoundRect(x, y, panelWidth, panelHeight, 8, 8);
        // Draw minimize/restore button (top-right corner)
        int btnSize = 20; // enlarged for easier click
        int btnPadding = 4;
        int btnX = x + panelWidth - btnSize - btnPadding;
        int btnY = y + btnPadding;
        minimizeButtonRect = new Rectangle(btnX, btnY, btnSize, btnSize);
        // Button background hover state not tracked; draw simple grey box
        gc.setColor(Color.DARK_GRAY);
        gc.fillRect(btnX, btnY, btnSize, btnSize);
        gc.setColor(Color.WHITE);
        // Draw symbol: '-' if not minimized, '+' if minimized
        if (isMinimized) {
            gc.drawLine(btnX + 3, btnY + btnSize/2, btnX + btnSize - 3, btnY + btnSize/2);
            gc.drawLine(btnX + btnSize/2, btnY + 3, btnX + btnSize/2, btnY + btnSize - 3);
        } else {
            gc.drawLine(btnX + 3, btnY + btnSize/2, btnX + btnSize - 3, btnY + btnSize/2);
        }
        
        // Draw ruler toggle button (to the left of minimize button)
        int rulerBtnX = btnX - btnSize - btnPadding;
        int rulerBtnY = btnY;
        rulerButtonRect = new Rectangle(rulerBtnX, rulerBtnY, btnSize, btnSize);
        gc.setColor(Color.DARK_GRAY);
        gc.fillRect(rulerBtnX, rulerBtnY, btnSize, btnSize);
        gc.setColor(Color.WHITE);
        // Draw 'R' for Ruler
        gc.drawString("R", rulerBtnX + 6, rulerBtnY + 15);

        // Draw title (centered)
        gc.setFont(titleFont);
        gc.setColor(Color.WHITE);
        
        int currentY = y + titleHeight + 5;
        gc.drawString(timeframe, x + (panelWidth - titleWidth) / 2, currentY);
        
        currentY += 10; // Add space below the title before the separator
        
        // Draw sections without section titles
        drawSection(gc, x, currentY, panelWidth, "", coreLines, contentMetrics, contentFont, lineSpacing, true);
        if (!isMinimized) {
            currentY += coreSectionHeight;
            drawHierarchySection(gc, x, currentY, panelWidth, hierarchyLines, contentMetrics, contentFont, lineSpacing);
        }
        
        // Restore graphics settings
        gc.setColor(origColor);
        gc.setFont(origFont);
        gc.setStroke(origStroke);
    }

    public boolean isInMinimizeButton(double x, double y) {
        return minimizeButtonRect != null && minimizeButtonRect.contains(x, y);
    }
    
    public boolean isInRulerButton(double x, double y) {
        return rulerButtonRect != null && rulerButtonRect.contains(x, y);
    }
    
    /**
     * Generate core lines content (cached for performance)
     */
    private List<String> generateCoreLines() {
        ArrayList<String> coreLines = PoolManager.getStringList();
        StringBuilder sb = PoolManager.getStringBuilder();
        
        try {
            // Calculate Control (C) as the average of SS and LS => 7T.
            double controlValue = (shortStep + longStep) / 2.0; // C = (SS + LS) / 2
            
            // Pre-calculate pip values to avoid repeated conversions
            double thPip = com.biotak.util.UnitConverter.priceToPip(thValue, instrument);
            double atrPip = com.biotak.util.UnitConverter.priceToPip(atrValue, instrument);
            double ssPip = com.biotak.util.UnitConverter.priceToPip(shortStep, instrument);
            double lsPip = com.biotak.util.UnitConverter.priceToPip(longStep, instrument);
            double cPip = com.biotak.util.UnitConverter.priceToPip(controlValue, instrument);
            double mValue = shortStep + controlValue + longStep;
            double mPip = com.biotak.util.UnitConverter.priceToPip(mValue, instrument);
            double livePip = com.biotak.util.UnitConverter.priceToPip(liveAtrValue, instrument);
            
            // Use StringBuilder for efficient string building with optimized formatting
            sb.setLength(0);
            sb.append("TH: ").append(StringUtils.format1f(thPip));
            coreLines.add(sb.toString());
            
            sb.setLength(0);
            sb.append("ATR: ").append(StringUtils.format1f(atrPip));
            coreLines.add(sb.toString());
            
            sb.setLength(0);
            sb.append("SS: ").append(StringUtils.format1f(ssPip));
            coreLines.add(sb.toString());
            
            sb.setLength(0);
            sb.append("LS: ").append(StringUtils.format1f(lsPip));
            coreLines.add(sb.toString());
            
            sb.setLength(0);
            sb.append("C: ").append(StringUtils.format1f(cPip));
            coreLines.add(sb.toString());
            
            sb.setLength(0);
            sb.append("M: ").append(StringUtils.format1f(mPip));
            coreLines.add(sb.toString());
            
            sb.setLength(0);
            sb.append("Live: ").append(StringUtils.format1f(livePip));
            coreLines.add(sb.toString());
            
            // Return a copy to avoid pool interference
            return new ArrayList<>(coreLines);
        } finally {
            PoolManager.releaseStringBuilder(sb);
            PoolManager.releaseStringList(coreLines);
        }
    }
    
    /**
     * Generate hierarchy lines content (cached for performance)
     */
    private List<String> generateHierarchyLines() {
        ArrayList<String> hierarchyLines = PoolManager.getStringList();
        StringBuilder sb = PoolManager.getStringBuilder();
        
        try {
            if (higherStructureTF != null && !higherStructureTF.isEmpty()) {
                sb.setLength(0);
                double cVal = higherStructureTH * 1.75;
                double thPip = com.biotak.util.UnitConverter.priceToPip(higherStructureTH, instrument);
                double cPip = com.biotak.util.UnitConverter.priceToPip(cVal, instrument);
                sb.append("▲ S [").append(higherStructureTF).append("]: ")
                  .append(StringUtils.format1f(thPip)).append("  (C:")
                  .append(StringUtils.format1f(cPip)).append(")");
                hierarchyLines.add(sb.toString());
            }
            
            if (higherPatternTF != null && !higherPatternTF.isEmpty()) {
                sb.setLength(0);
                double cVal = higherPatternTH * 1.75;
                double thPip = com.biotak.util.UnitConverter.priceToPip(higherPatternTH, instrument);
                double cPip = com.biotak.util.UnitConverter.priceToPip(cVal, instrument);
                sb.append("▲ P [").append(higherPatternTF).append("]: ")
                  .append(StringUtils.format1f(thPip)).append("  (C:")
                  .append(StringUtils.format1f(cPip)).append(")");
                hierarchyLines.add(sb.toString());
            }

            // Current timeframe – include star marker
            sb.setLength(0);
            double cVal = thValue * 1.75;
            double thPip = com.biotak.util.UnitConverter.priceToPip(thValue, instrument);
            double cPip = com.biotak.util.UnitConverter.priceToPip(cVal, instrument);
            sb.append("■ [").append(timeframe).append("]: ")
              .append(StringUtils.format1f(thPip)).append("  (C:")
              .append(StringUtils.format1f(cPip)).append(") *");
            hierarchyLines.add(sb.toString());

            if (lowerPatternTF != null && !lowerPatternTF.isEmpty()) {
                sb.setLength(0);
                cVal = lowerPatternTH * 1.75;
                thPip = com.biotak.util.UnitConverter.priceToPip(lowerPatternTH, instrument);
                cPip = com.biotak.util.UnitConverter.priceToPip(cVal, instrument);
                sb.append("▼ P [").append(lowerPatternTF).append("]: ")
                  .append(StringUtils.format1f(thPip)).append("  (C:")
                  .append(StringUtils.format1f(cPip)).append(")");
                hierarchyLines.add(sb.toString());
            }
            
            if (lowerTriggerTF != null && !lowerTriggerTF.isEmpty()) {
                sb.setLength(0);
                cVal = lowerTriggerTH * 1.75;
                thPip = com.biotak.util.UnitConverter.priceToPip(lowerTriggerTH, instrument);
                cPip = com.biotak.util.UnitConverter.priceToPip(cVal, instrument);
                sb.append("▼ T [").append(lowerTriggerTF).append("]: ")
                  .append(StringUtils.format1f(thPip)).append("  (C:")
                  .append(StringUtils.format1f(cPip)).append(")");
                hierarchyLines.add(sb.toString());
            }
            
            // Return a copy to avoid pool interference
            return new ArrayList<>(hierarchyLines);
        } finally {
            PoolManager.releaseStringBuilder(sb);
            PoolManager.releaseStringList(hierarchyLines);
        }
    }

    private void drawHierarchySection(Graphics2D gc, int x, int y, int panelWidth, List<String> lines, 
                                     FontMetrics fm, Font font, int spacing) {
        int currentY = y;
        
        // Separator line
        gc.setColor(new Color(60, 60, 70, 200));
        gc.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f}, 0.0f));
        gc.drawLine(x + 10, currentY, x + panelWidth - 10, currentY);
        currentY += SEPARATOR_PADDING; // Increased padding after separator line
        
        // Section Content - Centered single-column layout with special handling for current timeframe
        gc.setFont(font);
        for (String line : lines) {
            // Extract the timeframe from the string
            String timeframeStr = extractTimeframeFromLine(line);
            
            // Check if this is the current timeframe (contains the * marker)
            boolean isCurrentTimeframe = line.contains("*");
            
            int lineWidth = fm.stringWidth(line);
            int textX = x + (panelWidth - lineWidth) / 2;
            
            // Only highlight the current timeframe in yellow
            if (isCurrentTimeframe) {
                // Use a very distinct background color for current timeframe
                int padding = 6;
                
                // Use yellow for current timeframe
                gc.setColor(new Color(120, 100, 0, 180)); // Dark gold background
                gc.fillRoundRect(textX - padding, currentY - fm.getAscent(), lineWidth + (padding * 2), fm.getHeight(), 8, 8);
                
                // Draw the text with a bright yellow color
                gc.setColor(new Color(255, 255, 0)); // Bright yellow for maximum visibility
                gc.drawString(line, textX, currentY);
            } else {
                // Check if the line contains an arrow symbol
                if (line.contains("▲")) {
                    // Draw the arrow in light green and the rest of the text in white
                    String beforeArrow = line.substring(0, line.indexOf("▲"));
                    String arrow = "▲";
                    String afterArrow = line.substring(line.indexOf("▲") + 1);
                    
                    int beforeWidth = fm.stringWidth(beforeArrow);
                    int arrowWidth = fm.stringWidth(arrow);
                    
                    // Draw the text before the arrow in white
                    gc.setColor(Color.WHITE);
                    gc.drawString(beforeArrow, textX, currentY);
                    
                    // Draw the arrow in light green
                    gc.setColor(new Color(144, 238, 144)); // Light green
                    gc.drawString(arrow, textX + beforeWidth, currentY);
                    
                    // Draw the text after the arrow in white
                    gc.setColor(Color.WHITE);
                    gc.drawString(afterArrow, textX + beforeWidth + arrowWidth, currentY);
                } 
                else if (line.contains("▼")) {
                    // Draw the arrow in red and the rest of the text in white
                    String beforeArrow = line.substring(0, line.indexOf("▼"));
                    String arrow = "▼";
                    String afterArrow = line.substring(line.indexOf("▼") + 1);
                    
                    int beforeWidth = fm.stringWidth(beforeArrow);
                    int arrowWidth = fm.stringWidth(arrow);
                    
                    // Draw the text before the arrow in white
                    gc.setColor(Color.WHITE);
                    gc.drawString(beforeArrow, textX, currentY);
                    
                    // Draw the arrow in red
                    gc.setColor(new Color(255, 99, 71)); // Tomato red
                    gc.drawString(arrow, textX + beforeWidth, currentY);
                    
                    // Draw the text after the arrow in white
                    gc.setColor(Color.WHITE);
                    gc.drawString(afterArrow, textX + beforeWidth + arrowWidth, currentY);
                }
                else {
                    // No arrows, draw the entire line in white
                    gc.setColor(Color.WHITE);
                    gc.drawString(line, textX, currentY);
                }
            }
            
            currentY += fm.getHeight() + spacing;
        }
    }
    
    /**
     * Extracts timeframe string from a line in the hierarchy panel
     */
    private String extractTimeframeFromLine(String line) {
        try {
            // Lines are formatted like: "▲ S [M1]: 6.7" or "■ [S16]: 1.0 *"
            int startBracket = line.indexOf('[');
            int endBracket = line.indexOf(']');
            
            if (startBracket >= 0 && endBracket > startBracket) {
                return line.substring(startBracket + 1, endBracket);
            }
        } catch (Exception e) {
            // In case of any parsing error, return null
        }
        return null;
    }

    private void drawSection(Graphics2D gc, int x, int y, int panelWidth, String title, List<String> lines, FontMetrics fm, Font font, int spacing, boolean isTwoColumn) {
        int currentY = y;
        
        // Separator line
        gc.setColor(new Color(60, 60, 70, 200));
        gc.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f}, 0.0f));
        gc.drawLine(x + 10, currentY, x + panelWidth - 10, currentY);
        currentY += SEPARATOR_PADDING; // Increased padding after separator line
        
        // Section Content
        gc.setColor(Color.WHITE);
        gc.setFont(font);
        if (isTwoColumn) {
            int i = 0;
            for (; i + 1 < lines.size(); i += 2) {
                String left  = lines.get(i);
                String right = lines.get(i + 1);
                int leftWidth  = fm.stringWidth(left);
                int rightWidth = fm.stringWidth(right);
                int leftX  = x + (panelWidth / 2 - leftWidth)  / 2;
                int rightX = x + panelWidth / 2 + (panelWidth / 2 - rightWidth) / 2;
                gc.drawString(left , leftX , currentY);
                gc.drawString(right, rightX, currentY);
                currentY += fm.getHeight() + spacing;
            }
            // If there is an unpaired line left (odd count), center it
            if (i < lines.size()) {
                String lone = lines.get(i);
                int loneWidth = fm.stringWidth(lone);
                gc.drawString(lone, x + (panelWidth - loneWidth) / 2, currentY);
            }
        } else {
            // Centered single-column layout
            for(String line : lines) {
                int lineWidth = fm.stringWidth(line);
                gc.drawString(line, x + (panelWidth - lineWidth) / 2, currentY);
                currentY += fm.getHeight() + spacing;
            }
        }
    }
    
    @Override
    public boolean contains(double x, double y, DrawContext ctx) { 
        return panelBounds != null && panelBounds.contains(x, y);
    }
}