/* Copyright (C) 2005-2011 Fabio Riccardi */

#ifndef __HSB_H__
#define __HSB_H__

class HSB {
public:
    static void fromRGB(float rgb[], float hsb[]) {
        float hue, saturation, brightness;
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
    
    static void toRGB(float hsb[], float rgb[]) {
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
};

#endif
