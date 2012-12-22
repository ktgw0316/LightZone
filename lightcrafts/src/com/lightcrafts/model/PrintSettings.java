/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

import java.awt.color.ICC_Profile;
import java.awt.print.PageFormat;
import java.awt.geom.Rectangle2D;

/**
 * This is a settings bundle for printing.  It goes into
 * <code>Engine.print()</code>.
 * <p>
 * The bundle defines bounds for a printed image as a Rectangle2D.  The
 * coordinates of the rectangle are image bounds in the coordinates of a
 * PageFormat, measured in points (1/72nd's of an inch).  It further defines
 * an ICC_Profile for the output device, which can be null if a default
 * should be assumed; a RenderingIntent, which is an enum indicating the
 * algorithm to apply when converting to the profile's color space; and a
 * positive pixels-per-inch number, which defaults to 300.
 */
public final class PrintSettings {

    private ICC_Profile profile;
    private RenderingIntent intent;
    private Rectangle2D bounds;
    private int pixelsPerInch;

    /** Make up default PrintSettings.  The default PrintSettings has print
      * bounds equal to the imageable area of a default PageFormat.  The
      * default color profile is null, the default RenderingIntent is
      * PERCEPTUAL, and the default pixels per inch is 300.
      */
    public PrintSettings() {
        setPrintBounds(new PageFormat());
        setRenderingIntent(RenderingIntent.PERCEPTUAL);
        pixelsPerInch = 300;
    }

    public ICC_Profile getColorProfile() {
        return profile;
    }

    public void setColorProfile(ICC_Profile profile) {
        this.profile = profile;
    }

    public RenderingIntent getRenderingIntent() {
        return intent;
    }

    public void setRenderingIntent(RenderingIntent intent) {
        this.intent = intent;
    }

    public void setPrintBounds(Rectangle2D bounds) {
        this.bounds = (Rectangle2D) bounds.clone();
    }

    public Rectangle2D getPrintBounds() {
        return bounds;
    }

    public void setPixelsPerInch(int ppi) {
        pixelsPerInch = ppi;
    }

    public int getPixelsPerInch() {
        return pixelsPerInch;
    }

    public double getX() {
        return bounds.getX();
    }

    public double getY() {
        return bounds.getY();
    }

    public double getWidth() {
        return bounds.getWidth();
    }

    public double getHeight() {
        return bounds.getHeight();
    }

    private void setPrintBounds(PageFormat format) {
        bounds = new Rectangle2D.Double(
            format.getImageableX(),
            format.getImageableY(),
            format.getImageableWidth(),
            format.getImageableHeight()
        );
    }
}
/* vim:set et sw=4 ts=4: */
