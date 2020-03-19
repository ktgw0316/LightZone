/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.rotate;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.LinkedList;

import static com.lightcrafts.ui.rotate.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.LCSliderUI;

/** A slider that calls listeners with angles between (-pi, +pi), in radians.
  */
class MajorRotorSlider extends JSlider implements ChangeListener
 {
    private LinkedList<RotorListener> listeners;

    private boolean restoring;  // True if we're programmatically updating.

    MajorRotorSlider() {
        super(- 45, + 45);
        this.setUI(new LCSliderUI(this));
        setToolTipText(LOCALE.get("MajorRotorToolTip"));
        setMajorTickSpacing(45);
        setMinorTickSpacing(15);
        setPaintTicks(true);
        setFont(new java.awt.Font("Lucida Grande", 0, 9));
        addChangeListener(this);
        listeners = new LinkedList<RotorListener>();
    }

    double getAngle() {
        int value = getValue();
        double angle = RotorControl.degreesToRadians(value);
        return angle;
    }

    double setAngle(double angle) {
        int value = (int) Math.round(RotorControl.radiansToDegrees(angle));
        restoring = true;
        setValue(value);
        restoring = false;
        double remainder = angle - getAngle();
        return remainder;
    }

    int getDegrees() {
        return getValue();
    }

    double setDegrees(double degrees) {
        double radians = RotorControl.degreesToRadians(degrees);
        double radianRemainder = setAngle(radians);
        double degreeRemainder = RotorControl.radiansToDegrees(radianRemainder);
        return degreeRemainder;
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

    public void stateChanged(ChangeEvent event) {
        // Send out changes when the slider moves:
        if (! restoring) {
            double angle = getAngle();
            boolean isChanging = getValueIsAdjusting();
            notifyListeners(angle, isChanging);
        }
    }
}
