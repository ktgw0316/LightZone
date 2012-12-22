/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.ui.LightZoneSkin;

import java.awt.*;
import java.awt.geom.*;

/**
 * Draw a number next and a star to indicate an image's rating.
 */
class ImageDatumRatingRenderer {

    static Color TextColor = LightZoneSkin.Colors.BrowserLabelForeground;

    static Color BackColor = LightZoneSkin.Colors.BrowserImageTypeLabelBackground;

    // Inset from the left edge
    static int HInset = 4;

    // Inset from the bottom edge
    static int VInset = 28;

    // Roundness of the roundrect background
    static int ArcRadius = 8;

    // Characteristic size of the little star
    static int StarSize = 12;

    // Between the rating number and the star
    static int TextStarGap = 2;

    // The background and the little star
    static GeneralPath StarShape = new GeneralPath();

    // The StarShape is StarSize in diameter and centered at the origin
    static {
        double bigRadius = .5 * StarSize;
        double smallRadius = .2 * StarSize;
        double angle = - Math.PI / 2;
        double x = bigRadius * Math.cos(angle);
        double y = bigRadius * Math.sin(angle);
        StarShape.moveTo((float) x, (float) y);
        for (int n=1; n<=5; n++) {
            angle += Math.PI / 5;
            x = smallRadius * Math.cos(angle);
            y = smallRadius * Math.sin(angle);
            StarShape.lineTo((float) x, (float) y);
            angle += Math.PI / 5;
            x = bigRadius * Math.cos(angle);
            y = bigRadius * Math.sin(angle);
            StarShape.lineTo((float) x, (float) y);
        }
        StarShape.closePath();
    }

    static void paint(Graphics2D g, Rectangle2D rect, int rating) {
        String text = Integer.toString(rating);
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
        // Expand the text rectangle to make a background round rectangle,
        // with extra room for the star
        RoundRectangle2D background = new RoundRectangle2D.Double(
            textBounds.getX() - ArcRadius / 2,
            textBounds.getY() - ArcRadius / 2,
            textBounds.getWidth() + ArcRadius + StarSize,
            textBounds.getHeight() + ArcRadius,
            ArcRadius, ArcRadius
        );
        Color oldColor = g.getColor();

        g.setColor(BackColor);
        g.fill(background);

        g.setColor(TextColor);
        g.drawString(text, (float) x, (float) y);

        // The star goes right of the text
        AffineTransform xform = AffineTransform.getTranslateInstance(
            textBounds.getX() + textBounds.getWidth() +
                StarSize / 2 + TextStarGap,
            textBounds.getY() + textBounds.getHeight() / 2
        );
        Shape star = xform.createTransformedShape(StarShape);
        g.fill(star);
        
        g.setColor(oldColor);
    }
    
    // Get coordinates where the text origin should go.
    // This is the start of the basline, suitable for g.drawString().
    private static Point2D getTextOrigin(
        Rectangle2D rect, Graphics2D g, String text
    ) {
        Point2D ll = getLowerLeft(rect);
        FontMetrics metrics = g.getFontMetrics();
        Rectangle2D bounds = metrics.getStringBounds(text, g);
        double x = ll.getX() + bounds.getX();
        double y = ll.getY() - bounds.getHeight() - bounds.getY();
        return new Point2D.Double(x, y);
    }

    // Get coordinates where the lower-left corner of the rating number's
    // bounding rectangle should go.
    static Point2D getLowerLeft(Rectangle2D rect) {
        double x = rect.getX() + HInset + ArcRadius;
        double y = rect.getY() + rect.getHeight() - VInset - ArcRadius;
        return new Point2D.Double(x, y);
    }
}
