/*
 * $RCSfile: AttributedImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:03 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

/**
 * A class which associates a <code>PlanarImage</code> with an attribute
 * of unspecified type.  The class is itself a <code>PlanarImage</code>
 * equivalent to the one which it wraps.
 *
 * @since JAI 1.1
 */
public class AttributedImage extends RenderedImageAdapter {
    /** The attribute associated with the image. */
    protected Object attribute;

    /**
     * Constructs an <code>AttributedImage</code>.  The attribute parameter
     * may be <code>null</code>
     *
     * @throws IllegalArgumentException if <code>theImage</code> is
     *                                  <code>null</code>.
     */
    public AttributedImage(PlanarImage image, Object attribute) {
        super(image);
        this.attribute = attribute;
    }

    /** Retrieves the wrapped image. */
    public PlanarImage getImage() {
        return (PlanarImage)theImage;
    }

    /** Stores the attribute. */
    public void setAttribute(Object attribute) {
        this.attribute = attribute;
    }

    /** Retrieves the attribute. */
    public Object getAttribute() {
        return attribute;
    }

    /**
     * Tests for equality.  The parameter <code>Object</code> must be
     * an <code>AttributedImage</code> the image and attribute of which
     * are equal those of this object according to the <code>equals()</code>
     * methods of the image and attribute of this image, respectively.
     * Attributes are also considered equal if they are both <code>null</code>.
     */
    public boolean equals(Object o) {
        if (o != null && o instanceof AttributedImage) {
            AttributedImage ai = (AttributedImage)o;
	    Object a = ai.getAttribute();
            return getImage().equals(ai.getImage()) &&
                (attribute == null ? a == null :
                 ((a != null) && attribute.equals(a)));
        }

        return false;
    }

    /** toString() method. */
    public String toString() {
        return "Attribute=(" + getAttribute() + ")  Image=" + getImage();
    }
}
