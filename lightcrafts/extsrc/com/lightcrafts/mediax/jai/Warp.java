/*
 * $RCSfile: Warp.java,v $
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
import java.awt.geom.Point2D;
import java.io.Serializable;

/** 
 * A description of an image warp.
 *
 * <p> The central method of a <code>Warp</code> is
 * <code>warpSparseRect()</code>, which returns the source pixel positions
 * for a specified (subdivided) rectangular region of the output.
 *
 * <p> As in the <code>Interpolation</code> class, pixel positions are
 * represented using scaled integer coordinates, yielding subpixel
 * accuracy but still allowing the use of integer arithmetic.  The
 * degree of precision is set by means of the
 * <code>getSubSampleBitsH()</code> and <code>getSubSampleBitsV</code>
 * parameters to the <code>warpRect()</code> method.
 *
 * @see Interpolation
 * @see WarpAffine
 * @see WarpGrid
 * @see WarpPerspective
 * @see WarpPolynomial
 * @see WarpQuadratic
 * @see WarpCubic
 * @see WarpGeneralPolynomial
 * @see WarpOpImage
 */
public abstract class Warp extends Object implements Serializable {

    /** Default constructor. */
    protected Warp() {}

    /**
     * Computes the source subpixel positions for a given rectangular
     * destination region.  The destination region is specified using
     * normal integral (full pixel) coordinates.  The source positions
     * returned by the method are specified in fixed point, subpixel
     * coordinates using the <code>subsampleBitsH</code> and
     * <code>subsampleBitsV</code> parameters.
     *
     * <p> The integral destination rectangle coordinates should be 
     * considered pixel indices. The continuous plane of pixels
     * locates each pixel index at a half-pixel location. For example,
     * destination pixel (0,0) is located at the real location (0.5, 0.5).
     * Thus pixels are considered to have a dimension of (1.0 x 1.0) with
     * their "energy" concentrated in a "delta function" at relative 
     * coordinates (0.5, 0.5).
     *
     * <p> Destination to source mappings must keep this (0.5, 0.5) pixel
     * center in mind when formulating transformation functions. Given
     * integral destination pixel indices as an input, the fractional
     * source location, as calculated by functions X(xDst,yDst), Y(xDst,yDst) 
     * is given by:
     * <pre>
     *
     *     Xsrc = X(xDst+0.5, yDst+0.5) - 0.5
     *     Ysrc = Y(xDst+0.5, yDst+0.5) - 0.5
     *
     * </pre>
     *
     * <p> The subtraction of 0.5 in the above formula produces the
     * source pixel indices (in fractional form) needed to implement
     * the various types of interpolation algorithms.
     *
     * <p>All of the Sun-supplied warp mapping functions perform the 
     * above final subtraction, since they have no knowledge of what 
     * interpolation algorithm will be used by a WarpOpImage implementation.
     *
     * <p> As a convenience, an implementation is provided for this
     * method that calls <code>warpSparseRect()</code>.  Subclasses
     * may wish to provide their own implementations for better
     * performance.
     *
     * @param x The minimum X coordinate of the destination region.
     * @param y The minimum Y coordinate of the destination region.
     * @param width The width of the destination region. Must be positive.
     * @param height The height of the destination region. Must be positive.
     * @param subsampleBitsH The desired fixed-point precision of the
     *        output X coordinates. Must be positive.
     * @param subsampleBitsV The desired fixed-point precision of the
     *        output Y coordinates. Must be positive.
     * @param destRect An int array containing at least
     *        <code>2*width*height</code> elements, or
     *        <code>null</code>.  If <code>null</code>, a new array
     *        will be constructed.
     *
     * @return A reference to the destRect parameter if it is
     *         non-<code>null</code>, or a new <code>int</code> array
     *         of length <code>2*width*height</code> otherwise.
     */
    public int[] warpRect(int x, int y, int width, int height,
                          int subsampleBitsH, int subsampleBitsV,
                          int[] destRect) {
        if (destRect != null && destRect.length < (width * height * 2)) {
            throw new IllegalArgumentException(JaiI18N.getString("Warp0"));
        }
        return warpSparseRect(x, y, width, height, 1, 1,
                              subsampleBitsH, subsampleBitsV, destRect);
    }

    /**
     * Computes the source subpixel positions for a given rectangular
     * destination region.  The destination region is specified using
     * normal integral (full pixel) coordinates.  The source positions
     * returned by the method are specified in floating point.
     *
     * <p> As a convenience, an implementation is provided for this
     * method that calls <code>warpSparseRect()</code>.  Subclasses
     * may wish to provide their own implementations for better
     * performance.
     *
     * @param x The minimum X coordinate of the destination region.
     * @param y The minimum Y coordinate of the destination region.
     * @param width The width of the destination region.
     * @param height The height of the destination region.
     * @param destRect A <code>float</code> array containing at least
     *        <code>2*width*height</code> elements, or
     *        <code>null</code>.  If <code>null</code>, a new array
     *        will be constructed.
     *
     * @return A reference to the <code>destRect</code> parameter if
     *         it is non-<code>null</code>, or a new <code>float</code>
     *         array of length <code>2*width*height</code> otherwise.
     * @throws IllegalArgumentException if destRect is too small.
     */
    public float[] warpRect(int x, int y,
                            int width, int height,
                            float[] destRect) {
        if (destRect != null && destRect.length < (width * height * 2)) {
            throw new IllegalArgumentException(JaiI18N.getString("Warp0"));
        }
        return warpSparseRect(x, y, width, height, 1, 1, destRect);
    }

    /**
     * Computes the source subpixel position for a given destination
     * pixel.  The destination pixel is specified using normal
     * integral (full pixel) coordinates.  The source position
     * returned by the method is specified in fixed point, subpixel
     * coordinates using the <code>subsampleBitsH</code> and
     * <code>subsampleBitsV</code> parameters.
     *
     * <p> As a convenience, an implementation is provided for this
     * method that calls <code>warpSparseRect()</code>.  Subclasses
     * may wish to provide their own implementations for better
     * performance.
     *
     * @param x The minimum X coordinate of the destination region.
     * @param y The minimum Y coordinate of the destination region.
     * @param subsampleBitsH The desired fixed-point precision of the
     *        output X coordinates.
     * @param subsampleBitsV The desired fixed-point precision of the
     *        output Y coordinates.
     * @param destRect An <code>int</code> array containing at least 2
     *        elements, or <code>null</code>.  If <code>null</code>, a
     *        new array will be constructed.
     *
     * @return A reference to the destRect parameter if it is
     *         non-<code>null</code>, or a new <code>int</code> array
     *         of length 2 otherwise.
     * @throws IllegalArgumentException if destRect is too small.
     */
    public int[] warpPoint(int x, int y,
                           int subsampleBitsH, int subsampleBitsV,
                           int[] destRect) {
        if (destRect != null && destRect.length < 2) {
            throw new IllegalArgumentException(JaiI18N.getString("Warp0"));
        }
        return warpSparseRect(x, y, 1, 1, 1, 1,
                              subsampleBitsH, subsampleBitsV, destRect);
    }

    /**
     * Computes the source subpixel position for a given destination
     * pixel.  The destination pixel is specified using normal
     * integral (full pixel) coordinates.  The source position
     * returned by the method is specified in floating point.
     *
     * <p> As a convenience, an implementation is provided for this
     * method that calls <code>warpSparseRect()</code>.  Subclasses
     * may wish to provide their own implementations for better
     * performance.
     *
     * @param x The minimum X coordinate of the destination region.
     * @param y The minimum Y coordinate of the destination region.
     * @param destRect A <code>float</code> array containing at least
     *        2 elements, or <code>null</code>.  If <code>null</code>,
     *        a new array will be constructed.
     *
     * @return A reference to the <code>destRect</code> parameter if
     *         it is non-<code>null</code>, or a new
     *         <code>float</code> array of length 2 otherwise.
     * @throws IllegalArgumentException if destRect is too small.
     */
    public float[] warpPoint(int x, int y, float[] destRect) {
        if (destRect != null && destRect.length < 2) {
            throw new IllegalArgumentException(JaiI18N.getString("Warp0"));
        }
        return warpSparseRect(x, y, 1, 1, 1, 1, destRect);
    }

    /**
     * Computes the source subpixel positions for a given rectangular
     * destination region, subsampled with an integral period.  The
     * destination region is specified using normal integral (full
     * pixel) coordinates.  The source positions returned by the
     * method are specified in fixed point, subpixel coordinates using
     * the <code>subsampleBitsH</code> and <code>subsampleBitsV</code>
     * parameters.
     *
     * <p> As a convenience, an implementation is provided for this
     * method that calls <code>warpSparseRect()</code> with a
     * <code>float</code> <code>destRect</code> parameter.  Subclasses
     * may wish to provide their own implementations for better
     * performance.
     *
     * @param x the minimum X coordinate of the destination region.
     * @param y the minimum Y coordinate of the destination region.
     * @param width the width of the destination region.
     * @param height the height of the destination region.
     * @param periodX the horizontal sampling period.
     * @param periodY the horizontal sampling period.
     * @param subsampleBitsH The desired fixed-point precision of the
     *        output X coordinates.
     * @param subsampleBitsV The desired fixed-point precision of the
     *        output Y coordinates.
     * @param destRect An int array containing at least
     *        2*((width+periodX-1)/periodX)*((height+periodY-1)/periodY)
     *        elements, or <code>null</code>.  If <code>null</code>, a
     *        new array will be constructed.
     *
     * @return A reference to the <code>destRect</code> parameter if
     *         it is non-<code>null</code>, or a new <code>int</code>
     *         array otherwise.
     * @throws IllegalArgumentException if destRect is too small.
     */
    public int[] warpSparseRect(int x, int y, int width, int height,
                                int periodX, int periodY,
                                int subsampleBitsH, int subsampleBitsV,
                                int[] destRect) {
        int nVals = 2*((width+periodX-1)/periodX)*((height+periodY-1)/periodY);
        if (destRect != null && destRect.length < nVals) {
            throw new IllegalArgumentException(JaiI18N.getString("Warp0"));
        }
        float[] fdestRect = warpSparseRect(x, y, width, height,
                                           periodX, periodY, (float[])null);
        int size = fdestRect.length;

        if (destRect == null) {
            destRect = new int[size];
        }

        int precH = 1 << subsampleBitsH;
        int precV = 1 << subsampleBitsV;

        for (int i = 0; i < size; i += 2) {
            destRect[i] = (int)Math.floor(fdestRect[i]*precH);
            destRect[i+1] = (int)Math.floor(fdestRect[i + 1]*precV);
        }

        return destRect;
    }

    /**
     * <p> This method is must be implemented in all concrete subclasses.
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
     * @return a reference to the <code>destRect</code> parameter if
     *         it is non-<code>null</code>, or a new
     *         <code>float</code> array otherwise.
     */
    public abstract float[] warpSparseRect(int x, int y,
                                           int width, int height,
                                           int periodX, int periodY,
                                           float[] destRect);

    /**
     * Computes a rectangle that is guaranteed to enclose the region
     * of the destination that can potentially be affected by the
     * pixels of a rectangle of a given source.
     * Unlike the corresponding <code>WarpOpImage</code> method,
     * this routine may return <code>null</code>
     * if it is infeasible to compute such a bounding box.
     *
     * <p> The default implementation in this class returns <code>null</code>.
     *
     * @param sourceRect The Rectangle in source coordinates.
     *
     * @return A <code>Rectangle</code> in the destination coordinate
     *         system that enclose the region that can potentially be
     *         affected by the pixels of a rectangle of a given source,
     *         or <code>null</code>.
     */
    public Rectangle mapSourceRect(Rectangle sourceRect) {
        return null;
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

        int x = destRect.x;
        int y = destRect.y;
        int w = destRect.width;         // the column immediately to the right
        int h = destRect.height;        // and bottom of the last column

        // Alloc an array large enough for the largest destRect side
        float[] warpPoints = new float[Math.max(w * 2, (h - 2) * 2)];;

        // Map the pixels along the edges and find their min and max.

        // Map Top edge.
        int length = w * 2;  // length for top edge
        warpSparseRect(x, y, w, 1, 1, 1, warpPoints);

        // initialize min/maxX/Y to first point
        float minX = warpPoints[0];     
        float maxX = warpPoints[0];
        float minY = warpPoints[1];
        float maxY = warpPoints[1];

        float thisX, thisY;

        for (int i = 2; i < length; i += 2) {
            thisX = warpPoints[i];
            thisY = warpPoints[i+1];

            if (thisX < minX) {
                minX = thisX;
            } else if (thisX > maxX) {
                maxX = thisX;
            }

            if (thisY < minY) {
                minY = thisY;
            } else if (thisY > maxY) {
                maxY = thisY;
            }
        }

        // Map bottom edge.
        warpSparseRect(x, y + h - 1, w, 1, 1, 1, warpPoints);

        for (int i = 0; i < length; i += 2) {
            thisX = warpPoints[i];
            thisY = warpPoints[i+1];

            if (thisX < minX) {
                minX = thisX;
            } else if (thisX > maxX) {
                maxX = thisX;
            }

            if (thisY < minY) {
                minY = thisY;
            } else if (thisY > maxY) {
                maxY = thisY;
            }
        }

        // Map left edge.
        length = (h - 2) * 2;
        warpSparseRect(x, y + 1, 1, h - 2, 1, 1, warpPoints);

        for (int i = 0; i < length; i += 2) {
            thisX = warpPoints[i];
            thisY = warpPoints[i+1];

            if (thisX < minX) {
                minX = thisX;
            } else if (thisX > maxX) {
                maxX = thisX;
            }

            if (thisY < minY) {
                minY = thisY;
            } else if (thisY > maxY) {
                maxY = thisY;
            }
        }

        // Map right edge.
        warpSparseRect(x + w - 1, y + 1, 1, h - 2, 1, 1, warpPoints);

        for (int i = 0; i < length; i += 2) {
            thisX = warpPoints[i];
            thisY = warpPoints[i+1];

            if (thisX < minX) {
                minX = thisX;
            } else if (thisX > maxX) {
                maxX = thisX;
            }

            if (thisY < minY) {
                minY = thisY;
            } else if (thisY > maxY) {
                maxY = thisY;
            }
        }

        x = (int)Math.floor(minX);
        y = (int)Math.floor(minY);
        w = (int)Math.ceil(maxX - x) + 1;
        h = (int)Math.ceil(maxY - y) + 1;

        return new Rectangle(x, y, w, h);
    }

    /**
     * Computes the source point corresponding to the supplied point.
     *
     * <p>This method returns the value of <code>pt</code> in the following
     * code snippet:
     *
     * <pre>
     * float[] sourceXY = warpSparseRect((int)destPt.getX(),
     *                                   (int)destPt.getY(),
     *                                   1, 1, 1, 1, null);
     * Point2D pt = (Point2D)destPt.clone();
     * pt.setLocation(sourceXY[0], sourceXY[1]);
     * </pre>
     *
     * Subclasses requiring different behavior should override this
     * method. This would be the case for those which desire a more
     * precise mapping.</p>
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

        float[] sourceXY = warpSparseRect((int)destPt.getX(),
                                          (int)destPt.getY(),
                                          1, 1, 1, 1, null);
        Point2D pt = (Point2D)destPt.clone();
        pt.setLocation(sourceXY[0], sourceXY[1]);

        return pt;
    }

    /**
     * Computes the destination point corresponding to the supplied point.
     *
     * <p>This method returns <code>null</code>. Subclasses requiring
     * different behavior should override this method.</p>
     *
     * @param sourcePt the position in source image coordinates
     * to map to destination image coordinates.
     *
     * @return <code>null</code>.
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

        return null;
    }
}
