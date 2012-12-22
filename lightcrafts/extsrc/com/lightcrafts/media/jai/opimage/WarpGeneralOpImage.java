/*
 * $RCSfile: WarpGeneralOpImage.java,v $
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
import com.lightcrafts.mediax.jai.BorderExtender;
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
import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * An <code>OpImage</code> implementing the general "Warp" operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.WarpDescriptor</code>.
 * It supports all interpolation cases.
 *
 * @since EA2
 * @see com.lightcrafts.mediax.jai.Warp
 * @see com.lightcrafts.mediax.jai.WarpOpImage
 * @see com.lightcrafts.mediax.jai.operator.WarpDescriptor
 * @see WarpRIF
 *
 */
final class WarpGeneralOpImage extends WarpOpImage {

    /** Color table representing source's IndexColorModel. */
    private byte[][] ctable = null;

    /**
     * Constructs a WarpGeneralOpImage.
     *
     * @param source  The source image.
     * @param extender A BorderExtender, or null.
     * @param layout  The destination image layout.
     * @param warp    An object defining the warp algorithm.
     * @param interp  An object describing the interpolation method.
     */
    public WarpGeneralOpImage(RenderedImage source,
                              BorderExtender extender,
                              Map config,
                              ImageLayout layout,
                              Warp warp,
                              Interpolation interp,
                              double[] backgroundValues) {
        super(source,
              layout,
              config,
              false,
              extender,
              interp,
              warp,
              backgroundValues);

        /*
         * If the source has IndexColorModel, get the RGB color table.
         * Note, in this case, the source should have an integral data type.
         * And dest always has data type byte.
         */
        ColorModel srcColorModel = source.getColorModel();
        if (srcColorModel instanceof IndexColorModel) {
            IndexColorModel icm = (IndexColorModel)srcColorModel;
            ctable = new byte[3][icm.getMapSize()];
            icm.getReds(ctable[0]);
            icm.getGreens(ctable[1]);
            icm.getBlues(ctable[2]);
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
        int lpad, rpad, tpad, bpad;
        if(interp != null) {
            lpad = interp.getLeftPadding();
            rpad = interp.getRightPadding();
            tpad = interp.getTopPadding();
            bpad = interp.getBottomPadding();
        } else {
            lpad = rpad = tpad = bpad = 0;
        }

        int minX, maxX, minY, maxY;
        RandomIter iter;
        if(extender != null) {
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();
            Rectangle bounds = new Rectangle(src.getMinX() - lpad,
                                             src.getMinY() - tpad,
                                             src.getWidth() + lpad + rpad,
                                             src.getHeight() + tpad + bpad);
            iter = RandomIterFactory.create(src.getExtendedData(bounds,
                                                                extender),
                                            bounds);
        } else {
            minX = src.getMinX() + lpad;
            maxX = src.getMaxX() - rpad;
            minY = src.getMinY() + tpad;
            maxY = src.getMaxY() - bpad;
            iter = RandomIterFactory.create(src, src.getBounds());
        }

        int kwidth = interp.getWidth();
        int kheight = interp.getHeight();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        byte[][] data = dst.getByteDataArrays();

        int precH = 1 << interp.getSubsampleBitsH();
        int precV = 1 << interp.getSubsampleBitsV();

        float[] warpData = new float[2 * dstWidth];

        int[][] samples = new int[kheight][kwidth];

        int lineOffset = 0;

        byte[] backgroundByte = new byte[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundByte[i] = (byte)backgroundValues[i];

        if (ctable == null) {	// source does not have IndexColorModel
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    int xfrac = (int)((sx - xint) * precH);
                    int yfrac = (int)((sy - yint) * precV);

                    if (xint < minX || xint >= maxX ||
                        yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset+bandOffsets[b]] =
                                    backgroundByte[b];
                            }
                        }
                    } else {
                        xint -= lpad;
                        yint -= tpad;

                        for (int b = 0; b < dstBands; b++) {
                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    samples[j][i] = iter.getSample(
                                        xint+i, yint+j, b) & 0xFF;
                                }
                            }

                            data[b][pixelOffset+bandOffsets[b]] =
                                ImageUtil.clampByte(
                                interp.interpolate(samples, xfrac, yfrac));
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
        } else {	// source has IndexColorModel
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    int xfrac = (int)((sx - xint) * precH);
                    int yfrac = (int)((sy - yint) * precV);

                    if (xint < minX || xint >= maxX ||
                        yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset+bandOffsets[b]] =
                                    backgroundByte[b];
                            }
                        }
                    } else {
                        xint -= lpad;
                        yint -= tpad;

                        for (int b = 0; b < dstBands; b++) {
                            byte[] t = ctable[b];

                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    samples[j][i] = t[iter.getSample(
                                        xint+i, yint+j, 0) & 0xFF] & 0xFF;
                                }
                            }

                            data[b][pixelOffset+bandOffsets[b]] =
                                ImageUtil.clampByte(
                                interp.interpolate(samples, xfrac, yfrac));
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
        }
    }

    private void computeRectUShort(PlanarImage src, RasterAccessor dst) {
        int lpad, rpad, tpad, bpad;
        if(interp != null) {
            lpad = interp.getLeftPadding();
            rpad = interp.getRightPadding();
            tpad = interp.getTopPadding();
            bpad = interp.getBottomPadding();
        } else {
            lpad = rpad = tpad = bpad = 0;
        }

        int minX, maxX, minY, maxY;
        RandomIter iter;
        if(extender != null) {
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();
            Rectangle bounds = new Rectangle(src.getMinX() - lpad,
                                             src.getMinY() - tpad,
                                             src.getWidth() + lpad + rpad,
                                             src.getHeight() + tpad + bpad);
            iter = RandomIterFactory.create(src.getExtendedData(bounds,
                                                                extender),
                                            bounds);
        } else {
            minX = src.getMinX() + lpad;
            maxX = src.getMaxX() - rpad;
            minY = src.getMinY() + tpad;
            maxY = src.getMaxY() - bpad;
            iter = RandomIterFactory.create(src, src.getBounds());
        }

        int kwidth = interp.getWidth();
        int kheight = interp.getHeight();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        short[][] data = dst.getShortDataArrays();

        int precH = 1 << interp.getSubsampleBitsH();
        int precV = 1 << interp.getSubsampleBitsV();

        float[] warpData = new float[2 * dstWidth];

        int[][] samples = new int[kheight][kwidth];

        int lineOffset = 0;

	short[] backgroundUShort = new short[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundUShort[i] = (short)backgroundValues[i];

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1, warpData);

            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                float sx = warpData[count++];
                float sy = warpData[count++];

                int xint = floor(sx);
                int yint = floor(sy);
                int xfrac = (int)((sx - xint) * precH);
                int yfrac = (int)((sy - yint) * precV);

                if (xint < minX || xint >= maxX ||
                    yint < minY || yint >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundUShort[b];
                        }
                    }
                } else {
                    xint -= lpad;
                    yint -= tpad;

                    for (int b = 0; b < dstBands; b++) {
                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                samples[j][i] = iter.getSample(
                                    xint+i, yint+j, b) & 0xFFFF;
                            }
                        }

                        data[b][pixelOffset+bandOffsets[b]] =
                            ImageUtil.clampUShort(
                            interp.interpolate(samples, xfrac, yfrac));
                    }
                }

                pixelOffset += pixelStride;
            }
        }
    }

    private void computeRectShort(PlanarImage src, RasterAccessor dst) {
        int lpad, rpad, tpad, bpad;
        if(interp != null) {
            lpad = interp.getLeftPadding();
            rpad = interp.getRightPadding();
            tpad = interp.getTopPadding();
            bpad = interp.getBottomPadding();
        } else {
            lpad = rpad = tpad = bpad = 0;
        }

        int minX, maxX, minY, maxY;
        RandomIter iter;
        if(extender != null) {
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();
            Rectangle bounds = new Rectangle(src.getMinX() - lpad,
                                             src.getMinY() - tpad,
                                             src.getWidth() + lpad + rpad,
                                             src.getHeight() + tpad + bpad);
            iter = RandomIterFactory.create(src.getExtendedData(bounds,
                                                                extender),
                                            bounds);
        } else {
            minX = src.getMinX() + lpad;
            maxX = src.getMaxX() - rpad;
            minY = src.getMinY() + tpad;
            maxY = src.getMaxY() - bpad;
            iter = RandomIterFactory.create(src, src.getBounds());
        }

        int kwidth = interp.getWidth();
        int kheight = interp.getHeight();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        short[][] data = dst.getShortDataArrays();

        int precH = 1 << interp.getSubsampleBitsH();
        int precV = 1 << interp.getSubsampleBitsV();

        float[] warpData = new float[2 * dstWidth];

        int[][] samples = new int[kheight][kwidth];

        int lineOffset = 0;

        short[] backgroundShort = new short[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundShort[i] = (short)backgroundValues[i];

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1, warpData);

            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                float sx = warpData[count++];
                float sy = warpData[count++];

                int xint = floor(sx);
                int yint = floor(sy);
                int xfrac = (int)((sx - xint) * precH);
                int yfrac = (int)((sy - yint) * precV);

                if (xint < minX || xint >= maxX ||
                    yint < minY || yint >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundShort[b];
                        }
                    }
                } else {
                    xint -= lpad;
                    yint -= tpad;

                    for (int b = 0; b < dstBands; b++) {
                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                samples[j][i] = iter.getSample(
                                    xint+i, yint+j, b);
                            }
                        }

                        data[b][pixelOffset+bandOffsets[b]] =
                            ImageUtil.clampShort(
                            interp.interpolate(samples, xfrac, yfrac));
                    }
                }

                pixelOffset += pixelStride;
            }
        }
    }

    private void computeRectInt(PlanarImage src, RasterAccessor dst) {
        int lpad, rpad, tpad, bpad;
        if(interp != null) {
            lpad = interp.getLeftPadding();
            rpad = interp.getRightPadding();
            tpad = interp.getTopPadding();
            bpad = interp.getBottomPadding();
        } else {
            lpad = rpad = tpad = bpad = 0;
        }

        int minX, maxX, minY, maxY;
        RandomIter iter;
        if(extender != null) {
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();
            Rectangle bounds = new Rectangle(src.getMinX() - lpad,
                                             src.getMinY() - tpad,
                                             src.getWidth() + lpad + rpad,
                                             src.getHeight() + tpad + bpad);
            iter = RandomIterFactory.create(src.getExtendedData(bounds,
                                                                extender),
                                            bounds);
        } else {
            minX = src.getMinX() + lpad;
            maxX = src.getMaxX() - rpad;
            minY = src.getMinY() + tpad;
            maxY = src.getMaxY() - bpad;
            iter = RandomIterFactory.create(src, src.getBounds());
        }

        int kwidth = interp.getWidth();
        int kheight = interp.getHeight();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        int[][] data = dst.getIntDataArrays();

        int precH = 1 << interp.getSubsampleBitsH();
        int precV = 1 << interp.getSubsampleBitsV();

        float[] warpData = new float[2 * dstWidth];

        int[][] samples = new int[kheight][kwidth];

        int lineOffset = 0;

	int[] backgroundInt = new int[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundInt[i] = (int)backgroundValues[i];

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1, warpData);

            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                float sx = warpData[count++];
                float sy = warpData[count++];

                int xint = floor(sx);
                int yint = floor(sy);
                int xfrac = (int)((sx - xint) * precH);
                int yfrac = (int)((sy - yint) * precV);

                if (xint < minX || xint >= maxX ||
                    yint < minY || yint >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundInt[b];
                        }
                    }
                } else {
                    xint -= lpad;
                    yint -= tpad;

                    for (int b = 0; b < dstBands; b++) {
                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                samples[j][i] = iter.getSample(
                                    xint+i, yint+j, b);
                            }
                        }

                        data[b][pixelOffset+bandOffsets[b]] =
                            interp.interpolate(samples, xfrac, yfrac);
                    }
                }

                pixelOffset += pixelStride;
            }
        }
    }

    private void computeRectFloat(PlanarImage src, RasterAccessor dst) {
        int lpad, rpad, tpad, bpad;
        if(interp != null) {
            lpad = interp.getLeftPadding();
            rpad = interp.getRightPadding();
            tpad = interp.getTopPadding();
            bpad = interp.getBottomPadding();
        } else {
            lpad = rpad = tpad = bpad = 0;
        }

        int minX, maxX, minY, maxY;
        RandomIter iter;
        if(extender != null) {
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();
            Rectangle bounds = new Rectangle(src.getMinX() - lpad,
                                             src.getMinY() - tpad,
                                             src.getWidth() + lpad + rpad,
                                             src.getHeight() + tpad + bpad);
            iter = RandomIterFactory.create(src.getExtendedData(bounds,
                                                                extender),
                                            bounds);
        } else {
            minX = src.getMinX() + lpad;
            maxX = src.getMaxX() - rpad;
            minY = src.getMinY() + tpad;
            maxY = src.getMaxY() - bpad;
            iter = RandomIterFactory.create(src, src.getBounds());
        }

        int kwidth = interp.getWidth();
        int kheight = interp.getHeight();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        float[][] data = dst.getFloatDataArrays();

        float[] warpData = new float[2 * dstWidth];

        float[][] samples = new float[kheight][kwidth];

        int lineOffset = 0;

        float[] backgroundFloat = new float[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundFloat[i] = (float)backgroundValues[i];

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1, warpData);

            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                float sx = warpData[count++];
                float sy = warpData[count++];

                int xint = floor(sx);
                int yint = floor(sy);
                float xfrac = sx - xint;
                float yfrac = sy - yint;

                if (xint < minX || xint >= maxX ||
                    yint < minY || yint >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundFloat[b];
                        }
                    }
                } else {
                    xint -= lpad;
                    yint -= tpad;

                    for (int b = 0; b < dstBands; b++) {
                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                samples[j][i] = iter.getSampleFloat(
                                    xint+i, yint+j, b);
                            }
                        }

                        data[b][pixelOffset+bandOffsets[b]] =
                            interp.interpolate(samples, xfrac, yfrac);
                    }
                }

                pixelOffset += pixelStride;
            }
        }
    }

    private void computeRectDouble(PlanarImage src, RasterAccessor dst) {
        int lpad, rpad, tpad, bpad;
        if(interp != null) {
            lpad = interp.getLeftPadding();
            rpad = interp.getRightPadding();
            tpad = interp.getTopPadding();
            bpad = interp.getBottomPadding();
        } else {
            lpad = rpad = tpad = bpad = 0;
        }

        int minX, maxX, minY, maxY;
        RandomIter iter;
        if(extender != null) {
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();
            Rectangle bounds = new Rectangle(src.getMinX() - lpad,
                                             src.getMinY() - tpad,
                                             src.getWidth() + lpad + rpad,
                                             src.getHeight() + tpad + bpad);
            iter = RandomIterFactory.create(src.getExtendedData(bounds,
                                                                extender),
                                            bounds);
        } else {
            minX = src.getMinX() + lpad;
            maxX = src.getMaxX() - rpad;
            minY = src.getMinY() + tpad;
            maxY = src.getMaxY() - bpad;
            iter = RandomIterFactory.create(src, src.getBounds());
        }

        int kwidth = interp.getWidth();
        int kheight = interp.getHeight();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        double[][] data = dst.getDoubleDataArrays();

        float[] warpData = new float[2 * dstWidth];

        double[][] samples = new double[kheight][kwidth];

        int lineOffset = 0;

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1, warpData);

            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                float sx = warpData[count++];
                float sy = warpData[count++];

                int xint = floor(sx);
                int yint = floor(sy);
                float xfrac = sx - xint;
                float yfrac = sy - yint;

                if (xint < minX || xint >= maxX ||
                    yint < minY || yint >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundValues[b];
                        }
                    }
                } else {
                    xint -= lpad;
                    yint -= tpad;

                    for (int b = 0; b < dstBands; b++) {
                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                samples[j][i] = iter.getSampleDouble(
                                    xint+i, yint+j, b);
                            }
                        }

                        data[b][pixelOffset+bandOffsets[b]] =
                            interp.interpolate(samples, xfrac, yfrac);
                    }
                }

                pixelOffset += pixelStride;
            }
        }
    }

    /** Returns the "floor" value of a float. */
    private static final int floor(float f) {
        return f >= 0 ? (int)f : (int)f - 1;
    }
}
