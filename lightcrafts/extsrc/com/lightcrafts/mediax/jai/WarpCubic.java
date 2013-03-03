/*
 * $RCSfile: WarpCubic.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:23 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.geom.Point2D;


/**
 * A cubic-based description of an image warp.
 *
 * <p> The source position (x', y') of a point (x, y) is given by the
 * cubic polynomial:
 *
 * <pre>
 * x' = p(x, y) = c1 + c2*x + c3*y + c4*x^2 + c5*x*y + c6*y^2 +
 *                c7*x^3 + c8*x^2*y + c9*x*y^2 + c10*y^3
 * y' = q(x, y) = c11 + c12*x + c13*y + c14*x^2 + c15*x*y + c16*y^2 +
 *                c17*x^3 + c18*x^2*y + c19*x*y^2 + c20*y^3
 * </pre>
 *
 * <p> <code>WarpCubic</code> is marked final so that it may be
 * more easily inlined.
 *
 * @see WarpPolynomial
 *
 */
public final class WarpCubic extends WarpPolynomial {

    private float c1, c2, c3, c4, c5, c6, c7, c8, c9, c10;
    private float c11, c12, c13, c14, c15, c16, c17, c18, c19, c20;

    /**
     * Constructs a <code>WarpCubic</code> with a given transform mapping
     * destination pixels into source space.  Note that this is
     * a backward mapping as opposed to the forward mapping used in
     * AffineOpImage. The coeffs arrays must each contain 10 floats
     * corresponding to the coefficients c1, c2, etc. as shown in the
     * class comment.
     *
     * @param xCoeffs  The 10 destination to source transform coefficients for
     *                 the X coordinate.
     * @param yCoeffs  The 10 destination to source transform coefficients for
     *                 the Y coordinate.
     * @param preScaleX  The scale factor to apply to input (dest) X positions.
     * @param preScaleY  The scale factor to apply to input (dest) Y positions.
     * @param postScaleX  The scale factor to apply to the result of the X polynomial evaluation
     * @param postScaleY  The scale factor to apply to the result of the Y polynomial evaluation
     * @throws IllegalArgumentException if the length of the xCoeffs and yCoeffs arrays are not both 10.
     */
    public WarpCubic(float[] xCoeffs, float[] yCoeffs,
                     float preScaleX, float preScaleY,
                     float postScaleX, float postScaleY) {
        super(xCoeffs, yCoeffs, preScaleX, preScaleY, postScaleX, postScaleY);

        if (xCoeffs.length != 10 || yCoeffs.length != 10) {
            throw new IllegalArgumentException(JaiI18N.getString("WarpCubic0"));
        }

        c1 = xCoeffs[0];	// x coefficients
        c2 = xCoeffs[1];
        c3 = xCoeffs[2];
        c4 = xCoeffs[3];
        c5 = xCoeffs[4];
        c6 = xCoeffs[5];
        c7 = xCoeffs[6];
        c8 = xCoeffs[7];
        c9 = xCoeffs[8];
        c10 = xCoeffs[9];

        c11 = yCoeffs[0];	// y coefficients
        c12 = yCoeffs[1];
        c13 = yCoeffs[2];
        c14 = yCoeffs[3];
        c15 = yCoeffs[4];
        c16 = yCoeffs[5];
        c17 = yCoeffs[6];
        c18 = yCoeffs[7];
        c19 = yCoeffs[8];
        c20 = yCoeffs[9];
    }

    /**
     * Constructs a <code>WarpCubic</code> with pre- and post-scale
     * factors of 1.
     *
     * @param xCoeffs  The 10 destination to source transform coefficients for
     *                 the X coordinate.
     * @param yCoeffs  The 10 destination to source transform coefficients for
     *                 the Y coordinate.
     * @throws IllegalArgumentException if the length of the xCoeffs and yCoeffs arrays are not both 10.
     */
    public WarpCubic(float[] xCoeffs, float[] yCoeffs) {
        this(xCoeffs, yCoeffs, 1.0F, 1.0F, 1.0F, 1.0F);
    }
 
    /**
     * Computes the source subpixel positions for a given rectangular
     * destination region, subsampled with an integral period.  The
     * destination region is specified using normal integral (full
     * pixel) coordinates.  The source positions returned by the
     * method are specified in floating point.
     *
     * @param x  The minimum X coordinate of the destination region.
     * @param y  The minimum Y coordinate of the destination region.
     * @param width  The width of the destination region.
     * @param height  The height of the destination region.
     * @param periodX  The horizontal sampling period.
     * @param periodY  The vertical sampling period.
     *
     * @param destRect  A <code>float</code> array containing at least
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

        // XXX: This method should do its calculations in doubles

        if (destRect == null) {
            destRect = new float[((width + periodX - 1) / periodX) *
                                 ((height + periodY - 1) / periodY) * 2];
        }

        //
        // x' = c1 + c2*x + c3*y + c4*x^2 + c5*x*y + c6*y^2 +
        //      c7*x^3 + c8*x^2*y + c9*x*y^2 + c10*y^3
        // y' = c11 + c12*x + c13*y + c14*x^2 + c15*x*y + c16*y^2 +
        //      c17*x^3 + c18*x^2*y + c19*x*y^2 + c20*y^3
        //

        float px1 = periodX * preScaleX;	// power for periodX
        float px2 = px1 * px1;
        float px3 = px2 * px1;

        // Delta delta delta x and delta delta delta y
        float dddx = c7 * 6 * px3;
        float dddy = c17 * 6 * px3;

        float x1 = (x + 0.5F) * preScaleX;	// power for x
        float x2 = x1 * x1;
        float x3 = x2 * x1;

        width += x;
        height += y;
        int index = 0;

        for (int j = y; j < height; j += periodY) {
            float y1 = (j + 0.5F) * preScaleY;	// power for the current y
            float y2 = y1 * y1;
            float y3 = y2 * y1;

            // The warped position for the first point of the current line
            float wx = c1 + c2 * x1 + c3 * y1 +
                       c4 * x2 + c5 * x1 * y1 + c6 * y2 +
                       c7 * x3 + c8 * x2 * y1 + c9 * x1 * y2 + c10 * y3;
            float wy = c11 + c12 * x1 + c13 * y1 +
                       c14 * x2 + c15 * x1 * y1 + c16 * y2 +
                       c17 * x3 + c18 * x2 * y1 + c19 * x1 * y2 + c20 * y3;

            // Delta x and delta y
            float dx = c2 * px1 +
                       c4 * (2 * x1 * px1 + px2) +
                       c5 * px1 * y1 +
                       c7 * (3 * x2 * px1 + 3 * x1 * px2 + px3) +
                       c8 * (2 * x1 * px1 + px2) * y1 +
                       c9 * px1 * y2;
            float dy = c12 * px1 +
                       c14 * (2 * x1 * px1 + px2) +
                       c15 * px1 * y1 +
                       c17 * (3 * x2 * px1 + 3 * x1 * px2 + px3) +
                       c18 * (2 * x1 * px1 + px2) * y1 +
                       c19 * px1 * y2;
                       
            // Delta delta x and delta delta y
            float ddx = c4 * 2 * px2 +
                        c7 * (6 * x1 * px2 + 6 * px3) +
                        c8 * 2 * px2 * y1;
            float ddy = c14 * 2 * px2 +
                        c17 * (6 * x1 * px2 + 6 * px3) +
                        c18 * 2 * px2 * y1;

            for (int i = x; i < width; i += periodX) {
                destRect[index++] = wx * postScaleX - 0.5F;
                destRect[index++] = wy * postScaleY - 0.5F;

                wx += dx;
                wy += dy;
                dx += ddx;
                dy += ddy;
                ddx += dddx;
                ddy += dddy;
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
     * double x3 = x2*x1;
     *
     * double y1 = (destPt.getY() + 0.5F)*preScaleY;
     * double y2 = y1*y1;
     * double y3 = y2*y1;
     *
     * double sx = c1 + c2*x1 + c3*y1 +
     *     c4*x2 + c5*x1*y1 + c6*y2 +
     *     c7*x3 + c8*x2*y1 + c9*x1*y2 + c10*y3;
     * double sy = c11 + c12*x1 + c13*y1 +
     *     c14*x2 + c15*x1*y1 + c16*y2 +
     *     c17*x3 + c18*x2*y1 + c19*x1*y2 + c20*y3;
     *
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

        double x1 = (destPt.getX() + 0.5F)*preScaleX;
        double x2 = x1*x1;
        double x3 = x2*x1;

        double y1 = (destPt.getY() + 0.5F)*preScaleY;
        double y2 = y1*y1;
        double y3 = y2*y1;

        double sx = c1 + c2*x1 + c3*y1 +
            c4*x2 + c5*x1*y1 + c6*y2 +
            c7*x3 + c8*x2*y1 + c9*x1*y2 + c10*y3;
        double sy = c11 + c12*x1 + c13*y1 +
            c14*x2 + c15*x1*y1 + c16*y2 +
            c17*x3 + c18*x2*y1 + c19*x1*y2 + c20*y3;

        Point2D pt = (Point2D)destPt.clone();
        pt.setLocation(sx*postScaleX - 0.5, sy*postScaleY - 0.5);

        return pt;
    }
}
