/*
 * $RCSfile: Interpolation.java,v $
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
import java.io.Serializable;

/**
 * An object encapsulating a particular algorithm for image
 * interpolation (resampling).  An Interpolation captures the notion
 * of performing sampling on a regular grid of pixels using a local
 * neighborhood.  It is intended to be used by operations that
 * resample their sources, including affine mapping and warping.
 *
 * <p> Resampling is the action of computing a pixel value at a
 * possibly non-integral position of an image.  The image defines
 * pixel values at integer lattice points, and it is up to the
 * resampler to produce a reasonable value for positions not falling
 * on the lattice.  A number of techniques are used in practice, the
 * most common being nearest-neighbor, which simply takes the value of
 * the closest lattice point; bilinear, which interpolates linearly
 * between the four closest lattice points; and bicubic, which applies
 * a piecewise polynomial function to a 4x4 neighborhood of nearby
 * points.  The area over which a resampling function needs to be
 * computed is referred to as its support; thus the standard
 * resampling functions have supports of 1, 4, and 16 pixels
 * respectively.  Mathematically, the ideal resampling function for a
 * band-limited image (one containing no energy above a given
 * frequency) is the sinc function, equal to sin(x)/x.  This has
 * practical limitations, in particular its infinite support, which
 * lead to the use of the standard approximations described above.
 *
 * <p> Other interpolation functions may be required to solve problems
 * other than the resampling of band-limited image data.  When
 * shrinking an image, it is common to use a function that combines
 * area averaging with resampling in order to remove undesirable high
 * frequencies as part of the interpolation process.  Other
 * application areas may use interpolating functions that operate under
 * other assumptions about image data, such as taking the maximum
 * value of a 2x2 neighborhood.  The interpolation class provides a
 * framework in which a variety of interpolation schemes may be
 * expressed.
 *
 * <p> Many interpolations are separable, that is, they may be 
 * equivalently rewritten as a horizontal interpolation followed
 * by a vertical one (or vice versa).  In practice, some precision
 * may be lost by the rounding and truncation that takes place
 * between the passes.  The Interpolation class assumes separability
 * and implements all vertical interpolation methods in terms of
 * corresponding horizontal methods, and defines isSeparable() to
 * return true.  A subclass may override these methods to provide
 * distinct implementations of horizontal and vertical interpolation.
 * Some subclasses may implement the two-dimensional interpolation
 * methods directly, yielding more precise results, while others
 * may implement these using a two-pass approach.
 *
 * <p> A minimal Interpolation subclass must call the Interpolation
 *     constructor (super()) and then set at least the following fields.
 * <pre>
 *   leftPadding
 *   rightPadding
 *   topPadding
 *   bottomPadding
 *   width
 *   height
 *   subsampleBitsH
 *   subsampleBitsV
 * </pre>
 * <p> It must also implement at least the following methods.
 * <pre>
 *   int interpolateH(int[] samples, int xfrac)
 *   float interpolateH(float[] samples, float xfrac)
 *   double interpolateH(double[] samples, float xfrac)
 * </pre>
 * All other methods are defined in terms of these methods for ease of
 * implementation of new Interpolation subclasses.
 *
 * <p> Since interpolation is generally performed for every pixel of a
 * destination image, efficiency is important.  In particular, passing
 * source samples by means of arrays is likely to be unacceptably
 * slow.  Accordingly, methods are provided for the common cases of
 * 2x1, 1x2, 4x1, 1x4, 2x2, and 4x4 input grids.  These methods are
 * defined in the superclass to package their arguments into arrays
 * and forward the call to the array versions, in order to simplify
 * implementation.  They should be called only on Interpolation objects
 * with the correct width and height.  In other words, an implementor
 * of an Interpolation subclass may implement "interpolateH(int s0, int s1,
 * int xfrac)" assuming that the interpolation width is in fact equal to
 * 2, and does not need to enforce this constraint.
 *
 * <p> The fractional position of interpolation (xfrac, yfrac) is always 
 * between 0.0 and 1.0 (not including 1.0). For integral image data, 
 * the fraction is represented as a scaled integer between 0 and
 * 2<sup>n</sup> - 1, where n is a small integer. The value of n 
 * in the horizontal and vertical directions may be
 * obtained by calling getSubsampleBitsH() and getSubsampleBitsV().
 * In general, code that makes use of an externally-provided
 * Interpolation object must query that object to determine its
 * desired positional precision.
 *
 * <p> For float and double images, a float between 0.0F and 1.0F
 * (not including 1.0F) is used as a positional specifier in the interest of
 * greater accuracy.
 *
 * <p> It is important to understand that the subsampleBits precision
 * is used only to indicate the scaling implicit in the fractional locations
 * (xfrac, yfrac) for integral image data types. For example, for 
 * subsampleBitsH=8, xfrac must lie between 0 and 255 inclusive.
 * An implementation is not required to actually quantize its interpolation
 * coefficients to match the specified subsampling precision.
 *
 * <p> The diagrams below illustrate the pixels involved in one-dimensional
 * interpolation. Point s0 is the interpolation kernel key position.
 * xfrac and yfrac, indicated by the dots, represent the point of interpolation
 * between two pixels. This value lies between 0.0 and 1.0 exclusive for
 * floating point and 0 and 2<sup>subsampleBits</sup> exclusive for integer
 * interpolations.
 *
 * <pre>
 * <b>
 *         Horizontal              Vertical
 *
 *    s_    s0 .  s1    s2            s_                             
 *             ^                                                     
 *            xfrac                   s0                        
 *                                     .< yfrac                  
 *                                    s1                         
 *                                                                      
 *                                    s2                         
 *                                                                      
 *
 * </b>
 * </pre>
 *
 * <p> The diagram below illustrates the pixels involved in
 * two-dimensional interpolation. Point s00 is the interpolation kernel 
 * key position.
 *                                                                             
 * <pre>
 * <b>
 *                                                                      
 *               s__    s_0    s_1    s_2                              
 *                                                                      
 *                                                                      
 *                                                                      
 *               s0_    s00    s01    s02                              
 *                                                                      
 *                          .             < yfrac                      
 *                                                                      
 *               s1_    s10    s11    s12                              
 *                                                                      
 *                                                                      
 *                                                                      
 *               s2_    s20    s21    s22                              
 *                          ^                                           
 *                         xfrac                                        
 *                                                                      
 * </b>
 * </pre>
 *
 * <p> The subclasses of Interpolation include InterpolationNearest,
 * InterpolationBilinear, InterpolationBicubic, and
 * InterpolationBicubic2 (a variant defined by a different polynomial
 * function).  These subclasses are marked 'final,' so users may
 * identify them by name (using 'instanceof') and write specialized
 * code for them.  This may also allow inlining to occur on some virtual
 * machines.  These classes do provide correct, if less than
 * optimal code for performing their interpolations, so it is
 * possible to use any Interpolation object in a generic manner.
 * The Sun-provided InterpolationBilinear and InterpolationBicubic
 * classes provide a more optimal implementation while using the same semantics.
 *
 * <p> The InterpolationTable class is a subclass of Interpolation
 * that divides the set of subsample positions into a fixed number of
 * "bins" and stores a kernel for each bin.  InterpolationBicubic and
 * InterpolationBicubic2 are implemented in terms of
 * InterpolationTable since a direct implementation is very expensive.
 *
 * @see InterpolationNearest
 * @see InterpolationBilinear
 * @see InterpolationBicubic
 * @see InterpolationBicubic2
 * @see InterpolationTable
 *
 */
public abstract class Interpolation extends Object implements Serializable {

    /** 
     * A constant specifying interpolation by the InterpolationNearest class.
     */
    public static final int INTERP_NEAREST = 0;

    /** 
     * A constant specifying interpolation by the InterpolationBilinear class.
     */
    public static final int INTERP_BILINEAR = 1;

    /** 
     * A constant specifying interpolation by the InterpolationBicubic class.
     */
    public static final int INTERP_BICUBIC = 2;

    /** 
     * A constant specifying interpolation by the InterpolationBicubic2 class.
     */
    public static final int INTERP_BICUBIC_2 = 3;

    private static Interpolation nearestInstance = null; 

    private static Interpolation bilinearInstance = null;

    private static Interpolation bicubicInstance = null;

    private static Interpolation bicubic2Instance = null;


    /**
     * The number of pixels lying to the left
     * of the interpolation kernel key position.
     */
    protected int leftPadding;

    /**
     * The number of pixels lying to the right
     * of the interpolation kernel key position.
     */
    protected int rightPadding;

    /**
     * The number of pixels lying above the interpolation kernel key position.
     */
    protected int topPadding;

    /**
     * The number of pixels lying below the interpolation kernel key position.
     */
    protected int bottomPadding;

    /**
     * The numbers of bits used for the horizontal subsample position. 
     * This value determines how integer fractional positons are
     * to  be interpreted.
     */
    protected int subsampleBitsH;

    /**
     * The number of bits used for the vertical subsample position. 
     * This value determines how integer fractional positons are
     * to  be interpreted.
     */
    protected int subsampleBitsV;

    /**
     * The width of the interpolation kernel in pixels.
     */
    protected int width;

    /**
     * The height of the interpolation kernel in pixels.
     */
    protected int height;

    /**
     * Creates an interpolation of one of the standard types.
     * This is intended strictly as a convenience method.
     * The resulting static object is cached for later reuse.
     *
     * @param type one of:
     *        INTERP_NEAREST,
     *        INTERP_BILINEAR,
     *        INTERP_BICUBIC, or
     *        INTERP_BICUBIC_2
     * @return an appropriate Interpolation object.
     * @throws IllegalArgumentException if an unrecognized type is supplied.
     */
    public synchronized static Interpolation getInstance(int type) {
        Interpolation interp = null;

        switch (type) {
          case INTERP_NEAREST:
            if (nearestInstance == null) {
                interp = nearestInstance = new InterpolationNearest();
            } else {
                interp = nearestInstance;
            }
            break;
          case INTERP_BILINEAR:
            if (bilinearInstance == null) {
                interp = bilinearInstance = new InterpolationBilinear();
            } else {
                interp = bilinearInstance;
            }
            break;
          case INTERP_BICUBIC:
            if (bicubicInstance == null) {
                interp = bicubicInstance = new InterpolationBicubic(8);
            } else {
                interp = bicubicInstance;
            }
            break;
          case INTERP_BICUBIC_2:
            if (bicubic2Instance == null) {
                interp = bicubic2Instance = new InterpolationBicubic2(8);
            } else {
                interp = bicubic2Instance;
            }
            break;
          default:
            throw new IllegalArgumentException(
                JaiI18N.getString("Interpolation0"));
        }

        return interp;
    }

    /** 
     * Constructs an Interpolation object with no fields set. 
     * This constructor is only invoked by subclasses which 
     * will subsequently set all fields themselves.
     */
    protected Interpolation() {}

    /**
     * Construct interpolation object with all parameters set.
     * Subclasses must supply all parameters.
     */
    public Interpolation(int width,
                         int height,
                         int leftPadding,
                         int rightPadding,
                         int topPadding,
                         int bottomPadding,
                         int subsampleBitsH,
                         int subsampleBitsV) {

        this.width          = width;
        this.height         = height;
        this.leftPadding    = leftPadding;
        this.rightPadding   = rightPadding;
        this.topPadding     = topPadding;
        this.bottomPadding  = bottomPadding;
        this.subsampleBitsH = subsampleBitsH;
        this.subsampleBitsV = subsampleBitsV;
    }

    /** Returns the number of samples required to the left of the key element. */
    public int getLeftPadding() {
        return leftPadding;
    }

    /** Returns the number of samples required to the right of the key element.
     */
    public int getRightPadding() {
        return rightPadding;
    }

    /** Returns the number of samples required above the key element. */
    public int getTopPadding() {
        return topPadding;
    }
 
    /** Returns the number of samples required below the key element.
     */
    public int getBottomPadding() {
        return bottomPadding;
    }

    /** Returns the number of samples required for horizontal resampling. */
    public int getWidth() {
        return width;
    }

    /** Returns the number of samples required for vertical resampling.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns true if the interpolation can be performed in a separable
     * manner, that is, by performing a separate pass in each dimension.
     * It is the caller's responsibility to deal with issues of precision.
     * By default, true is returned.
     */
    public boolean isSeparable() {
        return true;
    }

    /**
     * Returns the number of bits used to index subsample positions in
     * the horizontal direction.  All integral 'xfrac' parameters
     * should range between 0 and 2<sup>(getSubsampleBitsH())</sup> - 1.
     *
     * <p> In general, the caller is responsible for determining the
     * number of subsample bits of any Interpolation object it
     * receives and setting up its position variables accordingly.
     * Some Interpolation objects allow the number of bits to be set
     * at construction time.
     */
    public int getSubsampleBitsH() {
        return subsampleBitsH;
    }

    /**
     * Returns the number of bits used to index subsample positions in
     * the vertical direction.  All integral 'yfrac' parameters
     * should range between 0 and 2<sup>(getSubsampleBitsV())</sup> - 1.
     */
    public int getSubsampleBitsV() {
        return subsampleBitsV;
    }

    /**
     * Performs horizontal interpolation on a 1-dimensional array of
     * integral samples.
     * <p>An implementation is not required to actually quantize its 
     * interpolation coefficients to match the specified subsampling precision.
     * However, the supplied value of xfrac (or yfrac) must match the precision 
     * of its corresponding subsampleBits. For example, with a subsampleBitsH
     * value of 8, xfrac must lie between 0 and 255.
     *
     * @param samples an array of ints.
     * @param xfrac the subsample position, multiplied by 2<sup>(subsampleBitsH)</sup>.
     * @return the interpolated value as an int.
     */
    public abstract int interpolateH(int[] samples, int xfrac);

    /**
     * Performs vertical interpolation on a 1-dimensional array of
     * integral samples.
     *
     * <p> By default, vertical interpolation is defined to be the
     * same as horizontal interpolation.  Subclasses may choose to
     * implement them differently.
     *
     * @param samples an array of ints.
     * @param yfrac the Y subsample position, multiplied by 2<sup>(subsampleBitsV)</sup>.
     * @return the interpolated value as an int.
     * @see #interpolateH(int[], int)
     */
    public int interpolateV(int[] samples, int yfrac) {
        return interpolateH(samples, yfrac);
    }

    /**
     * Performs interpolation on a 2-dimensional array of integral samples.
     * By default, this is implemented using a two-pass approach.
     *
     * @param samples a two-dimensional array of ints.
     * @param xfrac the X subsample position, multiplied by 2<sup>(subsampleBitsH)</sup>.
     * @param yfrac the Y subsample position, multiplied by 2<sup>(subsampleBitsV)</sup>.
     * @return the interpolated value as an int.
     * @see #interpolateH(int[], int)
     */
    public int interpolate(int[][] samples, int xfrac, int yfrac) {
        int[] interpH = new int[height];

        for (int i = 0; i < height; i++) {
            interpH[i] = interpolateH(samples[i], xfrac);
        }
        return interpolateV(interpH, yfrac);
    }

    /**
     * Performs horizontal interpolation on a pair of integral samples.
     * Subclasses may implement this method to provide a speed improvement
     * over the array method. This base class method merely calls the 
     * array method. It should only be called if width == 2 and 
     * leftPadding == 0.
     *
     * @param s0 the central sample.
     * @param s1 the sample to the right of the central sample.
     * @param xfrac the subsample position, ranging from zero to
     * 2<sup>(subsampleBitsH)</sup> - 1.
     * @return the interpolated value as an int.
     * @see #interpolateH(int[], int)
     */
    public int interpolateH(int s0, int s1, int xfrac) {
        int[] s = new int[2];
        s[0] = s0;
        s[1] = s1;
        return interpolateH(s, xfrac);
    }

    /**
     * Performs horizontal interpolation on a quadruple of integral samples.
     * Subclasses may implement this method to provide a speed improvement
     * over the array method. This base class method merely calls the 
     * array method.
     * It should only be called if width == 4 and leftPadding == 1.
     *
     * @param s_ the sample to the left of the central sample.
     * @param s0 the central sample.
     * @param s1 the sample to the right of the central sample.
     * @param s2 the sample to the right of s1.
     * @param xfrac the subsample position, multiplied by 2<sup>(subsampleBitsH)</sup>.
     * @return the interpolated value as an int.
     * @see #interpolateH(int[], int)
     */
    public int interpolateH(int s_, int s0, int s1, int s2, int xfrac) {
        int[] s = new int[4];
        s[0] = s_;
        s[1] = s0;
        s[2] = s1;
        s[3] = s2;
        return interpolateH(s, xfrac);
    }

    /**
     * Performs vertical interpolation on a pair of integral samples.
     * Subclasses may implement this method to provide a speed improvement
     * over the array method. This base class method merely calls the 
     * array method.
     * It should only be called if height == 2 and topPadding == 0.
     *
     * <p> By default, vertical interpolation is identical to
     * horizontal interpolation.  Subclasses may choose to implement
     * them differently.
     *
     * @param s0 the central sample.
     * @param s1 the sample below the central sample.
     * @param yfrac the Y subsample position, multiplied by 2<sup>(subsampleBitsV)</sup>.
     * @return the interpolated value as an int.
     * @see #interpolateH(int[], int)
     */
    public int interpolateV(int s0, int s1, int yfrac) {
        int[] s = new int[2];
        s[0] = s0;
        s[1] = s1;
        return interpolateV(s, yfrac);
    }

    /**
     * Performs vertical interpolation on a quadruple of integral samples.
     * Subclasses may implement this method to provide a speed improvement
     * over the array method. This base class method merely calls the 
     * array method.
     * It should only be called if height == 4 and topPadding == 1.
     *
     * <p> By default, vertical interpolation is identical to
     * horizontal interpolation.  Subclasses may choose to implement
     * them differently.
     *
     * @param s_ the sample above the central sample.
     * @param s0 the central sample.
     * @param s1 the sample below the central sample.
     * @param s2 the sample below s1.
     * @param yfrac the Y subsample position, multiplied by 2<sup>(subsampleBitsV)</sup>.
     * @return the interpolated value as an int.
     * @see #interpolateH(int[], int)
     */
    public int interpolateV(int s_, int s0, int s1, int s2, int yfrac) {
        int[] s = new int[4];
        s[0] = s_;
        s[1] = s0;
        s[2] = s1;
        s[3] = s2;
        return interpolateV(s, yfrac);
    }

    /**
     * Performs interpolation on a 2x2 grid of integral samples.
     * Subclasses may implement this method to provide a speed improvement
     * over the array method. This base class method merely calls the 
     * array method.
     * It should only be called if width == height == 2 and
     * leftPadding == topPadding == 0.
     *
     * @param s00 the central sample.
     * @param s01 the sample to the right of the central sample.
     * @param s10 the sample below the central sample.
     * @param s11 the sample below and to the right of the central sample.
     * @param xfrac the X subsample position, multiplied by 2<sup>(subsampleBitsH)</sup>.
     * @param yfrac the Y subsample position, multiplied by 2<sup>(subsampleBitsV)</sup>.
     * @return the interpolated value as an int.
     * @see #interpolateH(int[], int)
     */
    public int interpolate(int s00, int s01,
                           int s10, int s11,
                           int xfrac, int yfrac) {
        int[][] s = new int[4][4];
        s[0][0] = s00;
        s[0][1] = s01;
        s[1][0] = s10;
        s[1][1] = s11;
        return interpolate(s, xfrac, yfrac);
    }

    /**
     * Performs interpolation on a 4x4 grid of integral samples.
     * Subclasses may implement this method to provide a speed improvement
     * over the array method. This base class method merely calls the 
     * array method.
     * It should only be called if width == height == 4 and
     * leftPadding == topPadding == 1.
     *
     * @param s__ the sample above and to the left of the central sample.
     * @param s_0 the sample above the central sample.
     * @param s_1 the sample above and one to the right of the central sample.
     * @param s_2 the sample above and two to the right of the central sample.
     * @param s0_ the sample to the left of the central sample.
     * @param s00 the central sample.
     * @param s01 the sample to the right of the central sample.
     * @param s02 the sample two to the right of the central sample.
     * @param s1_ the sample below and one to the left of the central sample.
     * @param s10 the sample below the central sample.
     * @param s11 the sample below and one to the right of the central sample.
     * @param s12 the sample below and two to the right of the central sample.
     * @param s2_ the sample two below and one to the left of the central sample.
     * @param s20 the sample two below the central sample.
     * @param s21 the sample two below and one to the right of the central sample.
     * @param s22 the sample two below and two to the right of the central sample.
     * @param xfrac the X subsample position, multiplied by 2<sup>(subsampleBitsH)</sup>.
     * @param yfrac the Y subsample position, multiplied by 2<sup>(subsampleBitsV)</sup>.
     * @return the interpolated value as an int.
     * @see #interpolateH(int[], int)
     */
    public int interpolate(int s__, int s_0, int s_1, int s_2,
                           int s0_, int s00, int s01, int s02,
                           int s1_, int s10, int s11, int s12,
                           int s2_, int s20, int s21, int s22,
                           int xfrac, int yfrac) {
        int[][] s = new int[4][4];
        s[0][0] = s__;
        s[0][1] = s_0;
        s[0][2] = s_1;
        s[0][3] = s_2;
        s[1][0] = s0_;
        s[1][1] = s00;
        s[1][2] = s01;
        s[1][3] = s02;
        s[2][0] = s1_;
        s[2][1] = s10;
        s[2][2] = s11;
        s[2][3] = s12;
        s[3][0] = s2_;
        s[3][1] = s20;
        s[3][2] = s21;
        s[3][3] = s22;
        return interpolate(s, xfrac, yfrac);
    }

    /**
     * Performs horizontal interpolation on a 1-dimensional array of
     * floating-point samples representing a row of samples.
     * The setting of subsampleBits need not have any effect on the 
     * interpolation accuracy of an implementation of this method.
     *
     * @param samples an array of floats.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.
     */
    public abstract float interpolateH(float[] samples, float xfrac);

    /**
     * Performs vertical interpolation on a 1-dimensional array of
     * floating-point samples representing a column of samples.
     * The setting of subsampleBits need not have any effect on the 
     * interpolation accuracy of an implementation of this method.
     *
     * <p> By default, vertical interpolation is identical to
     * horizontal interpolation.  Subclasses may choose to implement
     * them differently.
     *
     * @param samples an array of floats.
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.

     */
    public float interpolateV(float[] samples, float yfrac) {
        return interpolateH(samples, yfrac);
    }

    /**
     * Performs interpolation on a 2-dimensional array of
     * floating-point samples.  By default, this is implemented using
     * a two-pass approach.
     * The setting of subsampleBits need not have any effect on the 
     * interpolation accuracy of an implementation of this method.
     *
     *
     * @param samples an array of floats.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.
     */
    public float interpolate(float[][] samples,
                             float xfrac, float yfrac) {
        float[] interpH = new float[height];

        for (int i = 0; i < height; i++) {
            interpH[i] = interpolateH(samples[i], xfrac);
        }
        return interpolateV(interpH, yfrac);
    }

    /**
     * Performs horizontal interpolation on a pair of floating-point
     * samples. Subclasses may implement this method to provide a 
     * speed improvement over the array method. This base class method 
     * merely calls the array method.
     * It should only be called if width == 2 and leftPadding == 0.
     * The setting of subsampleBits need not have any effect on the 
     * interpolation accuracy of an implementation of this method.
     *
     * @param s0 the central sample.
     * @param s1 the sample to the right of the central sample.
     * @param xfrac the subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.
     */
    public float interpolateH(float s0, float s1, float xfrac) {
        float[] s = new float[2];
        s[0] = s0;
        s[1] = s1;
        return interpolateH(s, xfrac);
    }

    /**
     * Performs horizontal interpolation on a quadruple of floating-point samples.  
     * Subclasses may implement this method to provide a speed improvement
     * over the array method. This base class method merely calls the 
     * array method. It should only be called if width == 4 and
     * leftPadding == 1.
     * The setting of subsampleBits need not have any effect on the 
     * interpolation accuracy of an implementation of this method.
     *
     * @param s_ the sample to the left of the central sample.
     * @param s0 the central sample.
     * @param s1 the sample to the right of the central sample.
     * @param s2 the sample to the right of s1.
     * @param xfrac the subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.
     */
    public float interpolateH(float s_, float s0, float s1, float s2,
                              float xfrac) {
        float[] s = new float[4];
        s[0] = s_;
        s[1] = s0;
        s[2] = s1;
        s[3] = s2;
        return interpolateH(s, xfrac);
    }

    /**
     * Performs vertical interpolation on a pair of floating-point samples. 
     * Subclasses may implement this method to provide a speed improvement
     * over the array method. This base class method merely calls the 
     * array method.
     * It should only be called if height == 2 and topPadding == 0.
     * The setting of subsampleBits need not have any effect on the 
     * interpolation accuracy of an implementation of this method.
     *
     * <p> By default, vertical interpolation is identical to
     * horizontal interpolation.  Subclasses may choose to implement
     * them differently.
     *
     * @param s0 the central sample.
     * @param s1 the sample below the central sample.
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.
     */
    public float interpolateV(float s0, float s1, float yfrac) {
        float[] s = new float[2];
        s[0] = s0;
        s[1] = s1;
        return interpolateV(s, yfrac);
    }

    /**
     * Performs vertical interpolation on a quadruple of floating-point 
     * samples. 
     * Subclasses may implement this method to provide a speed improvement
     * over the array method. This base class method merely calls the 
     * array method.
     * It should only be called if height == 4 and topPadding == 1.
     * The setting of subsampleBits need not have any effect on the 
     * interpolation accuracy of an implementation of this method.
     *
     * <p> By default, vertical interpolation is identical to
     * horizontal interpolation.  Subclasses may choose to implement
     * them differently.
     *
     * @param s_ the sample above the central sample.
     * @param s0 the central sample.
     * @param s1 the sample below the central sample.
     * @param s2 the sample below s1.
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.
     */
    public float interpolateV(float s_, float s0, float s1, float s2,
                              float yfrac) {
        float[] s = new float[4];
        s[0] = s_;
        s[1] = s0;
        s[2] = s1;
        s[3] = s2;
        return interpolateV(s, yfrac);
    }

    /**
     * Performs interpolation on a 2x2 grid of floating-point samples.
     * Subclasses may implement this method to provide a speed improvement
     * over the array method. This base class method merely calls the 
     * array method.
     * It should only be called if width == height == 2 and
     * leftPadding == topPadding == 0.
     * The setting of subsampleBits need not have any effect on the 
     * interpolation accuracy of an implementation of this method.
     *
     * @param s00 the central sample.
     * @param s01 the sample to the right of the central sample.
     * @param s10 the sample below the central sample.
     * @param s11 the sample below and to the right of the central sample.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.
     */
    public float interpolate(float s00, float s01,
                             float s10, float s11,
                             float xfrac, float yfrac) {
        float[][] s = new float[4][4];
        s[0][0] = s00;
        s[0][1] = s01;
        s[1][0] = s10;
        s[1][1] = s11;
        return interpolate(s, xfrac, yfrac);
    }

    /**
     * Performs interpolation on a 4x4 grid of floating-point samples.
     * Subclasses may implement this method to provide a speed improvement
     * over the array method. This base class method merely calls the 
     * array method.
     * It should only be called if width == height == 4 and
     * leftPadding == topPadding == 1.
     * The setting of subsampleBits need not have any effect on the 
     * interpolation accuracy of an implementation of this method.
     *
     * @param s__ the sample above and to the left of the central sample.
     * @param s_0 the sample above the central sample.
     * @param s_1 the sample above and one to the right of the central sample.
     * @param s_2 the sample above and two to the right of the central sample.
     * @param s0_ the sample to the left of the central sample.
     * @param s00 the central sample.
     * @param s01 the sample to the right of the central sample.
     * @param s02 the sample two to the right of the central sample.
     * @param s1_ the sample below and one to the left of the central sample.
     * @param s10 the sample below the central sample.
     * @param s11 the sample below and one to the right of the central sample.
     * @param s12 the sample below and two to the right of the central sample.
     * @param s2_ the sample two below and one to the left of the central sample.
     * @param s20 the sample two below the central sample.
     * @param s21 the sample two below and one to the right of the central sample.
     * @param s22 the sample two below and two to the right of the central sample.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a float.
     */
    public float interpolate(float s__, float s_0, float s_1, float s_2,
                             float s0_, float s00, float s01, float s02,
                             float s1_, float s10, float s11, float s12,
                             float s2_, float s20, float s21, float s22,
                             float xfrac, float yfrac) {
        float[][] s = new float[4][4];
        s[0][0] = s__;
        s[0][1] = s_0;
        s[0][2] = s_1;
        s[0][3] = s_2;
        s[1][0] = s0_;
        s[1][1] = s00;
        s[1][2] = s01;
        s[1][3] = s02;
        s[2][0] = s1_;
        s[2][1] = s10;
        s[2][2] = s11;
        s[2][3] = s12;
        s[3][0] = s2_;
        s[3][1] = s20;
        s[3][2] = s21;
        s[3][3] = s22;
        return interpolate(s, xfrac, yfrac);
    }

    /**
     * Performs horizontal interpolation on a 1-dimensional array of
     * double samples representing a row of samples.
     * The setting of subsampleBits need not have any effect on the 
     * interpolation accuracy of an implementation of this method.
     *
     * @param samples an array of doubles.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     */
    public abstract double interpolateH(double[] samples, float xfrac);

    /**
     * Performs vertical interpolation on a 1-dimensional array of
     * double samples representing a column of samples.
     * The setting of subsampleBits need not have any effect on the 
     * interpolation accuracy of an implementation of this method.
     *
     * <p> By default, vertical interpolation is identical to
     * horizontal interpolation.  Subclasses may choose to implement
     * them differently.
     *
     * @param samples an array of doubles.
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     */
    public double interpolateV(double[] samples, float yfrac) {
        return interpolateH(samples, yfrac);
    }

    /**
     * Performs interpolation on a 2-dimensional array of
     * double samples.  By default, this is implemented using
     * a two-pass approach.
     * The setting of subsampleBits need not have any effect on the 
     * interpolation accuracy of an implementation of this method.
     *
     * @param samples an array of doubles.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     */
    public double interpolate(double[][] samples,
                              float xfrac, float yfrac) {
        double[] interpH = new double[height];

        for (int i = 0; i < height; i++) {
            interpH[i] = interpolateH(samples[i], xfrac);
        }
        return interpolateV(interpH, yfrac);
    }

    /**
     * Performs horizontal interpolation on a pair of double samples. 
     * Subclasses may implement this method to provide a speed improvement
     * over the array method. This base class method merely calls the 
     * array method.
     * It should only be called if width == 2 and leftPadding == 0.
     * The setting of subsampleBits need not have any effect on the 
     * interpolation accuracy of an implementation of this method.
     *
     * @param s0 the central sample.
     * @param s1 the sample to the right of the central sample.
     * @param xfrac the subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     */
    public double interpolateH(double s0, double s1, float xfrac) {
        double[] s = new double[2];
        s[0] = s0;
        s[1] = s1;
        return interpolateH(s, xfrac);
    }

    /**
     * Performs horizontal interpolation on a quadruple of double samples. 
     * Subclasses may implement this method to provide a speed improvement
     * over the array method. This base class method merely calls the 
     * array method.
     * It should only be called if width == 4 and leftPadding == 1.
     * The setting of subsampleBits need not have any effect on the 
     * interpolation accuracy of an implementation of this method.
     *
     * @param s_ the sample to the left of the central sample.
     * @param s0 the central sample.
     * @param s1 the sample to the right of the central sample.
     * @param s2 the sample to the right of s1.
     * @param xfrac the subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     */
    public double interpolateH(double s_, double s0, double s1, double s2,
                               float xfrac) {
        double[] s = new double[4];
        s[0] = s_;
        s[1] = s0;
        s[2] = s1;
        s[3] = s2;
        return interpolateH(s, xfrac);
    }

    /**
     * Performs vertical interpolation on a pair of double samples. 
     * Subclasses may implement this method to provide a speed improvement
     * over the array method. This base class method merely calls the 
     * array method.
     * It should only be called if height == 2 and topPadding == 0.
     * The setting of subsampleBits need not have any effect on the 
     * interpolation accuracy of an implementation of this method.
     *
     * <p> By default, vertical interpolation is identical to
     * horizontal interpolation.  Subclasses may choose to implement
     * them differently.
     *
     * @param s0 the central sample.
     * @param s1 the sample below the central sample.
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     */
    public double interpolateV(double s0, double s1, float yfrac) {
        double[] s = new double[2];
        s[0] = s0;
        s[1] = s1;
        return interpolateV(s, yfrac);
    }

    /**
     * Performs vertical interpolation on a quadruple of double samples. 
     * Subclasses may implement this method to provide a speed improvement
     * over the array method. This base class method merely calls the 
     * array method.
     * It should only be called if height == 4 and topPadding == 1.
     * The setting of subsampleBits need not have any effect on the 
     * interpolation accuracy of an implementation of this method.
     *
     * <p> By default, vertical interpolation is identical to
     * horizontal interpolation.  Subclasses may choose to implement
     * them differently.
     *
     * @param s_ the sample above the central sample.
     * @param s0 the central sample.
     * @param s1 the sample below the central sample.
     * @param s2 the sample below s1.
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     */
    public double interpolateV(double s_, double s0, double s1, double s2,
                               float yfrac) {
        double[] s = new double[4];
        s[0] = s_;
        s[1] = s0;
        s[2] = s1;
        s[3] = s2;
        return interpolateV(s, yfrac);
    }

    /**
     * Performs interpolation on a 2x2 grid of double samples.
     * Subclasses may implement this method to provide a speed improvement
     * over the array method. This base class method merely calls the 
     * array method.
     * It should only be called if width == height == 2 and
     * leftPadding == topPadding == 0.
     * The setting of subsampleBits need not have any effect on the 
     * interpolation accuracy of an implementation of this method.
     *
     * @param s00 the central sample.
     * @param s01 the sample to the right of the central sample.
     * @param s10 the sample below the central sample.
     * @param s11 the sample below and to the right of the central sample.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     */
    public double interpolate(double s00, double s01,
                              double s10, double s11,
                              float xfrac, float yfrac) {
        double[][] s = new double[4][4];
        s[0][0] = s00;
        s[0][1] = s01;
        s[1][0] = s10;
        s[1][1] = s11;
        return interpolate(s, xfrac, yfrac);
    }

    /**
     * Performs interpolation on a 4x4 grid of double samples.
     * It should only be called if width == height == 4 and
     * leftPadding == topPadding == 1.
     * The setting of subsampleBits need not have any effect on the 
     * interpolation accuracy of an implementation of this method.
     *
     * @param s__ the sample above and to the left of the central sample.
     * @param s_0 the sample above the central sample.
     * @param s_1 the sample above and one to the right of the central sample.
     * @param s_2 the sample above and two to the right of the central sample.
     * @param s0_ the sample to the left of the central sample.
     * @param s00 the central sample.
     * @param s01 the sample to the right of the central sample.
     * @param s02 the sample two to the right of the central sample.
     * @param s1_ the sample below and one to the left of the central sample.
     * @param s10 the sample below the central sample.
     * @param s11 the sample below and one to the right of the central sample.
     * @param s12 the sample below and two to the right of the central sample.
     * @param s2_ the sample two below and one to the left of the central sample.
     * @param s20 the sample two below the central sample.
     * @param s21 the sample two below and one to the right of the central sample.
     * @param s22 the sample two below and two to the right of the central sample.
     * @param xfrac the X subsample position, in the range [0.0F, 1.0F).
     * @param yfrac the Y subsample position, in the range [0.0F, 1.0F).
     * @return the interpolated value as a double.
     */
    public double interpolate(double s__, double s_0, double s_1, double s_2,
                              double s0_, double s00, double s01, double s02,
                              double s1_, double s10, double s11, double s12,
                              double s2_, double s20, double s21, double s22,
                              float xfrac, float yfrac) {
        double[][] s = new double[4][4];
        s[0][0] = s__;
        s[0][1] = s_0;
        s[0][2] = s_1;
        s[0][3] = s_2;
        s[1][0] = s0_;
        s[1][1] = s00;
        s[1][2] = s01;
        s[1][3] = s02;
        s[2][0] = s1_;
        s[2][1] = s10;
        s[2][2] = s11;
        s[2][3] = s12;
        s[3][0] = s2_;
        s[3][1] = s20;
        s[3][2] = s21;
        s[3][3] = s22;
        return interpolate(s, xfrac, yfrac);
    }
}
