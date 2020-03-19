/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.rotate;

import com.lightcrafts.ui.mode.AbstractMode;
import com.lightcrafts.ui.editor.Editor;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.net.URL;

/** Rotation is a Mode that should only be active while the mouse interacts
  * with a RotorControl.  It draws a grid on its overlay to show the current
  * angle.
  */

public class RotorMode extends AbstractMode {

    private GridComponent overlay;
    private RotorControl control;

    public RotorMode() {
        control = new RotorControl();
        control.addRotorListener(
            new RotorListener() {
                private double startAngle;
                public void angleChanged(
                    double angle, boolean isChanging, boolean isNinetyDegrees
                ) {
                    if (! isChanging) {
                        startAngle = angle;
                    }
                    overlay.setAngle(startAngle - angle);
                    overlay.setShowGrid(isChanging);
                }
                public void angleReset() {
                    angleChanged(0, false, false);
                }
            }
        );
        overlay = new GridComponent(control);
        overlay.setAngle(0);
        overlay.setSpacing(40);
        overlay.setShowGrid(false);
    }

    public void addRotorListener(RotorListener listener) {
        control.addRotorListener(listener);
    }

    public void removeRotorListener(RotorListener listener) {
        control.removeRotorListener(listener);
    }

    public RotorControl getControl() {
        return control;
    }

    public double getAngle() {
        return control.getAngle();
    }

    // Slew the overlay's angle of rotation to the given angle, in radians,
    // with the sign convention that a positive angle means the image is rotated
    // clockwise on the screen.
    public void setAngle(double angle) {
        control.setAngleInternal(angle);
        overlay.setAngle(angle);
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

    static Icon getIcon(String name) {
        final String path = "resources/" + name + ".png";
        final URL url = RotorMode.class.getResource(path);
        final Image image = Toolkit.getDefaultToolkit().createImage(url);
        return new ImageIcon(image);
    }

    public void setEditor( Editor editor ) {
        control.setEditor( editor );
    }
}
