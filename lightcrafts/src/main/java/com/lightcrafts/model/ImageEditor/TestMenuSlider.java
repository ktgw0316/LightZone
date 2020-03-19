/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

// This class implements sliders for development and testing.
//
// To use it, construct one and return it in the list from
// Engine.getDebugItems().

class TestMenuSlider extends JMenu {

    static interface Listener {
        void sliderChanged(int value);
    }

    TestMenuSlider(
        String label, int min, int max, int value, final Listener listener
    ) {
        super(label);
        final JSlider slider = new JSlider(min, max, value);
        add(slider);
        slider.addChangeListener(
            new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent event) {
                    int value = slider.getValue();
                    listener.sliderChanged(value);
                }
            }
        );
    }
}
