/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import java.awt.color.ColorSpace;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Jul 27, 2006
 * Time: 4:48:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class LCMS_ColorSpace extends ColorSpace {
    private final LCMS.Profile profile;

    public LCMS_ColorSpace(LCMS.Profile profile) {
        super (profile instanceof LCMS.LABProfile ? TYPE_Lab : TYPE_RGB, 3);
        this.profile = profile;
    }

    public LCMS.Profile getProfile() {
        return profile;
    }

    public float[] toRGB(float[] colorvalue) {
        return new float[0];
    }

    public float[] fromRGB(float[] rgbvalue) {
        return new float[0];
    }

    public float[] toCIEXYZ(float[] colorvalue) {
        return new float[0];
    }

    public float[] fromCIEXYZ(float[] colorvalue) {
        return new float[0];
    }
}
