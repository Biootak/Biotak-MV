package com.biotak.ui;

import com.motivewave.platform.sdk.common.Coordinate;
import com.motivewave.platform.sdk.common.DrawContext;
import com.motivewave.platform.sdk.draw.Figure;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

/**
 * Simple label figure used for Control-Step level names (P, S, SS, C, LS).
 */
public class LevelLabel extends Figure {
    private long time;
    private double price;
    private String text;
    private final Font font = new Font("Arial", Font.BOLD, 11);

    public LevelLabel(long time, double price, String text) {
        this.time = time;
        this.price = price;
        this.text = text;
    }

    @Override
    public void draw(Graphics2D gc, DrawContext ctx) {
        if (time == 0 || text == null) return;
        Point2D p = ctx.translate(new Coordinate(time, price));
        gc.setFont(font);
        FontMetrics fm = gc.getFontMetrics();
        int textW = fm.stringWidth(text);
        int textH = fm.getAscent();
        gc.setColor(new Color(0,0,0,180));
        gc.fillRoundRect((int)p.getX()+4, (int)(p.getY()-textH/2-2), textW+6, textH+4, 8, 8);
        gc.setColor(Color.WHITE);
        gc.drawString(text, (int)p.getX()+7, (int)(p.getY()+textH/2-2));
    }

    @Override
    public boolean contains(double x, double y, DrawContext ctx) { return false; }
} 