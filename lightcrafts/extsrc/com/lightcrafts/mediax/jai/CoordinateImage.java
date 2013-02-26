/*
 * $RCSfile: CoordinateImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:07 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

/**
 * A class representing an image that is associated with a coordinate.
 * This class is used with <code>ImageStack</code>.
 *
 * @see ImageStack
 *
 * @deprecated as of JAI 1.1. Use
 * <code>AttributedImage</code> instead.
 */
public class CoordinateImage {

    /** The image. */
    public PlanarImage image;

    /**
     * The coordinate associated with the image.  The type of this
     * parameter is <code>Object</code> so that the application may choose
     * any class to represent a coordinate based on the individual's
     * needs.
     */
    public Object coordinate;

    /**
     * Constructor.
     *
     * @throws IllegalArgumentException if <code>pi</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>c</code> is <code>null</code>.
     */
    public CoordinateImage(PlanarImage pi,
                           Object c) {
        if (pi == null || c == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        image = pi;
        coordinate = c;
    }
}
