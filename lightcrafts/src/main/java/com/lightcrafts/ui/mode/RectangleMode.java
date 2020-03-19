/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.mode;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;

/** A very simple Mode that draws rubber-band rectangles on mouse drags.
  */

public class RectangleMode extends AbstractMode {

    private static Cursor Crosshair = new Cursor(Cursor.CROSSHAIR_CURSOR);
    private static Stroke BlackStroke;
    private static Stroke WhiteStroke;

    static {
        // Initialize the dashed-line Stroke for drawing rectangles:
        float[] dash = new float[] {5f, 5f};
        float width = 1f;
        BlackStroke = new BasicStroke(
            width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 1f, dash, 0f
        );
        WhiteStroke = new BasicStroke(
            width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 1f, dash, 5f
        );
    }

    private Rectangle rectangle;
    private JComponent overlay;

    public RectangleMode() {
        overlay = new RectangleOverlay();
        overlay.setCursor(Crosshair);
        RectangleListener listener = new RectangleListener();
        overlay.addMouseListener(listener);
        overlay.addMouseMotionListener(listener);
    }

    public Rectangle getRectangle() {
        return rectangle;
    }

    public JComponent getOverlay() {
        return overlay;
    }

    public void addMouseInputListener(MouseInputListener listener) {
        overlay.addMouseListener(listener);
        overlay.addMouseMotionListener(listener);
    }

    public void removeMouseInputListener(MouseInputListener listener) {
        overlay.removeMouseListener(listener);
        overlay.removeMouseMotionListener(listener);
    }

    public boolean wantsAutocroll() {
        return true;
    }

    private class RectangleOverlay extends JComponent {

        protected void paintComponent(Graphics graphics) {
            if (rectangle != null) {
                Graphics2D g = (Graphics2D) graphics;
                g.setStroke(WhiteStroke);
                g.setColor(Color.white);
                g.draw(rectangle);
                g.setStroke(BlackStroke);
                g.setColor(Color.black);
                g.draw(rectangle);
            }
        }
    }

    private class RectangleListener extends MouseInputAdapter {

        private Point start;

        public void mousePressed(MouseEvent e) {
            start = e.getPoint();
        }

        public void mouseReleased(MouseEvent e) {
            start = null;
        }

        public void mouseDragged(MouseEvent e) {
            Point end = e.getPoint();
            int x = start.x;
            int y = start.y;
            int w = end.x - start.x;
            int h = end.y - start.y;
            if (w < 0) {
                x += w;
                w = - w;
            }
            if (h < 0) {
                y += h;
                h = - h;
            }
            rectangle = new Rectangle(x, y, w, h);
            overlay.repaint();
        }
    }
}
