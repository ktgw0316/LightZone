/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.rotate;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * This overlay draws a gray grid aligned with the image, and repaints
 * during interactive updates like slider drags.  It takes mouse drags to
 * draw a rubber-band line, and this line gets interpreted as an incremental
 * change to the rotation angle.
 */

class GridComponent extends JComponent {

    private static Cursor RotateCursor = new Cursor(Cursor.CROSSHAIR_CURSOR);

    private static Color GridColor = new Color(55, 55, 55, 85);

    private static Stroke BlackStroke;
    private static Stroke WhiteStroke;

    private static int MinDragDistance = 10;

    static {
        // Initialize the dashed-line Strokes for drawing lines:
        float[] dash = new float[] {5f, 5f};
        float width = 1f;
        BlackStroke = new BasicStroke(
            width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 1f, dash, 0f
        );
        WhiteStroke = new BasicStroke(width);
    }

    private class LineListener extends MouseInputAdapter {

        private Point start;

        public void mousePressed(MouseEvent e) {
            start = e.getPoint();
        }

        public void mouseReleased(MouseEvent e) {
            if (line != null) {
                doRotate();
            }
            start = null;
            line = null;
            repaint();
        }

        public void mouseDragged(MouseEvent e) {
            Point end = e.getPoint();
            line = new Line2D.Float(start.x, start.y, end.x, end.y);
            repaint();
        }
    }

    private RotorControl control;

    private Line2D line;

    private double angle = 0;
    private double spacing = 10;
    private boolean showGrid = true;

    GridComponent(RotorControl control) {
        this.control = control;
        setCursor(RotateCursor);
        LineListener listener = new LineListener();
        addMouseListener(listener);
        addMouseMotionListener(listener);
    }

    void setShowGrid(boolean show) {
        if (this.showGrid != show) {
            this.showGrid = show;
            repaint();
        }
    }

    void setAngle(double angle) {
        this.angle = angle;
        repaint();
    }

    void setSpacing(double spacing) {
        if (spacing >= 1) {
            this.spacing = spacing;
            repaint();
        }
    }

    // Tell if the current dragged line is long enough to be used.
    private boolean isMinimumDragLength() {
        double dx = line.getX2() - line.getX1();
        double dy = line.getY2() - line.getY1();
        return ((Math.abs(dx) >= MinDragDistance) ||
                (Math.abs(dy) >= MinDragDistance));
    }

    // Get the angle of the current line.
    private double getLineAngle() {
        double dx = line.getX2() - line.getX1();
        double dy = line.getY2() - line.getY1();
        return Math.atan(dy / dx);
    }

    // Compute the minimum signed angle between the current line and the axes.
    private double getDeltaAngle() {
        double lineAngle = getLineAngle();
        int n = (int) Math.round(lineAngle / (Math.PI / 2));
        double nearestRightAngle = n * Math.PI / 2;
        return lineAngle - nearestRightAngle;
    }

    // Commit the current delta-angle to the RotorControl.
    private void doRotate() {
        if (isMinimumDragLength()) {
            double delta = getDeltaAngle();
            control.incAngle(- delta);
        }
    }

    protected void paintComponent(Graphics graphics) {

        // Save all the graphics context:
        Graphics2D g = (Graphics2D) graphics;
        RenderingHints oldHints = g.getRenderingHints();
        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON
        );
        Color oldColor = g.getColor();
        Stroke oldStroke = g.getStroke();

        // Maybe draw the interactive rubber-band line:
        if (line != null) {
            drawDashedShape(g, line);

            // Maybe draw the indicator of which axis is closest to the line:
            if (isMinimumDragLength()) {
                double angle = getLineAngle();
                // Check if the angle is on the wrong sheet:
                if (Math.cos(angle) * (line.getX2() - line.getX1()) < 0) {
                    angle -= Math.PI;
                }
                int n = (int) Math.round(angle / (Math.PI / 2));
                double axisAngle = n * (Math.PI / 2);
                double x1 = line.getX1();
                double y1 = line.getY1();
                double x2 = line.getX2();
                double y2 = line.getY2();
                double r =
                    Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
                Arc2D arc = createArc(x1, y1, r, angle, axisAngle);
                drawDashedShape(g, arc);
            }
        }
        // Maybe draw the grid:
        if (showGrid) {
            g.setColor(GridColor);

            Dimension size = getSize();
            int w = size.width;
            int h = size.height;
            Rectangle2D bounds = new Rectangle2D.Double(0, 0, w, h);
            double midX = w / 2d;
            double midY = h / 2d;

            InfiniteLine line;
            Line2D l;
            double normal = angle + Math.PI / 2;
            double x, y;
            int n;

            n = 0;
            do {
                x = midX + n * spacing * Math.cos(normal);
                y = midY + n * spacing * Math.sin(normal);
                line = new InfiniteLine(x, y, angle);
                l = line.getSegment(bounds);
                if (l != null) {
                    g.draw(l);
                }
                n++;
            } while (l != null);

            n = 0;
            do {
                x = midX - n * spacing * Math.cos(normal);
                y = midY - n * spacing * Math.sin(normal);
                line = new InfiniteLine(x, y, angle);
                l = line.getSegment(bounds);
                if (l != null) {
                    g.draw(l);
                }
                n++;
            }
            while (l != null);

            n = 0;
            do {
                x = midX + n * spacing * Math.cos(angle);
                y = midY + n * spacing * Math.sin(angle);
                line = new InfiniteLine(x, y, normal);
                l = line.getSegment(bounds);
                if (l != null) {
                    g.draw(l);
                }
                n++;
            }
            while (l != null);

            n = 0;
            do {
                x = midX - n * spacing * Math.cos(angle);
                y = midY - n * spacing * Math.sin(angle);
                line = new InfiniteLine(x, y, normal);
                l = line.getSegment(bounds);
                if (l != null) {
                    g.draw(l);
                }
                n++;
            } while (l != null);
        }
        // Restore all the context we saved:
        g.setStroke(oldStroke);
        g.setColor(oldColor);
        g.setRenderingHints(oldHints);
    }

    private static void drawDashedShape(Graphics2D g, Shape shape) {
        g.setStroke(WhiteStroke);
        g.setColor(Color.white);
        g.draw(shape);
        g.setStroke(BlackStroke);
        g.setColor(Color.black);
        g.draw(shape);
    }

    // Make an Arc2D from a center, a radius, and two angles:
    private static Arc2D createArc(
        double cx, double cy, double r, double start, double end
    ) {
        start = - RotorControl.radiansToDegrees(start);
        end = - RotorControl.radiansToDegrees(end);
        Arc2D arc = new Arc2D.Double(
            cx - r, cy - r, 2 * r, 2 * r, start, end - start, Arc2D.PIE
        );
        return arc;
    }
}
