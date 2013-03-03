/*
 * $RCSfile: WarpPerspective.java,v $
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
import java.awt.Rectangle;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

/**
 * A description of a perspective (projective) warp.
 *
 * <p> The transform is specified as a mapping from destination
 * space to source space.  This is a backward mapping, as opposed
 * to the forward mapping used in the "Affine" operation.
 *
 */
public final class WarpPerspective extends Warp {

    private PerspectiveTransform transform;
    private PerspectiveTransform invTransform;

    /**
     * Constructs a <code>WarpPerspective</code> with a given
     * transform mapping destination pixels into source space.  Note
     * that this is a backward mapping as opposed to the forward
     * mapping used in AffineOpImage.
     *
     * @param transform  The destination to source transform.
     * @throws IllegalArgumentException if transform is null
     */
    public WarpPerspective(PerspectiveTransform transform) {
        if (transform == null) {
            throw new IllegalArgumentException(JaiI18N.getString("WarpPerspective0"));
        }

        this.transform = transform;

        // Transform could be non-invertible.
        // If so the transform is set to null.
        try {
            invTransform = transform.createInverse();
        } catch (NoninvertibleTransformException e) {
            invTransform = null;
        } catch (CloneNotSupportedException e) {
            invTransform = null;
        }

    }

    /**
     * Returns a clone of the <code>PerspectiveTransform</code>
     * associated with this <code>WarpPerspective</code> object.
     *
     * @return An instance of <code>PerspectiveTransform</code>.
     */
    public PerspectiveTransform getTransform() {
        return (PerspectiveTransform)transform.clone();
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
     * @param periodY The horizontal sampling period.
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
     * @throw ArrayBoundsException if destRect is too small.
     */
    public float[] warpSparseRect(int x, int y,
                                  int width, int height,
                                  int periodX, int periodY,
                                  float[] destRect) {
        if (destRect == null) {
            destRect = new float[2 * ((width + periodX - 1) / periodX) *
                                     ((height + periodY - 1) / periodY)];
        }

        double[][] matrix = new double[3][3];
        matrix = transform.getMatrix(matrix);
        float m00 = (float)matrix[0][0];
        float m01 = (float)matrix[0][1];
        float m02 = (float)matrix[0][2];
        float m10 = (float)matrix[1][0];
        float m11 = (float)matrix[1][1];
        float m12 = (float)matrix[1][2];
        float m20 = (float)matrix[2][0];
        float m21 = (float)matrix[2][1];
        float m22 = (float)matrix[2][2];

        //
        // x' = (m00x + m01y + m02) / (m20x + m21y + m22)
        // y' = (m10x + m11y + m12) / (m20x + m21y + m22)
        //

        float dx = m00 * periodX;
        float dy = m10 * periodX;
        float dw = m20 * periodX;

        float sx = x + 0.5F;		// shift coordinate by 0.5

        width += x;
        height += y;
        int index = 0;			// destRect index

        for (int j = y; j < height; j += periodY) {
            float sy = j + 0.5F;

            float wx = m00 * sx + m01 * sy + m02;
            float wy = m10 * sx + m11 * sy + m12;
            float w = m20 * sx + m21 * sy + m22;

            for (int i = x; i < width; i += periodX) {
                float tx, ty;
                try {
                    tx = wx / w;
                    ty = wy / w;
                } catch (java.lang.ArithmeticException e) {
                    // w is 0, do not warp
                    tx = i + 0.5F; 	// to be subtracted below
                    ty = j + 0.5F;
                }

                destRect[index++] = tx - 0.5F;
                destRect[index++] = ty - 0.5F;

                wx += dx;
                wy += dy;
                w += dw;
            }
        }

        return destRect;
    }

    /**
     * Computes a Rectangle that is guaranteed to enclose the region
     * of the source that is required in order to produce a given
     * rectangular output region.
     *
     * @param destRect The <code>Rectangle</code> in destination coordinates.
     * @throws IllegalArgumentException if destRect is null.
     * @return A <code>Rectangle</code> in the source coordinate
     *         system that is guaranteed to contain all pixels
     *         referenced by the output of <code>warpRect()</code> on
     *         the destination region.
     */
    public Rectangle mapDestRect(Rectangle destRect) {
        if ( destRect == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        int x0 = destRect.x;
        int x1 = destRect.x + destRect.width;
        int y0 = destRect.y;
        int y1 = destRect.y + destRect.height;

        Point2D[] pts = new Point2D[4];
        pts[0] = new Point2D.Float(x0, y0);
        pts[1] = new Point2D.Float(x1, y0);
        pts[2] = new Point2D.Float(x0, y1);
        pts[3] = new Point2D.Float(x1, y1);

        transform.transform(pts, 0, pts, 0, 4);

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int i = 0; i < 4; i++) {
            int px = (int)pts[i].getX();
            int py = (int)pts[i].getY();

            minX = Math.min(minX, px);
            maxX = Math.max(maxX, px);
            minY = Math.min(minY, py);
            maxY = Math.max(maxY, py);
        }

        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Computes a Rectangle that is guaranteed to enclose the region
     * of the source that is required in order to produce a given
     * rectangular output region.
     *
     * @param srcRect The <code>Rectangle</code> in source coordinates.
     * @throws IllegalArgumentException is srcRect is null.
     * @return A <code>Rectangle</code> in the destination coordinate
     *         system that is guaranteed to contain all pixels
     *         within the forward mapping of the source rectangle.
     *
     * @since JAI 1.1
     */
    public Rectangle mapSourceRect(Rectangle srcRect) {
        if ( srcRect == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        // Return null if no forward mapping could be derived
        if (invTransform == null) {
            return null;
        }

        int x0 = srcRect.x;
        int x1 = srcRect.x + srcRect.width;
        int y0 = srcRect.y;
        int y1 = srcRect.y + srcRect.height;

        Point2D[] pts = new Point2D[4];
        pts[0] = new Point2D.Float(x0, y0);
        pts[1] = new Point2D.Float(x1, y0);
        pts[2] = new Point2D.Float(x0, y1);
        pts[3] = new Point2D.Float(x1, y1);

        invTransform.transform(pts, 0, pts, 0, 4);

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int i = 0; i < 4; i++) {
            int px = (int)pts[i].getX();
            int py = (int)pts[i].getY();

            minX = Math.min(minX, px);
            maxX = Math.max(maxX, px);
            minY = Math.min(minY, py);
            maxY = Math.max(maxY, py);
        }

        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Computes the source point corresponding to the supplied point.
     *
     * <p>This method returns the return value of
     * <code>transform.transform(destPt, null)</code>.</p>
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

        return transform.transform(destPt, null);
    }

    /**
     * Computes the destination point corresponding to the supplied point.
     *
     * <p>If the transform is invertible, this method returns the return
     * value of <code>transform.inverseTransform(destPt, null)</code>. If
     * the transform is not invertible, <code>null</code> is returned.</p>
     *
     * @param sourcePt the position in source image coordinates
     * to map to destination image coordinates.
     *
     * @return a <code>Point2D</code> of the same class as
     * <code>sourcePt</code> or <code>null> if the transform is
     * not invertible.
     *
     * @throws IllegalArgumentException if <code>sourcePt</code> is
     * <code>null</code>.
     *
     * @since JAI 1.1.2
     */
    public Point2D mapSourcePoint(Point2D sourcePt) {
        if (sourcePt == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        return invTransform != null ?
            invTransform.transform(sourcePt, null) : null;
    }
}

