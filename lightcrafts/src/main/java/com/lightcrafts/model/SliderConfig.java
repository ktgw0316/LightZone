/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.model;

import java.text.DecimalFormat;

import lombok.Getter;

/** This is a struct to bundle together options for sliders generated from
  * GenericOperation.
  * <p>
  * These options are:
  * <ul>
  * <li>minValue: a lower-bound number for the slider</li>
  * <li>maxValue: an upper-bound number for the slider</li>
  * <li>defaultValue: an initial value for the slider</li>
  * <li>isLogScale: a flag, false is linear, true is logarithmic</li>
  * <li>hasText: if true, the slider will be coupled to a validating,
  * editable text field</li>
  * </ul>
  * <p>
  * These options have various consistency conditions.  If a consistency
  * condtion is violated, the SliderConfig constructor will throw
  * IllegalArgumentException.
  * <p>
  * @see com.lightcrafts.model.GenericOperation
  */

public class SliderConfig {

    @Getter
    private double minValue;

    @Getter
    private double maxValue;

    @Getter
    private double defaultValue;

    @Getter
    private double increment;

    @Getter
    private boolean isLogScale;

    @Getter
    private DecimalFormat decimalFormat;

    /** Generate a default SliderConfig with minimum 0, maximum 1, and
      * default 0.5.  Log scale and text options are off by default.
      */
    public SliderConfig() {
        this(0., 1.);
    }

    /** Generate a SliderConfig with the given minimum and maximum values.
      * The Default value will be set to the midpoint between these
      * arguments, and log scale and text options will be off.
      */
    public SliderConfig(double minValue, double maxValue) {
        this(
            minValue,
            maxValue,
            (minValue + maxValue) / 2.,
            getDefaultIncrement(minValue, maxValue)
        );
    }

    /** Generate a SliderConfig with the given range and default value.  Log
      * scale and text options will be off.
      */
    public SliderConfig(
        double minValue,
        double maxValue,
        double defaultValue,
        double increment
    ) {
        this(minValue, maxValue, defaultValue, increment, false, null);
    }

    /** Completely specify a SliderConfig.
      */
    public SliderConfig(
        double minValue,
        double maxValue,
        double defaultValue,
        double increment,
        boolean isLogScale,
        DecimalFormat decimalFormat
    ) {
        boolean valid = true;
        valid &= minValue < maxValue;
        valid &= minValue <= defaultValue;
        valid &= defaultValue <= maxValue;
        if (isLogScale) {
            valid &= minValue > 0.;
        }
        if (! valid) {
            throw new IllegalArgumentException();
        }
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.defaultValue = defaultValue;
        this.increment = increment;
        this.isLogScale = isLogScale;
        this.decimalFormat = decimalFormat;
    }

    public boolean hasText() {
        return decimalFormat != null;
    }

    private static double getDefaultIncrement(double min, double max) {
        return Math.pow(10, log10(max - min) - 3);
    }

    private static double log10(double x) {
        return Math.log(x) / Math.log(10);
    }
}
