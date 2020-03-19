/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import java.awt.color.ColorSpace;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Mar 12, 2007
 * Time: 4:30:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class LCMS_LabColorSpace extends ColorSpace {
    public LCMS_LabColorSpace() {
        super (TYPE_Lab, 3);
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
