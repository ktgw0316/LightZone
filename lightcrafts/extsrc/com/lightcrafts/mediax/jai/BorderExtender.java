/*
 * $RCSfile: BorderExtender.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:04 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.io.Serializable;
import java.awt.image.WritableRaster;

/**
 * An abstract superclass for classes that extend or "pad" a
 * <code>WritableRaster</code> with additional pixel data taken from a
 * <code>PlanarImage</code>.  Instances of <code>BorderExtender</code>
 * are used by the <code>getExtendedData()</code> and
 * <code>copyExtendedData()</code> methods in
 * <code>PlanarImage</code>.
 *
 * <p> Each instance of <code>BorderExtender</code> has an
 * <code>extend()</code> method that takes a
 * <code>WritableRaster</code> and a <code>PlanarImage</code>.  The
 * portion of the raster that intersects the bounds of the image will
 * already contain a copy of the image data.  The remaining area is to
 * be filled in according to the policy of the
 * <code>BorderExtender</code> subclass.
 *
 * <p> The standard subclasses of <code>BorderExtender</code> are
 * <code>BorderExtenderZero</code>, which fills pixels with zeros;
 * <code>BorderExtenderConstant</code>, which fills pixels with a
 * given constant value; <code>BorderExtenderCopy</code>, which copies
 * the edge pixels of the image; <code>BorderExtenderWrap</code>,
 * which tiles the plane with repeating copies of the image; and
 * <code>BorderExtenderReflect</code>, which is like
 * <code>BorderExtenderWrap</code> except that each copy of the image
 * is suitably reflected.
 *
 * <p> Instances of <code>BorderExtenderConstant</code> are 
 * constructed in the usual way.  Instances of the other standard subclasses
 * are obtained by means of the <code>createInstance()</code> method
 * of this class.
 *
 * <p> <code>BorderExtenderCopy</code> is particularly useful as a way
 * of padding image data prior to performing area or geometric
 * operations such as convolution, scaling, and rotation.
 *
 * @see PlanarImage#getExtendedData
 * @see PlanarImage#copyExtendedData
 * @see BorderExtenderConstant
 * @see BorderExtenderCopy
 * @see BorderExtenderReflect
 * @see BorderExtenderWrap
 * @see BorderExtenderZero
 */
public abstract class BorderExtender implements Serializable {

    /** A constant for use in the <code>createInstance</code> method. */
    public static final int BORDER_ZERO = 0;

    /** A constant for use in the <code>createInstance</code> method. */
    public static final int BORDER_COPY = 1;

    /** A constant for use in the <code>createInstance</code> method. */
    public static final int BORDER_REFLECT = 2;

    /** A constant for use in the <code>createInstance</code> method. */
    public static final int BORDER_WRAP = 3;

    /** Lazily-constructed singleton BorderExtenderZero. */
    private static BorderExtender borderExtenderZero = null;

    /** Lazily-constructed singleton BorderExtenderCopy. */
    private static BorderExtender borderExtenderCopy = null;

    /** Lazily-constructed singleton BorderExtenderReflect. */
    private static BorderExtender borderExtenderReflect = null;

    /** Lazily-constructed singleton BorderExtenderWrap. */
    private static BorderExtender borderExtenderWrap = null;

    /**
     * Fills in the portions of a given <code>WritableRaster</code> that
     * lie outside the bounds of a given <code>PlanarImage</code>.
     * Depending on the policy of the <code>BorderExtender</code>, data
     * might or might not be derived from the <code>PlanarImage</code>.
     *
     * <p> The portion of <code>raster</code> that lies within 
     * <code>im.getBounds()</code> must not be altered.  The pixels
     * within this region should not be assumed to have any particular
     * values.
     *
     * <p> Each subclass may implement a different policy regarding
     * how the extension data is computed.
     *
     * @param raster The <code>WritableRaster</code> the border area of
     *               which is to be filled according to the policy of the
     *               <code>BorderExtender</code>.
     * @param im     The <code>PlanarImage</code> which may provide the
     *               data with which to fill the border area of the
     *               <code>WritableRaster</code>.
     *
     * @throws <code>IllegalArgumentException</code> if either parameter is
     *         <code>null</code>.
     */
    public abstract void extend(WritableRaster raster,
                                PlanarImage im);

    /**
     * Returns an instance of <code>BorderExtender</code> that
     * implements a given extension policy.  The policies understood
     * by this method are:
     *
     * <p> <code>BORDER_ZERO</code>: set sample values to zero.
     *
     * <p> <code>BORDER_COPY</code>: set sample values to copies of
     * the nearest valid pixel.  For example, pixels to the left of
     * the valid rectangle will take on the value of the valid edge
     * pixel in the same row.  Pixels both above and to the left of
     * the valid rectangle will take on the value of the upper-left
     * pixel.
     *
     * <p> <code>BORDER_REFLECT</code>: the output image is defined
     * as if mirrors were placed along the edges of the source image.
     * Thus if the left edge of the valid rectangle lies at X = 10,
     * pixel (9, Y) will be a copy of pixel (10, Y); pixel (6, Y)
     * will be a copy of pixel (13, Y).
     *
     * <p> <code>BORDER_WRAP</code>: the source image is tiled repeatedly
     * in the plane.
     *
     * <p> Note that this method may not be used to create an instance
     * of <code>BorderExtenderConstant</code>.
     *
     * <p> Any other input value will cause an
     * <code>IllegalArgumentException</code> to be thrown.
     *
     * @param extenderType The type of <code>BorderExtender</code> to create.
     *                     Must be one of the predefined class constants
     *                     <code>BORDER_COPY</code>,
     *                     <code>BORDER_REFLECT</code>,
     *                     <code>BORDER_WRAP</code>, or
     *                     <code>BORDER_ZERO</code>.
     *
     * @throws <code>IllegalArgumentException</code> if the supplied
     *         parameter is not one of the supported predefined constants.
     */
    public static BorderExtender createInstance(int extenderType) {
        switch (extenderType) {
        case BORDER_ZERO:
            if(borderExtenderZero == null) {
                borderExtenderZero = new BorderExtenderZero();
            }
            return borderExtenderZero;

        case BORDER_COPY:
            if(borderExtenderCopy == null) {
                borderExtenderCopy = new BorderExtenderCopy();
            }
            return borderExtenderCopy;

        case BORDER_REFLECT:
            if(borderExtenderReflect == null) {
                borderExtenderReflect = new BorderExtenderReflect();
            }
            return borderExtenderReflect;

        case BORDER_WRAP:
            if(borderExtenderWrap == null) {
                borderExtenderWrap = new BorderExtenderWrap();
            }
            return borderExtenderWrap;

        default:
            throw new IllegalArgumentException(JaiI18N.getString("BorderExtender0"));
        }
    }
}
