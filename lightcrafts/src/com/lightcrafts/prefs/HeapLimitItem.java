/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.prefs;

import com.lightcrafts.utils.MemoryLimits;

import static com.lightcrafts.prefs.Locale.LOCALE;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.prefs.Preferences;

class HeapLimitItem extends PreferencesItem implements ChangeListener {

    private final static String Package = "/com/lightcrafts/app";
    private final static String Key = "MaxMemory";

    private static Preferences Prefs = Preferences.userRoot().node(Package);

    private JSlider slider;
    private JLabel text;
    private Dimension textSize; // keep the text bounds fixed as text changes
    private Box box;

    HeapLimitItem(JTextArea help) {
        super(help);

        int minimum = MemoryLimits.getMinimum();
        int maximum = MemoryLimits.getMaximum();

        if (maximum > minimum) {
            slider = new JSlider(minimum, maximum);
        }
        else {
            // We are running with insufficient memory
            slider = new JSlider(0, 1000);
            slider.setEnabled(false);
        }
        slider.setMajorTickSpacing(maximum / 5);
        slider.setMinorTickSpacing(maximum / 20);
        slider.setPaintTicks(true);

        text = new JLabel();

        // Remember the right size for the text:
        text.setText(Integer.toString(maximum) + " MB");
        text.setHorizontalAlignment(SwingConstants.RIGHT);
        textSize = text.getPreferredSize();

        slider.setFocusable(false);

        slider.addChangeListener(this);

        box = Box.createHorizontalBox();
        box.add(slider);
        box.add(Box.createHorizontalStrut(3));
        box.add(text);

        addHelpListeners();
    }

    public String getLabel() {
        return LOCALE.get("HeapLimitItemLabel");
    }

    public String getHelp(MouseEvent e) {
        return LOCALE.get("HeapLimitItemHelp");
    }

    public boolean requiresRestart() {
        return true;
    }

    public JComponent getComponent() {
        return box;
    }

    public void commit() {
        if (slider.isEnabled()) {
            int value = slider.getValue();
            Prefs.putInt(Key, value);
        }
    }

    public void restore() {
        if (slider.isEnabled()) {
            int defaultValue = MemoryLimits.getDefault();
            int value = Prefs.getInt(Key, defaultValue);
            slider.setValue(value);
        }
    }

    public void stateChanged(ChangeEvent e) {
        int value = slider.getValue();
        String s = Integer.toString(value);
        text.setText(s + " MB");
        text.setPreferredSize(textSize);
    }
}
