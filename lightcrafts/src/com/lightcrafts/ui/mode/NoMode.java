/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.mode;

import javax.swing.*;
import javax.swing.event.MouseInputListener;

/** A Mode that shows nothing and does nothing.  This absurd behavior is
  * conventional in the context of mode palettes, where users may want
  * something that turns off whatever is currently happening.
  */

public class NoMode extends AbstractMode {

    private JComponent overlay;

    public NoMode() {
        overlay = new JPanel();
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
}
