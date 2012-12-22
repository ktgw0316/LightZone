/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.mode;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;

/** A very simple Mode that draws straight lines on mouse drags.
  */

public class LineMode extends AbstractMode {

    private static Cursor Crosshair = new Cursor(Cursor.CROSSHAIR_CURSOR);

    private static Stroke BlackStroke;
    private static Stroke WhiteStroke;

    static {
        // Initialize the dashed-line Stroke for drawing lines:
        final float[] dash = new float[] {5f, 5f};
        final float width = 1f;
        BlackStroke = new BasicStroke(
            width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 1f, dash, 0f
        );
        WhiteStroke = new BasicStroke(
            width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 1f, dash, 5f
        );
    }

    private Line2D line;
    private JComponent overlay;

    public LineMode() {
        overlay = new LineOverlay();
        overlay.setCursor(Crosshair);
        final LineListener listener = new LineListener();
        overlay.addMouseListener(listener);
        overlay.addMouseMotionListener(listener);
    }

    public Line2D getLine() {
        return line;
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

    private class LineOverlay extends JComponent {

        protected void paintComponent(Graphics graphics) {
            if (line != null) {
                final Graphics2D g = (Graphics2D) graphics;
                g.setStroke(WhiteStroke);
                g.setColor(Color.white);
                g.draw(line);
                g.setStroke(BlackStroke);
                g.setColor(Color.black);
                g.draw(line);
            }
        }
    }

    private class LineListener extends MouseInputAdapter {

        private Point start;

        public void mousePressed(MouseEvent e) {
            start = e.getPoint();
        }

        public void mouseReleased(MouseEvent e) {
            start = null;
        }

        public void mouseDragged(MouseEvent e) {
            final Point end = e.getPoint();
            line = new Line2D.Float(start.x, start.y, end.x, end.y);
            overlay.repaint();
        }
    }
}
