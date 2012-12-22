/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;

// A translucent overlay component that OpControls place on top of their
// other controls when they are in locked mode, to give a grayed look and to
// intercept input events.

class LockedOverlay extends JPanel {

    private final static Color OverlayColor = new Color(128, 128, 128, 128);

    LockedOverlay() {
        // Swallow events so they don't propagate to underlying widgets.
        addMouseListener(new MouseAdapter() {});
        addMouseMotionListener(new MouseMotionAdapter() {});
        setOpaque(false);
    }

    protected void paintComponent(Graphics g) {
        Color oldColor = g.getColor();
        g.setColor(OverlayColor);
        Dimension size = getSize();
        g.fillRect(0, 0, size.width, size.height);
        g.setColor(oldColor);
    }
}
