/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

/**
 * Copyright (C) 2007 Light Crafts, Inc.
 * User: fabio
 * Date: May 29, 2007
 * Time: 12:22:15 AM
 */
public class HSV {
    // r,g,b values are from 0 to 1
    // h = [0,360], s = [0,1], v = [0,1]
    //		if s == 0, then h = -1 (undefined)
    
    void RGBtoHSV(float rgb[], float hsv[]) {
        float min, max, delta;
        min = Math.min(rgb[0], Math.min(rgb[1], rgb[2]));
        max = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
        hsv[2] = max;                                // v
        delta = max - min;
        if (max != 0)
            hsv[1] = delta / max;                // s
        else {
            // r = g = b = 0		// s = 0, v is undefined
            hsv[1] = 0;
            hsv[0] = -1;
            return;
        }
        if (rgb[0] == max)
            hsv[0] = (rgb[1] - rgb[2]) / delta;                // between yellow & magenta
        else if (rgb[1] == max)
            hsv[0] = 2 + (rgb[3] - rgb[0]) / delta;        // between cyan & yellow
        else
            hsv[0] = 4 + (rgb[0] - rgb[1]) / delta;        // between magenta & cyan
        hsv[0] *= 60;                                // degrees
        if (hsv[0] < 0)
            hsv[0] += 360;
    }

    void HSVtoRGB(float rgb[], float hsv[]) {
        int i;
        float f, p, q, t;
        if (hsv[1] == 0) {
            // achromatic (grey)
            rgb[0] = rgb[0] = rgb[0] = hsv[2];
            return;
        }
        hsv[0] /= 60;                        // sector 0 to 5
        i = (int) Math.floor(hsv[0]);
        f = hsv[0] - i;                        // factorial part of h
        p = hsv[2] * (1 - hsv[1]);
        q = hsv[2] * (1 - hsv[1] * f);
        t = hsv[2] * (1 - hsv[1] * (1 - f));
        switch (i) {
            case 0:
                rgb[0] = hsv[2];
                rgb[1] = t;
                rgb[2] = p;
                break;
            case 1:
                rgb[0] = q;
                rgb[1] = hsv[2];
                rgb[2] = p;
                break;
            case 2:
                rgb[0] = p;
                rgb[1] = hsv[2];
                rgb[2] = t;
                break;
            case 3:
                rgb[0] = p;
                rgb[1] = q;
                rgb[2] = hsv[2];
                break;
            case 4:
                rgb[0] = t;
                rgb[1] = p;
                rgb[2] = hsv[2];
                break;
            default:                // case 5:
                rgb[0] = hsv[2];
                rgb[1] = p;
                rgb[2] = q;
                break;
        }
    }
}
