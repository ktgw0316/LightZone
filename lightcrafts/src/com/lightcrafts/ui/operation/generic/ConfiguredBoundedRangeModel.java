/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.generic;

import com.lightcrafts.model.SliderConfig;

import javax.swing.*;

class ConfiguredBoundedRangeModel extends DefaultBoundedRangeModel {

    private SliderConfig config;

    // In case a high-precision value arrives from the text field:
    private double preciseValue;

    // Precomputed parameters for logarithmic sliders:
    private double A;
    private double B;

    // Precomputed parameters for linear sliders:
    private double m;
    private double b;

    // Prevent update loops:
    private boolean isUpdating;

    ConfiguredBoundedRangeModel(SliderConfig config) {
        this.config = config;
        precompute();
        double confValue = config.getDefaultValue();
        setConfiguredValue(confValue);
    }

    public void setMaximum(int max) {
        super.setMaximum(max);
        precompute();
    }

    public void setMinimum(int min) {
        super.setMinimum(min);
        precompute();
    }

    public void setValue(int value) {
        if (! isUpdating) {
            if (config.isLogScale()) {
                preciseValue = A * Math.exp(B * value);
            }
            else {
                preciseValue = m * value + b;
            }
        }
        super.setValue(value);
    }

    public double getConfiguredValue() {
        return preciseValue;
    }

    public void setConfiguredValue(double confValue) {
        double oldPreciseValue = preciseValue;
        preciseValue = confValue;

        int sliderValue;
        if (config.isLogScale()) {
            // Invert "A * exp(B * x)":
            sliderValue = (int) Math.round(Math.log(confValue / A) / B);
        }
        else {
            // Invert "m * x + b":
            sliderValue = (int) Math.round((confValue - b) / m);
        }
        isUpdating = true;
        int oldValue = getValue();
        if (sliderValue != oldValue) {
            setValue(sliderValue);
        }
        else if (preciseValue != oldPreciseValue)  {
            // Make sure there's notification, even if the slider hasn't moved.
            fireStateChanged();
        }
        isUpdating = false;
    }

    public double getConfiguredMinimum() {
        return config.getMinValue();
    }

    public double getConfiguredMaximum() {
        return config.getMaxValue();
    }

    public double getConfiguredIncrement() {
        return config.getIncrement();
    }

    private void precompute() {
        int sliderMax = getMaximum();
        int sliderMin = getMinimum();
        double confMax = config.getMaxValue();
        double confMin = config.getMinValue();
        if (config.isLogScale()) {
            // Fit to "A * exp(B * x)" where "x" is JSlider.getValue():
            B = Math.log(confMax / confMin) / (sliderMax - sliderMin);
            A = confMax / Math.exp(B * sliderMax);
        }
        else {
            // Fit to "m * x + b" where "x" is JSlider.getValue():
            m = (confMax - confMin) / (sliderMax - sliderMin);
            b = confMax - m * sliderMax;
        }
    }
}
