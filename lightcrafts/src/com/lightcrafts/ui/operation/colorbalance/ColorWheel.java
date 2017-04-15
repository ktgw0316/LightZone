/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.colorbalance;

import com.lightcrafts.ui.toolkit.DropperButton;
import com.lightcrafts.ui.swing.ColorSwatch;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * A JComponent that renders a circular color picker and provides a facility
 * to convert Points (for instance, from MouseEvents) into Colors.
 */
class ColorWheel extends JComponent {

    // The currently picked Color, or null if no Color has been picked.
    private Color picked;

    public Dimension getPreferredSize() {
        return new Dimension(100, 100);
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
        return new Point2D.Double(size.width / 2, size.height / 2);
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

    Color pointToColor(Point p) {
        return pointToColor(p, true);
    }

    // Note: the 2.2 and 1.2 magic constants make sure that the picked point stays inside the color wheel

    Color pointToColor(Point p, boolean linear) {
        double r = getRadius(p);
        double theta = getAngle(p);

        r = Math.min(r, 1);

        float hue = (1 + (float) (theta / Math.PI)) / 2f;
        float saturation = linear ? (float) r : (float) Math.min(1.1 * (r * r), 1); // non linearity for reduced sensitivity
        float brightness = 1;

        Color color = new Color(Color.HSBtoRGB(hue, saturation, brightness));
        if (r <= .97 || !linear)
            return color;

        // Some alpha blending at the edge, for antialiasing:
        float alpha = (float) Math.max(0, 33.3 * (1d - r));
        float[] comps = color.getRGBComponents(null);
        return new Color(comps[0], comps[1], comps[2], alpha);
    }

    Point colorToPoint(Color c) {
        return colorToPoint(c, true);
    }

    Point colorToPoint(Color c, boolean linear) {
        float[] hsb = Color.RGBtoHSB(
            c.getRed(), c.getGreen(), c.getBlue(), null
        );
        float hue = hsb[0];
        float saturation = hsb[1];

        double r = linear ? saturation : Math.sqrt(saturation); // non linearity for reduced sensitivity
        double theta = Math.PI * (2 * hue - 1);

        Point2D center = getWheelCenter();
        double radius = getWheelSize() / (linear ? 2 : 2.1);

        int x = (int) Math.round(center.getX() - radius * r * Math.cos(theta));
        int y = (int) Math.round(center.getY() + radius * r * Math.sin(theta));

        return new Point(x, y);
    }

    private BufferedImage wheelImage = null;

    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;

        Point2D center = getWheelCenter();
        double radius = getWheelSize() / 2;

        int minX = (int) Math.round(center.getX() - radius);
        int maxX = (int) Math.round(center.getX() + radius);
        int minY = (int) Math.round(center.getY() - radius);
        int maxY = (int) Math.round(center.getY() + radius);

        if (wheelImage == null) {
            wheelImage = new BufferedImage((int) (2*radius), (int) (2*radius), BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = wheelImage.createGraphics();
            /* Shape circle = new Ellipse2D.Double(minX, minY, 2 * radius, 2 * radius);
            gg.setClip(circle); */

            for (int x=minX; x<=maxX; x++) {
                for (int y=minY; y<=maxY; y++) {
                    Point p = new Point(x, y);
                    Color color = pointToColor(p);
                    gg.setColor(color);
                    gg.fillRect((int) (x - center.getX() + radius), (int) (y - center.getY() + radius), 1, 1);
                }
            }
            gg.dispose();
        }
        g.drawImage(wheelImage, null, (int) (center.getX() - radius), (int) (center.getY() - radius));

        if (picked != null) {
            Point p = colorToPoint(picked, false);
            g.setColor(Color.black);
            g.drawRect(p.x - 2, p.y - 2, 3, 3);
        }
    }

    public static void main(String[] args) {
        ColorWheel wheel = new ColorWheel();

        ColorWheelMouseListener listener = new ColorWheelMouseListener(wheel) {
            void colorPicked(Color color, boolean isChanging) {
                System.out.println(color);
            }
        };
        wheel.addMouseListener(listener);
        wheel.addMouseMotionListener(listener);

        Box colorContent = Box.createHorizontalBox();

        colorContent.add(wheel);
        colorContent.add(Box.createHorizontalStrut(8));
        colorContent.add(new ColorSwatch(Color.gray));
        colorContent.add(Box.createHorizontalStrut(8));
        colorContent.add(new ColorText(Color.gray));
        colorContent.add(Box.createHorizontalStrut(8));
        colorContent.add(new DropperButton());

        colorContent.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        JFrame frame = new JFrame("Test");
        frame.setContentPane(colorContent);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
