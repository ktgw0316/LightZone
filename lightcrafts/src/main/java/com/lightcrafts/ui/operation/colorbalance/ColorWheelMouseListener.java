/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.colorbalance;

import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * A MouseInputListener that attaches to a ColorWheel and handles mouse
 * interaction with the wheel.  Implementations only need to implement
 * colorPicked() to learn about interactive and noninteractive color picks
 * and forward them to Operations.
 */
abstract class ColorWheelMouseListener implements MouseInputListener {

    private ColorWheel wheel;

    ColorWheelMouseListener(ColorWheel wheel) {
        this.wheel = wheel;
    }

    abstract void colorPicked(Color color, boolean isChanging);

    public void mouseClicked(MouseEvent event) {
    }

    public void mousePressed(MouseEvent e) {
        updatePicked(e, true);
    }

    public void mouseReleased(MouseEvent e) {
        updatePicked(e, false);
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
        updatePicked(e, true);
    }

    public void mouseMoved(MouseEvent e) {
    }

    private void updatePicked(MouseEvent event, boolean isChanging) {
        Point p = event.getPoint();
        Color color = wheel.pointToColor(p, false);
        wheel.pickColor(color);
        colorPicked(color, isChanging);
    }
}
