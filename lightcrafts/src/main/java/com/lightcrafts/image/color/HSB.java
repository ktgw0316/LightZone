/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.color;

/**
 * Copyright (C) 2007 Light Crafts, Inc.
 * User: fabio
 * Date: May 26, 2007
 * Time: 4:37:19 PM
 */
public class HSB {
    public static void fromRGB(float[] rgb, float[] hsb) {
        float hue, saturation, brightness;
        if (hsb == null)
            hsb = new float[3];
        float cmax = (rgb[0] > rgb[1]) ? rgb[0] : rgb[1];
        if (rgb[2] > cmax) cmax = rgb[2];
        float cmin = (rgb[0] < rgb[1]) ? rgb[0] : rgb[1];
        if (rgb[2] < cmin) cmin = rgb[2];

        brightness = cmax;
        if (cmax != 0)
            saturation = (cmax - cmin) / cmax;
        else
            saturation = 0;
        if (saturation == 0)
            hue = 0;
        else {
            float redc = (cmax - rgb[0]) / (cmax - cmin);
            float greenc = (cmax - rgb[1]) / (cmax - cmin);
            float bluec = (cmax - rgb[2]) / (cmax - cmin);
            if (rgb[0] == cmax)
                hue = bluec - greenc;
            else if (rgb[1] == cmax)
                hue = 2.0f + redc - bluec;
            else
                hue = 4.0f + greenc - redc;
            hue = hue / 6.0f;
            if (hue < 0)
                hue = hue + 1.0f;
        }
        hsb[0] = hue;
        hsb[1] = saturation;
        hsb[2] = brightness;
    }

    public static float hue(float r, float g, float b) {
        float hue;
        float cmax = (r > g) ? r : g;
        if (b > cmax) cmax = b;
        float cmin = (r < g) ? r : g;
        if (b < cmin) cmin = b;

        if (cmax - cmin == 0)
            hue = 0;
        else {
            float redc = (cmax - r) / (cmax - cmin);
            float greenc = (cmax - g) / (cmax - cmin);
            float bluec = (cmax - b) / (cmax - cmin);
            if (r == cmax)
                hue = bluec - greenc;
            else if (g == cmax)
                hue = 2.0f + redc - bluec;
            else
                hue = 4.0f + greenc - redc;
            hue = hue / 6.0f;
            if (hue < 0)
                hue = hue + 1.0f;
        }
        return hue;
    }

    public static void toRGBSlow(float[] hsb, float[] rgb) {
        if (hsb[1] == 0)
            rgb[0] = rgb[1] = rgb[2] = hsb[2];
        else {
            rgb[0] = rgb[1] = rgb[2] = 0;

            float h = (hsb[0] - (float) Math.floor(hsb[0])) * 6.0f;
            float f = h - (float) java.lang.Math.floor(h);
            float p = hsb[2] * (1.0f - hsb[1]);
            float q = hsb[2] * (1.0f - hsb[1] * f);
            float t = hsb[2] * (1.0f - (hsb[1] * (1.0f - f)));

            switch ((int) h) {
                case 0:
                    rgb[0] = hsb[2];
                    rgb[1] = t;
                    rgb[2] = p;
                    break;
                case 1:
                    rgb[0] = q;
                    rgb[1] = hsb[2];
                    rgb[2] = p;
                    break;
                case 2:
                    rgb[0] = p;
                    rgb[1] = hsb[2];
                    rgb[2] = t;
                    break;
                case 3:
                    rgb[0] = p;
                    rgb[1] = q;
                    rgb[2] = hsb[2];
                    break;
                case 4:
                    rgb[0] = t;
                    rgb[1] = p;
                    rgb[2] = hsb[2];
                    break;
                case 5:
                    rgb[0] = hsb[2];
                    rgb[1] = p;
                    rgb[2] = q;
                    break;
            }
        }
    }

    public static void toRGB(float[] hsb, float[] rgb) {
        if (hsb[2] == 0) {   // safety short circuit again
            rgb[0] = rgb[1] = rgb[2] = 0;
            return;
        }

        if (hsb[1] == 0) {   // grey
            rgb[0] = rgb[1] = rgb[2] = hsb[2];
            return;
        }

        if (hsb[0] < 1.0 / 6) {
            // red domain; green ascends
            float domainOffset = hsb[0];
            rgb[0] = hsb[2];
            rgb[2] = hsb[2] * (1 - hsb[1]);
            rgb[1] = rgb[2] + (hsb[2] - rgb[2]) * domainOffset * 6;
        } else if (hsb[0] < 2.0 / 6) {
            // yellow domain; red descends
            float domainOffset = hsb[0] - 1.0f / 6;
            rgb[1] = hsb[2];
            rgb[2] = hsb[2] * (1 - hsb[1]);
            rgb[0] = rgb[1] - (hsb[2] - rgb[2]) * domainOffset * 6;
        } else if (hsb[0] < 3.0 / 6) {
            // green domain; blue ascends
            float domainOffset = hsb[0] - 2.0f / 6;
            rgb[1] = hsb[2];
            rgb[0] = hsb[2] * (1 - hsb[1]);
            rgb[2] = rgb[0] + (hsb[2] - rgb[0]) * domainOffset * 6;
        } else if (hsb[0] < 4.0 / 6) {
            // cyan domain; green descends
            float domainOffset = hsb[0] - 3.0f / 6;
            rgb[2] = hsb[2];
            rgb[0] = hsb[2] * (1 - hsb[1]);
            rgb[1] = rgb[2] - (hsb[2] - rgb[0]) * domainOffset * 6;
        } else if (hsb[0] < 5.0 / 6) {
            // blue domain; red ascends
            float domainOffset = hsb[0] - 4.0f / 6;
            rgb[2] = hsb[2];
            rgb[1] = hsb[2] * (1 - hsb[1]);
            rgb[0] = rgb[1] + (hsb[2] - rgb[1]) * domainOffset * 6;
        } else {
            // magenta domain; blue descends
            float domainOffset = hsb[0] - 5.0f / 6;
            rgb[0] = hsb[2];
            rgb[1] = hsb[2] * (1 - hsb[1]);
            rgb[2] = rgb[0] - (hsb[2] - rgb[1]) * domainOffset * 6;
        }
    }
}
