/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Apr 28, 2005
 * Time: 9:27:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class PixelBlender {
    public static native void cUShortLoop(short[] s1, short[] s2, short[] d, byte[] m,
                                          int s1Bands, int s2Bands, int dBands,
                                          int s1LineOffset, int s2LineOffset, int dLineOffset, int mLineOffset,
                                          int s1LineStride, int s2LineStride, int dLineStride, int mLineStride,
                                          int s1PixelStride, int s2PixelStride, int dPixelStride, int mPixelStride,
                                          int dheight, int dwidth, int intOpacity, int mode, float[] colorSelection);

    public static native void cUShortLoopCS(short[] s1, short[] s2, short[] d, byte[] m, byte[] cs,
                                            int s1Bands, int s2Bands, int dBands,
                                            int s1LineOffset, int s2LineOffset, int dLineOffset, int mLineOffset, int csLineOffset,
                                            int s1LineStride, int s2LineStride, int dLineStride, int mLineStride, int csLineStride,
                                            int s1PixelStride, int s2PixelStride, int dPixelStride, int mPixelStride, int csPixelStride,
                                            int dheight, int dwidth, int intOpacity, int mode);
}
