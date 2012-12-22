/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Jun 23, 2005
 * Time: 5:02:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class BandCombine {
    public static native void cByteLoop(byte[] s1, byte[] d,
                                        int s1LineOffset, int dLineOffset,
                                        int s1LineStride, int dLineStride,
                                        int s1PixelStride, int dPixelStride,
                                        double[] matrix);
}
