/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.mode;

import com.lightcrafts.utils.awt.geom.HiDpi;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.util.*;

/** Add Mode overlays on top of some underlay component.  The Modes are in
  * a Stack.  All Modes in the Stack are visible, but only the top of the Stack
  * gets MouseEvents.
  */

public class ModeOverlay extends JLayeredPane {

    private final static Integer UnderlayLayer = 0;

    private Integer OverlayLayer = 1;

    private Component underlay;
    private Stack<Mode> modes;
    private MouseMotionListener autoscroller;

    private Collection<MouseInputListener> mouseListeners;

    public ModeOverlay(Component underlay) {
        this.underlay = underlay;
        modes = new Stack<Mode>();
        mouseListeners = new LinkedList<MouseInputListener>();
        setLayout(null);
        add(underlay, UnderlayLayer);

        // Add a listener on the overlay to support autoscrolling:
        autoscroller = new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Rectangle r = HiDpi.imageSpaceRectFrom(new Rectangle(e.getX(), e.getY(), 1, 1));
                scrollRectToVisible(r);
            }
        };
        // The Mode stack should never be empty, so that mouse events will
        // be handled consistently.  (An empty stack would cause mouse events
        // to dispatch to the underlay.)
        pushMode(new NoMode());
    }

    public Mode peekMode() {
        try {
            return modes.peek();
        }
        catch (EmptyStackException e) {
            return null;
        }
    }

    public Mode popMode() {
        try {
            Mode oldMode = modes.pop();
            if (oldMode != null) {
                removeMode(oldMode);
                decLayer();
                repaint();
            }
            return oldMode;
        }
        catch (EmptyStackException e) {
            return null;
        }
    }

    public void pushMode(Mode mode) {
        modes.push(mode);
        incLayer();
        addMode(mode);
        validate();
    }

    /** Adds the given MouseInputListener to the overlay of the active
      * Mode, and migrates the listener to the new overlay when the active
      * Mode changes.
      * @param listener A MouseInputListener to listen on the active Mode's
      * overlay.
      */
    public void addMouseInputListener(MouseInputListener listener) {
        Component[] comps = getComponents();
        for (Component comp : comps) {
            comp.addMouseListener(listener);
            comp.addMouseMotionListener(listener);
        }
        mouseListeners.add(listener);
        addMouseListener(listener);
        addMouseMotionListener(listener);
    }

    /**
     * Remove a MouseInputListener previously submitted to
     * <code>addMouseInputListener()</code>
     * @param listener A MouseInputListener previously submitted.
     */
    public void removeMouseInputListener(MouseInputListener listener) {
        Component[] comps = getComponents();
        for (Component comp : comps) {
            comp.removeMouseListener(listener);
            comp.removeMouseMotionListener(listener);
        }
        mouseListeners.remove(listener);
        removeMouseListener(listener);
        removeMouseMotionListener(listener);
    }

    private void incLayer() {
        int i = OverlayLayer;
        OverlayLayer = ++i;
    }

    private void decLayer() {
        int i = OverlayLayer;
        OverlayLayer = --i;
    }

    private void addMode(Mode mode) {
        JComponent overlay = mode.getOverlay();
        overlay.setOpaque(false);
        if (mode.wantsAutocroll()) {
            overlay.setAutoscrolls(true);
            overlay.addMouseMotionListener(autoscroller);
        }
        add(overlay, OverlayLayer);

        for (MouseInputListener listener : mouseListeners) {
            mode.addMouseInputListener(listener);
        }
    }

    private void removeMode(Mode mode) {
        JComponent overlay = mode.getOverlay();
        remove(overlay);
        overlay.removeMouseMotionListener(autoscroller);

        for (MouseInputListener listener : mouseListeners) {
            mode.removeMouseInputListener(listener);
        }
    }

    /**
     * Prefered size is just the preferred size of the underlay, unless we're
     * in a scroll pane, in which case it's the maximum of that size and the
     * size of the viewport.
     */
    public Dimension getPreferredSize() {
        Dimension underlaySize = underlay.getPreferredSize();
        Rectangle underlayRect = new Rectangle(
            0, 0, underlaySize.width, underlaySize.height
        );
        // We inset the underlay by a fixed fraction of its size, so you can
        // drag outside the image bounds.
        underlayRect = outsetPercent(underlayRect);
        Dimension viewportSize = getViewportSize();
        if (viewportSize != null) {
            int w = Math.max(underlayRect.width, viewportSize.width);
            int h = Math.max(underlayRect.height, viewportSize.height);
            return new Dimension(w, h);
        }
        else {
            return underlaySize;
        }
    }

    // The buffer space around the image bounds to use for overlays, as a
    // fraction of the average of the image's width and height.
    private final static double UnderlayBorder = 0.025;

    public static Rectangle outsetPercent(Rectangle rect) {
        double w = rect.width;
        double h = rect.height;
        double wp = (1 + UnderlayBorder) * w + UnderlayBorder * h;
        double hp = UnderlayBorder * w + (1 + UnderlayBorder) * h;
        double cx = rect.x + rect.width / 2;
        double cy = rect.x + rect.width / 2;
        return new Rectangle(
            (int) Math.round(cx - wp / 2),
            (int) Math.round(cy - hp / 2),
            (int) Math.round(wp),
            (int) Math.round(hp)
        );
    }

    // Inverse of outsetPercent().
    public static Rectangle insetPercent(Rectangle rect) {
        double c = 1 / (1 + 2 * UnderlayBorder);
        double wp = rect.width;
        double hp = rect.height;
        double w = c * ((1 + UnderlayBorder) * wp - UnderlayBorder * hp);
        double h = c * (- UnderlayBorder * wp + (1 + UnderlayBorder) * hp);
        double cx = rect.x + rect.width / 2;
        double cy = rect.x + rect.width / 2;
        return new Rectangle(
            (int) Math.round(cx - w / 2),
            (int) Math.round(cy - h / 2),
            (int) Math.round(w),
            (int) Math.round(h)
        );
    }

    /**
     * Get a transform mapping underlay coordinates to overlay coordinates.
     * <p>
     * If this ModeOverlay is inside a scroll pane, and if the underlay's
     * preferred size is smaller than the scroll pane's viewport, then the
     * overlay components may be larger than the underlay.  The transform
     * defines this relationship.
     */
    public AffineTransform getTransform() {
        // First ensure the underlay layout is current, since transform changes
        // often arrive close to layout changes:
        doUnderlayLayout();

        Point loc = underlay.getLocation();
        final double tx = loc.x * HiDpi.defaultTransform.getScaleX();
        final double ty = loc.y * HiDpi.defaultTransform.getScaleY();
        return AffineTransform.getTranslateInstance(tx, ty);
    }

    /**
     * Place the underlay in the center at its preferred size, and make all
     * the layers be the same size as this ModeOverlay.
     */
    public void doLayout() {
        doUnderlayLayout();
        Dimension size = getSize();
        Component[] comps = getComponents();
        for (Component comp : comps) {
            if (comp != underlay) {
                comp.setLocation(0, 0);
                comp.setSize(size);
            }
        }
    }

    // Keep track of recent layout data, to avoid thrashing the underlay's
    // size settings whenever anyone calls getTransform().
    private Dimension underlaySize;
    private Dimension size;

    /**
     * Place the underlay in the center at its preferred size.
     */
    private void doUnderlayLayout() {
        Dimension underlaySize = underlay.getPreferredSize();
        Dimension size = getSize();
        // NOTE: the following causes some minor thrashing but keeps things in sysnc in other places
//        if (size.equals(this.size) && (underlaySize.equals(this.underlaySize))) {
//            return;
//        }
        this.size = size;
        this.underlaySize = underlaySize;

        int centerX = size.width / 2;
        int centerY = size.height / 2;
        int x = centerX - underlaySize.width / 2;
        int y = centerY - underlaySize.height / 2;
        underlay.setLocation(x, y);
        underlay.setSize(underlaySize);
    }

    // In case we're in a scroll pane, we need to know whether the underlay
    // is smaller than the viewport.
    private Dimension getViewportSize() {
        JViewport viewport = (JViewport)
            SwingUtilities.getAncestorOfClass(JViewport.class, this);
        if (viewport != null) {
            return viewport.getSize();
        }
        else {
            return null;
        }
    }
}
