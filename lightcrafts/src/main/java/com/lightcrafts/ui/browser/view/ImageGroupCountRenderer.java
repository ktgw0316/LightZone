/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.ui.browser.model.ImageDatum;
import com.lightcrafts.ui.browser.model.ImageGroup;
import com.lightcrafts.ui.LightZoneSkin;

import java.awt.*;
import java.awt.geom.*;

/**
 * Draw a small label against a round rectangular background to indicate the
 * number of ImageDatums in an ImageGroup.
 */
class ImageGroupCountRenderer {

    // Inset from the left edge
    static int HInset = 4;

    // Inset from the top edge
    static int VInset = 12;

    // Roundness of the roundrect background
    static int ArcRadius = 8;

    static Color TextColor = LightZoneSkin.Colors.BrowserLabelForeground;

    static Color BackColor = LightZoneSkin.Colors.BrowserImageTypeLabelBackground;

    static Color BorderColor = Color.lightGray;

    static void paint(Graphics2D g, Rectangle2D rect, ImageDatum datum) {
        ImageGroup group = datum.getGroup();
        int count = group.getImageDatums().size();
        String text = Integer.toString(count);
        paint(g, rect, text);
    }

    static void paint(Graphics2D g, Rectangle2D rect, String text) {
        // Find where the text baseline starts
        Point2D textOrigin = getTextOrigin(rect, g, text);

        // Find the text bounds
        FontMetrics metrics = g.getFontMetrics();
        Rectangle2D textBounds = metrics.getStringBounds(text, g);

        // Translate the text bounds to the place where text will be rendered
        double x = textOrigin.getX();
        double y = textOrigin.getY();
        textBounds = new Rectangle2D.Double(
            x + textBounds.getX(),
            y + textBounds.getY(),
            textBounds.getWidth(),
            textBounds.getHeight()
        );
        // Expand the text rectangle to make a background round rectangle
        RoundRectangle2D background = new RoundRectangle2D.Double(
            textBounds.getX() - ArcRadius / 2,
            textBounds.getY() - ArcRadius / 2,
            textBounds.getWidth() + ArcRadius,
            textBounds.getHeight() + ArcRadius,
            ArcRadius, ArcRadius
        );
        Color oldColor = g.getColor();

        // Draw the background's under rectangle, representing a "stack"
        AffineTransform xform = AffineTransform.getTranslateInstance(-2, -2);
        Area stack = new Area(xform.createTransformedShape(background));
        stack.subtract(new Area(background));
        g.setColor(BackColor);
        g.fill(stack);
        g.setColor(BorderColor);
        g.draw(stack);

        // Draw the background's over rectangle
        g.setColor(BackColor);
        g.fill(background);
        g.setColor(BorderColor);
        g.draw(background);

        g.setColor(TextColor);
        g.drawString(text, (float) x, (float) y);

        g.setColor(oldColor);
    }
    
    // Get coordinates where the text origin should go.
    // This is the start of the basline, suitable for g.drawString().
    private static Point2D getTextOrigin(
        Rectangle2D rect, Graphics2D g, String text
    ) {
        Point2D ul = getUpperLeft(rect);
        FontMetrics metrics = g.getFontMetrics();
        Rectangle2D bounds = metrics.getStringBounds(text, g);
        double x = ul.getX() + bounds.getX();
        double y = ul.getY() + bounds.getY() + bounds.getHeight();
        return new Point2D.Double(x, y);
    }

    // Get coordinates where the upper-left corner of the text's bounding
    // rectangle should go.
    private static Point2D getUpperLeft(Rectangle2D rect) {
        double x = rect.getX() + HInset + ArcRadius;
        double y = rect.getY() + VInset + ArcRadius;
        return new Point2D.Double(x, y);
    }
}
