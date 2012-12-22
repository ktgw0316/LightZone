/*
 * $RCSfile: CropOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:21 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PointOpImage;

/**
 * An OpImage to crop an image by a rectangular region.
 *
 * <p> Tiles that are completely inside or intersect the cropped region
 * (this image's bounds) are simply forwarded from the source.  Do NOT
 * create a child raster even if a tile is not contained in this image's
 * bounds.  Tiles that are outside the croppped region result in a
 * <code>null</code> return.
 *
 * <p> This operator maintains the source image's tile grid setting and
 * sample and color models.
 *
 *
 * @since EA4
 */
final class CropOpImage extends PointOpImage {

    private static ImageLayout layoutHelper(RenderedImage source,
                                            float originX,
                                            float originY,
                                            float width,
                                            float height) {
        // Get minimum bounding box.
        Rectangle bounds = new Rectangle2D.Float(
                           originX, originY, width, height).getBounds();

        return new ImageLayout(bounds.x,
                               bounds.y,
                               bounds.width,
                               bounds.height,
                               source.getTileGridXOffset(),
                               source.getTileGridYOffset(),
                               source.getTileWidth(),
                               source.getTileHeight(),
                               source.getSampleModel(),
                               source.getColorModel());

    }

    /**
     * Construct an CropOpImage.
     *
     * @param source a RenderedImage.
     * @param originX the new cropping rectangle x origin.
     * @param originY the new cropping rectangle y origin.
     * @param width the width of the cropping rectangle.
     * @param height the width of the cropping rectangle.
     */
    public CropOpImage(RenderedImage source,
                       float originX,
                       float originY,
                       float width,
                       float height) {
        super(source,
              layoutHelper(source, originX, originY, width, height),
              null,
              false);
    }

    /**
     * Returns <code>false</code> as <code>computeTile()</code> invocations
     * are forwarded to the <code>RenderedImage</code> source and are
     * therefore not unique objects in the global sense.
     */
    public boolean computesUniqueTiles() {
        return false;
    }

    /**
     * Override computeTile() simply to invoke getTile().  Required
     * so that the TileScheduler may invoke computeTile().
     */
    public Raster computeTile(int tileX, int tileY) {
        return getTile(tileX, tileY);
    }

    /**
     * Returns a tile.
     *
     * <p> For those tiles that are completely contained in or intersect
     * with this image's bounds (the cropped region), simply returns the
     * source tile.  Tiles that are outside this image's bound result in
     * <code>null</code> return.
     *
     * @param tileX The X index of the tile.
     * @param tileY The Y index of the tile.
     */
    public Raster getTile(int tileX, int tileY) {
        Raster tile = null;	// the requested tile, to be returned

        /* Make sure the requested tile is inside this image's boundary. */
        if (tileX >= getMinTileX() && tileX <= getMaxTileX() &&
            tileY >= getMinTileY() && tileY <= getMaxTileY()) {
            /*
             * Get the source tile.
             * Do NOT create a child Raster even if the tile is not contained
             * but merely intersects this image's bounds.
             */
            tile = getSourceImage(0).getTile(tileX, tileY);
        }

        return tile;
    }
}
