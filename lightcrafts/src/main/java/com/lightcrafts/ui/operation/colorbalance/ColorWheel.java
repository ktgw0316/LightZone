/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.colorbalance;

import com.lightcrafts.image.color.OkColor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * A JComponent that renders a circular color picker and provides a facility
 * to convert Points (for instance, from MouseEvents) into Colors.
 */
class ColorWheel extends JComponent {

    public ColorWheel() {
        this.addComponentListener(new ResizeListener());
    }

    // The currently picked Color, or null if no Color has been picked.
    private Color picked;

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(150, 150);
    }

    Color getPickedColor() {
        return picked;
    }

    void pickColor(Color color) {
        picked = color;
        repaint();
    }

    // Determine the size of the wheel, in screen coordinates.
    private int getWheelSize() {
        Dimension size = getSize();
        return Math.min(size.width, size.height);
    }

    // Determine the centerpoint of the wheel, in screen coordinates.
    private Point2D getWheelCenter() {
        Dimension size = getSize();
        return new Point2D.Double(0.5 * size.width, 0.5 * size.height);
    }

    // Compute distance from the wheel center, in internal coordinates.
    private double getRadius(Point p) {
        Point2D center = getWheelCenter();
        int size = getWheelSize();
        double x = 2 * (p.x + .5 - center.getX()) / (double) size;
        double y = 2 * (p.y + .5 - center.getY()) / (double) size;
        return Math.sqrt(x * x + y * y);
    }

    private double getAngle(Point p) {
        Point2D center = getWheelCenter();
        double x = p.x + .5 - center.getX();
        double y = p.y + .5 - center.getY();
        return Math.atan2(y, - x);
    }

    Color pointToColor(Point p, boolean linear) {
        double r = Math.min(getRadius(p), 1);
        double theta = getAngle(p);
        return polarToColor(r, theta, linear);
    }

    // Note: the 2.2 and 1.2 magic constants make sure that the picked point stays inside the color wheel

    static Color polarToColor(double r, double theta, boolean linear) {
        final float hue = (1 + (float) (theta / Math.PI)) / 2f;
        final float saturation = linear ? (float) r : (float) Math.min(1.1 * (r * r), 1); // non linearity for reduced sensitivity
        final float luminosity = 0.5f;

        final Color color = new OkColor.Okhsl(hue, saturation, luminosity).toColor();
        if (r <= .97 || !linear)
            return color;

        // Some alpha blending at the edge, for antialiasing:
        float alpha = (float) Math.max(0, 33.3 * (1d - r));
        float[] comps = color.getRGBComponents(null);
        return new Color(comps[0], comps[1], comps[2], alpha);
    }

    Point colorToPoint(Color c, boolean linear) {
        var polar = colorToPolar(c, linear);
        double r = polar[0];
        double theta = polar[1];
        return polarToPoint(r, theta, linear);
    }

    static double[] colorToPolar(Color c, boolean linear) {
        final var okhsl = OkColor.Okhsl.from(c);
        final float hue = okhsl.h();
        final float saturation = okhsl.s();

        double r = linear ? saturation : Math.sqrt(saturation); // non linearity for reduced sensitivity
        double theta = Math.PI * (2 * hue - 1);
        return new double[]{r, theta};
    }

    Point polarToPoint(double r, double theta, boolean linear) {
        Point2D center = getWheelCenter();
        double radius = getWheelSize() / (linear ? 2 : 2.1);

        int x = (int) Math.round(center.getX() - radius * r * Math.cos(theta));
        int y = (int) Math.round(center.getY() + radius * r * Math.sin(theta));
        return new Point(x, y);
    }

    private BufferedImage wheelImage = null;

    public BufferedImage createWheelImage(Point2D center, double radius) {
        int minX = (int) Math.round(center.getX() - radius);
        int maxX = (int) Math.round(center.getX() + radius);
        int minY = (int) Math.round(center.getY() - radius);
        int maxY = (int) Math.round(center.getY() + radius);
        int diameter = (int) (2*radius);

        var img = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();

        for (int x=minX; x<=maxX; x++) {
            for (int y=minY; y<=maxY; y++) {
                Point p = new Point(x, y);
                Color color = pointToColor(p, true);
                gg.setColor(color);
                gg.fillRect(x - minX, y - minY, 1, 1);
            }
        }
        gg.dispose();
        return img;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;

        Point2D center = getWheelCenter();
        double radius = 0.5 * getWheelSize();

        if (wheelImage == null) {
            wheelImage = createWheelImage(center, radius);
        }
        g.drawImage(wheelImage, null, (int) (center.getX() - radius), (int) (center.getY() - radius));

        if (picked != null) {
            Point p = colorToPoint(picked, false);
            g.setColor(Color.black);
            g.drawRect(p.x - 2, p.y - 2, 3, 3);
        }
    }

    private class ResizeListener extends ComponentAdapter {
        @Override
        public void componentResized(ComponentEvent e) {
            wheelImage = null;
            repaint();
        }
    }
}
