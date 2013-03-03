/*
 * $RCSfile: WarpNearestOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:47 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import java.util.Map;
import com.lightcrafts.mediax.jai.Warp;
import com.lightcrafts.mediax.jai.WarpOpImage;
import com.lightcrafts.mediax.jai.iterator.RandomIter;
import com.lightcrafts.mediax.jai.iterator.RandomIterFactory;

/**
 * An <code>OpImage</code> implementing the general "Warp" operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.WarpDescriptor</code>.
 * It supports the nearest-neighbor interpolation.
 *
 * <p>The layout for the destination image may be specified via the
 * <code>ImageLayout</code> parameter. However, only those settings
 * suitable for this operation will be used. The unsuitable settings
 * will be replaced by default suitable values.
 *
 * @since EA2
 * @see com.lightcrafts.mediax.jai.Warp
 * @see com.lightcrafts.mediax.jai.WarpOpImage
 * @see com.lightcrafts.mediax.jai.operator.WarpDescriptor
 * @see WarpRIF
 *
 */
final class WarpNearestOpImage extends WarpOpImage {

    /**
     * Constructs a WarpNearestOpImage.
     *
     * @param source  The source image.
     * @param layout  The destination image layout.
     * @param warp    An object defining the warp algorithm.
     * @param interp  An object describing the interpolation method.
     */
    public WarpNearestOpImage(RenderedImage source,
                              Map config,
                              ImageLayout layout,
                              Warp warp,
                              Interpolation interp,
                              double[] backgroundValues) {
        super(source,
              layout,
              config,
              false,
              null,   // extender
              interp,
              warp,
              backgroundValues);

        /*
         * If the source has IndexColorModel, override the default setting
         * in OpImage. The dest shall have exactly the same SampleModel and
         * ColorModel as the source.
         * Note, in this case, the source should have an integral data type.
         */
        ColorModel srcColorModel = source.getColorModel();
        if (srcColorModel instanceof IndexColorModel) {
             sampleModel = source.getSampleModel().createCompatibleSampleModel(
                                                   tileWidth, tileHeight);
             colorModel = srcColorModel;
        }
    }

    /** Warps a rectangle. */
    protected void computeRect(PlanarImage[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        RasterAccessor d = new RasterAccessor(dest, destRect,
                                              formatTags[1], getColorModel());

        switch (d.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            computeRectByte(sources[0], d);
            break;
        case DataBuffer.TYPE_USHORT:
            computeRectUShort(sources[0], d);
            break;
        case DataBuffer.TYPE_SHORT:
            computeRectShort(sources[0], d);
            break;
        case DataBuffer.TYPE_INT:
            computeRectInt(sources[0], d);
            break;
        case DataBuffer.TYPE_FLOAT:
            computeRectFloat(sources[0], d);
            break;
        case DataBuffer.TYPE_DOUBLE:
            computeRectDouble(sources[0], d);
            break;
        }

        if (d.isDataCopy()) {
            d.clampDataArrays();
            d.copyDataToRaster();
        }
    }

    private void computeRectByte(PlanarImage src, RasterAccessor dst) {
        RandomIter iter = RandomIterFactory.create(src, src.getBounds());

        int minX = src.getMinX();
        int maxX = src.getMaxX();
        int minY = src.getMinY();
        int maxY = src.getMaxY();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        byte[][] data = dst.getByteDataArrays();

        float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

	byte[] backgroundByte = new byte[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundByte[i] = (byte)backgroundValues[i];

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1,
                          warpData);
            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                /*
                 * The warp object subtract 0.5 from backward mapped
                 * source coordinate. Need to do a round to get the
                 * nearest neighbor. This is different from the standard
                 * nearest implementation.
                 */
                int sx = round(warpData[count++]);
                int sy = round(warpData[count++]);

                if (sx < minX || sx >= maxX || sy < minY || sy >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundByte[b];
                        }
                    }
                } else {
                    for (int b = 0; b < dstBands; b++) {
                        data[b][pixelOffset+bandOffsets[b]] =
                            (byte)(iter.getSample(sx, sy, b) & 0xFF);
                    }
                }

                pixelOffset += pixelStride;
            }
        }
    }

    private void computeRectUShort(PlanarImage src, RasterAccessor dst) {
        RandomIter iter = RandomIterFactory.create(src, src.getBounds());

        int minX = src.getMinX();
        int maxX = src.getMaxX();
        int minY = src.getMinY();
        int maxY = src.getMaxY();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        short[][] data = dst.getShortDataArrays();

        float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

	short[] backgroundUShort = new short[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundUShort[i] = (short)backgroundValues[i];

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1,
                          warpData);
            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                /*
                 * The warp object subtract 0.5 from backward mapped
                 * source coordinate. Need to do a round to get the
                 * nearest neighbor. This is different from the standard
                 * nearest implementation.
                 */
                int sx = round(warpData[count++]);
                int sy = round(warpData[count++]);

                if (sx < minX || sx >= maxX || sy < minY || sy >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundUShort[b];
                        }
                    }
                } else {
                    for (int b = 0; b < dstBands; b++) {
                        data[b][pixelOffset+bandOffsets[b]] =
                            (short)(iter.getSample(sx, sy, b) & 0xFFFF);
                    }
                }

                pixelOffset += pixelStride;
            }
        }
    }

    private void computeRectShort(PlanarImage src, RasterAccessor dst) {
        RandomIter iter = RandomIterFactory.create(src, src.getBounds());

        int minX = src.getMinX();
        int maxX = src.getMaxX();
        int minY = src.getMinY();
        int maxY = src.getMaxY();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        short[][] data = dst.getShortDataArrays();

        float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

        short[] backgroundShort = new short[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundShort[i] = (short)backgroundValues[i];

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1,
                          warpData);
            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                /*
                 * The warp object subtract 0.5 from backward mapped
                 * source coordinate. Need to do a round to get the
                 * nearest neighbor. This is different from the standard
                 * nearest implementation.
                 */
                int sx = round(warpData[count++]);
                int sy = round(warpData[count++]);

                if (sx < minX || sx >= maxX || sy < minY || sy >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundShort[b];
                        }
                    }
                } else {
                    for (int b = 0; b < dstBands; b++) {
                        data[b][pixelOffset+bandOffsets[b]] =
                            (short)iter.getSample(sx, sy, b);
                    }
                }

                pixelOffset += pixelStride;
            }
        }
    }

    private void computeRectInt(PlanarImage src, RasterAccessor dst) {
        RandomIter iter = RandomIterFactory.create(src, src.getBounds());

        int minX = src.getMinX();
        int maxX = src.getMaxX();
        int minY = src.getMinY();
        int maxY = src.getMaxY();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        int[][] data = dst.getIntDataArrays();

        float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

	int[] backgroundInt = new int[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundInt[i] = (int)backgroundValues[i];

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1,
                          warpData);
            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                /*
                 * The warp object subtract 0.5 from backward mapped
                 * source coordinate. Need to do a round to get the
                 * nearest neighbor. This is different from the standard
                 * nearest implementation.
                 */
                int sx = round(warpData[count++]);
                int sy = round(warpData[count++]);

                if (sx < minX || sx >= maxX || sy < minY || sy >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundInt[b];
                        }
                    }
                } else {
                    for (int b = 0; b < dstBands; b++) {
                        data[b][pixelOffset+bandOffsets[b]] =
                            iter.getSample(sx, sy, b);
                    }
                }

                pixelOffset += pixelStride;
            }
        }
    }

    private void computeRectFloat(PlanarImage src, RasterAccessor dst) {
        RandomIter iter = RandomIterFactory.create(src, src.getBounds());

        int minX = src.getMinX();
        int maxX = src.getMaxX();
        int minY = src.getMinY();
        int maxY = src.getMaxY();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        float[][] data = dst.getFloatDataArrays();

        float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

	float[] backgroundFloat = new float[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundFloat[i] = (float)backgroundValues[i];

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1,
                          warpData);
            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                /*
                 * The warp object subtract 0.5 from backward mapped
                 * source coordinate. Need to do a round to get the
                 * nearest neighbor. This is different from the standard
                 * nearest implementation.
                 */
                int sx = round(warpData[count++]);
                int sy = round(warpData[count++]);

                if (sx < minX || sx >= maxX || sy < minY || sy >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundFloat[b];
                        }
                    }
                } else {
                    for (int b = 0; b < dstBands; b++) {
                        data[b][pixelOffset+bandOffsets[b]] =
                            iter.getSampleFloat(sx, sy, b);
                    }
                }

                pixelOffset += pixelStride;
            }
        }
    }

    private void computeRectDouble(PlanarImage src, RasterAccessor dst) {
        RandomIter iter = RandomIterFactory.create(src, src.getBounds());

        int minX = src.getMinX();
        int maxX = src.getMaxX();
        int minY = src.getMinY();
        int maxY = src.getMaxY();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        double[][] data = dst.getDoubleDataArrays();

        float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1,
                          warpData);
            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                /*
                 * The warp object subtract 0.5 from backward mapped
                 * source coordinate. Need to do a round to get the
                 * nearest neighbor. This is different from the standard
                 * nearest implementation.
                 */
                int sx = round(warpData[count++]);
                int sy = round(warpData[count++]);

                if (sx < minX || sx >= maxX || sy < minY || sy >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundValues[b];
                        }
                    }
                } else {
                    for (int b = 0; b < dstBands; b++) {
                        data[b][pixelOffset+bandOffsets[b]] =
                            iter.getSampleDouble(sx, sy, b);
                    }
                }

                pixelOffset += pixelStride;
            }
        }
    }

    /** Returns the "round" value of a float. */
    private static final int round(float f) {
        return f >= 0 ? (int)(f + 0.5F) : (int)(f - 0.5F);
    }
}
