/*
 * $RCSfile: TranslateIntOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:46 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.Map;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OpImage;

/**
 * An OpImage to translate an image by in integral number of pixels.
 *
 * <p> The translation is accomplished by simply shifting the tile
 * grid.
 */
public final class TranslateIntOpImage extends OpImage {

    private int transX;
    private int transY;

    private static ImageLayout layoutHelper(RenderedImage source,
                                            int transX,
                                            int transY) {

        ImageLayout layout =
            new ImageLayout(source.getMinX() + transX,
                            source.getMinY() + transY,
                            source.getWidth(),
                            source.getHeight(),
                            source.getTileGridXOffset() + transX,
                            source.getTileGridYOffset() + transY,
                            source.getTileWidth(),
                            source.getTileHeight(),
                            source.getSampleModel(),
                            source.getColorModel());
        return layout;
    }

    // Since this operation does not touch the data at all, we do not need
    // to expand the IndexColorModel
    private static Map configHelper(Map configuration) {
	
	Map config;
	if (configuration == null) {

	    config = new RenderingHints(JAI.KEY_REPLACE_INDEX_COLOR_MODEL,
					Boolean.FALSE);
	} else {

	    config = configuration;

	    if (!(config.containsKey(JAI.KEY_REPLACE_INDEX_COLOR_MODEL))) {

		RenderingHints hints = (RenderingHints)configuration;
		config = (RenderingHints)hints.clone();
		config.put(JAI.KEY_REPLACE_INDEX_COLOR_MODEL, Boolean.FALSE);
		config.remove(JAI.KEY_TILE_CACHE);
		
	    } else if (config.containsKey(JAI.KEY_TILE_CACHE)) {

		RenderingHints hints = (RenderingHints)configuration;
		config = (RenderingHints)hints.clone();
		config.remove(JAI.KEY_TILE_CACHE);
	    }
	}

	return config;
    }

    /**
     * Construct an TranslateIntOpImage.
     *
     * @param source a RenderedImage.
     * @param config Configurable attributes of the image including
     *        configuration variables indexed by
     *        <code>RenderingHints.Key</code>s and image properties indexed
     *        by <code>String</code>s or <code>CaselessStringKey</code>s.
     *        This is simply forwarded to the superclass constructor.
     * @param transX the number of pixels of horizontal translation.
     * @param transY the number of pixels of vertical translation.
     */
    public TranslateIntOpImage(RenderedImage source,
			       Map config,
                               int transX,
                               int transY) {
        super(vectorize(source),
              layoutHelper(source, transX, transY),
              configHelper(config),
              false);
        this.transX = transX;
        this.transY = transY;
    }

    /**
     * Returns <code>false</code> as <code>computeTile()</code> invocations
     * return child <code>Raster</code>s of the <code>RenderedImage</code>
     * source and are therefore not unique objects in the global sense.
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
     * Get a tile.
     *
     * @param tileX The X index of the tile.
     * @param tileY The Y index of the tile.
     */
    public Raster getTile(int tileX, int tileY) {
        Raster tile = getSource(0).getTile(tileX, tileY);

	if (tile == null)
	    return null;

	return tile.createTranslatedChild(tileXToX(tileX), tileYToY(tileY));
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
            throw new IllegalArgumentException(JaiI18N.getString("TranslateIntOpImage0"));
        }

        Rectangle r = new Rectangle(sourceRect);
        r.translate(transX, transY);
        return r;
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
            throw new IllegalArgumentException(JaiI18N.getString("TranslateIntOpImage0"));
        }

        Rectangle r = new Rectangle(destRect);
        r.translate(-transX, -transY);
        return r;
    }
}
