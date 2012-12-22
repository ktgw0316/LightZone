/*
 * $RCSfile: InterpolationBilinear.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:10 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

/**
 * A class representing bilinear interpolation.  The class is marked
 * 'final' so it may be either automatically or manually inlined.
 *
 * <p> Bilinear interpolation requires a neighborhood extending one
 * pixel to the right and below the central sample.  If the fractional
 * subsample position is given by (xfrac, yfrac), the resampled pixel value 
 * will be:
 *
 * <pre>
 *    (1 - yfrac) * [(1 - xfrac)*s00 + xfrac*s01] + 
 *    yfrac * [(1 - xfrac)*s10 + xfrac*s11]
 * </pre>
 *
 * <p> A neighborhood extending one sample to the right of, and one
 * sample below the central sample is required to perform bilinear
 * interpolation. This implementation maintains equal subsampleBits in x and y.
 *
 * <p> The diagrams below illustrate the pixels involved in one-dimensional
 * bilinear interpolation. Point s0 is the interpolation kernel key position.
 * xfrac and yfrac, indicated by the dots, represent the point of interpolation
 * between two pixels. This value lies between 0.0 and 1.0 exclusive for
 * floating point and 0 and 2<sup>subsampleBits</sup> exclusive for integer
 * interpolations.
 *
 * <pre>
 * <b>
 *         Horizontal              Vertical
 *
 *          s0 .  s1                  s0                             
 *             ^                       .< yfrac                      
 *            xfrac                   s1                        
 * </b>
 * </pre>
 *
 * <p> The diagram below illustrates the pixels involved in
 * two-dimensional bilinear interpolation.
 *                                                                             
 * <pre>
 * <b>
 *                      s00    s01                                     
 *                                                                      
 *                          .      < yfrac                      
 *                                                                      
 *                      s10    s11                                     
 *                          ^                                           
 *                         xfrac                                        
 * </b>
 * </pre>
 *
 * <p> The class is marked 'final' so that it may be more easily inlined.
 */
public final class InterpolationBilinear extends Interpolation {

    /** The value of 1.0 scaled by 2^subsampleBits */
    private int one;

    /** The value of 0.5 scaled by 2^subsampleBits */
    private int round;

    /** The number of bits to shift integer pixels to account for
     *  subsampleBits 
     */
    private int shift;

    /** Twice the value of 'shift'. Accounts for accumulated
     *  scaling shifts in two-axis interpolation
     */
    private int shift2;

    /** The value of 0.5 scaled by 2^shift2 */ 
    private int round2;

    static final int DEFAULT_SUBSAMPLE_BITS = 8;

    /**
     * Constructs an InterpolationBilinear with a given subsample
     * precision, in bits. This precision is applied to both axes.
     *
     * @param subsampleBits the subsample precision.
     */
    public InterpolationBilinear(int subsampleBits) {

        super(2, 2, 0, 1, 0, 1, subsampleBits, subsampleBits);

        shift = subsampleBits;
        one = 1 << shift;
        round = 1 << (shift - 1);

        shift2 = 2*subsampleBits;
        round2 = 1 << (shift2 - 1);
    }

    /**
     * Constructs an InterpolationBilinear with the default subsample
     * precision 0f 8 bits.
     */
    public InterpolationBilinear() {
        this(DEFAULT_SUBSAMPLE_BITS);
    }

    /**
     * Performs horizontal interpolation on a one-dimensional array of integral samples.
     *
     * @param samples an array of ints.
     * @param xfrac the subsample position, multiplied by 2^(subsampleBits).
     * @return the interpolated value as an int.
     */
    public final int interpolateH(int[] samples, int xfrac) {
        return interpolateH(samples[0], samples[1], xfrac);
    }

    /**
     * Performs vertical interpolation on a one-dimensional array of integral samples.
     *
     * @param samples an array of ints.
     * @param yfrac the Y subsample position, multiplied by 2^(subsampleBits).
     * @return the interpolated value as an int.
     */
    public final int interpolateV(int[] samples, int yfrac) {
        return interpolateV(samples[0], samples[1], yfrac);
    }

    /**
     * Performs interpolation on a two-dimensional array of integral samples.
     *
     * @param samples a two-dimensional array of ints.
     * @param xfrac the X subsample position, multiplied by 2^(subsampleBits).
     * @param yfrac the Y subsample position, multiplied by 2^(subsampleBits).
     * @return the interpolated value as an int.
     */
    public final int interpolate(int[][] samples, int xfrac, int yfrac) {
        return interpolate(samples[0][0], samples[0][1],
                           samples[1][0], samples[1][1],
                           xfrac, yfrac);
    }

    /**
     * Performs horizontal interpolation on a pair of integral samples.
     * This method may be used instead of the array version for speed.
     *
     * @param s0 the central sample.
     * @param s1 the sample to the right of the central sample.
     * @param xfrac the subsample position, multiplied by 2^(subsampleBits).
     * @return the interpolated value as an int.
     */
    public final int interpolateH(int s0, int s1, int xfrac) {
	return ( (s1 - s0) * xfrac + (s0 << shift) + round) >> shift;
    }

    /**
     * Performs vertical interpolation on a pair of integral samples.
     * This method may be used instead of the array version for speed.
     *
     * @param s0 the central sample.
     * @param s1 the sample below the central sample.
     * @param yfrac the Y subsample position, multiplied by 2^(subsampleBits).
     * @return the interpolated value as an int.
     */
    public final int interpolateV(int s0, int s1, int yfrac) {
	return ( (s1 - s0) * yfrac + (s0 << shift) + round) >> shift;
    }

    /**
     * Performs horizontal interpolation on a quadruple of integral samples.
     * The outlying samples are ignored. 
     */
    public final int interpolateH(int s_, int s0, int s1, int s2, int xfrac) {
        return interpolateH(s0, s1, xfrac);
    }

    /**
     * Performs vertical interpolation on a quadruple of integral samples.
     * The outlying samples are ignored. 
     */
    public final int interpolateV(int s_, int s0, int s1, int s2, int yfrac) {
        return interpolateV(s0, s1, yfrac);
    }

    /**
     * Performs interpolation on a 2x2 grid of integral samples.
     *
     * @param s00 the central sample.
     * @param s01 the sample to the right of the central sample.
     * @param s10 the sample below the central sample.
     * @param s11 the sample below and to the right of the central sample.
     * @param xfrac the X subsample position, multiplied by 2^(subsampleBits).
     * @param yfrac the Y subsample position, multiplied by 2^(subsampleBits).
     * @return the interpolated value as an int.
     */
    public final int interpolate(int s00, int s01,
                                 int s10, int s11,
                                 int xfrac, int yfrac) {
	int s0 = (s01 - s00) * xfrac + (s00 << shift);
	int s1 = (s11 - s10) * xfrac + (s10 << shift);
	return ( (s1 - s0) * yfrac + (s0 << shift) + round2) >> shift2;
    }

    /**
     * Performs interpolation on a 4x4 grid of integral samples.
     * The outlying samples are ignored.
     */
    public final int interpolate(int s__, int s_0, int s_1, int s_2,
                                 int s0_, int s00, int s01, int s02,
                                 int s1_, int s10, int s11, int s12,
                                 int s2_, int s20, int s21, int s22,
                                 int xfrac, int yfrac) {
        return interpolate(s00, s01, s10, s11, xfrac, yfrac);
    }

    /**
     * Performs horizontal interpolation on a one-dimensional array of
     * floating-point samples.
     *
     * @param samples an array of floats.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.
     */
    public final float interpolateH(float[] samples, float xfrac) {
        return interpolateH(samples[0], samples[1], xfrac);
    }

    /**
     * Performs vertical interpolation on a one-dimensional array of
     * floating-point samples.
     *
     * @param samples an array of floats.
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.
     */
    public final float interpolateV(float[] samples, float yfrac) {
        return interpolateV(samples[0], samples[1], yfrac);
    }

    /**
     * Performs interpolation on a two-dimensional array of
     * floating-point samples.
     *
     * @param samples an array of floats.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.
     */
    public final float interpolate(float[][] samples,
                                   float xfrac, float yfrac) {
        return interpolate(samples[0][0], samples[0][1],
                           samples[1][0], samples[1][1],
                           xfrac, yfrac);
    }

    /**
     * Performs horizontal interpolation on a horizontal pair of floating-point
     * samples.  This method may be used instead of the array version
     * for speed.
     *
     * @param s0 the central sample.
     * @param s1 the sample to the right of the central sample.
     * @param xfrac the subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.
     */
    public final float interpolateH(float s0, float s1, float xfrac) {
        return (s1 - s0)*xfrac + s0;
    }

    /**
     * Performs vertical interpolation on a vertical pair of floating-point
     * samples.  This method may be used instead of the array version
     * for speed.
     *
     * @param s0 the central sample.
     * @param s1 the sample below the central sample.
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.
     */
    public final float interpolateV(float s0, float s1, float yfrac) {
        return (s1 - s0)*yfrac + s0;
    }

    /**
     * Performs horizontal interpolation on a horizontal quad of floating-point
     * samples.  The outlying samples are ignored.
     */
    public final float interpolateH(float s_, float s0, float s1, float s2,
                                   float frac) {
        return interpolateH(s0, s1, frac);
    }

    /**
     * Performs vertical interpolation on a horizontal quad of floating-point
     * samples.  The outlying samples are ignored.
     */
    public final float interpolateV(float s_, float s0, float s1, float s2,
                                    float frac) {
        return interpolateV(s0, s1, frac);
    }

    /**
     * Performs interpolation on a 2x2 grid of floating-point samples.
     *
     * @param s00 the central sample.
     * @param s01 the sample to the right of the central sample.
     * @param s10 the sample below the central sample.
     * @param s11 the sample below and to the right of the central sample.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.
     */
    public final float interpolate(float s00, float s01,
                                   float s10, float s11,
                                   float xfrac, float yfrac) {
        float s0 = (s01 - s00)*xfrac + s00;
        float s1 = (s11 - s10)*xfrac + s10;
        return (s1 - s0)*yfrac + s0;
    }

    /**
     * Performs interpolation on a 4x4 grid.  The outlying samples
     * are ignored.
     */
    public final float interpolate(float s__, float s_0, float s_1, float s_2,
                                   float s0_, float s00, float s01, float s02,
                                   float s1_, float s10, float s11, float s12,
                                   float s2_, float s20, float s21, float s22,
                                   float xfrac, float yfrac) {
        return interpolate(s00, s01, s10, s11, xfrac, yfrac);
    }

    /**
     * Performs horizontal interpolation on a one-dimensional array of
     * double samples.
     *
     * @param samples an array of doubles.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     */
    public final double interpolateH(double[] samples, float xfrac) {
        return interpolateH(samples[0], samples[1], xfrac);
    }

    /**
     * Performs vertical interpolation on a one-dimensional array of
     * double samples.
     *
     * @param samples an array of doubles.
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     */
    public final double interpolateV(double[] samples, float yfrac) {
        return interpolateV(samples[0], samples[1], yfrac);
    }

    /**
     * Performs interpolation on a two-dimensional array of
     * double samples.
     *
     * @param samples an array of doubles.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     */
    public final double interpolate(double[][] samples,
				    float xfrac, float yfrac) {
        return interpolate(samples[0][0], samples[0][1],
                           samples[1][0], samples[1][1],
                           xfrac, yfrac);
    }

    /**
     * Performs horizontal interpolation on a horizontal pair of double
     * samples.  This method may be used instead of the array version
     * for speed.
     *
     * @param s0 the central sample.
     * @param s1 the sample to the right of the central sample.
     * @param xfrac the subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     */
    public final double interpolateH(double s0, double s1, float xfrac) {
        return (s1 - s0)*xfrac + s0;
    }

    /**
     * Performs vertical interpolation on a vertical pair of double
     * samples.  This method may be used instead of the array version
     * for speed.
     *
     * @param s0 the central sample.
     * @param s1 the sample below the central sample.
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     */
    public final double interpolateV(double s0, double s1, float yfrac) {
        return (s1 - s0)*yfrac + s0;
    }

    /**
     * Performs interpolation on a horizontal quad of double
     * samples.  The outlying samples are ignored.
     */
    public final double interpolateH(double s_, double s0, double s1, double s2,
				     float xfrac) {
        return interpolateH(s0, s1, xfrac);
    }

    /**
     * Performs vertical interpolation on a vertical quad of double
     * samples.  The outlying samples are ignored.
     */
    public final double interpolateV(double s_, double s0, double s1, double s2,
				     float yfrac) {
        return interpolateV(s0, s1, yfrac);
    }

    /**
     * Performs interpolation on a 2x2 grid of double samples.
     *
     * @param s00 the central sample.
     * @param s01 the sample to the right of the central sample.
     * @param s10 the sample below the central sample.
     * @param s11 the sample below and to the right of the central sample.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     */
    public final double interpolate(double s00, double s01,
				    double s10, double s11,
				    float xfrac, float yfrac) {
        double s0 = (s01 - s00)*xfrac + s00;
        double s1 = (s11 - s10)*xfrac + s10;
        return (s1 - s0)*yfrac + s0;
    }

    /**
     * Performs interpolation on a 4x4 grid.  The outlying samples
     * are ignored.
     */
    public final double interpolate(double s__,double s_0,double s_1, double s_2,
				    double s0_,double s00,double s01, double s02,
				    double s1_,double s10,double s11, double s12,
				    double s2_,double s20,double s21, double s22,
				    float xfrac, float yfrac) {
        return interpolate(s00, s01, s10, s11, xfrac, yfrac);
    }
}

