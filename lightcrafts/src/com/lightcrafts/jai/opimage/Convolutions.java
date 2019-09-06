/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Jun 3, 2005
 * Time: 2:49:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class Convolutions {
    public static native void cByteLoop(byte[] s1, byte[] d,
                                        int s1LineOffset, int dLineOffset,
                                        int s1LineStride, int dLineStride,
                                        int s1PixelStride, int dPixelStride,
                                        int dheight, int dwidth, int kw, int kh,
                                        float[] hValues, float[] vValues);

    public static native void cShortLoop(short[] s1, short[] d,
                                         int s1LineOffset, int dLineOffset,
                                         int s1LineStride, int dLineStride,
                                         int s1PixelStride, int dPixelStride,
                                         int dheight, int dwidth, int kw, int kh,
                                         float[] hValues, float[] vValues);

    public static native void cUShortLoop(short[] s1, short[] d,
                                          int s1LineOffset, int dLineOffset,
                                          int s1LineStride, int dLineStride,
                                          int s1PixelStride, int dPixelStride,
                                          int dheight, int dwidth, int kw, int kh,
                                          float[] hValues, float[] vValues);

    public static native void cIntLoop(int[] s1, int[] d,
                                       int s1LineOffset, int dLineOffset,
                                       int s1LineStride, int dLineStride,
                                       int s1PixelStride, int dPixelStride,
                                       int dheight, int dwidth, int kw, int kh,
                                       float[] hValues, float[] vValues);

    public static native void cFloatLoop(float[] s1, float[] d,
                                         int s1LineOffset, int dLineOffset,
                                         int s1LineStride, int dLineStride,
                                         int s1PixelStride, int dPixelStride,
                                         int dheight, int dwidth, int kw, int kh,
                                         float[] hValues, float[] vValues);

    public static native void cDoubleLoop(double[] s1, double[] d,
                                          int s1LineOffset, int dLineOffset,
                                          int s1LineStride, int dLineStride,
                                          int s1PixelStride, int dPixelStride,
                                          int dheight, int dwidth, int kw, int kh,
                                          float[] hValues, float[] vValues);

    public static native void cInterleaved3ByteLoop(byte[] s1, byte[] d,
                                                    int s1LineOffset, int dLineOffset,
                                                    int s1LineStride, int dLineStride,
                                                    int dheight, int dwidth, int kw, int kh,
                                                    float[] hValues, float[] vValues);

    public static native void cInterleaved3ShortLoop(short[] s1, short[] d,
                                                     int s1LineOffset, int dLineOffset,
                                                     int s1LineStride, int dLineStride,
                                                     int dheight, int dwidth, int kw, int kh,
                                                     float[] hValues, float[] vValues);

    public static native void cInterleaved3UShortLoop(short[] s1, short[] d,
                                                      int s1LineOffset, int dLineOffset,
                                                      int s1LineStride, int dLineStride,
                                                      int dheight, int dwidth, int kw, int kh,
                                                      float[] hValues, float[] vValues);

    public static native void cInterleaved3IntLoop(int[] s1, int[] d,
                                                   int s1LineOffset, int dLineOffset,
                                                   int s1LineStride, int dLineStride,
                                                   int dheight, int dwidth, int kw, int kh,
                                                   float[] hValues, float[] vValues);
}
