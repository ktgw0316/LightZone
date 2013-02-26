/*
 * $RCSfile: RenderedImageAdapter.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:20 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashSet;
import java.util.Set;
import com.lightcrafts.mediax.jai.util.CaselessStringKey;

/**
 * A <code>PlanarImage</code> wrapper for a non-writable
 * <code>RenderedImage</code>.  The tile layout, sample model, and so forth
 * are preserved.  Calls to <code>getTile()</code>, <code>getData()</code>,
 * and <code>copyData()</code> are forwarded to the image being adapted.
 *
 * <p> The set of properties available on the image will be a combination of
 * those defined locally via <code>setProperty()</code> and those defined
 * on the source image with the local properties taking precedence.  No
 * <code>PropertySourceChangeEvent</code> will be generated as a result of
 * changes to the property set of the source image.
 *
 * <p> From JAI's point of view, this image is a <code>PlanarImage</code> of
 * unknown type, with no sources.  The source image is assumed to be
 * immutable.  If the <code>RenderedImage</code> source implements
 * <code>WritableRenderedImage</code>, a
 * <code>WritableRenderedImageAdapter</code> should be used.
 *
 * <p> The methods are marked 'final' in order to allow dynamic inlining to
 * take place.  This should eliminate any performance penalty associated with
 * the use of an adapter class.
 *
 * <p> Since the methods of this class all derive from
 * <code>PlanarImage</code>, they are not commented in detail.
 *
 * @see PlanarImage
 * @see WritableRenderedImageAdapter
 * @see java.awt.image.RenderedImage
 * @see java.awt.image.WritableRenderedImage
 */
public class RenderedImageAdapter extends PlanarImage {

    /** The RenderedImage being adapted. */
    protected RenderedImage theImage;

    /** Tile index bounds. */
    private Rectangle tileIndexBounds;
    
    /**
     * Merge the <code>String</code>s in the two arrays with the local names
     * taking precedence.  Comparison is performed independent of case.  Both
     * parameters may be <code>null</code>.
     *
     * @return The merged name arrays or <code>null</code>.
     */
    static String[] mergePropertyNames(String[] localNames,
                                       String[] sourceNames) {
        // Set the output names to the other array if one array is null
        // or zero-length.
        String[] names = null;
        if(localNames == null || localNames.length == 0) {
            names = sourceNames;
        } else if(sourceNames == null || sourceNames.length == 0) {
            names = localNames;
        } else {
            // Merge the name arrays.

            // Allocate a Set.
            Set nameSet = new HashSet((localNames.length+sourceNames.length)/2);

            // Add source names first as they have lower priority.
            int numSourceNames = sourceNames.length;
            for(int i = 0; i < numSourceNames; i++) {
                nameSet.add(new CaselessStringKey(sourceNames[i]));
            }

            // Add local names which will "bump" duplicate source names.
            int numLocalNames = localNames.length;
            for(int i = 0; i < numLocalNames; i++) {
                nameSet.add(new CaselessStringKey(localNames[i]));
            }

            // Convert result to an array of Strings.
            int numNames = nameSet.size();
            CaselessStringKey[] caselessNames = new CaselessStringKey[numNames];
            nameSet.toArray(caselessNames);
            names = new String[numNames];
            for(int i = 0; i < numNames; i++) {
                names[i] = caselessNames[i].getName();
            }
        }

        // Set return value to null if zero-length.
        if(names != null && names.length == 0) {
            names = null;
        }

        return names;
    }

    /**
     * Constructs a RenderedImageAdapter.
     *
     * @param im a RenderedImage to be `wrapped' as a PlanarImage.
     * @throws IllegalArgumentException if <code>im</code> is
     *         <code>null</code>.
     */
    public RenderedImageAdapter(RenderedImage im) {
        super(im != null ? new ImageLayout(im) : null, null, null);
	if (im == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

        theImage = im;
        
        tileIndexBounds = new Rectangle(theImage.getMinTileX(),
                                        theImage.getMinTileY(),
                                        theImage.getNumXTiles(),
                                        theImage.getNumYTiles());
    }
    
    /**
     * Returns the reference to the external <code>RenderedImage</code>
     * originally supplied to the constructor.
     *
     * @since JAI 1.1.2
     */
    public final RenderedImage getWrappedImage() {
        return theImage;
    }

    /**
     * Forwards call to the true source unless the specified tile indices
     * refer to a tile which does not overlap the image bounds in which
     * case <code>null</code> is returned.
     */
    public final Raster getTile(int x, int y) {
        return tileIndexBounds.contains(x, y) ? theImage.getTile(x, y) : null;
    }

    /** Forwards call to the true source. */
    public final Raster getData() {
        return theImage.getData();
    }

    /** Forwards call to the true source. */
    public final Raster getData(Rectangle rect) {
        return theImage.getData(rect);
    }

    /** Forwards call to the true source. */
    public final WritableRaster copyData(WritableRaster raster) {
        return theImage.copyData(raster);
    }

    /**
     * Retrieves a list of property names recognized by this image.
     * The locally defined property names are combined with those derived
     * from the true source.
     */
    public final String[] getPropertyNames() {
        return mergePropertyNames(super.getPropertyNames(),
                                  theImage.getPropertyNames());
    }

    /**
     * Retrieves the property from those set locally on the image or,
     * if the property is not available locally, the call is forwarded to
     * the true source.
     * @exception IllegalArgumentException if <code>name</code>
     *                                     is <code>null</code>.
     */
    public final Object getProperty(String name) {
        // Retrieve the property from the local cache.
        Object property = super.getProperty(name);

        // If it is still undefined, forward the call.
        if(property == java.awt.Image.UndefinedProperty) {
            property = theImage.getProperty(name);
        }

        return property;
    }

    /**
     * Returns the class expected to be returned by a request for
     * the property with the specified name.  If this information
     * is unavailable, <code>null</code> will be returned.
     *
     * @exception IllegalArgumentException if <code>name</code>
     *                                     is <code>null</code>.
     *
     * @return The <code>Class</code> expected to be return by a
     *         request for the value of this property or <code>null</code>.
     *
     * @since JAI 1.1
     */
    public final Class getPropertyClass(String name) {
        // Get the class if the property is local.
        Class propClass = super.getPropertyClass(name);

        // If not local ...
        if(propClass == null) {
            // Get the property value.
            Object propValue = getProperty(name);

            if(propValue != java.awt.Image.UndefinedProperty) {
                // If the property is defined, get the class.
                propClass = propValue.getClass();
            }
        }

        return propClass;
    }
}
