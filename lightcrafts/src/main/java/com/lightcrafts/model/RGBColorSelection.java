/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Jul 30, 2007
 * Time: 11:48:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class RGBColorSelection {
    public final float red;
    public final float green;
    public final float blue;

    public final float radius;

    public final float luminosityLower;
    public final float luminosityUpper;
    public final float luminosityLowerFeather;
    public final float luminosityUpperFeather;

    public final boolean isInverted;

    public final boolean isColorEnabled;
    public final boolean isLuminosityEnabled;

    public RGBColorSelection() {
        this( 0.5f, 0.5f, 0.5f, -1f, 0, 0, 1, 0, false, true, true );
    }

    private static float lastRed = 0.5f;
    private static float lastGreen = 0.5f;
    private static float lastBlue = 0.5f;

    public RGBColorSelection( float red, float green, float blue, float radius,
                              float luminosityLower,
                              float luminosityLowerFeather,
                              float luminosityUpper,
                              float luminosityUpperFeather,
                              boolean isInverted,
                              boolean isColorEnabled,
                              boolean isLuminosityEnabled ) {
        lastRed = this.red = red;
        lastGreen = this.green = green;
        lastBlue = this.blue = blue;
        this.radius = radius;
        this.luminosityLower = luminosityLower;
        this.luminosityUpper = luminosityUpper;
        this.luminosityLowerFeather = luminosityLowerFeather;
        this.luminosityUpperFeather = luminosityUpperFeather;
        this.isInverted = isInverted;
        this.isColorEnabled = isColorEnabled;
        this.isLuminosityEnabled = isLuminosityEnabled;
    }

    private static final float[] gm_blue = {0.0902f, 0.0471f, 0.2745f};
    private static final float[] gm_green = {0.1294f, 0.2667f, 0.0824f};
    private static final float[] gm_red = {0.2392f, 0.0706f, 0.0471f};
    private static final float[] gm_yellow = {0.5882f, 0.5882f, 0.0863f};
    private static final float[] gm_magenta = {0.3529f, 0.1412f, 0.2980f};
    private static final float[] gm_cyan = {0.1373f, 0.2275f, 0.3765f};

    public RGBColorSelectionPreset getPreset() {
        if (radius == -1) {
            // System.out.println("AllColors");
            return RGBColorSelectionPreset.AllColors;
        }

        final float eps = 0.001f;

        if (Math.abs(red - gm_red[0]) < eps && Math.abs(green - gm_red[1]) < eps && Math.abs(blue - gm_red[2]) < eps) {
            // System.out.println("Reds");
            return RGBColorSelectionPreset.Reds;
        } else if (Math.abs(red - gm_yellow[0]) < eps && Math.abs(green - gm_yellow[1]) < eps && Math.abs(blue - gm_yellow[2]) < eps) {
            // System.out.println("Yellows");
            return RGBColorSelectionPreset.Yellows;
        } else if (Math.abs(red - gm_green[0]) < eps && Math.abs(green - gm_green[1]) < eps && Math.abs(blue - gm_green[2]) < eps) {
            // System.out.println("Greens");
            return RGBColorSelectionPreset.Greens;
        } else if (Math.abs(red - gm_cyan[0]) < eps && Math.abs(green - gm_cyan[1]) < eps && (blue - gm_cyan[2]) < eps) {
            // System.out.println("Cyans");
            return RGBColorSelectionPreset.Cyans;
        } else if (Math.abs(red - gm_blue[0]) < eps && Math.abs(green - gm_blue[1]) < eps && Math.abs(blue - gm_blue[2]) < eps) {
            // System.out.println("Blues");
            return RGBColorSelectionPreset.Blues;
        } else if (Math.abs(red - gm_magenta[0]) < eps && Math.abs(green - gm_magenta[1]) < eps && Math.abs(blue - gm_magenta[2]) < eps) {
            // System.out.println("Magentas");
            return RGBColorSelectionPreset.Magentas;
        }

        // System.out.println("SampledColors");
        return RGBColorSelectionPreset.SampledColors;
    }

    public int hashCode() {
        return  Float.floatToIntBits( red ) ^
                Float.floatToIntBits( green ) ^
                Float.floatToIntBits( blue ) ^
                Float.floatToIntBits( radius ) ^
                Float.floatToIntBits( luminosityLower ) ^
                Float.floatToIntBits( luminosityLowerFeather ) ^
                Float.floatToIntBits( luminosityUpper ) ^
                Float.floatToIntBits( luminosityUpperFeather ) ^
                (isColorEnabled ? 2 : 0) ^
                (isLuminosityEnabled ? 1 : 0) ^
                (isInverted ? 1 : 0);
    }

    public RGBColorSelection( RGBColorSelectionPreset p, boolean isInverted ) {
        switch (p) {
            case AllColors:
                red = 0.5f;
                green = 0.5f;
                blue = 0.5f;
                radius = -1;
                break;

            case SampledColors:
                red = lastRed;
                green = lastGreen;
                blue = lastBlue;
                radius = 0.4f;
                break;

            case Reds:
                red = gm_red[0];
                green = gm_red[1];
                blue = gm_red[2];
                radius = 0.4f;
                break;

            case Yellows:
                red = gm_yellow[0];
                green = gm_yellow[1];
                blue = gm_yellow[2];
                radius = 0.4f;
                break;

            case Greens:
                red = gm_green[0];
                green = gm_green[1];
                blue = gm_green[2];
                radius = 0.4f;
                break;

            case Cyans:
                red = gm_cyan[0];
                green = gm_cyan[1];
                blue = gm_cyan[2];
                radius = 0.4f;
                break;

            case Blues:
                red = gm_blue[0];
                green = gm_blue[1];
                blue = gm_blue[2];
                radius = 0.4f;
                break;

            case Magentas:
                red = gm_magenta[0];
                green = gm_magenta[1];
                blue = gm_magenta[2];
                radius = 0.4f;
                break;

            default:
                throw new IllegalArgumentException("Unknown Color Selection Preset: " + p.name());
        }

//        float feather = 0.1f;
//
//        float luminosity = (float) (Math.log1p(0xff * ColorScience.Wr * red +
//                                               0xff * ColorScience.Wg * green +
//                                               0xff * ColorScience.Wb * blue) / (8 * Math.log(2)));
//
//        float minLuminosity = Math.max(luminosity-feather, 0);
//        float minLuminosityFeather = Math.min(minLuminosity, feather);
//
//        float maxLuminosity = Math.min(luminosity+feather, 1);
//        float maxLuminosityFeather = Math.min(1-maxLuminosity, feather);

        luminosityLower = 0;
        luminosityUpper = 1;
        luminosityLowerFeather = 0;
        luminosityUpperFeather = 0;
        this.isInverted = isInverted;
        isColorEnabled = true;
        isLuminosityEnabled = true;
    }

    public boolean isAllSelected() {
        return ((!isColorEnabled || radius < 0)
                && ((luminosityLowerFeather == 0
                     && luminosityUpperFeather == 0
                     && luminosityUpper - luminosityLower == 1) || !isLuminosityEnabled));
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RGBColorSelection that = (RGBColorSelection) o;

        if (Float.compare(that.blue, blue) != 0) return false;
        if (Float.compare(that.green, green) != 0) return false;
        if (isColorEnabled != that.isColorEnabled) return false;
        if (isLuminosityEnabled != that.isLuminosityEnabled) return false;
        if (isInverted != that.isInverted) return false;
        if (Float.compare(that.luminosityLower, luminosityLower) != 0) return false;
        if (Float.compare(that.luminosityLowerFeather, luminosityLowerFeather) != 0) return false;
        if (Float.compare(that.luminosityUpper, luminosityUpper) != 0) return false;
        if (Float.compare(that.luminosityUpperFeather, luminosityUpperFeather) != 0) return false;
        if (Float.compare(that.radius, radius) != 0) return false;
        if (Float.compare(that.red, red) != 0) return false;

        return true;
    }

    public Color toColor() {
        return new Color( red, green, blue );
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
        buffer.append("]); rgb ([");
        buffer.append(red);
        buffer.append(", ");
        buffer.append(green);
        buffer.append(", ");
        buffer.append(blue);
        buffer.append(" : ");
        buffer.append(radius);
        buffer.append("])");
        buffer.append(" hue=");
        buffer.append(isColorEnabled);
        buffer.append(" brightness=");
        buffer.append(isLuminosityEnabled);
        buffer.append(" inverted=");
        buffer.append(isInverted);
        return buffer.toString();
    }
}
