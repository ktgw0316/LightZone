/*
 * $RCSfile: WarpGeneralPolynomial.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:24 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

/**
 * A general polynomial-based description of an image warp.
 *
 * <p> The mapping is defined by two bivariate polynomial functions
 * X(x, y) and Y(x, y) that define the source X and Y positions
 * that map to a given destination (x, y) pixel coordinate.
 *
 * <p> The functions X(x, y) and Y(x, y) have the form:
 * <pre>
 * SUM{i = 0 to n} {SUM{j = 0 to i}{a_ij*x^(i - j)*y^j}}
 * </pre>
 *
 * @see WarpPolynomial
 *
 */
public final class WarpGeneralPolynomial extends WarpPolynomial {

    /**
     * Constructs a WarpGeneralPolynomial with a given transform mapping
     * destination pixels into source space.  Note that this is
     * a backward mapping as opposed to the forward mapping used in
     * AffineOpImage.
     *
     * <p>The <code>xCoeffs</code> and <code>yCoeffs</code> parameters
     * must contain the same number of coefficients,
     * <code>(n + 1)(n + 2)/2</code>, for some <code>n</code>, where
     * <code>n</code> is the non-negative degree power of the polynomial.
     * The coefficients, in order, are associated with the terms:
     *
     * <pre>
     * 1, x, y, x^2, x*y, y^2, ..., x^n, x^(n - 1)*y, ..., x*y^(n - 1), y^n
     * </pre>
     *
     * and coefficients of value 0 cannot be omitted.
     *
     * <p> The destination pixel coordinates (the arguments to the X()
     * and Y() functions) are given in normal integral pixel
     * coordinates, while the output of the functions is given in
     * fixed-point, subpixel coordinates with a number of fractional
     * bits specified by the subsampleBitsH and subsampleBitsV
     * parameters.
     *
     * @param xCoeffs  The destination to source transform coefficients for
     *                 the X coordinate.
     * @param yCoeffs  The destination to source transform coefficients for
     *                 the Y coordinate.
     * @param preScaleX  The scale factor to apply to input (dst) X positions.
     * @param preScaleY  The scale factor to apply to input (dst) Y positions.
     * @param postScaleX  The scale factor to apply to output (src) X positions.
     * @param postScaleY  The scale factor to apply to output (src) Y positions.
     * @throws IllegalArgumentException if arrays xCoeffs and yCoeffs do not
     *         have the correct number of entries.
     */
    public WarpGeneralPolynomial(float[] xCoeffs, float[] yCoeffs,
                                 float preScaleX, float preScaleY,
                                 float postScaleX, float postScaleY) {
        super(xCoeffs, yCoeffs, preScaleX, preScaleY, postScaleX, postScaleY);
    }

    /**
     * Constructs a WarpGeneralPolynomial with pre- and post-scale
     * factors of 1.
     *
     * @param xCoeffs  The destination to source transform coefficients for
     *                 the X coordinate.
     * @param yCoeffs  The destination to source transform coefficients for
     *                 the Y coordinate.
     * @throws IllegalArgumentException if arrays xCoeffs and yCoeffs do not
     *         have the correct number of entries.
     */
    public WarpGeneralPolynomial(float[] xCoeffs, float[] yCoeffs) {
        this(xCoeffs, yCoeffs, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Computes the source subpixel positions for a given rectangular
     * destination region, subsampled with an integral period.
     *
     * @param x The minimum X coordinate of the destination region.
     * @param y The minimum Y coordinate of the destination region.
     * @param width The width of the destination region.
     * @param height The height of the destination region.
     * @param periodX The horizontal sampling period.
     * @param periodY The vertical sampling period.
     * @param destRect An int array containing at least
     *        2*((width+periodX-1)/periodX)*((height+periodY-1)/periodY)
     *        elements, or <code>null</code>.  If <code>null</code>, a
     *        new array will be constructed.
     *
     * @return a reference to the destRect parameter if it is
     *         non-<code>null</code>, or a new int array of length
     *         2*width*height otherwise.
     * @throws ArrayBoundsException if destRect array is too small
     */
    public float[] warpSparseRect(int x, int y,
                                  int width, int height,
                                  int periodX, int periodY,
                                  float[] destRect) {

        // XXX: Calculations should be performed in doubles
        // XXX: Calculations should use Horner's rule

        if (destRect == null) {
            destRect = new
                float[2 * ((width + periodX - 1) / periodX) *
                          ((height + periodY - 1) / periodY)];
        }

        // Power tables for x and y
        float[] xPows = new float[degree + 1];
        float[] yPows = new float[degree + 1];
        xPows[0] = 1.0F;
        yPows[0] = 1.0F;

        width += x;
        height += y;
        int index = 0;			// destRect index

        for (int j = y; j < height; j += periodY) {
            // Initialize power table for the current y position (j)
            float y1 = (j + 0.5F) * preScaleY;
            for (int n = 1; n <= degree; n++) {
                yPows[n] = yPows[n - 1] * y1;
            }

            for (int i = x; i < width; i += periodX) {
                // Initialize power table for current x position (i)
                float x1 = (i + 0.5F) * preScaleX;
                for (int n = 1; n <= degree; n++) {
                    xPows[n] = xPows[n - 1] * x1;
                }

                float wx = 0.0F;	// warped x
                float wy = 0.0F;	// warped y
                int c = 0;		// coefficient index

                for (int nx = 0; nx <= degree; nx++) {
                    for (int ny = 0; ny <= nx; ny++) {
                        float t = xPows[nx - ny] * yPows[ny];
                        wx += xCoeffs[c] * t;
                        wy += yCoeffs[c] * t;
                        c++;
                    }
                }

                destRect[index++] = wx * postScaleX - 0.5F;
                destRect[index++] = wy * postScaleY - 0.5F;
            }
        }

        return destRect;
    }
}
