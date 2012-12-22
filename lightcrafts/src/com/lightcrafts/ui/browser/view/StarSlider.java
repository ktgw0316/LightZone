/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.ui.browser.model.ImageDatum;
import com.lightcrafts.ui.LightZoneSkin;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.*;

/**
 * The widget in the ImageDatumControl that shows the row of stars and
 * controls image ratings.
 */
class StarSlider {

    private static int StarSize = 12;

    private final static GeneralPath StarShape = new GeneralPath();
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

    private final static Shape DotShape = new Arc2D.Double(
        0, 0, 2, 2, 0, 360, Arc2D.CHORD
    );
    // The slider accepts values zero through five.
    // The value is noninteger only during mouse interaction.
    private double stars;

    // A rollover highlight, to show this component is interactive.
    private boolean highlight;

    // The ImageDatum currently being rated
    private ImageDatum datum;

    // The bounds where the stars get drawn, matching the label text area
    // defined in ImageDatumRenderer
    private Rectangle rect;

    // A reference to the browser must be held, for error notifications.
    private AbstractImageBrowser browser;

    StarSlider(AbstractImageBrowser browser) {
        this.browser = browser;
    }

    // ImageDatumControl calls this when its focus shifts to a new index
    void setup(ImageDatum datum, Rectangle rect) {
        this.datum = datum;
        this.rect = ImageDatumRenderer.getTextRectangle(rect);

        ImageMetadata meta = datum.getMetadata(true);
        int rating = meta.getRating();
        if (rating > 0) {
            stars = rating;
        }
        else {
            // Zero means "unrated"
            stars = 0;
        }
    }

    void paint(Graphics2D g) {
        Color oldColor = g.getColor();
        Object oldHint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Shape oldClip = g.getClip();

        g.setColor(LightZoneSkin.Colors.BrowserLabelBackground);
        g.fill(rect);

        if (highlight) {
            g.setColor(LightZoneSkin.Colors.BrowserLabelForeground.brighter());
        }
        else {
            g.setColor(LightZoneSkin.Colors.BrowserLabelForeground);
        }
        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON
        );
        // Draw the five dots
        for (int n=1; n<=5; n++) {
            Point2D loc = starToPoint(n - .5);
            AffineTransform xform = AffineTransform.getTranslateInstance(
                loc.getX(), loc.getY()
            );
            Shape dot = xform.createTransformedShape(DotShape);
            g.fill(dot);
        }
        // Draw the right amount of stars
        Point2D left = starToPoint(0);
        Point2D right = starToPoint(stars);

        Rectangle2D clip = new Rectangle2D.Double(
            left.getX(), rect.y, right.getX() - left.getX(), rect.height
        );
        g.setClip(clip);

        for (int n=1; n<=5; n++) {
            Point2D loc = starToPoint(n - .5);
            AffineTransform xform = AffineTransform.getTranslateInstance(
                loc.getX(), loc.getY()
            );
            Shape star = xform.createTransformedShape(StarShape);
            g.fill(star);
        }
        g.setClip(oldClip);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldHint);
        g.setColor(oldColor);
    }

    // Called from ImageDatumControl.isControllerEvent().
    boolean isStarsEvent(MouseEvent event) {
        if ((event.getID() == MouseEvent.MOUSE_PRESSED) ||
            (event.getID() == MouseEvent.MOUSE_DRAGGED) ||
            (event.getID() == MouseEvent.MOUSE_RELEASED))
        {
            Point p = event.getPoint();
            if ((rect != null) && (rect.contains(p))) {
                return true;
            }
        }
        return false;
    }

    void handleEvent(MouseEvent event) {
        if (event.getID() == MouseEvent.MOUSE_PRESSED) {
            stars = pointToStar(event.getPoint());
        }
        if (event.getID() == MouseEvent.MOUSE_DRAGGED) {
            stars = pointToStar(event.getPoint());
        }
        if (event.getID() == MouseEvent.MOUSE_RELEASED) {
            int rating = (int) Math.round(stars);
            if (rating <= 0) {
                stars = 0;
                RatingActions.clearRating(datum, browser);
            }
            else {
                rating = Math.min(rating, 5);
                stars = rating;
                RatingActions.setRating(datum, rating, browser);
            }
        }
    }

    // Update the flag that controls highlighted rendering of the stars,
    // and return whether the highlight flag has changed, so ImageDatumControl
    // can enqueue a repaint.
    boolean updateHighlighted(Point p) {
        boolean wasHighlight = highlight;
        highlight = rect.contains(p);
        return ((highlight && ! wasHighlight) || (! highlight && wasHighlight));
    }

    Rectangle getStarsRect() {
        return (Rectangle) rect.clone();
    }

    // Given a point, find the star value where the left margin of the rect
    // is zero and the right margin is five, ignoring the point's y value.
    private double pointToStar(Point p) {
        double mid = rect.getX() + rect.getWidth() / 2;
        double star = (p.x - mid)  / (1.1 * StarSize) + 2.5;
        return star;
    }

    // Given a star value between zero and five, get the corresponding point
    // along the horizontal midline of the rect.
    private Point2D starToPoint(double star) {
        double mid = rect.getX() + rect.getWidth() / 2;
        double x = mid + (star - 2.5) * (1.1 * StarSize);
        double y = rect.getY() + rect.getHeight() / 2;
        return new Point2D.Double(x, y);
    }
}
