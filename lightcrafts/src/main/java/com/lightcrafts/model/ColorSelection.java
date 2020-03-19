/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

/**
 * An immutable, fuzzy interval in a hue-brightness color space where an
 * Operation may be constrained to act.  All numbers are in the interval
 * 0 to 1.
 * @see Operation#setColorSelection(ColorSelection)
 */
public class ColorSelection {

    public final float hueLower;
    public final float hueUpper;
    public final float hueLowerFeather;
    public final float hueUpperFeather;

    public final float luminosityLower;
    public final float luminosityUpper;
    public final float luminosityLowerFeather;
    public final float luminosityUpperFeather;

    public final boolean isHueEnabled;
    public final boolean isLuminosityEnabled;

    /**
     * A default ColorSelection has intervals of size one with widths of zero,
     * which includes all colors and works like no selection.
     */
    @SuppressWarnings({"ParameterHidesMemberVariable"})
    public ColorSelection( float hueLower, float hueLowerFeather,
                           float hueUpper, float hueUpperFeather,
                           float brightnessLower, float brightnessLowerFeather,
                           float brightnessUpper, float brightnessUpperFeather,
                           boolean isHueEnabled, boolean isBrightnessEnabled )
    {
        if ( hueLower > hueUpper ||
             brightnessLower > brightnessUpper )
            throw new IllegalArgumentException();

        this.hueLower = hueLower;
        this.hueUpper = hueUpper;
        this.hueLowerFeather = hueLowerFeather;
        this.hueUpperFeather = hueUpperFeather;
        this.luminosityLower = brightnessLower;
        this.luminosityUpper = brightnessUpper;
        this.luminosityLowerFeather = brightnessLowerFeather;
        this.luminosityUpperFeather = brightnessUpperFeather;
        this.isHueEnabled = isHueEnabled;
        this.isLuminosityEnabled = isBrightnessEnabled;
    }

    public ColorSelection() {
        this(0, 0, 1, 0, 0, 0, 1, 0, true, true);
    }

    public boolean isAllSelected() {
        return ((hueLowerFeather == 0
                 && hueUpperFeather == 0
                 && hueUpper - hueLower == 1) || !isHueEnabled)
               && ((luminosityLowerFeather == 0
                    && luminosityUpperFeather == 0
                    && luminosityUpper - luminosityLower == 1) || !isLuminosityEnabled);
    }

    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        buffer.append("brightness ([");
        buffer.append(luminosityLower);
        buffer.append(" +/- ");
        buffer.append(luminosityLowerFeather);
        buffer.append("] -> [");
        buffer.append(luminosityUpper);
        buffer.append(" +/- ");
        buffer.append(luminosityUpperFeather);
        buffer.append("]); hue ([");
        buffer.append(hueLower);
        buffer.append(" +/- ");
        buffer.append(hueLowerFeather );
        buffer.append("] -> [");
        buffer.append(hueUpper);
        buffer.append(" +/- ");
        buffer.append(hueUpperFeather );
        buffer.append("])");
        buffer.append(" hue=");
        buffer.append(isHueEnabled);
        buffer.append(" brightness=");
        buffer.append(isLuminosityEnabled);
        return buffer.toString();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ColorSelection that = (ColorSelection) o;

        if (that.luminosityLower != luminosityLower) return false;
        if (that.luminosityLowerFeather != luminosityLowerFeather) return false;
        if (that.luminosityUpper != luminosityUpper) return false;
        if (that.luminosityUpperFeather != luminosityUpperFeather) return false;
        if (that.hueLower != hueLower) return false;
        if (that.hueLowerFeather != hueLowerFeather) return false;
        if (that.hueUpper != hueUpper) return false;
        if (that.hueUpperFeather != hueUpperFeather) return false;
        if (isLuminosityEnabled != that.isLuminosityEnabled) return false;
        if (isHueEnabled != that.isHueEnabled) return false;

        return true;
    }
}
