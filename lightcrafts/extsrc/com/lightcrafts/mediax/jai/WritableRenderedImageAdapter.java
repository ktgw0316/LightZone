/*
 * $RCSfile: WritableRenderedImageAdapter.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:25 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.Point;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.awt.image.TileObserver;

/**
 * A <code>PlanarImage</code> wrapper for a
 * <code>WritableRenderedImage</code>.  The tile layout, sample model,
 * and so forth are preserved.  Calls to <code>getTile()</code> and so
 * forth are forwarded.
 *
 * <p> From JAI's point of view, this image is a
 * <code>PlanarImage</code> of unknown type, with no sources, and
 * additionally an implementer of the
 * <code>WritableRenderedImage</code> interface.  The image's pixel
 * data appear to be variable.
 *
 * <p> The class and all its methods are marked <code>final</code> in
 * order to allow dynamic inlining to take place.  This should
 * eliminate any performance penalty associated with the use of an
 * adapter class.
 *
 * @see PlanarImage
 * @see RenderedImageAdapter
 * @see java.awt.image.RenderedImage
 * @see java.awt.image.WritableRenderedImage
 *
 */
public final class WritableRenderedImageAdapter extends RenderedImageAdapter 
    implements WritableRenderedImage {

    /** The WritableRenderedImage being adapted. */
    private WritableRenderedImage theWritableImage;

    /**
     * Constructs a <code>WritableRenderedImageAdapter</code>.
     *
     * @param im A <code>WritableRenderedImage</code> to be `wrapped'
     * as a <code>PlanarImage</code>.
     * @throws <code>IllegalArgumentException</code> if <code>im</code> is
     *         <code>null</code>.
     */
    public WritableRenderedImageAdapter(WritableRenderedImage im) {
        super(im);
        theWritableImage = im;
    }

    /**
     * Adds an observer.  If the observer is already present,
     * it will receive multiple notifications.
     *
     * @param tileObserver The <code>TileObserver</code> to be added.
     * @throws <code>IllegalArgumentException</code> if
     *         <code>tileObserver</code> is <code>null</code>.
     */
    public final void addTileObserver(TileObserver tileObserver) {
	if (tileObserver == null) {
	    throw new IllegalArgumentException(JaiI18N.getString("WritableRenderedImageAdapter0"));
	}
        theWritableImage.addTileObserver(tileObserver);
    }
 
    /**
     * Removes an observer.  If the observer was not registered,
     * nothing happens.  If the observer was registered for multiple
     * notifications, it will now be registered for one fewer.
     *
     * @param tileObserver The <code>TileObserver</code> to be removed.
     * @throws <code>IllegalArgumentException</code> if
     *         <code>tileObserver</code> is <code>null</code>.
     */
    public final void removeTileObserver(TileObserver tileObserver) {
	if (tileObserver == null) {
	    throw new IllegalArgumentException(JaiI18N.getString("WritableRenderedImageAdapter0"));
	}
        theWritableImage.removeTileObserver(tileObserver);
    }
 
    /**
     * Checks out a tile for writing.  
     * 
     * <p> The <code>WritableRenderedImage</code> is responsible for
     * notifying all of its <code>TileObservers</code> when a tile
     * goes from having no writers to having one writer.
     *
     * @param tileX The X index of the tile.
     * @param tileY The Y index of the tile.
     * @return The tile as a <code>WritableRaster</code>.
     */
    public final WritableRaster getWritableTile(int tileX, int tileY) {
        return theWritableImage.getWritableTile(tileX, tileY);
    }
    
    /**
     * Relinquishes the right to write to a tile.  If the caller
     * continues to write to the tile, the results are undefined.
     * Calls to this method should only appear in matching pairs with
     * calls to <code>getWritableTile()</code>; any other use will
     * lead to undefined results.
     *
     * <p> The <code>WritableRenderedImage</code> is responsible for
     * notifying all of its <code>TileObserver</code>s when a tile
     * goes from having one writer to having no writers.
     *
     * @param tileX The X index of the tile.
     * @param tileY The Y index of the tile.
     */
    public final void releaseWritableTile(int tileX, int tileY) {
        theWritableImage.releaseWritableTile(tileX, tileY);
    }
    
    /**
     * Returns whether a tile is currently checked out for writing.
     *
     * @param tileX The X index of the tile.
     * @param tileY The Y index of the tile.
     *
     * @return <code>true</code> if the tile currently has writers.
     */
    public final boolean isTileWritable(int tileX, int tileY) {
        return theWritableImage.isTileWritable(tileX, tileY);
    }
    
    /**
     * Returns an array of <code>Point</code> objects indicating which tiles
     * are checked out for writing.
     *
     * @return an array of <code>Point</code>s or <code>null</code> if no
     * tiles are checked out for writing.
     */
    public final Point[] getWritableTileIndices() {
        return theWritableImage.getWritableTileIndices();
    }
    
    /**
     * Returns whether any tile is checked out for writing.
     * Semantically equivalent to (getWritableTiles().size() != 0).
     *
     * @return <code>true</code> if any tile currently has writers.
     */
    public final boolean hasTileWriters() {
        return theWritableImage.hasTileWriters();
    }
    
    /**
     * Sets a rectangular region of the image to the contents of
     * <code>raster</code>.
     *
     * @param raster A <code>Raster</code>.
     * @throws <code>IllegalArgumentException</code> if <code>raster</code> is
     *         <code>null</code>.
     */
    public final void setData(Raster raster) {
	if (raster == null) {
	    throw new IllegalArgumentException(JaiI18N.getString("WritableRenderedImageAdapter1"));
	}
        theWritableImage.setData(raster);
    }
}
