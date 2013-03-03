/*
 * $RCSfile: PatternOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:39 $
 * $State: Exp $
 */ 
package com.lightcrafts.media.jai.opimage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.SourcelessOpImage;

/**
 * An OpImage class to generate a repeating pattern of pixels.
 *
 * <p> PatternOpImage defines an image consisting of a repeated
 * pattern.  The pattern is stored internally as a Raster, and
 * translated versions of the master tile (sharing the same
 * DataBuffer) are returned by computeTile().
 *
 */
// public since ../test/OpImageTester.java uses it
public class PatternOpImage extends SourcelessOpImage {

    /** The master tile (0, 0) containing the pattern. */
    protected Raster pattern;

    /** Set up image layout. */
    private static ImageLayout layoutHelper(Raster pattern,
                                            ColorModel colorModel) {
        return new ImageLayout(pattern.getMinX(), pattern.getMinY(),
                               pattern.getWidth(), pattern.getHeight(),
                               pattern.getSampleModel(), colorModel);
    }

    /**
     * Constructs a PatternOpImage from a Raster.
     *
     * @param pattern The Raster pattern to be repeated.
     * @param colorModel The output image ColorModel.
     * @param width The output image width.
     * @param height The output image height.
     */
    public PatternOpImage(Raster pattern,
                          ColorModel colorModel,
                          int minX, int minY,
                          int width, int height) {
        super(layoutHelper(pattern, colorModel),
              null,
              pattern.getSampleModel(),
              minX, minY, width, height);

        this.pattern = pattern;
    }

    public Raster getTile(int tileX, int tileY) {
        return computeTile(tileX,tileY);
    }

    /**
     * Returns a suitably translated version of the pattern tile
     * for reading.
     *
     * @param tileX the X index of the tile
     * @param tileY the Y index of the tile
     */
    public Raster computeTile(int tileX, int tileY) {
        return pattern.createChild(tileGridXOffset,
                                   tileGridYOffset,
                                   tileWidth, tileHeight,
                                   tileXToX(tileX),
                                   tileYToY(tileY),
                                   null);
    }
}
