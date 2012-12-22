/*
 * $RCSfile: WarpAffine.java,v $
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
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * A description of an Affine warp.
 *
 * <p> The transform is specified as a mapping from destination
 * space to source space, a backward mapping, as opposed to the
 * forward mapping used in AffineOpImage.
 *
 * <p> The source position (x', y') of a point (x, y) is given by the
 * first order (affine) bivariate polynomials:
 *
 * <pre>
 * x' = p(x, y) = c1 + c2*x + c3*y
 * y' = q(x, y) = c4 + c5*x + c6*y
 * </pre>
 *
 * <p> <code>WarpAffine</code> is marked final so that it may be more
 * easily inlined.
 *
 */
public final class WarpAffine extends WarpPolynomial {

    private float c1, c2, c3;	// coefficients for X
    private float c4, c5, c6;	// coefficients for Y

    private float invc1, invc2, invc3;	// inverse xform coefficients for X
    private float invc4, invc5, invc6;	// inverse xform coefficients for Y

    private AffineTransform transform;
    private AffineTransform invTransform;

    /**
     * @param transform
     * @return An array of <code>float</code>s.
     */
    private static final float[] xCoeffsHelper(AffineTransform transform) {
        float[] coeffs = new float[3];
        coeffs[0] = (float)transform.getTranslateX();
        coeffs[1] = (float)transform.getScaleX();
        coeffs[2] = (float)transform.getShearX();
        return coeffs;
    }

    private static final float[] yCoeffsHelper(AffineTransform transform) {
        float[] coeffs = new float[3];
        coeffs[0] = (float)transform.getTranslateY();
        coeffs[1] = (float)transform.getShearY();
        coeffs[2] = (float)transform.getScaleY();
        return coeffs;
    }

    /**
     * Constructs a <code>WarpAffine</code> with a given transform mapping
     * destination pixels into source space.  The transform is
     * given by:
     *
     * <pre>
     * x' = xCoeffs[0] + xCoeffs[1]*x + xCoeffs[2]*y;
     * y' = yCoeffs[0] + yCoeffs[1]*x + yCoeffs[2]*y;
     * </pre>
     *
     * where <code>x', y'</code> are the source image coordinates
     * and <code>x, y</code> are the destination image coordinates.
     *
     * @param xCoeffs  The 3 destination to source transform coefficients for
     *                 the X coordinate.
     * @param yCoeffs  The 3 destination to source transform coefficients for
     *                 the Y coordinate.
     * @param preScaleX  The scale factor to apply to input (dest) X positions.
     * @param preScaleY  The scale factor to apply to input (dest) Y positions.
     * @param postScaleX  The scale factor to apply to the evaluated x transform
     * @param postScaleY  The scale factor to apply to the evaluated y transform
     *
     * @throws IllegalArgumentException if array <code>xCoeffs</code> or
     *         <code>yCoeffs</code> does not have length of 3.
     */
    public WarpAffine(float[] xCoeffs, float[] yCoeffs,
                      float preScaleX, float preScaleY,
                      float postScaleX, float postScaleY) {
        super(xCoeffs, yCoeffs, preScaleX, preScaleY, postScaleX, postScaleY);

        if (xCoeffs.length != 3 || yCoeffs.length != 3) {
            throw new IllegalArgumentException(
                      JaiI18N.getString("WarpAffine0"));
        }

        c1 = xCoeffs[0];
        c2 = xCoeffs[1];
        c3 = xCoeffs[2];

        c4 = yCoeffs[0];
        c5 = yCoeffs[1];
        c6 = yCoeffs[2];

        transform = getTransform();

        // Transform inversion may throw NoninvertibleTransformException
        try {
            invTransform = transform.createInverse();

            invc1 = (float)invTransform.getTranslateX();
            invc2 = (float)invTransform.getScaleX();
            invc3 = (float)invTransform.getShearX();

            invc4 = (float)invTransform.getTranslateY();
            invc5 = (float)invTransform.getShearY();
            invc6 = (float)invTransform.getScaleY();
        } catch (java.awt.geom.NoninvertibleTransformException e) {
            // Transform can't be inverted, so set inverse to null
            invTransform = null;
        }
    }

    /**
     * Constructs a <code>WarpAffine</code> with pre- and post-scale
     * factors of 1.
     *
     * @param xCoeffs  The 3 destination to source transform coefficients for
     *                 the X coordinate.
     * @param yCoeffs  The 3 destination to source transform coefficients for
     *                 the Y coordinate.
     */
    public WarpAffine(float[] xCoeffs, float[] yCoeffs) {
        this(xCoeffs, yCoeffs, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Constructs a <code>WarpAffine</code> with a given transform mapping
     * destination pixels into source space.  Note that this is
     * a backward mapping as opposed to the forward mapping used in
     * AffineOpImage.
     *
     * @param transform The destination to source transform.
     * @param preScaleX The scale factor to apply to source X positions.
     * @param preScaleY The scale factor to apply to source Y positions.
     * @param postScaleX The scale factor to apply to destination X positions.
     * @param postScaleY The scale factor to apply to destination Y positions.
     */
    public WarpAffine(AffineTransform transform,
                      float preScaleX, float preScaleY,
                      float postScaleX, float postScaleY) {
        this(xCoeffsHelper(transform), yCoeffsHelper(transform),
             preScaleX, preScaleY, postScaleX, postScaleY);
    }

    /**
     * Constructs a <code>WarpAffine</code> with pre- and post-scale
     * factors of 1.
     *
     * @param transform An <code>AffineTransform</code> mapping dest to source
     *                  coordinates.
     */
    public WarpAffine(AffineTransform transform) {
        this(transform, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Returns a clone of the <code>AffineTransform</code> associated
     * with this <code>WarpAffine</code> object.
     *
     * @return An <code>AffineTransform</code>.
     */
    public AffineTransform getTransform() {
        return new AffineTransform(c2, c5, c3, c6, c1, c4);
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
        // Original formula
        //     x' = c1 + c2*x + c3*y
        //     y' = c4 + c5*x + c6*y
        //
        // Take in preScale, postScale, and 0.5 shift
        //     x' = (c1 + c2*(x+0.5)*preScaleX +
        //           c3*(y+0.5)*preScaleY) * postScaleX - 0.5
        //     y' = (c4 + c5*(x+0.5)*preScaleX +
        //           c6*(y+0.5)*preScaleY) * postScaleY - 0.5
        //
        // The next point, increment by periodX
        //     x' = (c1 + c2*(x+periodX+0.5)*preScaleX +
        //           c3*(y+0.5)*preScaleY) * postScaleX - 0.5
        //     y' = (c4 + c5*(x+periodX+0.5)*preScaleX +
        //           c6*(y+0.5)*preScaleY) * postScaleY - 0.5
        //
        // The difference between the 2 points
        //     dx = c2 * periodX * preScaleX * postScaleX
        //     dy = c5 * periodX * preScaleX * postScaleY
        //

        float px1 = periodX * preScaleX;	// power for period X

        float dx = c2 * px1 * postScaleX;	// delta x for x poly
        float dy = c5 * px1 * postScaleY;	// delta x for y poly

        float x1 = (x + 0.5F) * preScaleX;	// power for x

        width += x;
        height += y;
        int index = 0;			// destRect index

        for (int j = y; j < height; j += periodY) {
            float y1 = (j + 0.5F) * preScaleY;	// power for current y

            // The warped position for the first point of the current line.
            float wx = (c1 + c2 * x1 + c3 * y1) * postScaleX - 0.5F;
            float wy = (c4 + c5 * x1 + c6 * y1) * postScaleY - 0.5F;

            for (int i = x; i < width; i += periodX) {
                destRect[index++] = wx;
                destRect[index++] = wy;

                wx += dx;
                wy += dy;
            }
        }

        return destRect;
    }

    /**
     * Computes a Rectangle that is guaranteed to enclose the region
     * of the source that is required in order to produce a given
     * rectangular output region.
     *
     * @param destRect The Rectangle in destination coordinates.
     *
     * @return A <code>Rectangle</code> in the source coordinate
     *         system that is guaranteed to contain all pixels
     *         referenced by the output of <code>warpRect()</code> on
     *         the destination region, or <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>destRect</code> is
     *         <code>null</code>.
     */
    public Rectangle mapDestRect(Rectangle destRect) {
        if ( destRect == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        int dx0 = destRect.x;
        int dx1 = destRect.x + destRect.width;
        int dy0 = destRect.y;
        int dy1 = destRect.y + destRect.height;

        float[] pt;
        float sx0, sx1, sy0, sy1;

        pt = mapDestPoint(dx0, dy0);
        sx0 = pt[0];
        sx1 = pt[0];
        sy0 = pt[1];
        sy1 = pt[1];

        pt = mapDestPoint(dx1, dy0);
        sx0 = Math.min(sx0, pt[0]);
        sx1 = Math.max(sx1, pt[0]);
        sy0 = Math.min(sy0, pt[1]);
        sy1 = Math.max(sy1, pt[1]);

        pt = mapDestPoint(dx0, dy1);
        sx0 = Math.min(sx0, pt[0]);
        sx1 = Math.max(sx1, pt[0]);
        sy0 = Math.min(sy0, pt[1]);
        sy1 = Math.max(sy1, pt[1]);

        pt = mapDestPoint(dx1, dy1);
        sx0 = Math.min(sx0, pt[0]);
        sx1 = Math.max(sx1, pt[0]);
        sy0 = Math.min(sy0, pt[1]);
        sy1 = Math.max(sy1, pt[1]);

        int x = (int)Math.floor(sx0);
        int y = (int)Math.floor(sy0);
        int w = (int)Math.ceil(sx1 - x);
        int h = (int)Math.ceil(sy1 - y);

        return new Rectangle(x, y, w, h);
    }

    /**
     * Computes a Rectangle that is guaranteed to enclose the region
     * of the destination to which the source rectangle maps.
     *
     * @param srcRect The Rectangle in source coordinates.
     *
     * @return A <code>Rectangle</code> in the destination coordinate
     *         system that is guaranteed to contain all pixels
     *         within the forward mapping of the source rectangle.
     *
     * @throws IllegalArgumentException if <code>srctRect</code> is
     *         <code>null</code>.
     *
     * @since JAI 1.1
     */
    public Rectangle mapSourceRect(Rectangle srcRect) {
        if ( srcRect == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        //
        // According to spec, we return null if no forward
        // mapping can be derived.
        //
        if (invTransform == null) {
            return null;
        }

        int sx0 = srcRect.x;
        int sx1 = srcRect.x + srcRect.width;
        int sy0 = srcRect.y;
        int sy1 = srcRect.y + srcRect.height;

        float[] pt;
        float dx0, dx1, dy0, dy1;

        pt = mapSrcPoint(sx0, sy0);
        dx0 = pt[0];
        dx1 = pt[0];
        dy0 = pt[1];
        dy1 = pt[1];

        pt = mapSrcPoint(sx1, sy0);
        dx0 = Math.min(dx0, pt[0]);
        dx1 = Math.max(dx1, pt[0]);
        dy0 = Math.min(dy0, pt[1]);
        dy1 = Math.max(dy1, pt[1]);

        pt = mapSrcPoint(sx0, sy1);
        dx0 = Math.min(dx0, pt[0]);
        dx1 = Math.max(dx1, pt[0]);
        dy0 = Math.min(dy0, pt[1]);
        dy1 = Math.max(dy1, pt[1]);

        pt = mapSrcPoint(sx1, sy1);
        dx0 = Math.min(dx0, pt[0]);
        dx1 = Math.max(dx1, pt[0]);
        dy0 = Math.min(dy0, pt[1]);
        dy1 = Math.max(dy1, pt[1]);

        int x = (int)Math.floor(dx0);
        int y = (int)Math.floor(dy0);
        int w = (int)Math.ceil(dx1 - x);
        int h = (int)Math.ceil(dy1 - y);

        return new Rectangle(x, y, w, h);
    }

    // Maps a dest point to the source.
    private float[] mapDestPoint(int x, int y) {
        //XXX: This method should do its calculations in doubles

        //
        //     x' = (c1 + c2*(x+0.5)*preScaleX +
        //           c3*(y+0.5)*preScaleY) * postScaleX - 0.5
        //     y' = (c4 + c5*(x+0.5)*preScaleX +
        //           c6*(y+0.5)*preScaleY) * postScaleY - 0.5
        //
        float fx = (x + 0.5F) * preScaleX;	// pixel energy is at center
        float fy = (y + 0.5F) * preScaleY;

        float[] p = new float[2];
        p[0] = (c1 + c2 * fx + c3 * fy) * postScaleX - 0.5F;
        p[1] = (c4 + c5 * fx + c6 * fy) * postScaleY - 0.5F;

        return p;
    }

    // Maps a source point to the dest.
    private float[] mapSrcPoint(int x, int y) {
        //XXX: This method should do its calculations in doubles

        //
        //     x' = (invc1 + invc2*(x+0.5)*preScaleX +
        //           invc3*(y+0.5)*preScaleY) * postScaleX - 0.5
        //     y' = (invc4 + invc5*(x+0.5)*preScaleX +
        //           invc6*(y+0.5)*preScaleY) * postScaleY - 0.5
        //
        float fx = (x + 0.5F) * preScaleX;	// pixel energy is at center
        float fy = (y + 0.5F) * preScaleY;

        float[] p = new float[2];
        p[0] = (invc1 + invc2 * fx + invc3 * fy) * postScaleX - 0.5F;
        p[1] = (invc4 + invc5 * fx + invc6 * fy) * postScaleY - 0.5F;

        return p;
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
     * Point2D pt = (Point2D)destPt.clone();
     * pt.setLocation((c1 + c2*dx + c3*dy)*postScaleX - 0.5F,
     *                (c4 + c5*dx + c6*dy)*postScaleY - 0.5F);
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

        Point2D pt = (Point2D)destPt.clone();

        pt.setLocation((c1 + c2*dx + c3*dy)*postScaleX - 0.5F,
                       (c4 + c5*dx + c6*dy)*postScaleY - 0.5F);

        return pt;
    }

    /**
     * Computes the destination point corresponding to the supplied point.
     *
     * <p>If the transform is invertible, this method returns the value of
     * <code>pt</code> in the following code snippet:
     *
     * <pre>
     * double sx = (sourcePt.getX() + 0.5F)/postScaleX;
     * double sy = (sourcePt.getY() + 0.5F)/postScaleY;
     * Point2D pt = (Point2D)sourcePt.clone();
     * pt.setLocation((invc1 + invc2*sx + invc3*sy)/preScaleX - 0.5F,
     *                (invc4 + invc5*sx + invc6*sy)/preScaleY - 0.5F);
     * </pre>
     *
     * where <code>invc*</code> are the inverse transform coefficients. If
     * the transform is not invertible, <code>null</code> is returned.</p>
     *
     * @param sourcePt the position in source image coordinates
     * to map to destination image coordinates.
     *
     * @return a <code>Point2D</code> of the same class as
     * <code>sourcePt</code> or <code>null> if the transform is
     * not invertible.
     *
     * @throws IllegalArgumentException if <code>destPt</code> is
     * <code>null</code>.
     *
     * @since JAI 1.1.2
     */
    public Point2D mapSourcePoint(Point2D sourcePt) {
        if (sourcePt == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if(invTransform == null) {
            return null;
        }

        double sx = (sourcePt.getX() + 0.5F)/postScaleX;
        double sy = (sourcePt.getY() + 0.5F)/postScaleY;

        Point2D pt = (Point2D)sourcePt.clone();

        pt.setLocation((invc1 + invc2*sx + invc3*sy)/preScaleX - 0.5F,
                       (invc4 + invc5*sx + invc6*sy)/preScaleY - 0.5F);

        return pt;
    }
}
