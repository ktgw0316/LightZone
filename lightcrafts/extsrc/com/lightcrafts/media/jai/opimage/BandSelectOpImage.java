/*
 * $RCSfile: BandSelectOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:15 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PointOpImage;
import com.lightcrafts.media.jai.util.JDKWorkarounds;

/**
 * An <code>OpImage</code> implementing the "BandSelect" operation.
 *
 * <p>This <code>OpImage</code> copies the specified bands of the source
 * image to the destination image in the order that is specified.
 *
 * @see com.lightcrafts.mediax.jai.operator.BandSelectDescriptor
 * @see BandSelectCRIF
 *
 *
 * @since EA2
 */
final class BandSelectOpImage extends PointOpImage {

    // Set if the source has a SinglePixelPackedSampleModel and
    // bandIndices.length < 3.
    private boolean areDataCopied;

    private int[] bandIndices;

    private static ImageLayout layoutHelper(ImageLayout layout,
                                            RenderedImage source,
                                            int[] bandIndices) {
        ImageLayout il = layout == null ?
            new ImageLayout() : (ImageLayout)layout.clone();

        // Create a sub-banded SampleModel.
        SampleModel sourceSM = source.getSampleModel();
        int numBands = bandIndices.length;

        // The only ColorModel compatible with a SinglePixelPackedSampleModel
        // in the J2SE is a DirectColorModel which is by definition of
        // ColorSpace.TYPE_RGB. Therefore if there are fewer than 3 bands
        // a data copy is obligatory if a ColorModel will be possible.
        SampleModel sm = null;
        if(sourceSM instanceof SinglePixelPackedSampleModel && numBands < 3) {
            sm = new PixelInterleavedSampleModel(
                     DataBuffer.TYPE_BYTE,
                     sourceSM.getWidth(), sourceSM.getHeight(),
                     numBands, sourceSM.getWidth()*numBands,
                     numBands == 1 ? new int[] {0} : new int[] {0, 1});
        } else {
            sm = sourceSM.createSubsetSampleModel(bandIndices);
        }
        il.setSampleModel(sm);

        // Clear the ColorModel mask if needed.
        ColorModel cm = il.getColorModel(null);
        if(cm != null &&
           !JDKWorkarounds.areCompatibleDataModels(sm, cm)) {
            // Clear the mask bit if incompatible.
            il.unsetValid(ImageLayout.COLOR_MODEL_MASK);
        }

        // Force the tile grid to be identical to that of the source.
        il.setTileGridXOffset(source.getTileGridXOffset());
        il.setTileGridYOffset(source.getTileGridYOffset());
        il.setTileWidth(source.getTileWidth());
        il.setTileHeight(source.getTileHeight());

        return il;
    }

    /**
     * Constructor.
     *
     * @param source       The source image.
     * @param layout       The destination image layout.
     * @param bandIndices  The selected band indices of the source.
     *                     The number of bands of the destination is
     *                     determined by <code>bandIndices.length</code>.
     */
    public BandSelectOpImage(RenderedImage source,
                             Map config,
                             ImageLayout layout,
                             int[] bandIndices) {
        super(vectorize(source),
              layoutHelper(layout, source, bandIndices),
              config, true);

        this.areDataCopied =
            source.getSampleModel() instanceof SinglePixelPackedSampleModel &&
            bandIndices.length < 3;
        this.bandIndices = (int[])bandIndices.clone();
    }

    public boolean computesUniqueTiles() {
        return areDataCopied;
    }

    public Raster computeTile(int tileX, int tileY) {
        Raster tile = getSourceImage(0).getTile(tileX, tileY);

        if(areDataCopied) {
            // Copy the data as there is no concrete ColorModel for
            // a SinglePixelPackedSampleModel with numBands < 3.
            tile = tile.createChild(tile.getMinX(), tile.getMinY(),
                                    tile.getWidth(), tile.getHeight(),
                                    tile.getMinX(), tile.getMinY(),
                                    bandIndices);
            WritableRaster raster = createTile(tileX, tileY);
            raster.setRect(tile);

            return raster;
        } else {
            // Simply return a child of the corresponding source tile.
            return tile.createChild(tile.getMinX(), tile.getMinY(),
                                    tile.getWidth(), tile.getHeight(),
                                    tile.getMinX(), tile.getMinY(),
                                    bandIndices);
        }
    }

    public Raster getTile(int tileX, int tileY) {
        // Just to return computeTile() result so as to avoid caching.
        return computeTile(tileX, tileY);
    }
}
