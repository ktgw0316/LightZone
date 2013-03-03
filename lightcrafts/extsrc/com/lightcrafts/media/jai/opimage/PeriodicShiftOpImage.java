/*
 * $RCSfile: PeriodicShiftOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:40 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.OpImage;
import java.util.Map;
import com.lightcrafts.media.jai.util.JDKWorkarounds;

/**
 * The OpImage implementation of the "PeriodicShift" operation as described in
 * com.lightcrafts.mediax.jai.operator.PeriodicShiftDescriptor.
 *
 * <p> The layout the extended shifted image is copied from its source if
 * the layout parameter is null.
 *
 * @see com.lightcrafts.mediax.jai.operator.PeriodicShiftDescriptor
 * @see com.lightcrafts.mediax.jai.ImageLayout
 * @see com.lightcrafts.mediax.jai.OpImage
 *
 * @since EA4
 */
final class PeriodicShiftOpImage extends OpImage {

    /** The horizontal translation in pixels for each translated image. */
    private int[] xTrans;

    /** The vertical translation in pixels for each translated image. */
    private int[] yTrans;

    /** The source image translated in four different directions. */
    private TranslateIntOpImage[] images;

    /** The bounds of each of the four translated images. */
    private Rectangle[] bounds;

    /**
     * Creates a OpImage to return the tiles of a periodic extension.
     *
     * @param source a RenderedImage.
     * @param layout an ImageLayout optionally containing the tile grid
     *        layout, SampleModel, and ColorModel, or null.
     * @param shiftX the number of pixels of horizontal translation.
     * @param shiftY the number of pixels of vertical translation.
     */
    public PeriodicShiftOpImage(RenderedImage source,
                                Map config,
                                ImageLayout layout,
                                int shiftX,
                                int shiftY) {
        super(vectorize(source),
              layout == null ? new ImageLayout() : (ImageLayout)layout.clone(),
              config,
              false);

        // Calculate the four translation factors.
        xTrans =
            new int[] {-shiftX, -shiftX, width - shiftX, width - shiftX};
        yTrans =
            new int[] {-shiftY, height - shiftY, -shiftY, height - shiftY};

        // Translate the source image in four separate directions.
        images = new TranslateIntOpImage[4];
        for (int i = 0; i < 4; i++) {
            images[i] = new TranslateIntOpImage(source, null, xTrans[i], yTrans[i]);
        }

        // Compute the intersection of the translated sources with the
        // destination bounds.
        Rectangle destBounds = getBounds();
        bounds = new Rectangle[4];
        for (int i = 0; i < 4; i++) {
            bounds[i] = destBounds.intersection(images[i].getBounds());
        }
    }
                
    /** 
     * Computes a tile of the destination by copying the data which
     * overlaps the tile in the four translated source images.
     *
     * @param tileX the X index of the tile.
     * @param tileY the Y index of the tile.
     */
    public Raster computeTile(int tileX, int tileY) {
        // Create a new WritableRaster to represent this tile.
        Point org = new Point(tileXToX(tileX), tileYToY(tileY));
        WritableRaster dest = createWritableRaster(sampleModel, org);

        // Clip output rectangle to image bounds.
        Rectangle rect = new Rectangle(org.x, org.y,
                                       sampleModel.getWidth(),
                                       sampleModel.getHeight());
        Rectangle destRect = rect.intersection(getBounds());

        // Fill the destination raster.
        for (int i = 0; i < 4; i++) {
            // Calculate the overlap with the current translated source.
            Rectangle overlap = destRect.intersection(bounds[i]);

            // If the overlap is non-empty, copy the data within it.
            if (!overlap.isEmpty()) {
                //dest.setRect(images[i].getData(overlap));
                JDKWorkarounds.setRect(dest, images[i].getData(overlap));
            }
        }

        return dest; 
    }

    /**
     * Returns a conservative estimate of the destination region that
     * can potentially be affected by the pixels of a rectangle of a
     * given source.
     *
     * @param sourceRect the Rectangle in source coordinates.
     * @param sourceIndex the index of the source image.
     * @return a Rectangle indicating the potentially affected
     *         destination region.  or null if the region is unknown.
     * @throws IllegalArgumentException if the source index is
     *         negative or greater than that of the last source.
     * @throws IllegalArgumentException if sourceRect is null.
     */
    public Rectangle mapSourceRect(Rectangle sourceRect,
                                   int sourceIndex) {

        if ( sourceRect == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sourceIndex < 0 || sourceIndex >= getNumSources()) {
            throw new IllegalArgumentException(JaiI18N.getString("PeriodicShiftOpImage0"));
        }

        Rectangle destRect = null;
        for (int i = 0; i < 4; i++) {
            Rectangle srcRect = sourceRect;
            srcRect.translate(xTrans[i], yTrans[i]);
            Rectangle overlap = srcRect.intersection(getBounds());
            if (!overlap.isEmpty()) {
                destRect = destRect == null ?
                    overlap : destRect.union(overlap);
            }
        }

        return destRect;
    }
    
    /**
     * Returns a conservative estimate of the region of a specified
     * source that is required in order to compute the pixels of a
     * given destination rectangle.
     *
     * @param destRect the Rectangle in destination coordinates.
     * @param sourceIndex the index of the source image.
     * @return a Rectangle indicating the required source region.
     * @throws IllegalArgumentException if the source index is
     *         negative or greater than that of the last source.
     * @throws IllegalArgumentException if destRect is null.
     */
    public Rectangle mapDestRect(Rectangle destRect,
                                 int sourceIndex) {

        if ( destRect == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sourceIndex < 0 || sourceIndex >= getNumSources()) {
            throw new IllegalArgumentException(JaiI18N.getString("PeriodicShiftOpImage0"));
        }

        Rectangle sourceRect = null;
        for (int i = 0; i < 4; i++) {
            Rectangle overlap = destRect.intersection(bounds[i]);
            if (!overlap.isEmpty()) {
                overlap.translate(-xTrans[i], -yTrans[i]);
                sourceRect = sourceRect == null ?
                    overlap : sourceRect.union(overlap);
            }
        }

        return sourceRect;
    }
}
