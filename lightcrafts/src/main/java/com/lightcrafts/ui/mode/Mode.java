/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.mode;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.geom.AffineTransform;
import java.awt.*;

/**
 * Modes for mouse interaction with images.  A Mode has an overlay
 * JComponent where it may draw things that will be superposed with an image;
 * an AffineTransform that will help it figure out the relation between the
 * dimensions of its overlay and an Engine's image bounds; and an autoscroll
 * property, to determine whether the overlay should receive synthetic mouse
 * events in a JScrollPane.
 */

public interface Mode {

    void enter();

    void exit();

    boolean isIn();

    /**
     * Get an overlay JComponent where this Mode will draw things, to
     * superimpose on an image being edited.
     */
    JComponent getOverlay();

    /**
     * Subscribe a MouseInputListener to mouse events on this Mode's
     * overlay.  This is useful for dropper features.  Adding listeners
     * directly on the JComponent returned by <code>getOverlay()</code> may
     * not be sufficient, since the overlay could have children.
     * @param listener A MouseInputListener to hear about mouse activity
     * within this Mode's overlay JComponent.
     */
    void addMouseInputListener(MouseInputListener listener);

    /**
     * Unsubscribe a MouseInputListener from mouse events on this Mode's
     * overlay.
     * @param listener A MouseInputListener previously submitted to
     * <code>addMouseInputListener()</code>.
     */
    void removeMouseInputListener(MouseInputListener listener);

    /**
     * Set a new AffineTransform to use for drawing in the overlay and for
     * sending data to an image processing Engine.
     * <p>
     * The rule for the transform is: if you use the argument for
     * Graphics2D.setTransform(xform), and then do drawing in the original
     * image coordinates, the image and the overlay will align correctly.
     */
    void setTransform(AffineTransform xform);

    /**
     * Set the location and size of the underlay component, in the coordinates
     * of this Mode's overlay component.
     */
    void setUnderlayBounds(Rectangle bounds);

    /**
     * Return true if this Mode's overlay wants to receive synthetic mouse
     * events when its overlay is in a JScrollPane and the user drags the
     * mouse from the overlay to outside the viewport.
     */
    boolean wantsAutocroll();

    void dispose();
}
