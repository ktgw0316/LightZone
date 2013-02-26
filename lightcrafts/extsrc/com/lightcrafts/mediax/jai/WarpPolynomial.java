/*
 * $RCSfile: WarpPolynomial.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:25 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.geom.Point2D;
import com.lightcrafts.media.jai.util.PolyWarpSolver;

/**
 * A polynomial-based description of an image warp.
 *
 * <p>The mapping is defined by two bivariate polynomial functions
 * X(x, y) and Y(x, y) that map destination (x, y) coordinates
 * to source X and Y positions respectively
 *
 * <p>The functions X(x, y) and Y(x, y) have the form:
 * <pre>
 * SUM{i = 0 to n} {SUM{j = 0 to i}{a_ij*x^(i - j)*y^j}}
 *
 * where n is the degree os the polynomial
 * </pre>
 *
 * <p>WarpAffine, WarpQuadratic, and WarpCubic are special cases of
 * WarpPolynomial for n equal to 1, 2, and 3 respectively.
 * WarpGeneralPolynomial provides a concrete implementation for
 * polynomials of higher degree.
 *
 * @see WarpAffine
 * @see WarpQuadratic
 * @see WarpCubic
 * @see WarpGeneralPolynomial
 *
 */
public abstract class WarpPolynomial extends Warp {

    /**
     * An array of coefficients that maps a destination point to
     * the source's X coordinate.
     */
    protected float[] xCoeffs;

    /**
     * An array of coefficients that maps a destination point to
     * the source's Y coordinate.
     */
    protected float[] yCoeffs;

    /**
     * A scaling factor applied to input (dest) x coordinates to
     * improve computational accuracy.
     */
    protected float preScaleX;

    /**
     * A scaling factor applied to input (dest) y coordinates to
     * improve computational accuracy.
     */
    protected float preScaleY;

    /**
     * A scaling factor applied to the result of the X polynomial
     * evaluation which compensates for the input scaling, so that
     * the correctly scaled result is obtained.
     */
    protected float postScaleX;

    /**
     * A scaling factor applied to the result of the Y polynomial
     * evaluation which compensates for the input scaling, so that
     * the correctly scaled result is obtained.
     */
    protected float postScaleY;

    /**
     * The degree of the polynomial, determined by the number of
     * coefficients supplied via the X and Y coefficients arrays.
     */
    protected int degree;
    
    /**
     * Constructs a WarpPolynomial with a given transform mapping
     * destination pixels into source space.  Note that this is
     * a backward mapping as opposed to the forward mapping used in
     * AffineOpImage.
     *
     * <p>The <code>xCoeffs</code> and <code>yCoeffs</code> parameters
     * must contain the same number of coefficients of the form
     * <code>(n + 1)(n + 2)/2</code> for some <code>n</code>, where
     * <code>n</code> is the non-negative degree power of the polynomial.
     * The coefficients, in order, are associated with the terms:
     *
     * <pre>
     * 1, x, y, x^2, x*y, y^2, ..., x^n, x^(n - 1)*y, ..., x*y^(n - 1), y^n
     * </pre>
     *
     * and coefficients of value 0 cannot be omitted.
     *
     * <p>The source (x, y) coordinate is pre-scaled by the factors
     * preScaleX and preScaleY prior to the evaluation of the
     * polynomial.  The result of the polynomial evaluations are
     * scaled by postScaleX and postScaleY to produce the destination
     * pixel coordinates.  This process allows for better precision of the
     * results.
     *
     * @param xCoeffs  The destination to source transform coefficients for
     *                 the X coordinate.
     * @param yCoeffs  The destination to source transform coefficients for
     *                 the Y coordinate.
     * @param preScaleX  The scale factor to apply to input (dest) X positions.
     * @param preScaleY  The scale factor to apply to input (dest) Y positions.
     * @param postScaleX  The scale factor to apply to the X polynomial output.
     * @param postScaleY  The scale factor to apply to the Y polynomial output.
     * @throws IllegalArgumentException if xCoeff or yCoeff have an illegal number of entries.
     */
    public WarpPolynomial(float[] xCoeffs, float[] yCoeffs,
                          float preScaleX, float preScaleY,
                          float postScaleX, float postScaleY) {
        if (xCoeffs == null || yCoeffs == null ||
            xCoeffs.length < 1 || yCoeffs.length < 1 ||
            xCoeffs.length != yCoeffs.length) {
            throw new IllegalArgumentException(
                JaiI18N.getString("WarpPolynomial0"));
        }

        int numCoeffs = xCoeffs.length;
        degree = -1;
        while (numCoeffs > 0) {
            degree++;
            numCoeffs -= degree + 1;
        }
        if (numCoeffs != 0) {
            throw new IllegalArgumentException(
                JaiI18N.getString("WarpPolynomial0"));
        }

        this.xCoeffs = (float[])(xCoeffs.clone());
        this.yCoeffs = (float[])(yCoeffs.clone());
        this.preScaleX = preScaleX;
        this.preScaleY = preScaleY;
        this.postScaleX = postScaleX;
        this.postScaleY = postScaleY;
    }

    /**
     * Constructs a WarpPolynomial with pre- and post-scale factors of 1.
     *
     * @param xCoeffs  The destination to source transform coefficients for
     *                 the X coordinate.
     * @param yCoeffs  The destination to source transform coefficients for
     *                 the Y coordinate.
     */
    public WarpPolynomial(float[] xCoeffs, float[] yCoeffs) {
        this(xCoeffs, yCoeffs, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Returns the raw coefficients array for the X coordinate mapping.
     *
     * @return A cloned array of <code>float</code>s giving the
     *         polynomial coefficients for the X coordinate mapping.
     */
    public float[] getXCoeffs() {
        return (float[])xCoeffs.clone();
    }

    /**
     * Returns the raw coefficients array for the Y coordinate mapping.
     *
     * @return A cloned array of <code>float</code>s giving the
     *         polynomial coefficients for the Y coordinate mapping.
     */
    public float[] getYCoeffs() {
        return (float[])yCoeffs.clone();
    }

    /**
     * Returns the raw coefficients array for both the X and Y coordinate mapping.
     *
     * @return A cloned two-dimensional array of <code>float</code>s giving the
     *         polynomial coefficients for the X and Y coordinate mapping.
     */
    public float[][] getCoeffs() {
        float[][] coeffs = new float[2][];
        coeffs[0] = (float[])xCoeffs.clone();
        coeffs[1] = (float[])yCoeffs.clone();

        return coeffs;
    }

    /** Returns the scaling factor applied to input (dest) X coordinates. */
    public float getPreScaleX() {
        return preScaleX;
    }

    /** Returns the scaling factor applied to input (dest) Y coordinates. */
    public float getPreScaleY() {
        return preScaleY;
    }

    /** Returns the scaling factor applied to the result of the X polynomial. */
    public float getPostScaleX() {
        return postScaleX;
    }

    /** Returns the scaling factor applied to the result of the Y polynomial. */
    public float getPostScaleY() {
        return postScaleY;
    }

    /**
     * Returns the degree of the warp polynomials.
     *
     * @return The degree as an <code>int</code>.
     */
    public int getDegree() {
        return degree;
    }

    /**
     * Returns an instance of <code>WarpPolynomial</code> or its
     * subclasses that approximately maps the given scaled destination
     * image coordinates into the given scaled source image
     * coordinates.  The mapping is given by:
     *
     * <pre>
     * x' = postScaleX*(xpoly(x*preScaleX, y*preScaleY));
     * x' = postScaleY*(ypoly(x*preScaleX, y*preScaleY));
     * </pre>
     *
     * <p> Typically, it is useful to set <code>preScaleX</code> to
     * <code>1.0F/destImage.getWidth()</code> and
     * <code>postScaleX</code> to <code>srcImage.getWidth()</code> so
     * that the input and output of the polynomials lie between 0 and
     * 1.
     *
     * <p> The degree of the polynomial is supplied as an argument.
     *
     * @param sourceCoords An array of <code>float</code>s containing the
     *        source coordinates with X and Y alternating.
     * @param sourceOffset the initial entry of <code>sourceCoords</code>
     *        to be used.
     * @param destCoords An array of <code>float</code>s containing the
     *        destination coordinates with X and Y alternating.
     * @param destOffset The initial entry of <code>destCoords</code>
     *        to be used.
     * @param numCoords The number of coordinates from
     *        <code>sourceCoords</code> and <code>destCoords</code> to be used.
     * @param preScaleX The scale factor to apply to input (dest) X positions.
     * @param preScaleY The scale factor to apply to input (dest) Y positions.
     * @param postScaleX The scale factor to apply to X polynomial output.
     * @param postScaleY The scale factor to apply to the Y polynomial output.
     * @param degree The desired degree of the warp polynomials.
     *
     * @return An instance of <code>WarpPolynomial</code>.
     * @throws IllegalArgumentException if arrays sourceCoords or destCoords
     *         are too small 
     */
    public static WarpPolynomial createWarp(float[] sourceCoords,
                                            int sourceOffset,
                                            float[] destCoords,
                                            int destOffset,
                                            int numCoords,
                                            float preScaleX,
                                            float preScaleY,
                                            float postScaleX,
                                            float postScaleY,
                                            int degree) {

        int minNumPoints = (degree+1) * (degree+2);
        if ((sourceOffset + minNumPoints) > sourceCoords.length ||
            (destOffset + minNumPoints) > destCoords.length) {

            throw new IllegalArgumentException(
                JaiI18N.getString("WarpPolynomial1"));
        }
        float[] coeffs = PolyWarpSolver.getCoeffs(sourceCoords, sourceOffset,
                                                  destCoords, destOffset,
                                                  numCoords,
                                                  preScaleX, preScaleY,
                                                  postScaleX, postScaleY,
                                                  degree);

        int numCoeffs = coeffs.length / 2;
        float[] xCoeffs = new float[numCoeffs];
        float[] yCoeffs = new float[numCoeffs];

        for (int i = 0; i < numCoeffs; i++) {
            xCoeffs[i] = coeffs[i];
            yCoeffs[i] = coeffs[i + numCoeffs];
        }

        if (degree == 1) {
            return new WarpAffine(xCoeffs, yCoeffs,
                                  preScaleX, preScaleY,
                                  postScaleX, postScaleY);
        } else if (degree == 2) {
            return new WarpQuadratic(xCoeffs, yCoeffs,
                                     preScaleX, preScaleY,
                                     postScaleX, postScaleY);
        } else if (degree == 3) {
            return new WarpCubic(xCoeffs, yCoeffs,
                                 preScaleX, preScaleY,
                                 postScaleX, postScaleY);
        } else {
            return new WarpGeneralPolynomial(xCoeffs, yCoeffs,
                                             preScaleX, preScaleY,
                                             postScaleX, postScaleY);
        }
    }

    /**
     * Computes the source point corresponding to the supplied point.
     *
     * <p>This method returns the value of <code>pt</code> in the following
     * code snippet:
     *
     * <pre>
     * double dx = (destPt.getX() + 0.5)*preScaleX;
     * double dy = (destPt.getY() + 0.5)*preScaleY;
     *
     * double sx = 0.0;
     * double sy = 0.0;
     * int c = 0;
     *
     * for(int nx = 0; nx <= degree; nx++) {
     *     for(int ny = 0; ny <= nx; ny++) {
     *         double t = Math.pow(dx, nx - ny)*Math.pow(dy, ny);
     *         sx += xCoeffs[c] * t;
     *         sy += yCoeffs[c] * t;
     *         c++;
     *     }
     * }

     * Point2D pt = (Point2D)destPt.clone();
     * pt.setLocation(sx*postScaleX - 0.5, sy*postScaleY - 0.5);
     * </pre>
     * </p>
     *
     * @param destPt the position in destination image coordinates
     * to map to source image coordinates.
     *
     * @return a <code>Point2D</code> of the same class as
     * <code>destPt</code>.
     *
     * @throws IllegalArgumentException if <code>destPt</code> is
     * <code>null</code>.
     *
     * @since JAI 1.1.2
     */
    public Point2D mapDestPoint(Point2D destPt) {
        if (destPt == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        double dx = (destPt.getX() + 0.5)*preScaleX;
        double dy = (destPt.getY() + 0.5)*preScaleY;

        double sx = 0.0;
        double sy = 0.0;
        int c = 0;

        for(int nx = 0; nx <= degree; nx++) {
            for(int ny = 0; ny <= nx; ny++) {
                double t = Math.pow(dx, nx - ny)*Math.pow(dy, ny);
                sx += xCoeffs[c] * t;
                sy += yCoeffs[c] * t;
                c++;
            }
        }

        Point2D pt = (Point2D)destPt.clone();
        pt.setLocation(sx*postScaleX - 0.5, sy*postScaleY - 0.5);

        return pt;
    }
}
