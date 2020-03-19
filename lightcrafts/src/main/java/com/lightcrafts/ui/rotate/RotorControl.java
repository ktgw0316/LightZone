/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.rotate;

import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.editor.Editor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;

public final class RotorControl extends Box implements RotorListener {

    private final static Icon RightUnselectedIcon =
        RotorMode.getIcon("rotRight_U");

    private final static Icon LeftUnselectedIcon =
        RotorMode.getIcon("rotLeft_U");

    private Box sliders;

    private MajorRotorSlider major;
    private MinorRotorSlider minor;

    private RotorTextField text;

    private CoolButton rightButton;
    private CoolButton leftButton;

    private ResetButton reset;

    private LinkedList<RotorListener> listeners;

    private double coarseAngle;     // multiple of pi/2

    private RotateLeftAction leftAction;
    private RotateRightAction rightAction;

    RotorControl() {
        super(BoxLayout.X_AXIS);

        major = new MajorRotorSlider();
        minor = new MinorRotorSlider();

        sliders = Box.createVerticalBox();
        sliders.add(major);
//        sliders.add(minor);

        text = new RotorTextField(this);

        // Mouse clicks on the slider focus the text field:
        major.setFocusable(false);
        minor.setFocusable(false);
        final MouseListener focusListener = new MouseAdapter() {
            public void mousePressed(MouseEvent event) {
                text.requestFocusInWindow();
            }
        };
        major.addMouseListener(focusListener);
        minor.addMouseListener(focusListener);

        // Forward mouse wheel events to the text field, but only if it
        // has focus:
        text.addFocusListener(
            new FocusAdapter() {
                public void focusGained(FocusEvent event) {
                    major.addMouseWheelListener(text);
                    minor.addMouseWheelListener(text);
                }
                public void focusLost(FocusEvent event) {
                    major.removeMouseWheelListener(text);
                    minor.removeMouseWheelListener(text);
                }
            }
        );
        add(sliders);

        listeners = new LinkedList<RotorListener>();

        major.addRotorListener(this);
        minor.addRotorListener(this);

        reset = new ResetButton(this);

        leftAction = new RotateLeftAction(this);
        rightAction = new RotateRightAction(this);

        leftButton = new CoolButton();
        leftButton.setStyle(CoolButton.ButtonStyle.LEFT);
        leftButton.setAction(leftAction);
        leftButton.setIcon(LeftUnselectedIcon);

        rightButton = new CoolButton();
        rightButton.setStyle(CoolButton.ButtonStyle.RIGHT);
        rightButton.setAction(rightAction);
        rightButton.setIcon(RightUnselectedIcon);

        add(leftButton);
        add(rightButton);
        add(Box.createHorizontalStrut(2));
        add(text);
        add(Box.createHorizontalStrut(2));
        add(reset);
    }

    public void setBackground(Color color) {
        super.setBackground(color);
        sliders.setBackground(color);
        major.setBackground(color);
        minor.setBackground(color);
        rightButton.setBackground(color);
        leftButton.setBackground(color);
        reset.setBackground(color);
    }

    public void addRotorListener(RotorListener listener) {
        listeners.add(listener);
    }

    public void removeRotorListener(RotorListener listener) {
        listeners.remove(listener);
    }

    public void angleChanged(
        double angle, boolean isChanging, boolean isNinetyDegrees
    ) {
        text.update();
        notifyListeners(isChanging);
    }

    public void angleReset() {
        angleChanged(0, false, false);
    }

    public double getAngle() {
        double fine = major.getAngle();
        double hyperFine = minor.getAngle();
        return coarseAngle + fine + hyperFine;
    }

    public void setAngle(double angle) {
        setAngleInternal(angle);
        notifyListeners(false);
    }

    void setAngleInternal(double angle) {
        int n = (int) Math.round(angle / (Math.PI / 2));
        coarseAngle = n * (Math.PI / 2);
        double remainder = major.setAngle(angle - coarseAngle);
        minor.setAngle(remainder);
        text.update();
    }

    public Action getLeftAction() {
        return leftAction;
    }

    public Action getRightAction() {
        return rightAction;
    }

    // For the RotorTextField only:
    double getDegrees() {
        double angle = getAngle();
        return radiansToDegrees(angle);
    }

    // For the RotorTextField only (notifies listeners, doesn't set the text):
    void setDegrees(double degrees) {
        double angle = degreesToRadians(degrees);
        int n = (int) Math.round(angle / Math.PI / 2);
        coarseAngle = n * (Math.PI / 2);
        double remainder = major.setAngle(angle - coarseAngle);
        minor.setAngle(remainder);
        notifyListeners(false);
    }

    // For the GridComponent overlay only (incremental changes):
    void incAngle(double delta) {
        double angle = getAngle();
        angle += delta;
        setAngleInternal(angle);
        notifyListeners(false);
    }

    void notifyListeners(boolean isChanging) {
        double angle = getAngle();
        for (RotorListener listener : listeners) {
            listener.angleChanged(angle, isChanging, false);
        }
    }

    void notifyListenersNinetyDegrees() {
        double angle = getAngle();
        for (RotorListener listener : listeners) {
            listener.angleChanged(angle, false, true);
        }
    }

    void notifyListenersReset() {
        for (RotorListener listener : listeners) {
            listener.angleReset();
        }
    }

    static double degreesToRadians(double degrees) {
        return Math.PI * degrees / 180;
    }

    static double radiansToDegrees(double radians) {
        return 180 * radians / Math.PI;
    }

    public void setEditor( Editor editor ) {
        leftAction.setEditor( editor );
        rightAction.setEditor( editor );
    }
}
