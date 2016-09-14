/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.model.Engine;
import static com.lightcrafts.model.ImageEditor.Locale.LOCALE;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;

class RawSettings extends JPanel {

    abstract class FloatSlider extends JSlider {
        FloatSlider(final double min, final double max) {
            addChangeListener(
                new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        int value = getValue();
                        double floatValue = min + (max - min) * (value / 100d);
                        sliderMoved(floatValue);
                    }
                }
            );
        }
        abstract void sliderMoved(double value);
        abstract String getSliderName();
    }
    private JFrame frame;   // save this so we can dispose it

    private int count;      // the number of sliders

    RawSettings(final Engine engine) {
        super(new GridBagLayout());

        addSlider(
            new FloatSlider(1000, 10000) {
                @Override
                String getSliderName() {
                    return "Temperature";
                }
                @Override
                void sliderMoved(double value) {
//                    engine.setTemperature(value);
                    System.out.println("set RAW temperature to " + value);
                }
            }
        );

        addSlider(
            new FloatSlider(1, 100) {
                @Override
                String getSliderName() {
                    return "Noise Reduction";
                }
                @Override
                void sliderMoved(double value) {
//                    engine.setNoiseReduction(value);
                    System.out.println("set RAW noise reduction to " + value);
                }
            }
        );
        addSlider(
            new FloatSlider(5, 25) {
                @Override
                String getSliderName() {
                    return "Exposure";
                }
                @Override
                void sliderMoved(double value) {
//                    engine.setExposure(value);
                    System.out.println("set RAW exposure to " + value);
                }
            }
        );
    }

    private void addSlider(FloatSlider slider) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = count++;
        c.gridx = 0;
        add(new JLabel(slider.getSliderName()), c);
        c.gridx = 1;
        add(slider, c);
    }

    void showFrame() {
        frame = new JFrame(LOCALE.get("RawSettingTitle"));
        frame.getContentPane().add(this);
        frame.pack();
        frame.setLocation(0, 0);
        frame.setAlwaysOnTop(true);
        frame.setVisible(true);
    }

    void hideFrame() {
        frame.setVisible(false);
    }
}
