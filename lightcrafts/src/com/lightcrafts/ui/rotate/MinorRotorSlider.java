/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.rotate;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.LinkedList;

import static com.lightcrafts.ui.rotate.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.LCSliderUI;

/** A slider that calls listeners with angles in radians.
  */
class MinorRotorSlider extends JSlider {

    private LinkedList<RotorListener> listeners;

    private boolean restoring;  // True if we're programmatically updating.

    MinorRotorSlider() {
        super(- 50, + 50);
        this.setUI(new LCSliderUI(this));
        setToolTipText(LOCALE.get("MinorRotorToolTip"));
        setMajorTickSpacing(25);
        setMinorTickSpacing(5);
        setPaintTicks(true);
        setFont(new java.awt.Font("Lucida Grande", 0, 9));
        addChangeListener(
            new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    if (! restoring) {
                        double angle = getAngle();
                        boolean isChanging = getValueIsAdjusting();
                        notifyListeners(angle, isChanging);
                    }
                }
            }
        );
        listeners = new LinkedList<RotorListener>();
    }

    double getAngle() {
        int value = getValue();
        double angle = RotorControl.degreesToRadians(value);
        return angle / 100;
    }

    void setAngle(double angle) {
        int value = (int) Math.round(100 * RotorControl.radiansToDegrees(angle));
        restoring = true;
        setValue(value);
        restoring = false;
    }

    double getDegrees() {
        return getValue() / 100D;
    }

    void setDegrees(double degrees) {
        double radians = RotorControl.degreesToRadians(degrees);
        setAngle(radians);
    }

    void addRotorListener(RotorListener listener) {
        listeners.add(listener);
    }

    void removeRotorListener(RotorListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(double angle, boolean isChanging) {
        for (RotorListener listener : listeners) {
            listener.angleChanged(angle, isChanging, false);
        }
    }
}
