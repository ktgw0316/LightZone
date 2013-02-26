/*
 * $RCSfile: RenderedImageList.java,v $
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
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

/**
 * A <code>CollectionImage</code> which is also a
 * <code>RenderedImage</code>.  The underlying <code>Collection</code>
 * in this case is required to be a <code>List</code> containing only
 * <code>RenderedImage</code>s.
 *
 * <p> Instances of this class may be returned from either a
 * <code>RenderedImageFactory</code> or from a
 * <code>CollectionImageFactory</code>.  This class would be
 * particularly useful for implementing operations the result of which
 * includes a single primary image and one or more secondary or
 * dependant images the data of which are less likely to be requested.
 *
 * <p> Invocations of <code>RenderedImage</code> methods on an instance
 * of this class will be forwarded to the first <code>RenderedImage</code>
 * in the underlying <code>List</code>, i.e., the image at index zero.
 * This should be the index assigned to the primary image in the case
 * alluded to above.  If there are no images in the <code>List</code>
 * when a <code>RenderedImage</code> method is invoked an
 * <code>IllegalStateException</code> will be thrown.
 *
 * <p> One example of the use of this class is in generating a classmap
 * image using a classification algorithm. A by-product image of such an
 * operation is often an error image wherein the value of each pixel is
 * some measure of the classification error at that pixel. In this case
 * the classmap image would be stored at index zero in the internal list
 * and the error image at the unity index. The error image would be
 * an <code>OpImage</code> that has the classmap image as its source.
 * The result is that a reference to the error image is always available
 * with the classmap image but the computation of the error image pixel
 * values may be deferred until such time as the data are needed, if ever.
 *
 * <p> Methods defined in the <code>RenderedImage</code> and <code>List</code>
 * interfaces are not all commented in detail.  The <code>List</code> methods
 * merely forward the call in most cases directly to the underlying
 * <code>List</code>; as previously stated, <code>RenderedImage</code> method
 * invocations are forwarded to the <code>RenderedImage</code> at position
 * zero in the <code>List</code>.
 *
 * @since JAI 1.1
 * @see CollectionImage
 * @see java.awt.image.RenderedImage
 * @see java.util.List
 */
public class RenderedImageList extends CollectionImage
    implements List, RenderedImage, Serializable {

    /**
     * Creates an empty <code>RenderedImageList</code>.
     */
    protected RenderedImageList() {
        super();
    }

    /**
     * Creates a <code>RenderedImageList</code> from the supplied
     * <code>List</code>.
     *
     * @throws <code>IllegalArgumentException</code> if any objects in the
     *	       <code>List</code> are not <code>RenderedImage</code>s.
     * @throws <code>IllegalArgumentException</code> if the <code>List</code>
     *	       is empty.
     * @throws <code>IllegalArgumentException</code> if the <code>List</code>
     *	       parameter is <code>null</code>. 
     */
    public RenderedImageList(List renderedImageList) {
        super();

        // separate throws, for better error reporting
        if ( renderedImageList == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("RenderedImageList0"));
        }

        if ( renderedImageList.isEmpty() ) {
            throw new IllegalArgumentException(JaiI18N.getString("RenderedImageList1"));
        }

        Iterator iter = renderedImageList.iterator();
        imageCollection = new Vector();

        while( iter.hasNext() ) {
            Object item = iter.next();

            if ( item instanceof RenderedImage ) {
                imageCollection.add(item);
            } else {
                throw new IllegalArgumentException(JaiI18N.getString("RenderedImageList2"));
            }
        }
    }

    /// --- RenderedImage methods. ---

    /**
     * Returns the image <code>Collection</code> as a <code>List</code>.
     */
    private List getList() {
        return (List)imageCollection;
    }

    /**
     * Returns the first image in the underlying list of
     * <code>RenderedImage</code>s.
     * The call is forwarded to the first image in the <code>List</code>.
     */
    public RenderedImage getPrimaryImage() {
        return (RenderedImage)getList().get(0);
    }

    /**
     * Returns the X coordinate of the leftmost column of the
     * primary image.
     * The call is forwarded to the first image in the <code>List</code>.
     */
    public int getMinX() {
        return ((RenderedImage) getList().get(0)).getMinX();
    }

    /**
     * Returns the X coordinate of the uppermost row of the primary image.
     * The call is forwarded to the first image in the <code>List</code>.
     */
    public int getMinY() {
        return ((RenderedImage) getList().get(0)).getMinY();
    }

    /**
     * Returns the width of the primary image.
     * The call is forwarded to the first image in the <code>List</code>.
     */
    public int getWidth() {
        return ((RenderedImage) getList().get(0)).getWidth();
    }

    /**
     * Returns the height of the primary image.
     * The call is forwarded to the first image in the <code>List</code>.
     */
    public int getHeight() {
        return ((RenderedImage) getList().get(0)).getHeight();
    }
    
    /**
     * Returns the width of a tile of the primary image.
     * The call is forwarded to the first image in the <code>List</code>.
     */
    public int getTileWidth() {
        return ((RenderedImage) getList().get(0)).getTileWidth();
    }

    /**
     * Returns the height of a tile of the primary image.
     * The call is forwarded to the first image in the <code>List</code>.
     */
    public int getTileHeight() {
        return ((RenderedImage) getList().get(0)).getTileHeight();
    }

    /** 
     * Returns the X coordinate of the upper-left pixel of tile (0, 0)
     * of the primary image.
     * The call is forwarded to the first image in the <code>List</code>.
     */
    public int getTileGridXOffset() {
        return ((RenderedImage) getList().get(0)).getTileGridXOffset();
    }

    /** 
     * Returns the Y coordinate of the upper-left pixel of tile (0, 0)
     * of the primary image.
     * The call is forwarded to the first image in the <code>List</code>.
     */
    public int getTileGridYOffset() {
        return ((RenderedImage) getList().get(0)).getTileGridYOffset();
    }

    /**
     * Returns the horizontal index of the leftmost column of tiles
     * of the primary image.
     * The call is forwarded to the first image in the <code>List</code>.
     */
    public int getMinTileX() {
        return ((RenderedImage) getList().get(0)).getMinTileX();
    }

    /**
     * Returns the number of tiles of the primary image along the
     * tile grid in the horizontal direction.
     * The call is forwarded to the first image in the <code>List</code>.
     */
    public int getNumXTiles() {
        return ((RenderedImage) getList().get(0)).getNumXTiles();
    }
    
    /**
     * Returns the vertical index of the uppermost row of tiles
     * of the primary image.
     * The call is forwarded to the first image in the <code>List</code>.
     */
    public int getMinTileY() {
        return ((RenderedImage) getList().get(0)).getMinTileY();
    }

    /**
     * Returns the number of tiles of the primary image along the
     * tile grid in the vertical direction.
     * The call is forwarded to the first image in the <code>List</code>.
     */
    public int getNumYTiles() {
        return ((RenderedImage) getList().get(0)).getNumYTiles();
    }

    /**
     * Returns the SampleModel of the primary image.
     * The call is forwarded to the first image in the <code>List</code>.
     */
    public SampleModel getSampleModel() {
        return ((RenderedImage) getList().get(0)).getSampleModel();
    }

    /**
     * Returns the ColorModel of the primary image.
     * The call is forwarded to the first image in the <code>List</code>.
     */
    public ColorModel getColorModel() {
        return ((RenderedImage) getList().get(0)).getColorModel();
    }

    /**
     * Gets a property from the property set of this image.  If the
     * property name is not recognized,
     * <code>java.awt.Image.UndefinedProperty</code> will be returned.
     * The call is forwarded to the first image in the <code>List</code>.
     *
     * @param name the name of the property to get, as a
     * <code>String</code>.
     * @return a reference to the property
     * <code>Object</code>, or the value
     * <code>java.awt.Image.UndefinedProperty.</code>
     * @throws IllegalArgumentException if <code>name</code> is
     *         <code>null</code>.
     */
    public Object getProperty(String name) {
        if ( name == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("RenderedImageList0"));
        }

        return ((RenderedImage) getList().get(0)).getProperty(name);
    }
    
    /**
     * Returns a list of the properties recognized by this image.  If
     * no properties are available, <code>null</code> will be
     * returned.
     * The call is forwarded to the first image in the <code>List</code>.
     *
     * @return an array of <code>String</code>s representing valid
     *         property names.
     */
    public String[] getPropertyNames() {
        return ((RenderedImage) getList().get(0)).getPropertyNames();
    }

    /**
     * Returns a <code>Vector</code> containing the image sources.
     * The call is forwarded to the first image in the <code>List</code>.
     */
    public Vector getSources() {
        return ((RenderedImage) getList().get(0)).getSources();
    }

    /** 
     * Returns tile (<code>tileX</code>, <code>tileY</code>) of the
     * primary image as a <code>Raster</code>.  Note that <code>tileX</code>
     * and <code>tileY</code> are indices into the tile array, not pixel
     * locations.
     * The call is forwarded to the first image in the <code>List</code>.
     *
     * @param tileX  The X index of the requested tile in the tile array.
     * @param tileY  The Y index of the requested tile in the tile array.
     */
    public Raster getTile(int tileX, int tileY) {
        return ((RenderedImage) getList().get(0)).getTile(tileX, tileY);
    }

    /**
     * Returns the entire primary image in a single <code>Raster</code>.
     * For images with multiple tiles this will require making a copy.
     * The returned <code>Raster</code> is semantically a copy.
     * The call is forwarded to the first image in the <code>List</code>.
     *
     * @return a <code>Raster</code> containing a copy of this image's data.
     */
    public Raster getData() {
        return ((RenderedImage) getList().get(0)).getData();
    }

    /**
     * Returns an arbitrary rectangular region of the primary image
     * in a <code>Raster</code>.  The returned <code>Raster</code> is
     * semantically a copy.
     * The call is forwarded to the first image in the <code>List</code>.
     *
     * @param bounds the region of the <code>RenderedImage</code> to be
     *        returned.
     */
    public Raster getData(Rectangle bounds) {
        return ((RenderedImage) getList().get(0)).getData(bounds);
    }

    /**
     * Copies an arbitrary rectangular region of the primary image
     * into a caller-supplied WritableRaster.  The region to be
     * computed is determined by clipping the bounds of the supplied
     * WritableRaster against the bounds of the image.  The supplied
     * WritableRaster must have a SampleModel that is compatible with
     * that of the image.
     *
     * <p> If the raster argument is null, the entire image will
     * be copied into a newly-created WritableRaster with a SampleModel
     * that is compatible with that of the image.
     * The call is forwarded to the first image in the <code>List</code>.
     *
     * @param dest a WritableRaster to hold the returned portion of
     *        the image.
     * @return a reference to the supplied WritableRaster, or to a 
     *         new WritableRaster if the supplied one was null.
     */
    public WritableRaster copyData(WritableRaster dest) {
        return ((RenderedImage) getList().get(0)).copyData(dest);
    }

    /// --- List methods. ---

    /**
     * Inserts the specified element at the specified position in this list.
     *
     * @throws    IllegalArgumentException if the specified element
     *            is not a <code>RenderedImage</code>.
     * @throws    IndexOutOfBoundsException if the index is out of range
     *            (index &lt; 0 || index &gt; size()).     
     */
    public void add(int index, Object element) {
        if ( element instanceof RenderedImage ) {
            if ( index >=0 && index <= imageCollection.size() ) {
                ((List)imageCollection).add(index, element);
            } else {
                throw new IndexOutOfBoundsException(JaiI18N.getString("RenderedImageList3"));
            }
        } else {
            throw new IllegalArgumentException(JaiI18N.getString("RenderedImageList2"));
        }
    }

    /**
     * Inserts into this <code>List</code> at the indicated position
     * all elements in the specified <code>Collection</code> which are
     * <code>RenderedImage</code>s.
     *
     * @return <code>true</code> if this <code>List</code> changed
     * as a result of the call.
     * @throws IndexOutOfBoundsException if the index is out of range
     *         (index &lt; 0 || index &gt; size()).
     */
    public boolean addAll(int index, Collection c) {
        // Add only elements of c which are RenderedImages.
        if ( index < 0 || index > imageCollection.size() ) {
            throw new IndexOutOfBoundsException(JaiI18N.getString("RenderedImageList3"));
        }

        // Only allow RenderedImages
        Vector temp = null;
        Iterator iter = c.iterator();

        while( iter.hasNext() ) {
            Object o = iter.next();

            if ( o instanceof RenderedImage ) {
                if ( temp == null ) {
                    temp = new Vector();
                }

                temp.add( o );
            }
        }

        return ((List)imageCollection).addAll(index, temp);
    }

    /**
     * @return the <code>RenderedImage</code> object at the
     * specified index.
     *
     * @param index index of element to return.
     * @return the element at the specified position in this list.
     *
     * @throws IndexOutOfBoundsException if the index is out of range (index
     *            &lt; 0 || index &gt;= size()).
     */
    public Object get(int index) {
        if ( index < 0 || index >= imageCollection.size() ) {
            throw new IndexOutOfBoundsException(JaiI18N.getString("RenderedImageList3"));
        }

        return ((List)imageCollection).get(index);
    }

    public int indexOf(Object o) {
        // Do not throw an <code>IllegalArgumentException</code> even
        // if o is not a RenderedImage.
        return ((List)imageCollection).indexOf(o);
    }

    public int lastIndexOf(Object o) {
        // Do not throw an <code>IllegalArgumentException</code> even
        // if o is not a RenderedImage.
        return ((List)imageCollection).lastIndexOf(o);
    }

    public ListIterator listIterator() {
        return ((List)imageCollection).listIterator();
    }

    public ListIterator listIterator(int index) {
        return ((List)imageCollection).listIterator(index);
    }

    public Object remove(int index) {
        return ((List)imageCollection).remove(index);
    }

    public Object set(int index, Object element) {
        if ( element instanceof RenderedImage ) {
            return ((List)imageCollection).set(index, element);
        }

        throw new IllegalArgumentException(JaiI18N.getString("RenderedImageList2"));
    }

    public List subList(int fromIndex, int toIndex) {
        return ((List)imageCollection).subList(fromIndex, toIndex);
    }

    // --- Collection methods: overridden to require RenderedImages. ---

    /**
     * Adds the specified object to this <code>List</code>.
     *
     * @throws IllegalArgumentException if <code>o</code> is <code>null</code>
     *         or is not an <code>RenderedImage</code>.
     *
     * @return <code>true</code> if and only if the parameter is added to the
     *         <code>List</code>.
     */
    public boolean add(Object o) {
        if ( o == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("RenderedImageList0"));
        }

        if ( o instanceof RenderedImage ) {
            imageCollection.add(o);
            return true;
        } else {
            throw new IllegalArgumentException(JaiI18N.getString("RenderedImageList2"));
        }
    }

    /**
     * Adds to this <code>List</code> all elements in the specified
     * <code>Collection</code> which are <code>RenderedImage</code>s.
     *
     * @return <code>true</code> if this <code>List</code> changed
     * as a result of the call.
     */
    public boolean addAll(Collection c) {
        Iterator iter  = c.iterator();
        boolean status = false;

        while( iter.hasNext() ) {
            Object o = iter.next();

            if ( o instanceof RenderedImage ) {
                imageCollection.add(o);
                status = true;
            }
        }

        return status;
    }
}
