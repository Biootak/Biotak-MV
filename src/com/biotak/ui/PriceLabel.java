package com.biotak.ui;

import com.motivewave.platform.sdk.common.Coordinate;
import com.motivewave.platform.sdk.common.DrawContext;
import com.motivewave.platform.sdk.draw.Figure;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.Rectangle;

/**
 * Small figure to draw price text right next to the custom price point.
 */
public class PriceLabel extends Figure {
    private long time;
    private double price;
    private String text;
    private final Font font = new Font("Arial", Font.BOLD, 14);

    public void setData(long time, double price, String text) {
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
        int padding = 6;
        Rectangle gb = ctx.getBounds();
        int x = (int) (gb.getX() + gb.getWidth()/2 - textW/2); // center of chart
        int y = (int) (p.getY() - textH/2); // exactly on the line
        gc.setColor(new Color(160, 160, 160, 200)); // light gray translucent background
        gc.fillRoundRect(x - padding, y - textH, textW + 2 * padding, textH + padding, 10, 10);
        gc.setColor(new Color(100, 100, 100)); // dark gray border
        gc.drawRoundRect(x - padding, y - textH, textW + 2 * padding, textH + padding, 10, 10);
        gc.setColor(Color.BLACK); // black text for better readability
        gc.drawString(text, x, y);
    }

    @Override
    public boolean contains(double x, double y, DrawContext ctx) {
        return false;
    }
}