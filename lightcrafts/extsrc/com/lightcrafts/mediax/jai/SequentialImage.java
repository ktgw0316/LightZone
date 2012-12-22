/*
 * $RCSfile: SequentialImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:21 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

/**
 * A class representing an image that is associated with a time stamp
 * and a camera position.  This class is used with <code>ImageSequence</code>.
 *
 * <p> This class is equivalent to an <code>AttributedImage</code> with an
 * attribute defined as:
 *
 * <pre>
 * public class SequentialAttribute {
 *     protected Object position;
 *     protected Float timeStamp;
 *
 *     public SequentialAttribute(Object position, float timeStamp);
 *
 *     public Object getPosition();
 *     public float getTimeStamp();
 *
 *     public boolean equals(Object o) {
 *         if(o instanceof SequentialAttribute) {
 *	       SequentialAttribute sa = (SequentialAttribute)o;
 *	       return sa.getPosition().equals(position) &&
 *	              sa.getTimeStamp().equals(timeStamp);
 *	   }
 *         return false;
 *     }
 * }
 * </pre>
 *
 * @see ImageSequence
 *
 * @deprecated as of JAI 1.1. Use
 * <code>AttributedImage</code> instead.
 */
public class SequentialImage {

    /** The image. */
    public PlanarImage image;

    /** The time stamp associated with the image. */
    public float timeStamp;

    /**
     * The camera position associated with the image.  The type of this
     * parameter is <code>Object</code> so that the application may choose
     * any class to represent a camera position based on the individual's
     * needs.
     */
    public Object cameraPosition;

    /**
     * Constructor.
     *
     * @param pi The specified planar image.
     * @param ts The time stamp, as a float.
     * @param cp The camera position object.
     * @throws IllegalArgumentException if <code>pi</code> is <code>null</code>.
     */
    public SequentialImage(PlanarImage pi,
                           float ts,
                           Object cp) {
        if (pi == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        image = pi;
        timeStamp = ts;
        cameraPosition = cp;
    }
}
