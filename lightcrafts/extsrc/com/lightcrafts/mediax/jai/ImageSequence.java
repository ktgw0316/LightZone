/*
 * $RCSfile: ImageSequence.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:09 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.util.Collection;
import java.util.Iterator;

/**
 * A class representing a sequence of images, each associated with a
 * time stamp and a camera position.  The images are of the type
 * <code>com.lightcrafts.mediax.jai.PlanarImage</code>; the time stamps are of the
 * type <code>float</code>; the camera positions are of the type
 * <code>java.lang.Object</code>.  The tuple (image, time stamp, camera
 * position) is represented by class
 * <code>com.lightcrafts.mediax.jai.SequentialImage</code>.
 *
 * <p> This class can be used to represent video or time-lapse photography.
 *
 * @see PlanarImage
 * @see SequentialImage
 *
 * @deprecated as of JAI 1.1. Use
 * <code>AttributedImageCollection</code> instead.
 */

public class ImageSequence extends CollectionImage {

    /** The default constrctor. */
    protected ImageSequence() {}

    /**
     * Constructs a class that represents a sequence of images.
     *
     * @param images  A collection of <code>SequentialImage</code>.
     *
     * @throws IllegalArgumentException if <code>images</code> is <code>null</code>.
     */
    public ImageSequence(Collection images) {
        super(images);
    }

    /**
     * Returns the image associated with the specified time stamp,
     * or <code>null</code> if no match is found.
     */
    public PlanarImage getImage(float ts) {
        Iterator iter = iterator();

        while (iter.hasNext()) {
            SequentialImage si = (SequentialImage)iter.next();
            if (si.timeStamp == ts) {
                return si.image;
            }
        }

        return null;
    }

    /**
     * Returns the image associated with the specified camera position,
     * or <code>null</code> if <code>cp</code> is <code>null</code> or
     * if no match is found.
     */
    public PlanarImage getImage(Object cp) {
        if (cp != null) {
            Iterator iter = iterator();

            while (iter.hasNext()) {
                SequentialImage si = (SequentialImage)iter.next();
                if (si.cameraPosition.equals(cp)) {
                    return si.image;
                }
            }
        }

        return null;
    }

    /**
     * Returns the time stamp associated with the specified image, or
     * <code>-Float.MAX_VALUE</code> if <code>pi</code> is <code>null</code>
     * or if no match is found.
     */
    public float getTimeStamp(PlanarImage pi) {
        if (pi != null) {
            Iterator iter = iterator();

            while (iter.hasNext()) {
                SequentialImage si = (SequentialImage)iter.next();
                if (si.image.equals(pi)) {
                    return si.timeStamp;
                }
            }
        }

        return -Float.MAX_VALUE;
    }

    /**
     * Returns the camera position associated with the specified image,
     * or <code>null</code> if <code>pi</code> is <code>null</code> or
     * if no match is found.
     */
    public Object getCameraPosition(PlanarImage pi) {
        if (pi != null) {
            Iterator iter = iterator();

            while (iter.hasNext()) {
                SequentialImage si = (SequentialImage)iter.next();
                if (si.image.equals(pi)) {
                    return si.cameraPosition;
                }
            }
        }

        return null;
    }

    /**
     * Adds a <code>SequentialImage</code> to this collection.  If the
     * specified image is <code>null</code>, it is not added to the
     * collection.
     *
     * @return <code>true</code> if and only if the
     *         <code>SequentialImage</code> is added to the collection.
     */
    public boolean add(Object o) {
        if (o != null && o instanceof SequentialImage) {
            return super.add(o);
        } else {
            return false;
        }
    }

    /**
     * Removes the <code>SequentialImage</code> that contains the
     * specified image from this collection.
     *
     * @return <code>true</code> if and only if a
     *         <code>SequentialImage</code> with the
     *         specified image is removed from the collection.
     */
    public boolean remove(PlanarImage pi) {
        if (pi != null) {
            Iterator iter = iterator();

            while (iter.hasNext()) {
                SequentialImage si = (SequentialImage)iter.next();
                if (si.image.equals(pi)) {
                    return super.remove(si);
                }
            }
        }

        return false;
    }

    /**
     * Removes the <code>SequentialImage</code> that contains the
     * specified time stamp from this collection.
     *
     * @return <code>true</code> if and only if a
     *         <code>SequentialImage</code> with the
     *         specified time stamp is removed from the collection.
     */
    public boolean remove(float ts) {
        Iterator iter = iterator();

        while (iter.hasNext()) {
            SequentialImage si = (SequentialImage)iter.next();
            if (si.timeStamp == ts) {
                return super.remove(si);
            }
        }

        return false;
    }

    /**
     * Removes the <code>SequentialImage</code> that contains the
     * specified camera position from this collection.
     *
     * @return <code>true</code> if and only if a
     *         <code>SequentialImage</code> with the
     *         specified camera position is removed from the collection.
     */
    public boolean remove(Object cp) {
        if (cp != null) {
            Iterator iter = iterator();

            while (iter.hasNext()) {
                SequentialImage si = (SequentialImage)iter.next();
                if (si.cameraPosition.equals(cp)) {
                    return super.remove(si);
                }
            }
        }

        return false;
    }
}
