/*
 * $RCSfile: AttributedImageCollection.java,v $
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

/**
 * Class representing a CollectionImage wherein all image elements are
 * AttributedImage instances. All Collection methods will be overridden
 * such that contained images are forced to be AttributedImages.
 *
 * <p> Note that the methods <code>getAll(attribute)</code> and
 * <code>removeAll(attribute)</code> use the <code>equals()</code> method
 * of the attribute parameter rather that that of the
 * <code>AttributedImage</code>s in the <code>Collection</code>.  This
 * permits "filtering" if the attribute of the <code>AttributedImage</code>s
 * contains more than one type of value.  For example, if the attribute
 * contained both position and time, then the parameter <code>attribute</code>
 * could be an instance of a class which compared only the position if it
 * were desired to obtain or remove all images at a given position
 * irrespective of the time stamp.
 *
 * @since JAI 1.1
 */
public class AttributedImageCollection extends CollectionImage {

    protected AttributedImageCollection() { }

    /**
     * Constructs an <code>AttributedImageCollection</code> with contents
     * set to the contents of the supplied <code>Collection</code>.  Only
     * elements in the <code>Collection</code> which are instances of
     * <code>AttributedImage</code> will be added.
     *
     * @throws IllegalArgumentException if <code>images</code> is <code>null</code>
     */
    public AttributedImageCollection(Collection images) {
        super();

        if ( images == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("AttributedImageCollection0"));
        }

        try {
            // Try to create a Collection of the same class.
            imageCollection = (Collection)images.getClass().newInstance();
        } catch(Exception e) {
            // As a fallback create a List.
            imageCollection = new ArrayList(images.size());
        }

        // only add AttributedImages which have not yet been added
        Iterator iter = images.iterator();
        while( iter.hasNext() ) {
            Object o = iter.next();

            if ( o instanceof AttributedImage &&
                 !imageCollection.contains(o)) {
                imageCollection.add( o);
            }
        }
    }

    /**
     * Returns a Set of all AttributedImages the attribute of which is
     * equal to the parameter object according to the equals() method of
     * the parameter object. If no match is found null will be returned.
     * If the parameter is null a Set view of all AttributedImages in the
     * Collection will be returned.
     */
    public Set getAll(Object attribute) {

        if ( attribute == null ) {
            return (Set) imageCollection;
        } else {
            HashSet set = null;
            Iterator iter = iterator();

            while( iter.hasNext() ) {
                AttributedImage ai = (AttributedImage)iter.next();

                if ( attribute.equals(ai.getAttribute()) ) {
                    if ( set == null ) {
                        set = new HashSet();
                    }

                    set.add(ai);
                }
            }

            return set;
        }
    }

    /**
     * Returns a Set of all AttributedImages the image of which is equal
     * to the parameter image. If no match is found null will be returned.
     * If the parameter is null a Set view of all AttributedImages in the
     * Collection will be returned.
     */
    public Set getAll(PlanarImage image) {

        if ( image == null ) {
            return (Set)imageCollection;
        } else {
            HashSet set = null;
            Iterator iter = iterator();

            while( iter.hasNext() ) {
                AttributedImage ai = (AttributedImage)iter.next();

                if ( image.equals(ai.getImage()) ) {
                    if ( set == null ) {
                        set = new HashSet();
                    }

                    set.add(ai);
                }
            }

            return set;
        }
    }

    /**
     * Removes all AttributedImages the attribute of which is
     * equal to the parameter object according to the equals() method of the
     * parameter object.  The returned value contains all AttributedImages
     * which were removed from the underlying Collection or null if no
     * match was found.  If the parameter is null, null will be returned.
     */
    public Set removeAll(Object attribute) {

        if ( attribute == null ) {
            return null;
        } else {
            Iterator iter = iterator();
            Set removed = null;

            while( iter.hasNext() ) {
                AttributedImage ai = (AttributedImage)iter.next();

                if ( attribute.equals(ai.getAttribute()) ) {
                    iter.remove();
                    if(removed == null) {
                        removed = new HashSet();
                    }
                    removed.add(ai);
                }
            }

            return (Set)removed;
        }
    }

    /**
     * Removes all AttributedImages the image of which is equal to the
     * parameter image.  The returned value contains all AttributedImages
     * which were removed from the underlying Collection or null if no
     * match was found.  If the parameter is null, null will be returned.
     */
    public Set removeAll(PlanarImage image) {

        if ( image == null ) {
            return null;
        } else {
            Iterator iter = iterator();
            Set removed = null;

            while( iter.hasNext() ) {
                AttributedImage ai = (AttributedImage)iter.next();

                if ( image.equals(ai.getImage()) ) {
                    iter.remove();
                    if(removed == null) {
                        removed = new HashSet();
                    }
                    removed.add(ai);
                }
            }

            return (Set)removed;
        }
    }

    /* -- CollectionImage methods: ensure elements are AttributedImages. -- */

    /**
     * Adds the specified object to this <code>Collection</code>.  This
     * method overrides the superclass method in order to perform a
     * type check on the object being added.
     *
     * @throws IllegalArgumentException if <code>o</code> is <code>null</code>
     *         or is not an <code>AttributedImage</code>.
     *
     * @return <code>true</code> if and only if the parameter is added to the
     *         <code>Collection</code>.
     */
    public boolean add(Object o) {

        if ( o == null || !(o instanceof AttributedImage) ) {
            throw new IllegalArgumentException(JaiI18N.getString("AttributedImageCollection1"));
        }

        // don't add an object that's there already
        if(imageCollection.contains(o)) {
            return false;
        }

        return imageCollection.add(o);
    }

    /**
     * Adds to this <code>Collection</code> all elements in the specified
     * <code>Collection</code> which are <code>AttributedImage</code>s.
     *
     * @return <code>true</code> if this <code>Collection</code> changed
     * as a result of the call.
     */
    public boolean addAll(Collection c) {
        if ( c == null ) return false;

        // iterate over collection
        Iterator iter = c.iterator();
        boolean flag  = false;

        while( iter.hasNext() ) {
            Object o = iter.next();

            // only add AttributedImages which have not yet been added
            if ( o instanceof AttributedImage ) {
                if( !imageCollection.contains(o) &&
                    imageCollection.add(o) ) {
                    flag = true;   // one shot switch
                }
            }
        }

        return flag;
    }

    /**
     *  Returns the first attributed image found in the collection
     *  that contains the planar image argument.  If the parameter is
     *  null, null will be returned.
     */
    public AttributedImage getAttributedImage(PlanarImage image) {

        if ( image == null ) {
            return null;
        } else {
            Iterator iter = iterator();

            while( iter.hasNext() ) {
                AttributedImage ai = (AttributedImage)iter.next();

                if ( image.equals(ai.getImage()) ) {
                    return ai;
                }
            }
        }

        return null;
    }

    /**
     *  Returns the first attributed image found in the collection
     *  that contains the attribute.  If the parameter is
     *  null, null will be returned.
     */
    public AttributedImage getAttributedImage(Object attribute) {

        if ( attribute == null ) {
            return null;
        } else {
            Iterator iter = iterator();

            while( iter.hasNext() ) {
                AttributedImage ai = (AttributedImage)iter.next();

                if ( attribute.equals(ai.getAttribute()) ) {
                    return ai;
                }
            }
        }

        return null;
    }
}
