/*
 * $RCSfile: WarpQuadratic.java,v $
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


/**
 * A quadratic-based description of an image warp.
 *
 * <p> The source position (x', y') of a point (x, y) is given by the
 * quadratic bivariate polynomials:
 *
 * <pre>
 * x' = p(x, y) = c1 + c2*x + c3*y + c4*x^2 + c5*x*y + c6*y^2
 * y' = q(x, y) = c7 + c8*x + c9*y + c10*x^2 + c11*x*y + c12*y^2
 * </pre>
 *
 * <p> <code>WarpQuadratic</code> is marked final so that it may be
 * more easily inlined.
 *
 * @see WarpPolynomial
 *
 */
public final class WarpQuadratic extends WarpPolynomial {

    private float c1, c2, c3, c4, c5, c6;	// coefficients for X
    private float c7, c8, c9, c10, c11, c12;	// coefficients for Y

    /**
     * Constructs a <code>WarpQuadratic</code> with a given transform mapping
     * destination pixels into source space.  Note that this is
     * a backward mapping as opposed to the forward mapping used in
     * AffineOpImage. The coeffs arrays must each contain 6 floats
     * corresponding to the coefficients c1, c2, etc. as shown in the
     * class comment.
     *
     * @param xCoeffs  The six destination to source transform coefficients for
     *                 the X coordinate.
     * @param yCoeffs  The six destination to source transform coefficients for
     *                 the Y coordinate.
     * @param preScaleX  The scale factor to apply to input (dest) X positions.
     * @param preScaleY  The scale factor to apply to input (dest) Y positions.
     * @param postScaleX  The scale factor to apply to the result of the
     *                    X polynomial evaluation
     * @param postScaleY  The scale factor to apply to the result of the
     *                    Y polynomial evaluation
     * @throws IllegalArgumentException if the xCoeff and yCoeff arrays
     *         do not each have size entries.                             
     */
    public WarpQuadratic(float[] xCoeffs, float[] yCoeffs,
                         float preScaleX, float preScaleY,
                         float postScaleX, float postScaleY) {
        super(xCoeffs, yCoeffs, preScaleX, preScaleY, postScaleX, postScaleY);

        if (xCoeffs.length != 6 || yCoeffs.length != 6) {
            throw new IllegalArgumentException(JaiI18N.getString("WarpQuadratic0"));
        }

        c1 = xCoeffs[0];
        c2 = xCoeffs[1];
        c3 = xCoeffs[2];
        c4 = xCoeffs[3];
        c5 = xCoeffs[4];
        c6 = xCoeffs[5];

        c7 = yCoeffs[0];
        c8 = yCoeffs[1];
        c9 = yCoeffs[2];
        c10 = yCoeffs[3];
        c11 = yCoeffs[4];
        c12 = yCoeffs[5];
    }

    /**
     * Constructs a <code>WarpQuadratic</code> with pre- and
     * post-scale factors of 1.
     *
     * @param xCoeffs  The 6 destination to source transform coefficients for
     *                 the X coordinate.
     * @param yCoeffs  The 6 destination to source transform coefficients for
     *                 the Y coordinate.
     * @throws IllegalArgumentException if the xCoeff and yCoeff arrays
     *         do not each have size entries.                             
     */
    public WarpQuadratic(float[] xCoeffs, float[] yCoeffs) {
        this(xCoeffs, yCoeffs, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Computes the source subpixel positions for a given rectangular
     * destination region, subsampled with an integral period.  The
     * destination region is specified using normal integral (full
     * pixel) coordinates.  The source positions returned by the
     * method are specified in floating point.
     *
     * @param x The minimum X coordinate of the destination region.
     * @param y The minimum Y coordinate of the destination region.
     * @param width The width of the destination region.
     * @param height The height of the destination region.
     * @param periodX The horizontal sampling period.
     * @param periodY The vertical sampling period.
     *
     * @param destRect A <code>float</code> array containing at least
     *        <code>2*((width+periodX-1)/periodX)*
     *                ((height+periodY-1)/periodY)</code>
     *        elements, or <code>null</code>.  If <code>null</code>, a
     *        new array will be constructed.
     *
     * @return A reference to the <code>destRect</code> parameter if
     *         it is non-<code>null</code>, or a new
     *         <code>float</code> array otherwise.
     * @throws ArrayBoundsException if destRect is too small
     */
    public float[] warpSparseRect(int x, int y,
                                  int width, int height,
                                  int periodX, int periodY,
                                  float[] destRect) {

        //XXX: This method should do its calculations in doubles

        if (destRect == null) {
            destRect = new
                float[((width + periodX - 1) / periodX) *
                      ((height + periodY - 1) / periodY) * 2];
        }

        //
        // x' = c1 + c2*x + c3*y + c4*x^2 + c5*x*y + c6*y^2
        // y' = c7 + c8*x + c9*y + c10*x^2 + c11*x*y + c12*y^2
        //

        float px1 = periodX * preScaleX;	// powers for periodX
        float px2 = px1 * px1;

        // Delta delta x for both polys.
        float ddx = c4 * 2 * px2;
        float ddy = c10 * 2 * px2;

        float x1 = (x + 0.5F) * preScaleX;	// powers for x
        float x2 = x1 * x1;

        width += x;
        height += y;
        int index = 0;

        for (int j = y; j < height; j += periodY) {
            // Pre-scaled input coordinates and step.
            float y1 = (j + 0.5F) * preScaleY;	// powers for current y
            float y2 = y1 * y1;

            // The warped position for the first point of the current line 
            float wx = c1 + c2 * x1 + c3 * y1 +
                       c4 * x2 + c5 * x1 * y1 + c6 * y2;
            float wy = c7 + c8 * x1 + c9 * y1 +
                       c10 * x2 + c11 * x1 * y1 + c12 * y2;

            // Delta x and delta y
            float dx = c2 * px1 + c4 * (2 * x1 * px1 + px2) + c5 * px1 * y1;
            float dy = c8 * px1 + c10 * (2 * x1 * px1 + px2) + c11 * px1 * y1;

            for (int i = x; i < width; i += periodX) {
                destRect[index++] = wx * postScaleX - 0.5F;
                destRect[index++] = wy * postScaleY - 0.5F;

                wx += dx;
                wy += dy;
                dx += ddx;
                dy += ddy;
            }
        }

        return destRect;
    }

    /**
     * Computes the source point corresponding to the supplied point.
     *
     * <p>This method returns the value of <code>pt</code> in the following
     * code snippet:
     *
     * <pre>
     * double x1 = (destPt.getX() + 0.5F)*preScaleX;
     * double x2 = x1*x1;
     *
     * double y1 = (destPt.getY() + 0.5F)*preScaleY;
     * double y2 = y1*y1;
     *
     * double x = c1 + c2*x1 + c3*y1 + c4*x2 + c5*x1*y1 + c6*y2;
     * double y = c7 + c8*x1 + c9*y1 + c10*x2 + c11*x1*y1 + c12*y2;
     *
     * Point2D pt = (Point2D)destPt.clone();
     * pt.setLocation(x*postScaleX - 0.5, y*postScaleY - 0.5);
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

        double x1 = (destPt.getX() + 0.5F)*preScaleX;
        double x2 = x1*x1;

        double y1 = (destPt.getY() + 0.5F)*preScaleY;
        double y2 = y1*y1;

        double x = c1 + c2*x1 + c3*y1 + c4*x2 + c5*x1*y1 + c6*y2;
        double y = c7 + c8*x1 + c9*y1 + c10*x2 + c11*x1*y1 + c12*y2;

        Point2D pt = (Point2D)destPt.clone();
        pt.setLocation(x*postScaleX - 0.5, y*postScaleY - 0.5);

        return pt;
    }
}
