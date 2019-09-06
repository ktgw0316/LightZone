/*
 * Copyright (c) 2018. Masahiro Kitagawa
 */

package com.lightcrafts.utils.awt.geom;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.RenderedImage;

public class HiDpi {

    public static final AffineTransform defaultTransform;
    public static final AffineTransform inverseDefaultTransform;

    static {
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice gd = ge.getScreenDevices()[0];
        final GraphicsConfiguration gc = gd.getConfigurations()[0];

        defaultTransform = gc.getDefaultTransform();

        AffineTransform inv;
        try {
            inv = defaultTransform.createInverse();
        } catch (NoninvertibleTransformException ignored) {
            inv = null;
        }
        inverseDefaultTransform = inv;
    }

    public static void resetTransformScaleOf(Graphics2D g) {
        final AffineTransform tx = g.getTransform();
        tx.concatenate(HiDpi.inverseDefaultTransform);

        // Translation values must be integer; otherwise shapes drawn on the g
        // will be 1 pixel smaller and will cause gaps between the shapes.
        final double dX = tx.getTranslateX() % 1;
        final double dY = tx.getTranslateY() % 1;
        tx.translate(-dX, -dY);

        g.setTransform(tx);
    }

    public static Dimension userSpaceDimensionFrom(Dimension d) {
        return userSpaceDimensionFrom(d.getWidth(), d.getHeight());
    }

    public static Dimension userSpaceDimensionFrom(RenderedImage image) {
        return userSpaceDimensionFrom(image.getWidth(), image.getHeight());
    }

    private static Dimension userSpaceDimensionFrom(double width, double height) {
        return new Dimension(
                scaleToInt(width, inverseDefaultTransform.getScaleX()),
                scaleToInt(height, inverseDefaultTransform.getScaleY()));
    }

    public static Dimension imageSpaceDimensionFrom(Dimension d) {
        return imageSpaceDimensionFrom(d.getWidth(), d.getHeight());
    }

    private static Dimension imageSpaceDimensionFrom(double width, double height) {
        return new Dimension(
                scaleToInt(width, defaultTransform.getScaleX()),
                scaleToInt(height, defaultTransform.getScaleY()));
    }

    public static Point imageSpacePointFrom(Point point) {
        final Point p = new Point();
        HiDpi.defaultTransform.transform(point, p);
        return p;
    }

    public static Rectangle imageSpaceRectFrom(Rectangle rect) {
        return imageSpaceRectFrom(rect.x, rect.y, rect.width, rect.height);
    }

    private static Rectangle imageSpaceRectFrom(int x, int y, int width, int height) {
        final double scaleX = defaultTransform.getScaleX();
        final double scaleY = defaultTransform.getScaleY();
        return new Rectangle(
                scaleToInt(x, scaleX),
                scaleToInt(y, scaleY),
                scaleToInt(width, scaleX),
                scaleToInt(height, scaleY));
    }

    static private int scaleToInt(double value, double scale) {
        return (int) Math.round(value * scale);
    }
}
