/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.scroll;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * A transparent overlay component that takes a scroll pane and renders a
 * navigation view of its viewport that a user can drag.
 */
public class Panner
    extends JComponent implements MouseListener, MouseMotionListener
{
    // A preferred size for this component
    private final static int Size = 100;

    private JScrollPane scroll;

    private Point2D startPt;        // track drag gestures
    private Rectangle2D startRect;

    private boolean isDragging;     // decide the highlight stroke
    private boolean isMouseInside;

    public Panner(JScrollPane scroll) {
        this.scroll = scroll;

        AdjustmentListener repainter = new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                // Enable according to whether there's scrolling to be done.
                boolean wasEnabled = isEnabled();
                updateEnabled();
                // Repaint every time there is a scroll event.
                if (isEnabled() || wasEnabled) {
                    repaint();
                }
            }
        };
        JScrollBar hBar = scroll.getHorizontalScrollBar();
        JScrollBar vBar = scroll.getVerticalScrollBar();
        hBar.addAdjustmentListener(repainter);
        vBar.addAdjustmentListener(repainter);

        addMouseListener(this);
        addMouseMotionListener(this);

        // Add a margin for outsetting the view bounds
        setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        startPt = e.getPoint();
        startRect = getViewportBounds();
        isDragging = true;
        setCursor(ScrollMode.ClosedHand);
    }

    public void mouseReleased(MouseEvent e) {
        // If the cursor wasn't dragged, the perform a block scroll
        Point p = e.getPoint();
        if (startPt.equals(p)) {
            doBlockScroll(p);
        }
        setCursor(ScrollMode.OpenHand);
        isDragging = false;
        if (! isMouseInside) {
            repaint();
        }
    }

    private void doBlockScroll(Point p) {
        int dx = 0;
        int dy = 0;
        // Is the given point North, South, East or West of the viewport?
        Rectangle2D rect = getViewportBounds();
        double angle = Math.atan2(
            p.y - rect.getCenterY(),
            p.x - rect.getCenterX()
        );
        double neAngle = Math.atan2(
            rect.getMinY() - rect.getCenterY(),
            rect.getMaxX() - rect.getCenterX()
        );
        double seAngle = Math.atan2(
            rect.getMaxY() - rect.getCenterY(),
            rect.getMaxX() - rect.getCenterX()
        );
        double swAngle = Math.atan2(
            rect.getMaxY() - rect.getCenterY(),
            rect.getMinX() - rect.getCenterX()
        );
        double nwAngle = Math.atan2(
            rect.getMinY() - rect.getCenterY(),
            rect.getMinX() - rect.getCenterX()
        );
        if ((angle > swAngle) || (angle < nwAngle)) {
            dx = -1;
        }
        if ((angle > nwAngle) && (angle < neAngle)) {
            dy = -1;
        }
        if ((angle > neAngle) && (angle < seAngle)) {
            dx = +1;
        }
        if ((angle > seAngle) && (angle < swAngle)) {
            dy = +1;
        }
        Rectangle2D pannerRect = new Rectangle2D.Double(
            startRect.getX() + dx * rect.getWidth(),
            startRect.getY() + dy * rect.getHeight(),
            startRect.getWidth(),
            startRect.getHeight()
        );
        repaint();
        try {
            AffineTransform xform = getTransform();
            xform = xform.createInverse();
            Rectangle2D viewportRect =
                xform.createTransformedShape(pannerRect).getBounds2D();
            Rectangle intRect = new Rectangle(
                (int) Math.round(viewportRect.getX()),
                (int) Math.round(viewportRect.getY()),
                (int) Math.round(viewportRect.getWidth()),
                (int) Math.round(viewportRect.getHeight())
            );
            JComponent view = (JComponent) scroll.getViewport().getView();
            view.scrollRectToVisible(intRect);
        }
        catch (NoninvertibleTransformException e1) {
            // View has zero size; do nothing.
        }
    }

    public void mouseEntered(MouseEvent e) {
        isMouseInside = true;
        repaint();
    }

    public void mouseExited(MouseEvent e) {
        isMouseInside = false;
        if (! isDragging) {
            repaint();
        }
    }

    public void mouseDragged(MouseEvent e) {
        Point2D p = e.getPoint();
        Point2D dp = new Point2D.Double(
            p.getX() - startPt.getX(), p.getY() - startPt.getY()
        );
        Rectangle2D pannerRect = new Rectangle2D.Double(
            startRect.getX() + dp.getX(),
            startRect.getY() + dp.getY(),
            startRect.getWidth(),
            startRect.getHeight()
        );
        repaint();
        try {
            AffineTransform xform = getTransform();
            xform = xform.createInverse();
            Rectangle2D viewportRect =
                xform.createTransformedShape(pannerRect).getBounds2D();
            Rectangle intRect = new Rectangle(
                (int) Math.round(viewportRect.getX()),
                (int) Math.round(viewportRect.getY()),
                (int) Math.round(viewportRect.getWidth()),
                (int) Math.round(viewportRect.getHeight())
            );
            JComponent view = (JComponent) scroll.getViewport().getView();
            view.scrollRectToVisible(intRect);
        }
        catch (NoninvertibleTransformException e1) {
            // View has zero size; do nothing.
        }
    }

    public void mouseMoved(MouseEvent e) {
    }

    public Dimension getPreferredSize() {
        return new Dimension(Size, Size);
    }

    protected void paintComponent(Graphics graphics) {
        if (isEnabled()) {
            Graphics2D g = (Graphics2D) graphics;
            Stroke oldStroke = g.getStroke();
            Color oldColor = g.getColor();

            Rectangle2D outer = getViewBounds();
            // Outset the view rect by one pixel, so it doesn't get clobbered
            // by the viewport rect.
            outer.setRect(
                outer.getX() - 1,
                outer.getY() - 1,
                outer.getWidth() + 2,
                outer.getHeight() + 2
            );
            // A translucent gray underlay
            g.setColor(new Color(128, 128, 128, 128));
            g.fill(outer);

            if (isDragging || isMouseInside) {
                g.setColor(Color.white);
            }
            else {
                g.setColor(Color.lightGray);
            }
            g.draw(outer);

            Rectangle2D inner = getViewportBounds();
            g.setColor(new Color(255, 0, 0, 128));
            g.fill(inner);
            g.setColor(Color.red);
            g.draw(inner);

            g.setColor(oldColor);
            g.setStroke(oldStroke);
        }
    }

    // Get the view bounds in panner coordinates
    private Rectangle2D getViewBounds() {
        Dimension viewSize = scroll.getViewport().getView().getSize();
        Rectangle viewRect = new Rectangle(0, 0, viewSize.width, viewSize.height);
        AffineTransform xform = getTransform();
        Shape pannerRect = xform.createTransformedShape(viewRect);
        return pannerRect.getBounds2D();
    }

    // Get the viewport bounds in panner coordinates
    private Rectangle2D getViewportBounds() {
        JViewport viewport = scroll.getViewport();
        Rectangle viewportRect = viewport.getViewRect();
        AffineTransform xform = getTransform();
        Shape pannerRect = xform.createTransformedShape(viewportRect);
        return pannerRect.getBounds2D();
    }

    // Get the transform that maps view coordinates into panner coordinates
    private AffineTransform getTransform() {
        Dimension size = getSize();
        Insets insets = getInsets();
        size.width -= insets.left + insets.right + 1;
        size.height -= insets.top + insets.bottom + 1;

        JViewport viewport = scroll.getViewport();
        Component view = viewport.getView();
        Dimension viewSize = view.getSize();

        double ratio = viewSize.width / (double) viewSize.height;
        double x, y, w, h;
        if (ratio < 1) {
            // The panner is fully tall, centered horizontally
            w = size.height * ratio;
            h = size.height;
            x = (size.width - w) / 2;
            y = 0;
        }
        else {
            // The panner is fully wide, centered vertically
            w = size.width;
            h = size.width / ratio;
            x = 0;
            y = (size.height - h) / 2;
        }
        AffineTransform scale = AffineTransform.getScaleInstance(
            w / (double) viewSize.width,
            h / (double) viewSize.height
        );
        AffineTransform trans = AffineTransform.getTranslateInstance(
            x + insets.left, y + insets.top
        );
        trans.concatenate(scale);
        return trans;
    }

    // Enable and disable according to whether scrolling is possible.
    private void updateEnabled() {
        JScrollBar hBar = scroll.getHorizontalScrollBar();
        JScrollBar vBar = scroll.getVerticalScrollBar();
        setEnabled(isActive(hBar) || isActive(vBar));
    }

    private static boolean isActive(JScrollBar bar) {
        BoundedRangeModel model = bar.getModel();
        int range = model.getMaximum() - model.getMinimum();
        int extent = model.getExtent();
        return (range > extent + 1);    // Rounding error in zoom-to-fit
    }

    public static void main(String[] args) {
        JComponent comp = new JTree();
        JScrollPane scroll = new JScrollPane(comp);
        Panner panner = new Panner(scroll);

        JFrame scrollFrame = new JFrame("Scroll");
        scrollFrame.setContentPane(scroll);
        scrollFrame.setSize(new Dimension(100, 100));
        scrollFrame.setLocationRelativeTo(null);
        scrollFrame.setVisible(true);

        JFrame panFrame = new JFrame("Pane");
        panFrame.getContentPane().setLayout(new FlowLayout());
        panFrame.getContentPane().add(panner);
        panFrame.pack();
        panFrame.setVisible(true);
    }
}
