/*
 * $RCSfile: ImageStack.java,v $
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
import java.util.Collection;
import java.util.Iterator;

/**
 * A class representing a stack of images, each associated with a
 * spatial position/orientation defined in a common coordinate system.
 * The images are of the type <code>com.lightcrafts.mediax.jai.PlanarImage</code>;
 * the coordinates are of the type <code>java.lang.Object</code>.
 * The tuple (image, coordinate) is represented by class
 * <code>com.lightcrafts.mediax.jai.CoordinateImage</code>.
 *
 * <p> This class can be used to represent medical or geophysical images.
 *
 * @see PlanarImage
 *
 * @deprecated as of JAI 1.1. Use
 * <code>AttributedImageCollection</code> instead.
 */
public abstract class ImageStack extends CollectionImage {

    /** The default constructor. */
    protected ImageStack() {}

    /**
     * Constructor.
     *
     * @param images  A collection of <code>CoordinateImage</code>.
     *
     * @throws IllegalArgumentException if <code>images</code> is <code>null</code>.
     */
    public ImageStack(Collection images) {
        super(images);
    }

    /**
     * Returns the image associated with the specified coordinate,
     * or <code>null</code> if <code>c</code> is <code>null</code> or
     * if no match is found.
     *
     * @param c The specified coordinate object.
     */
    public PlanarImage getImage(Object c) {
        if (c != null) {
            Iterator iter = iterator();

            while (iter.hasNext()) {
                CoordinateImage ci = (CoordinateImage)iter.next();
                if (ci.coordinate.equals(c)) {
                    return ci.image;
                }
            }
        }

        return null;
    }

    /**
     * Returns the coordinate associated with the specified image,
     * or <code>null</code> if <code>pi</code> is <code>null</code> or
     * if no match is found.
     *
     *  @param pi The specified planar image.
     */
    public Object getCoordinate(PlanarImage pi) {
        if (pi != null) {
            Iterator iter = iterator();

            while (iter.hasNext()) {
                CoordinateImage ci = (CoordinateImage)iter.next();
                if (ci.image.equals(pi)) {
                    return ci.coordinate;
                }
            }
        }

        return null;
    }

    /**
     * Adds a <code>CoordinateImage</code> to this collection.  If the
     * specified image is <code>null</code>, it is not added to the
     * collection.
     *
     * @return true if and only if the <code>CoordinateImage</code> is added
     *         to the collection.
     */
    public boolean add(Object o) {
        if (o != null && o instanceof CoordinateImage) {
            return super.add(o);
        } else {
            return false;
        }
    }

    /**
     * Removes the <code>CoordinateImage</code> that contains the
     * specified image from this collection.
     *
     * @param pi The specified planar image.
     * @return true if and only if a <code>CoordinateImage</code> containing
     *         the specified image is removed from the collection.
     */
    public boolean remove(PlanarImage pi) {
        if (pi != null) {
            Iterator iter = iterator();

            while (iter.hasNext()) {
                CoordinateImage ci = (CoordinateImage)iter.next();
                if (ci.image.equals(pi)) {
                    return super.remove(ci);
                }
            }
        }

        return false;
    }

    /**
     * Removes the <code>CoordinateImage</code> that contains the
     * specified coordinate from this collection.
     *
     * @param c The specified coordinate object.
     * @return true if and only if a <code>CoordinateImage</code> containing
     *         the specified coordinate is removed from the collection.
     */
    public boolean remove(Object c) {
        if (c != null) {
            Iterator iter = iterator();

            while (iter.hasNext()) {
                CoordinateImage ci = (CoordinateImage)iter.next();
                if (ci.coordinate.equals(c)) {
                    return super.remove(ci);
                }
            }
        }

        return false;
    }
}
