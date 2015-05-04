/*
 * $RCSfile: OverlayOpImage.java,v $
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

import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.JDKWorkarounds;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;
import java.util.Vector;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.PointOpImage;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;

/**
 * An <code>OpImage</code> implementing the "Overlay" operation.
 *
 * <p>This <code>OpImage</code> overlays one rendered image (source2)
 * on top of another (source1). The two sources are required to have
 * the same data type and number of bands.
 *
 * @see com.lightcrafts.mediax.jai.operator.OverlayDescriptor
 * @see OverlayCRIF
 *
 */
final class OverlayOpImage extends PointOpImage {

    /**
     * OverlayOpImage always has the bounds of the sourceUnder image.
     * The image bounds specified in the "layout" are always ignored.
     * Hence unset the image bounds so that PointImage.layoutHelper checks
     * dont fail.
     */
    private static ImageLayout layoutHelper(ImageLayout layout,
                                            Vector sources,
                                            Map config) {

	if (layout != null) {
            layout = (ImageLayout)layout.clone();
	    layout.unsetImageBounds();
	}

	return layout;
    }

    /**
     * Construct an <code>OverlayOpImage</code>.
     *
     * @param sourceUnder  The source image on the bottom.
     * @param sourceOver   The source image to be overlayed on top of
     *                     <code>sourceUnder</code>.
     * @param layout       The image layout for the destination image.
     */
    public OverlayOpImage(RenderedImage sourceUnder,
                          RenderedImage sourceOver,
                          Map config,
                          ImageLayout layout) {
        super(sourceUnder, sourceOver,
	      layoutHelper(layout, vectorize(sourceUnder, sourceOver), config),
	      config, true);

        /* Validate destination sampleModel. */
        SampleModel srcSM = sourceUnder.getSampleModel();
        if (sampleModel.getTransferType() != srcSM.getTransferType() ||
            sampleModel.getNumBands() != srcSM.getNumBands()) {
            sampleModel = srcSM.createCompatibleSampleModel(
                                tileWidth, tileHeight);

            if(colorModel != null &&
               !JDKWorkarounds.areCompatibleDataModels(sampleModel,
                                                       colorModel)) {
                colorModel = ImageUtil.getCompatibleColorModel(sampleModel,
                                                               config);
            }
        }

        /*
         * Unlike other multi-source OpImages, the image dimension in this
         * case is set to the same values as that of the source image on
         * the bottom.
         */
        minX = sourceUnder.getMinX();
        minY = sourceUnder.getMinY();
        width = sourceUnder.getWidth();
        height = sourceUnder.getHeight();
    }

    /**
     * Computes a tile. This method overrides PointOpImage.computeTile()
     *
     * @param tileX  The X index of the tile.
     * @param tileY  The Y index of the tile.
     */
    public Raster computeTile(int tileX, int tileY) {
        /* Create a new WritableRaster to represent this tile. */
        WritableRaster dest = createTile(tileX, tileY);

        /* Clip the raster bound to image bounds. */
        Rectangle destRect = dest.getBounds().intersection(getBounds());

        PlanarImage srcUnder = getSource(0);
        PlanarImage srcOver = getSource(1);

        Rectangle srcUnderBounds = srcUnder.getBounds();
        Rectangle srcOverBounds = srcOver.getBounds();

        /* In case of PointOpImage, mapDestRect(destRect, i) = destRect). */

        Raster[] sources = new Raster[2];
        if (srcOverBounds.contains(destRect)) {
            /* Tile is entirely inside sourceOver. */
            sources[0] = null;
            sources[1] = srcOver.getData(destRect);
            computeRect(sources, dest, destRect);

            // Recycle the source tile
            if(srcOver.overlapsMultipleTiles(destRect)) {
                recycleTile(sources[1]);
            }

            return dest;

        } else if (srcUnderBounds.contains(destRect) &&
                   !srcOverBounds.intersects(destRect)) {
            /* Tile is entirely inside sourceUnder. */
            sources[0] = srcUnder.getData(destRect);
            sources[1] = null;
            computeRect(sources, dest, destRect);

            // Recycle the source tile
            if(srcUnder.overlapsMultipleTiles(destRect)) {
                recycleTile(sources[0]);
            }

            return dest;

        } else {
            /* Tile is inside both sources. */
            Rectangle isectUnder = destRect.intersection(srcUnderBounds);
            sources[0] = srcUnder.getData(isectUnder);
            sources[1] = null;
            computeRect(sources, dest, isectUnder);

            // Recycle the source tile
            if(srcUnder.overlapsMultipleTiles(isectUnder)) {
                recycleTile(sources[0]);
            }

            if (srcOverBounds.intersects(destRect)) {
                Rectangle isectOver = destRect.intersection(srcOverBounds);
                sources[0] = null;
                sources[1] = srcOver.getData(isectOver);
                computeRect(sources, dest, isectOver);

                // Recycle the source tile
                if(srcOver.overlapsMultipleTiles(isectOver)) {
                    recycleTile(sources[1]);
                }
            }

            return dest;
        }
    }

    /**
     * Copies the pixel values of a source image within a specified
     * rectangle.
     *
     * @param sources   Cobbled sources, guaranteed to provide all the
     *                  source data necessary for computing the rectangle.
     * @param dest      The tile containing the rectangle to be computed.
     * @param destRect  The rectangle within the tile to be computed.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();


        RasterAccessor src = new RasterAccessor(sources[0] != null ? sources[0] : sources[1],
                                                destRect,
                                                formatTags[0],
                                                getSource(0).getColorModel());
        RasterAccessor dst = new RasterAccessor(dest, destRect,  
                                                formatTags[1], getColorModel());

        switch (dst.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            computeRectByte(src, dst);
            break;
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            computeRectShort(src, dst);
            break;
        case DataBuffer.TYPE_INT:
            computeRectInt(src, dst);
            break;
        case DataBuffer.TYPE_FLOAT:
            computeRectFloat(src, dst);
            break;
        case DataBuffer.TYPE_DOUBLE:
            computeRectDouble(src, dst);
            break;
        }

        dst.copyDataToRaster();
    }

    private void computeRectByte(RasterAccessor src,
                                 RasterAccessor dst) {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        byte[][] dstData = dst.getByteDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        byte[][] srcData = src.getByteDataArrays();

        for (int b = 0; b < dstBands; b++) {
            byte[] d = dstData[b];
            byte[] s = srcData[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    d[dstPixelOffset] = s[srcPixelOffset];

                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
            }
        }
    }

    private void computeRectShort(RasterAccessor src,
                                  RasterAccessor dst) {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        short[][] dstData = dst.getShortDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        short[][] srcData = src.getShortDataArrays();

        for (int b = 0; b < dstBands; b++) {
            short[] d = dstData[b];
            short[] s = srcData[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    d[dstPixelOffset] = s[srcPixelOffset];

                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
            }
        }
    }

    private void computeRectInt(RasterAccessor src,
                                RasterAccessor dst) {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        int[][] dstData = dst.getIntDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        int[][] srcData = src.getIntDataArrays();

        for (int b = 0; b < dstBands; b++) {
            int[] d = dstData[b];
            int[] s = srcData[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    d[dstPixelOffset] = s[srcPixelOffset];

                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
            }
        }
    }

    private void computeRectFloat(RasterAccessor src,
                                  RasterAccessor dst) {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        float[][] dstData = dst.getFloatDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        float[][] srcData = src.getFloatDataArrays();

        for (int b = 0; b < dstBands; b++) {
            float[] d = dstData[b];
            float[] s = srcData[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    d[dstPixelOffset] = s[srcPixelOffset];

                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
            }
        }
    }

    private void computeRectDouble(RasterAccessor src,
                                   RasterAccessor dst) {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        double[][] dstData = dst.getDoubleDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        double[][] srcData = src.getDoubleDataArrays();

        for (int b = 0; b < dstBands; b++) {
            double[] d = dstData[b];
            double[] s = srcData[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    d[dstPixelOffset] = s[srcPixelOffset];

                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
            }
        }
    }
}
