/*
 * $RCSfile: CompositeNoDestAlphaOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:18 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PointOpImage;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import java.util.Map;

/**
 * An <code>OpImage</code> implementing the "Composite" operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.CompositeDescriptor</code>.
 * This implementation handles the case where the destination image does
 * not include its result alpha channel.
 *
 * <p> For two source images <code>source1</code> and <code>source2</code>,
 * this <code>OpImage</code> places the foreground <code>source1</code>
 * in front of the background <code>source2</code>. This is what commonly
 * known as the "over" composite.  The destination color values are
 * calculated using the following formula:
 * <pre>
 * dest = source1 * alpha1 + source2 * alpha2 * (1 - alpha1)
 * </pre>
 * where <code>source1</code> and <code>source2</code> are the color values
 * of the two source images, without their alpha multiplied to them, and
 * <code>alpha1</code> and <code>alpha2</code> are the two sources's alpha
 * values in fraction.
 *
 * @see com.lightcrafts.mediax.jai.operator.CompositeDescriptor
 * @see CompositeCRIF
 *
 */
final class CompositeNoDestAlphaOpImage extends PointOpImage {

    /** The alpha image for the first source. */
    private RenderedImage alpha1;

    /** The alpha image for the second source. */
    private RenderedImage alpha2;

    /** Indicates whether alpha has been premultiplied. */
    private boolean premultiplied;

    /** The RasterAccessor format tags. */
    private RasterFormatTag[] tags;

    /**
     * Constructor.
     *
     * @param source1  The foreground source image.
     * @param source2  The background source image.

     * @param layout  The destination image layout.
     * @param alpha1  The alpha image for the first source.
     * @param alpha2  The alpha image for the second source. If
     *        <code>null</code>, the second source is assumed to be opaque.
     * @param premultiplied  Indicates whether both sources and destination
     *        have their alpha premultiplied.
     */
    public CompositeNoDestAlphaOpImage(RenderedImage source1,
                                       RenderedImage source2,
                                       Map config,
                                       ImageLayout layout,
                                       RenderedImage alpha1,
                                       RenderedImage alpha2,
                                       boolean premultiplied) {
        super(source1, source2, layout, config, true);

        this.alpha1 = alpha1;
        this.alpha2 = alpha2;
        this.premultiplied = premultiplied;

	tags = getFormatTags();
    }

    /**
     * Composites two images within a specified rectangle.
     *
     * @param sources  Cobbled sources, guaranteed to provide all the
     *        source data necessary for computing the rectangle.
     * @param dest  The tile containing the rectangle to be computed.
     * @param destRect  The rectangle within the tile to be computed.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        /* For PointOpImage, srcRect = destRect. */
        RasterAccessor s1 = new RasterAccessor(
            sources[0], destRect, tags[0], getSourceImage(0).getColorModel());

        RasterAccessor s2 = new RasterAccessor(
            sources[1], destRect, tags[1], getSourceImage(1).getColorModel());

        RasterAccessor a1 = new RasterAccessor(
            alpha1.getData(destRect), destRect,
            tags[2], alpha1.getColorModel());

        RasterAccessor a2 = null, d;
        if (alpha2 == null) {
            d = new RasterAccessor(dest, destRect, 
                                   tags[3], getColorModel());
        } else {
            a2 = new RasterAccessor(alpha2.getData(destRect), destRect, 
                                    tags[3], alpha2.getColorModel());
            d = new RasterAccessor(dest, destRect,
                                   tags[4], getColorModel());
        }

        switch (d.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(s1, s2, a1, a2, d);
            break;
        case DataBuffer.TYPE_USHORT:
            ushortLoop(s1, s2, a1, a2, d);
            break;
        case DataBuffer.TYPE_SHORT:
            shortLoop(s1, s2, a1, a2, d);
            break;
        case DataBuffer.TYPE_INT:
            intLoop(s1, s2, a1, a2, d);
            break;
        case DataBuffer.TYPE_FLOAT:
            floatLoop(s1, s2, a1, a2, d);
            break;
        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(s1, s2, a1, a2, d);
            break;
        }

        if (d.isDataCopy()) {
            d.clampDataArrays();
            d.copyDataToRaster();
        }
    }

    /*
     * Formulas for integral data types:
     *
     * if (premultiplied) {
     *     dest = source1 + source2 * (1 - alpha1/maxValue)
     * } else {
     *     if (alpha2 == null) {
     *         dest = source1 * alpha1/maxValue +
     *                source2 * (1 - alpha1/maxValue)
     *     } else {
     *         dest = (source1 * alpha1 +
     *                 source2 * alpha2 * (1 - alpha1/maxValue)) /
     *                (alpha1 + alpha2 * (1 - alpha1/maxValue))
     *     }
     * }
     */

    private void byteLoop(RasterAccessor s1, RasterAccessor s2,
                          RasterAccessor a1, RasterAccessor a2,
                          RasterAccessor d) {
        /* First source color channels. */
        int s1LineStride = s1.getScanlineStride();
        int s1PixelStride = s1.getPixelStride();
        int[] s1BandOffsets = s1.getBandOffsets();
        byte[][] s1Data = s1.getByteDataArrays();

        /* Second source color channels. */
        int s2LineStride = s2.getScanlineStride();
        int s2PixelStride = s2.getPixelStride();
        int[] s2BandOffsets = s2.getBandOffsets();
        byte[][] s2Data = s2.getByteDataArrays();

        /* First source alpha channel. */
        int a1LineStride = a1.getScanlineStride();
        int a1PixelStride = a1.getPixelStride();
        int a1BandOffset = a1.getBandOffset(0);
        byte[] a1Data = a1.getByteDataArray(0);

        /* Second source alpha channel (if any). */
        int a2LineStride = 0;
        int a2PixelStride = 0;
        int a2BandOffset = 0;
        byte[] a2Data = null;
        if (alpha2 != null) {
            a2LineStride = a2.getScanlineStride();
            a2PixelStride = a2.getPixelStride();
            a2BandOffset = a2.getBandOffset(0);
            a2Data = a2.getByteDataArray(0);
        }

        /* Destination color channels. */
        int dLineStride = d.getScanlineStride();
        int dPixelStride = d.getPixelStride();
        int[] dBandOffsets = d.getBandOffsets();
        byte[][] dData = d.getByteDataArrays();

        int dwidth = d.getWidth();
        int dheight = d.getHeight();
        int dbands = d.getNumBands();

        float invMax = 1.0F / 0xFF;

        int s1LineOffset = 0, s2LineOffset = 0,
            a1LineOffset = 0, a2LineOffset = 0,
            dLineOffset = 0,
            s1PixelOffset, s2PixelOffset,
            a1PixelOffset, a2PixelOffset,
            dPixelOffset;

        if (premultiplied) {
            /* dest = source1 + source2 * (1 - alpha1/max) */

            for (int h = 0; h < dheight; h++) {
                s1PixelOffset = s1LineOffset;
                s2PixelOffset = s2LineOffset;
                a1PixelOffset = a1LineOffset + a1BandOffset;
                dPixelOffset = dLineOffset;

                s1LineOffset += s1LineStride;
                s2LineOffset += s2LineStride;
                a1LineOffset += a1LineStride;
                dLineOffset += dLineStride;

                for (int w = 0; w < dwidth; w++) {
                    float t = 1.0F - (a1Data[a1PixelOffset] & 0xFF) * invMax;

                    /* Destination color channels. */
                    for (int b = 0; b < dbands; b++) {
                        dData[b][dPixelOffset+dBandOffsets[b]] = (byte)
                       ((s1Data[b][s1PixelOffset+s1BandOffsets[b]] & 0xFF) +
                        (s2Data[b][s2PixelOffset+s2BandOffsets[b]] & 0xFF) * t);
                    }

                    s1PixelOffset += s1PixelStride;
                    s2PixelOffset += s2PixelStride;
                    a1PixelOffset += a1PixelStride;
                    dPixelOffset += dPixelStride;
                }
            }

        } else {
            if (alpha2 == null) {
                /* dest = source1 * alpha1/max + source2 * (1 - alpha1/max) */

                for (int h = 0; h < dheight; h++) {
                    s1PixelOffset = s1LineOffset;
                    s2PixelOffset = s2LineOffset;
                    a1PixelOffset = a1LineOffset + a1BandOffset;
                    dPixelOffset = dLineOffset;

                    s1LineOffset += s1LineStride;
                    s2LineOffset += s2LineStride;
                    a1LineOffset += a1LineStride;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {
                        float t1 = (a1Data[a1PixelOffset] & 0xFF) * invMax;
                        float t2 = 1.0F - t1;

                        /* Destination color channels. */
                        for (int b = 0; b < dbands; b++) {
                            dData[b][dPixelOffset+dBandOffsets[b]] = (byte)
                      ((s1Data[b][s1PixelOffset+s1BandOffsets[b]] & 0xFF) * t1 +
                       (s2Data[b][s2PixelOffset+s2BandOffsets[b]] & 0xFF) * t2);
                        }

                        s1PixelOffset += s1PixelStride;
                        s2PixelOffset += s2PixelStride;
                        a1PixelOffset += a1PixelStride;
                        dPixelOffset += dPixelStride;
                    }
                }
            } else {
                /*
                 * dest = (source1 * alpha1 +
                 *         source2 * alpha2 * (1 - alpha1/maxValue)) /
                 *        (alpha1 + alpha2 * (1 - alpha1/maxValue))
                 */

                for (int h = 0; h < dheight; h++) {
                    s1PixelOffset = s1LineOffset;
                    s2PixelOffset = s2LineOffset;
                    a1PixelOffset = a1LineOffset + a1BandOffset;
                    a2PixelOffset = a2LineOffset + a2BandOffset;
                    dPixelOffset = dLineOffset;

                    s1LineOffset += s1LineStride;
                    s2LineOffset += s2LineStride;
                    a1LineOffset += a1LineStride;
                    a2LineOffset += a2LineStride;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {
                        int t1 = a1Data[a1PixelOffset] & 0xFF;
                        float t2 = (a2Data[a2PixelOffset] & 0xFF) *
                                   (1.0F - t1 * invMax);
                        float t3 = t1 + t2;
                        float t4, t5;
                        if (t3 == 0.0F) {
                            t4 = 0.0F;
                            t5 = 0.0F;
                        } else {
                            t4 = t1 / t3;
                            t5 = t2 / t3;
                        }

                        /* Destination color channels. */
                        for (int b = 0; b < dbands; b++) {
                            dData[b][dPixelOffset+dBandOffsets[b]] = (byte)
                      ((s1Data[b][s1PixelOffset+s1BandOffsets[b]] & 0xFF) * t4 +
                       (s2Data[b][s2PixelOffset+s2BandOffsets[b]] & 0xFF) * t5);
                        }

                        s1PixelOffset += s1PixelStride;
                        s2PixelOffset += s2PixelStride;
                        a1PixelOffset += a1PixelStride;
                        a2PixelOffset += a2PixelStride;
                        dPixelOffset += dPixelStride;
                    }
                }
            }
        }
    }

    private void ushortLoop(RasterAccessor s1, RasterAccessor s2,
                            RasterAccessor a1, RasterAccessor a2,
                            RasterAccessor d) {
        /* First source color channels. */
        int s1LineStride = s1.getScanlineStride();
        int s1PixelStride = s1.getPixelStride();
        int[] s1BandOffsets = s1.getBandOffsets();
        short[][] s1Data = s1.getShortDataArrays();

        /* Second source color channels. */
        int s2LineStride = s2.getScanlineStride();
        int s2PixelStride = s2.getPixelStride();
        int[] s2BandOffsets = s2.getBandOffsets();
        short[][] s2Data = s2.getShortDataArrays();

        /* First source alpha channel. */
        int a1LineStride = a1.getScanlineStride();
        int a1PixelStride = a1.getPixelStride();
        int a1BandOffset = a1.getBandOffset(0);
        short[] a1Data = a1.getShortDataArray(0);

        /* Second source alpha channel (if any). */
        int a2LineStride = 0;
        int a2PixelStride = 0;
        int a2BandOffset = 0;
        short[] a2Data = null;
        if (alpha2 != null) {
            a2LineStride = a2.getScanlineStride();
            a2PixelStride = a2.getPixelStride();
            a2BandOffset = a2.getBandOffset(0);
            a2Data = a2.getShortDataArray(0);
        }

        /* Destination color channels. */
        int dLineStride = d.getScanlineStride();
        int dPixelStride = d.getPixelStride();
        int[] dBandOffsets = d.getBandOffsets();
        short[][] dData = d.getShortDataArrays();

        int dwidth = d.getWidth();
        int dheight = d.getHeight();
        int dbands = d.getNumBands();

        float invMax = 1.0F / 0xFFFF;

        int s1LineOffset = 0, s2LineOffset = 0,
            a1LineOffset = 0, a2LineOffset = 0,
            dLineOffset = 0,
            s1PixelOffset, s2PixelOffset,
            a1PixelOffset, a2PixelOffset,
            dPixelOffset;

        if (premultiplied) {
            /* dest = source1 + source2 * (1 - alpha1/max) */

            for (int h = 0; h < dheight; h++) {
                s1PixelOffset = s1LineOffset;
                s2PixelOffset = s2LineOffset;
                a1PixelOffset = a1LineOffset + a1BandOffset;
                dPixelOffset = dLineOffset;

                s1LineOffset += s1LineStride;
                s2LineOffset += s2LineStride;
                a1LineOffset += a1LineStride;
                dLineOffset += dLineStride;

                for (int w = 0; w < dwidth; w++) {
                    float t = 1.0F - (a1Data[a1PixelOffset] & 0xFFFF) * invMax;

                    /* Destination color channels. */
                    for (int b = 0; b < dbands; b++) {
                        dData[b][dPixelOffset+dBandOffsets[b]] = (short)
                     ((s1Data[b][s1PixelOffset+s1BandOffsets[b]] & 0xFFFF) +
                      (s2Data[b][s2PixelOffset+s2BandOffsets[b]] & 0xFFFF) * t);
                    }

                    s1PixelOffset += s1PixelStride;
                    s2PixelOffset += s2PixelStride;
                    a1PixelOffset += a1PixelStride;
                    dPixelOffset += dPixelStride;
                }
            }

        } else {
            if (alpha2 == null) {
                /* dest = source1 * alpha1/max + source2 * (1 - alpha1/max) */

                for (int h = 0; h < dheight; h++) {
                    s1PixelOffset = s1LineOffset;
                    s2PixelOffset = s2LineOffset;
                    a1PixelOffset = a1LineOffset + a1BandOffset;
                    dPixelOffset = dLineOffset;

                    s1LineOffset += s1LineStride;
                    s2LineOffset += s2LineStride;
                    a1LineOffset += a1LineStride;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {
                        float t1 = (a1Data[a1PixelOffset] & 0xFFFF) * invMax;
                        float t2 = 1.0F - t1;

                        /* Destination color channels. */
                        for (int b = 0; b < dbands; b++) {
                            dData[b][dPixelOffset+dBandOffsets[b]] = (short)
                    ((s1Data[b][s1PixelOffset+s1BandOffsets[b]] & 0xFFFF) * t1 +
                     (s2Data[b][s2PixelOffset+s2BandOffsets[b]] & 0xFFFF) * t2);
                        }

                        s1PixelOffset += s1PixelStride;
                        s2PixelOffset += s2PixelStride;
                        a1PixelOffset += a1PixelStride;
                        dPixelOffset += dPixelStride;
                    }
                }
            } else {
                /*
                 * dest = (source1 * alpha1 +
                 *         source2 * alpha2 * (1 - alpha1/maxValue)) /
                 *        (alpha1 + alpha2 * (1 - alpha1/maxValue))
                 */

                for (int h = 0; h < dheight; h++) {
                    s1PixelOffset = s1LineOffset;
                    s2PixelOffset = s2LineOffset;
                    a1PixelOffset = a1LineOffset + a1BandOffset;
                    a2PixelOffset = a2LineOffset + a2BandOffset;
                    dPixelOffset = dLineOffset;

                    s1LineOffset += s1LineStride;
                    s2LineOffset += s2LineStride;
                    a1LineOffset += a1LineStride;
                    a2LineOffset += a2LineStride;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {
                        int t1 = a1Data[a1PixelOffset] & 0xFFFF;
                        float t2 = (a2Data[a2PixelOffset] & 0xFFFF) *
                                   (1.0F - t1 * invMax);
                        float t3 = t1 + t2;
                        float t4, t5;
                        if (t3 == 0.0F) {
                            t4 = 0.0F;
                            t5 = 0.0F;
                        } else {
                            t4 = t1 / t3;
                            t5 = t2 / t3;
                        }

                        /* Destination color channels. */
                        for (int b = 0; b < dbands; b++) {
                            dData[b][dPixelOffset+dBandOffsets[b]] = (short)
                    ((s1Data[b][s1PixelOffset+s1BandOffsets[b]] & 0xFFFF) * t4 +
                     (s2Data[b][s2PixelOffset+s2BandOffsets[b]] & 0xFFFF) * t5);
                        }

                        s1PixelOffset += s1PixelStride;
                        s2PixelOffset += s2PixelStride;
                        a1PixelOffset += a1PixelStride;
                        a2PixelOffset += a2PixelStride;
                        dPixelOffset += dPixelStride;
                    }
                }
            }
        }
    }

    private void shortLoop(RasterAccessor s1, RasterAccessor s2,
                           RasterAccessor a1, RasterAccessor a2,
                           RasterAccessor d) {
        /* First source color channels. */
        int s1LineStride = s1.getScanlineStride();
        int s1PixelStride = s1.getPixelStride();
        int[] s1BandOffsets = s1.getBandOffsets();
        short[][] s1Data = s1.getShortDataArrays();

        /* Second source color channels. */
        int s2LineStride = s2.getScanlineStride();
        int s2PixelStride = s2.getPixelStride();
        int[] s2BandOffsets = s2.getBandOffsets();
        short[][] s2Data = s2.getShortDataArrays();

        /* First source alpha channel. */
        int a1LineStride = a1.getScanlineStride();
        int a1PixelStride = a1.getPixelStride();
        int a1BandOffset = a1.getBandOffset(0);
        short[] a1Data = a1.getShortDataArray(0);

        /* Second source alpha channel (if any). */
        int a2LineStride = 0;
        int a2PixelStride = 0;
        int a2BandOffset = 0;
        short[] a2Data = null;
        if (alpha2 != null) {
            a2LineStride = a2.getScanlineStride();
            a2PixelStride = a2.getPixelStride();
            a2BandOffset = a2.getBandOffset(0);
            a2Data = a2.getShortDataArray(0);
        }

        /* Destination color channels. */
        int dLineStride = d.getScanlineStride();
        int dPixelStride = d.getPixelStride();
        int[] dBandOffsets = d.getBandOffsets();
        short[][] dData = d.getShortDataArrays();

        int dwidth = d.getWidth();
        int dheight = d.getHeight();
        int dbands = d.getNumBands();

        float invMax = 1.0F / Short.MAX_VALUE;

        int s1LineOffset = 0, s2LineOffset = 0,
            a1LineOffset = 0, a2LineOffset = 0,
            dLineOffset = 0,
            s1PixelOffset, s2PixelOffset,
            a1PixelOffset, a2PixelOffset,
            dPixelOffset;

        if (premultiplied) {
            /* dest = source1 + source2 * (1 - alpha1/max) */

            for (int h = 0; h < dheight; h++) {
                s1PixelOffset = s1LineOffset;
                s2PixelOffset = s2LineOffset;
                a1PixelOffset = a1LineOffset + a1BandOffset;
                dPixelOffset = dLineOffset;

                s1LineOffset += s1LineStride;
                s2LineOffset += s2LineStride;
                a1LineOffset += a1LineStride;
                dLineOffset += dLineStride;

                for (int w = 0; w < dwidth; w++) {
                    float t = 1.0F - a1Data[a1PixelOffset] * invMax;

                    /* Destination color channels. */
                    for (int b = 0; b < dbands; b++) {
                        dData[b][dPixelOffset+dBandOffsets[b]] = (short)
                            (s1Data[b][s1PixelOffset+s1BandOffsets[b]] +
                             s2Data[b][s2PixelOffset+s2BandOffsets[b]] * t);
                    }

                    s1PixelOffset += s1PixelStride;
                    s2PixelOffset += s2PixelStride;
                    a1PixelOffset += a1PixelStride;
                    dPixelOffset += dPixelStride;
                }
            }

        } else {
            if (alpha2 == null) {
                /* dest = source1 * alpha1/max + source2 * (1 - alpha1/max) */

                for (int h = 0; h < dheight; h++) {
                    s1PixelOffset = s1LineOffset;
                    s2PixelOffset = s2LineOffset;
                    a1PixelOffset = a1LineOffset + a1BandOffset;
                    dPixelOffset = dLineOffset;

                    s1LineOffset += s1LineStride;
                    s2LineOffset += s2LineStride;
                    a1LineOffset += a1LineStride;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {
                        float t1 = a1Data[a1PixelOffset] * invMax;
                        float t2 = 1.0F - t1;

                        /* Destination color channels. */
                        for (int b = 0; b < dbands; b++) {
                            dData[b][dPixelOffset+dBandOffsets[b]] = (short)
                               (s1Data[b][s1PixelOffset+s1BandOffsets[b]] * t1 +
                                s2Data[b][s2PixelOffset+s2BandOffsets[b]] * t2);
                        }

                        s1PixelOffset += s1PixelStride;
                        s2PixelOffset += s2PixelStride;
                        a1PixelOffset += a1PixelStride;
                        dPixelOffset += dPixelStride;
                    }
                }
            } else {
                /*
                 * dest = (source1 * alpha1 +
                 *         source2 * alpha2 * (1 - alpha1/maxValue)) /
                 *        (alpha1 + alpha2 * (1 - alpha1/maxValue))
                 */

                for (int h = 0; h < dheight; h++) {
                    s1PixelOffset = s1LineOffset;
                    s2PixelOffset = s2LineOffset;
                    a1PixelOffset = a1LineOffset + a1BandOffset;
                    a2PixelOffset = a2LineOffset + a2BandOffset;
                    dPixelOffset = dLineOffset;

                    s1LineOffset += s1LineStride;
                    s2LineOffset += s2LineStride;
                    a1LineOffset += a1LineStride;
                    a2LineOffset += a2LineStride;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {
                        int t1 = a1Data[a1PixelOffset];
                        float t2 = a2Data[a2PixelOffset] * (1.0F - t1 * invMax);
                        float t3 = t1 + t2;
                        float t4, t5;
                        if (t3 == 0.0F) {
                            t4 = 0.0F;
                            t5 = 0.0F;
                        } else {
                            t4 = t1 / t3;
                            t5 = t2 / t3;
                        }

                        /* Destination color channels. */
                        for (int b = 0; b < dbands; b++) {
                            dData[b][dPixelOffset+dBandOffsets[b]] = (short)
                               (s1Data[b][s1PixelOffset+s1BandOffsets[b]] * t4 +
                                s2Data[b][s2PixelOffset+s2BandOffsets[b]] * t5);
                        }

                        s1PixelOffset += s1PixelStride;
                        s2PixelOffset += s2PixelStride;
                        a1PixelOffset += a1PixelStride;
                        a2PixelOffset += a2PixelStride;
                        dPixelOffset += dPixelStride;
                    }
                }
            }
        }
    }

    private void intLoop(RasterAccessor s1, RasterAccessor s2,
                         RasterAccessor a1, RasterAccessor a2,
                         RasterAccessor d) {
        /* First source color channels. */
        int s1LineStride = s1.getScanlineStride();
        int s1PixelStride = s1.getPixelStride();
        int[] s1BandOffsets = s1.getBandOffsets();
        int[][] s1Data = s1.getIntDataArrays();

        /* Second source color channels. */
        int s2LineStride = s2.getScanlineStride();
        int s2PixelStride = s2.getPixelStride();
        int[] s2BandOffsets = s2.getBandOffsets();
        int[][] s2Data = s2.getIntDataArrays();

        /* First source alpha channel. */
        int a1LineStride = a1.getScanlineStride();
        int a1PixelStride = a1.getPixelStride();
        int a1BandOffset = a1.getBandOffset(0);
        int[] a1Data = a1.getIntDataArray(0);

        /* Second source alpha channel (if any). */
        int a2LineStride = 0;
        int a2PixelStride = 0;
        int a2BandOffset = 0;
        int[] a2Data = null;
        if (alpha2 != null) {
            a2LineStride = a2.getScanlineStride();
            a2PixelStride = a2.getPixelStride();
            a2BandOffset = a2.getBandOffset(0);
            a2Data = a2.getIntDataArray(0);
        }

        /* Destination color channels. */
        int dLineStride = d.getScanlineStride();
        int dPixelStride = d.getPixelStride();
        int[] dBandOffsets = d.getBandOffsets();
        int[][] dData = d.getIntDataArrays();

        int dwidth = d.getWidth();
        int dheight = d.getHeight();
        int dbands = d.getNumBands();

        float invMax = 1.0F / Integer.MAX_VALUE;

        int s1LineOffset = 0, s2LineOffset = 0,
            a1LineOffset = 0, a2LineOffset = 0,
            dLineOffset = 0,
            s1PixelOffset, s2PixelOffset,
            a1PixelOffset, a2PixelOffset,
            dPixelOffset;

        if (premultiplied) {
            /* dest = source1 + source2 * (1 - alpha1/max) */

            for (int h = 0; h < dheight; h++) {
                s1PixelOffset = s1LineOffset;
                s2PixelOffset = s2LineOffset;
                a1PixelOffset = a1LineOffset + a1BandOffset;
                dPixelOffset = dLineOffset;

                s1LineOffset += s1LineStride;
                s2LineOffset += s2LineStride;
                a1LineOffset += a1LineStride;
                dLineOffset += dLineStride;

                for (int w = 0; w < dwidth; w++) {
                    float t = 1.0F - a1Data[a1PixelOffset] * invMax;

                    /* Destination color channels. */
                    for (int b = 0; b < dbands; b++) {
                        dData[b][dPixelOffset+dBandOffsets[b]] = (int)
                            (s1Data[b][s1PixelOffset+s1BandOffsets[b]] +
                             s2Data[b][s2PixelOffset+s2BandOffsets[b]] * t);
                    }

                    s1PixelOffset += s1PixelStride;
                    s2PixelOffset += s2PixelStride;
                    a1PixelOffset += a1PixelStride;
                    dPixelOffset += dPixelStride;
                }
            }

        } else {
            if (alpha2 == null) {
                /* dest = source1 * alpha1/max + source2 * (1 - alpha1/max) */

                for (int h = 0; h < dheight; h++) {
                    s1PixelOffset = s1LineOffset;
                    s2PixelOffset = s2LineOffset;
                    a1PixelOffset = a1LineOffset + a1BandOffset;
                    dPixelOffset = dLineOffset;

                    s1LineOffset += s1LineStride;
                    s2LineOffset += s2LineStride;
                    a1LineOffset += a1LineStride;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {
                        float t1 = a1Data[a1PixelOffset] * invMax;
                        float t2 = 1.0F - t1;

                        /* Destination color channels. */
                        for (int b = 0; b < dbands; b++) {
                            dData[b][dPixelOffset+dBandOffsets[b]] = (int)
                               (s1Data[b][s1PixelOffset+s1BandOffsets[b]] * t1 +
                                s2Data[b][s2PixelOffset+s2BandOffsets[b]] * t2);
                        }

                        s1PixelOffset += s1PixelStride;
                        s2PixelOffset += s2PixelStride;
                        a1PixelOffset += a1PixelStride;
                        dPixelOffset += dPixelStride;
                    }
                }
            } else {
                /*
                 * dest = (source1 * alpha1 +
                 *         source2 * alpha2 * (1 - alpha1/maxValue)) /
                 *        (alpha1 + alpha2 * (1 - alpha1/maxValue))
                 */

                for (int h = 0; h < dheight; h++) {
                    s1PixelOffset = s1LineOffset;
                    s2PixelOffset = s2LineOffset;
                    a1PixelOffset = a1LineOffset + a1BandOffset;
                    a2PixelOffset = a2LineOffset + a2BandOffset;
                    dPixelOffset = dLineOffset;

                    s1LineOffset += s1LineStride;
                    s2LineOffset += s2LineStride;
                    a1LineOffset += a1LineStride;
                    a2LineOffset += a2LineStride;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {
                        int t1 = a1Data[a1PixelOffset];
                        float t2 = a2Data[a2PixelOffset] * (1.0F - t1 * invMax);
                        float t3 = t1 + t2;
                        float t4, t5;
                        if (t3 == 0.0F) {
                            t4 = 0.0F;
                            t5 = 0.0F;
                        } else {
                            t4 = t1 / t3;
                            t5 = t2 / t3;
                        }

                        /* Destination color channels. */
                        for (int b = 0; b < dbands; b++) {
                            dData[b][dPixelOffset+dBandOffsets[b]] = (int)
                               (s1Data[b][s1PixelOffset+s1BandOffsets[b]] * t4 +
                                s2Data[b][s2PixelOffset+s2BandOffsets[b]] * t5);
                        }

                        s1PixelOffset += s1PixelStride;
                        s2PixelOffset += s2PixelStride;
                        a1PixelOffset += a1PixelStride;
                        a2PixelOffset += a2PixelStride;
                        dPixelOffset += dPixelStride;
                    }
                }
            }
        }
    }

    private void floatLoop(RasterAccessor s1, RasterAccessor s2,
                           RasterAccessor a1, RasterAccessor a2,
                           RasterAccessor d) {
        /* First source color channels. */
        int s1LineStride = s1.getScanlineStride();
        int s1PixelStride = s1.getPixelStride();
        int[] s1BandOffsets = s1.getBandOffsets();
        float[][] s1Data = s1.getFloatDataArrays();

        /* Second source color channels. */
        int s2LineStride = s2.getScanlineStride();
        int s2PixelStride = s2.getPixelStride();
        int[] s2BandOffsets = s2.getBandOffsets();
        float[][] s2Data = s2.getFloatDataArrays();

        /* First source alpha channel. */
        int a1LineStride = a1.getScanlineStride();
        int a1PixelStride = a1.getPixelStride();
        int a1BandOffset = a1.getBandOffset(0);
        float[] a1Data = a1.getFloatDataArray(0);

        /* Second source alpha channel (if any). */
        int a2LineStride = 0;
        int a2PixelStride = 0;
        int a2BandOffset = 0;
        float[] a2Data = null;
        if (alpha2 != null) {
            a2LineStride = a2.getScanlineStride();
            a2PixelStride = a2.getPixelStride();
            a2BandOffset = a2.getBandOffset(0);
            a2Data = a2.getFloatDataArray(0);
        }

        /* Destination color channels. */
        int dLineStride = d.getScanlineStride();
        int dPixelStride = d.getPixelStride();
        int[] dBandOffsets = d.getBandOffsets();
        float[][] dData = d.getFloatDataArrays();

        int dwidth = d.getWidth();
        int dheight = d.getHeight();
        int dbands = d.getNumBands();

        int s1LineOffset = 0, s2LineOffset = 0,
            a1LineOffset = 0, a2LineOffset = 0,
            dLineOffset = 0,
            s1PixelOffset, s2PixelOffset,
            a1PixelOffset, a2PixelOffset,
            dPixelOffset;

        if (premultiplied) {
            /* dest = source1 + source2 * (1 - alpha1) */

            for (int h = 0; h < dheight; h++) {
                s1PixelOffset = s1LineOffset;
                s2PixelOffset = s2LineOffset;
                a1PixelOffset = a1LineOffset + a1BandOffset;
                dPixelOffset = dLineOffset;

                s1LineOffset += s1LineStride;
                s2LineOffset += s2LineStride;
                a1LineOffset += a1LineStride;
                dLineOffset += dLineStride;

                for (int w = 0; w < dwidth; w++) {
                    float t = 1.0F - a1Data[a1PixelOffset];

                    /* Destination color channels. */
                    for (int b = 0; b < dbands; b++) {
                        dData[b][dPixelOffset+dBandOffsets[b]] =
                            s1Data[b][s1PixelOffset+s1BandOffsets[b]] +
                            s2Data[b][s2PixelOffset+s2BandOffsets[b]] * t;
                    }

                    s1PixelOffset += s1PixelStride;
                    s2PixelOffset += s2PixelStride;
                    a1PixelOffset += a1PixelStride;
                    dPixelOffset += dPixelStride;
                }
            }

        } else {
            if (alpha2 == null) {
                /* dest = source1 * alpha1 + source2 * (1 - alpha1) */

                for (int h = 0; h < dheight; h++) {
                    s1PixelOffset = s1LineOffset;
                    s2PixelOffset = s2LineOffset;
                    a1PixelOffset = a1LineOffset + a1BandOffset;
                    dPixelOffset = dLineOffset;

                    s1LineOffset += s1LineStride;
                    s2LineOffset += s2LineStride;
                    a1LineOffset += a1LineStride;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {
                        float t1 = a1Data[a1PixelOffset];
                        float t2 = 1.0F - t1;

                        /* Destination color channels. */
                        for (int b = 0; b < dbands; b++) {
                            dData[b][dPixelOffset+dBandOffsets[b]] =
                                s1Data[b][s1PixelOffset+s1BandOffsets[b]] * t1 +
                                s2Data[b][s2PixelOffset+s2BandOffsets[b]] * t2;
                        }

                        s1PixelOffset += s1PixelStride;
                        s2PixelOffset += s2PixelStride;
                        a1PixelOffset += a1PixelStride;
                        dPixelOffset += dPixelStride;
                    }
                }
            } else {
                /*
                 * dest = (source1 * alpha1 + source2 * alpha2 * (1 - alpha1)) /
                 *        (alpha1 + alpha2 * (1 - alpha1))
                 */

                for (int h = 0; h < dheight; h++) {
                    s1PixelOffset = s1LineOffset;
                    s2PixelOffset = s2LineOffset;
                    a1PixelOffset = a1LineOffset + a1BandOffset;
                    a2PixelOffset = a2LineOffset + a2BandOffset;
                    dPixelOffset = dLineOffset;

                    s1LineOffset += s1LineStride;
                    s2LineOffset += s2LineStride;
                    a1LineOffset += a1LineStride;
                    a2LineOffset += a2LineStride;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {
                        float t1 = a1Data[a1PixelOffset];
                        float t2 = a2Data[a2PixelOffset] * (1.0F - t1);
                        float t3 = t1 + t2;
                        float t4, t5;
                        if (t3 == 0.0F) {
                            t4 = 0.0F;
                            t5 = 0.0F;
                        } else {
                            t4 = t1 / t3;
                            t5 = t2 / t3;
                        }

                        /* Destination color channels. */
                        for (int b = 0; b < dbands; b++) {
                            dData[b][dPixelOffset+dBandOffsets[b]] =
                                s1Data[b][s1PixelOffset+s1BandOffsets[b]] * t4 +
                                s2Data[b][s2PixelOffset+s2BandOffsets[b]] * t5;
                        }

                        s1PixelOffset += s1PixelStride;
                        s2PixelOffset += s2PixelStride;
                        a1PixelOffset += a1PixelStride;
                        a2PixelOffset += a2PixelStride;
                        dPixelOffset += dPixelStride;
                    }
                }
            }
        }
    }

    private void doubleLoop(RasterAccessor s1, RasterAccessor s2,
                            RasterAccessor a1, RasterAccessor a2,
                            RasterAccessor d) {
        /* First source color channels. */
        int s1LineStride = s1.getScanlineStride();
        int s1PixelStride = s1.getPixelStride();
        int[] s1BandOffsets = s1.getBandOffsets();
        double[][] s1Data = s1.getDoubleDataArrays();

        /* Second source color channels. */
        int s2LineStride = s2.getScanlineStride();
        int s2PixelStride = s2.getPixelStride();
        int[] s2BandOffsets = s2.getBandOffsets();
        double[][] s2Data = s2.getDoubleDataArrays();

        /* First source alpha channel. */
        int a1LineStride = a1.getScanlineStride();
        int a1PixelStride = a1.getPixelStride();
        int a1BandOffset = a1.getBandOffset(0);
        double[] a1Data = a1.getDoubleDataArray(0);

        /* Second source alpha channel (if any). */
        int a2LineStride = 0;
        int a2PixelStride = 0;
        int a2BandOffset = 0;
        double[] a2Data = null;
        if (alpha2 != null) {
            a2LineStride = a2.getScanlineStride();
            a2PixelStride = a2.getPixelStride();
            a2BandOffset = a2.getBandOffset(0);
            a2Data = a2.getDoubleDataArray(0);
        }

        /* Destination color channels. */
        int dLineStride = d.getScanlineStride();
        int dPixelStride = d.getPixelStride();
        int[] dBandOffsets = d.getBandOffsets();
        double[][] dData = d.getDoubleDataArrays();

        int dwidth = d.getWidth();
        int dheight = d.getHeight();
        int dbands = d.getNumBands();

        int s1LineOffset = 0, s2LineOffset = 0,
            a1LineOffset = 0, a2LineOffset = 0,
            dLineOffset = 0,
            s1PixelOffset, s2PixelOffset,
            a1PixelOffset, a2PixelOffset,
            dPixelOffset;

        if (premultiplied) {
            /* dest = source1 + source2 * (1 - alpha1) */

            for (int h = 0; h < dheight; h++) {
                s1PixelOffset = s1LineOffset;
                s2PixelOffset = s2LineOffset;
                a1PixelOffset = a1LineOffset + a1BandOffset;
                dPixelOffset = dLineOffset;

                s1LineOffset += s1LineStride;
                s2LineOffset += s2LineStride;
                a1LineOffset += a1LineStride;
                dLineOffset += dLineStride;

                for (int w = 0; w < dwidth; w++) {
                    double t = 1.0 - a1Data[a1PixelOffset];

                    /* Destination color channels. */
                    for (int b = 0; b < dbands; b++) {
                        dData[b][dPixelOffset+dBandOffsets[b]] =
                            s1Data[b][s1PixelOffset+s1BandOffsets[b]] +
                            s2Data[b][s2PixelOffset+s2BandOffsets[b]] * t;
                    }

                    s1PixelOffset += s1PixelStride;
                    s2PixelOffset += s2PixelStride;
                    a1PixelOffset += a1PixelStride;
                    dPixelOffset += dPixelStride;
                }
            }

        } else {
            if (alpha2 == null) {
                /* dest = source1 * alpha1 + source2 * (1 - alpha1) */

                for (int h = 0; h < dheight; h++) {
                    s1PixelOffset = s1LineOffset;
                    s2PixelOffset = s2LineOffset;
                    a1PixelOffset = a1LineOffset + a1BandOffset;
                    dPixelOffset = dLineOffset;

                    s1LineOffset += s1LineStride;
                    s2LineOffset += s2LineStride;
                    a1LineOffset += a1LineStride;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {
                        double t1 = a1Data[a1PixelOffset];
                        double t2 = 1.0 - t1;

                        /* Destination color channels. */
                        for (int b = 0; b < dbands; b++) {
                            dData[b][dPixelOffset+dBandOffsets[b]] =
                                s1Data[b][s1PixelOffset+s1BandOffsets[b]] * t1 +
                                s2Data[b][s2PixelOffset+s2BandOffsets[b]] * t2;
                        }

                        s1PixelOffset += s1PixelStride;
                        s2PixelOffset += s2PixelStride;
                        a1PixelOffset += a1PixelStride;
                        dPixelOffset += dPixelStride;
                    }
                }
            } else {
                /*
                 * dest = (source1 * alpha1 + source2 * alpha2 * (1 - alpha1)) /
                 *        (alpha1 + alpha2 * (1 - alpha1))
                 */

                for (int h = 0; h < dheight; h++) {
                    s1PixelOffset = s1LineOffset;
                    s2PixelOffset = s2LineOffset;
                    a1PixelOffset = a1LineOffset + a1BandOffset;
                    a2PixelOffset = a2LineOffset + a2BandOffset;
                    dPixelOffset = dLineOffset;

                    s1LineOffset += s1LineStride;
                    s2LineOffset += s2LineStride;
                    a1LineOffset += a1LineStride;
                    a2LineOffset += a2LineStride;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {
                        double t1 = a1Data[a1PixelOffset];
                        double t2 = a2Data[a2PixelOffset] * (1.0 - t1);
                        double t3 = t1 + t2;
                        double t4, t5;
                        if (t3 == 0.0) {
                            t4 = 0.0;
                            t5 = 0.0;
                        } else {
                            t4 = t1 / t3;
                            t5 = t2 / t3;
                        }

                        /* Destination color channels. */
                        for (int b = 0; b < dbands; b++) {
                            dData[b][dPixelOffset+dBandOffsets[b]] =
                                s1Data[b][s1PixelOffset+s1BandOffsets[b]] * t4 +
                                s2Data[b][s2PixelOffset+s2BandOffsets[b]] * t5;
                        }

                        s1PixelOffset += s1PixelStride;
                        s2PixelOffset += s2PixelStride;
                        a1PixelOffset += a1PixelStride;
                        a2PixelOffset += a2PixelStride;
                        dPixelOffset += dPixelStride;
                    }
                }
            }
        }
    }

    /** Returns the format tags to be used with <code>RasterAccessor</code>. */
    protected synchronized RasterFormatTag[] getFormatTags() {
        RenderedImage[] ri;
        if (alpha2 == null) {
            ri = new RenderedImage[3];
        } else {
            ri = new RenderedImage[4];
            ri[3] = alpha2;
        }
        ri[0] = getSourceImage(0);
        ri[1] = getSourceImage(1);
        ri[2] = alpha1;

        return RasterAccessor.findCompatibleTags(ri, this);
    }
}
