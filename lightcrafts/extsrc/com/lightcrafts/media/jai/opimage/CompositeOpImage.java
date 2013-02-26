/*
 * $RCSfile: CompositeOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/12/08 00:58:33 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PointOpImage;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import com.lightcrafts.mediax.jai.RasterFactory;
import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.JDKWorkarounds;
// import com.lightcrafts.media.jai.test.OpImageTester;

/**
 * An <code>OpImage</code> implementing the "Composite" operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.CompositeDescriptor</code>.
 *
 * <p>For two source images <code>src1</code> and <code>src2</code>,
 * this <code>OpImage</code> places the foreground <code>src1</code>
 * in front of the background <code>src2</code>. This is what commonly
 * known as the "over" composite.
 *
 * <p>The destination image contains both the alpha channel and the
 * color channels. The alpha channel index is determined by the parameter
 * <code>alphaFirst</code>: if true, alpha is the first channel; if false,
 * alpha is the last channel. The formulas used to calculate destination
 * alpha and color values are:
 * <pre>
 * dstAlpha = src1Alpha + src2Alpha * (1 - src1Alpha)
 * dstColor = src1Color + src2Color * (1 - src1Alpha)
 * </pre>
 * where alpha values are in fraction format, and color values are alpha
 * pre-multiplied.
 *
 * <p>The following assumptions are made:
 * <li>The source images, their alpha images, and the destination image all
 * have the same data type.</li>
 * <li>The two source images have the same number of bands and should only
 * contain the color channels.</li>
 * <li>The alpha images are of the same dimension as their cooresponding source
 * image and should be single-banded. If a multi-banded image is supplied, the
 * default band (band 0) is used.</li>
 * <li>If <code>alphaPremultiplied</code> is true, both the source and
 * destination images have alpha pre-multiplied, and vice versa.</li>
 * <li>The destination image must have at least one extra band than the two
 * sources, which represents the alpha channel. It may be user-specified
 * to be the first or the last band of the pixel data.</li>
 *
 * @see com.lightcrafts.mediax.jai.operator.CompositeDescriptor
 * @see CompositeCRIF
 *
 */
final class CompositeOpImage extends PointOpImage {

    /** The alpha image that overrides the alpha for source1. */
    protected RenderedImage source1Alpha;

    /** The alpha image that overrides the alpha for source2. */
    protected RenderedImage source2Alpha;

    /** Indicates whether alpha has been premultiplied. */
    protected boolean alphaPremultiplied;

    /** The alpha and color band offset. */
    private int aOffset;	// alpha channel offset
    private int cOffset;	// color channels offset

    /** Maximum Value supported by the data type if it's integral types. */
    private byte maxValueByte;
    private short maxValueShort;
    private int maxValue;
    private float invMaxValue;	// 1 / maxValue

    /**
     * Constructs an <code>CompositeOpImage</code>.
     *
     * @param source1             The foreground source image.
     * @param source2             The background source image.
     * @param layout              The destination image layout.
     * @param source1Alpha        The alpha image that overrides the
     *                            alpha for source1; may not be null.
     * @param source2Alpha        The alpha image that overrides the alpha for
     *                            source2; may be null, in which case source2
     *                            is considered completely opaque.
     * @param alphaPremultiplied  Indicates whether alpha has been
     *                            premultiplied to both sources.
     * @param alphaFirst          If true, alpha is the first band (band 0) in
     *                            destination image; if false, alpha is the
     *                            last band.
     */
    public CompositeOpImage(RenderedImage source1,
                            RenderedImage source2,
                            Map config,
                            ImageLayout layout,
                            RenderedImage source1Alpha,
                            RenderedImage source2Alpha,
                            boolean alphaPremultiplied,
                            boolean alphaFirst) {
        super(source1, source2, layout, config, true);

        this.source1Alpha = source1Alpha;
        this.source2Alpha = source2Alpha;
        this.alphaPremultiplied = alphaPremultiplied;

        SampleModel sm = source1.getSampleModel();
        ColorModel cm = source1.getColorModel();
        int dtype = sm.getTransferType();
        int bands;
        if (cm instanceof IndexColorModel) {
            bands = cm.getNumComponents();
        } else {
            bands = sm.getNumBands();
        }
        bands += 1;                             // one additional alpha channel

        if (sampleModel.getTransferType() != dtype ||
            sampleModel.getNumBands() != bands) {
            /*
             * The current destination sampleModel is not suitable for the
             * two sources and their alpha images.
             * Create a suitable sampleModel for the destination image.
             */
            sampleModel = RasterFactory.createComponentSampleModel(sampleModel,
                          dtype, tileWidth, tileHeight, bands);

            if(colorModel != null &&
               !JDKWorkarounds.areCompatibleDataModels(sampleModel,
                                                       colorModel)) {
                colorModel = ImageUtil.getCompatibleColorModel(sampleModel,
                                                               config);
            }
        }

        aOffset = alphaFirst ? 0 : bands - 1;
        cOffset = alphaFirst ? 1 : 0;

        switch (dtype) {
        case DataBuffer.TYPE_BYTE:
            maxValue = 0xFF;		// byte is unsigned
            maxValueByte = (byte)0xFF;
            break;
        case DataBuffer.TYPE_USHORT:
            maxValue = 0xFFFF;
            maxValueShort = (short)0xFFFF;
            break;
        case DataBuffer.TYPE_SHORT:
            maxValue = Short.MAX_VALUE;
            maxValueShort = Short.MAX_VALUE;
            break;
        case DataBuffer.TYPE_INT:
            maxValue = Integer.MAX_VALUE;
            break;
        default:
        }
        invMaxValue = 1.0F / maxValue;
    }

    /**
     * Composites two images within a specified rectangle.
     *
     * @param sources   Cobbled source, guaranteed to provide all the
     *                  source data necessary for computing the rectangle.
     * @param dest      The tile containing the rectangle to be computed.
     * @param destRect  The rectangle within the tile to be computed.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        /* For PointOpImage, srcRect = destRect. */
        RenderedImage[] renderedSources = 
               source2Alpha == null ? new RenderedImage[3] : 
                                      new RenderedImage[4];
        renderedSources[0] = getSourceImage(0);
        renderedSources[1] = getSourceImage(1);
        renderedSources[2] = source1Alpha;
        Raster source1AlphaRaster = source1Alpha.getData(destRect);
        Raster source2AlphaRaster = null;
        if (source2Alpha != null) {
            renderedSources[3] = source2Alpha;
            source2AlphaRaster = source2Alpha.getData(destRect);    
        }

        RasterFormatTag tags[] = 
            RasterAccessor.findCompatibleTags(renderedSources, this);
        RasterAccessor s1 = new RasterAccessor(sources[0], destRect, 
                                          tags[0],getSourceImage(0).getColorModel());
        RasterAccessor s2 = new RasterAccessor(sources[1], destRect, 
                                          tags[1],getSourceImage(1).getColorModel());
        RasterAccessor a1 = new RasterAccessor(source1AlphaRaster, 
                                               destRect, tags[2],
                                               source1Alpha.getColorModel());
        RasterAccessor a2=null,d=null;

        if (source2Alpha != null) {
            a2 = new RasterAccessor(source2AlphaRaster, destRect, 
                                    tags[3], source2Alpha.getColorModel());
            d = new RasterAccessor(dest, destRect,
                                   tags[4], this.getColorModel());
        } else {
            a2 = null;
            d = new RasterAccessor(dest, destRect, 
                                   tags[3], this.getColorModel());
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
        d.copyDataToRaster();
    }

    /*
     * Formulas for integral data types:
     *
     * d[alpha] = dstAlpha * maxValue
     *          = (src1Alpha + src2Alpha * (1 - src1Alpha)) * maxValue
     * if (source2 is opaque (src2Alpha = 1)) {
     *     d[alpha] = (src1Alpha + 1 * (1 - src1Alpha)) * maxValue
     *              = maxValue
     * } else {
     *     d[alpha] = (a1/maxValue + a2/maxValue * (1 - a1/maxValue)) * maxValue
     *              = a1 + a2 * (1 - a1/maxValue)
     * }
     *
     * if (alpha pre-multiplied to sources and destination) {
     *     d[color] = dstColor
     *              = src1Color + src2Color * (1 - src1Alpha)
     *              = s1 + s2 * (1 - a1/maxValue)
     * } else {
     *     if (source2 is opaque (src2Alpha = 1 & dstAlpha = 1)) {
     *         d[color] = dstColor / dstAlpha
     *                  = (src1Color + src2Color * (1 - src1Alpha))
     *                  = s1 * a1/maxValue + s2 * (1 - a1/maxValue)
     *     } else {
     *         d[color] = dstColor / dstAlpha
     *                  = (src1Color + src2Color * (1 - src1Alpha)) / dstAlpha
     *                  = (s1 * a1/maxValue + s2 * a2/maxValue *
     *                    (1 - a1/maxValue)) / (d[alpha]/maxValue)
     *                  = (s1 * a1 + s2 * a2 * (1 - a1/maxValue)) / d[alpha]
     *     }
     * }
     */

    private void byteLoop(RasterAccessor src1, RasterAccessor src2,
                          RasterAccessor afa1, RasterAccessor afa2,
                          RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int numBands = src1.getNumBands();

        /* First source color channels. */
        byte[][] s1 = src1.getByteDataArrays();
        int s1ss = src1.getScanlineStride();	// scanline stride
        int s1ps = src1.getPixelStride();	// pixel stride
        int[] s1bo = src1.getBandOffsets();	// band offsets

        /* Second source color channels. */
        byte[][] s2 = src2.getByteDataArrays();
        int s2ss = src2.getScanlineStride();	// scanline stride
        int s2ps = src2.getPixelStride();	// pixel stride
        int[] s2bo = src2.getBandOffsets();	// band offsets

        /* First source alpha channel. */
        byte[] a1 = afa1.getByteDataArray(0);	// use band 0
        int a1ss = afa1.getScanlineStride();	// scanline stride
        int a1ps = afa1.getPixelStride();	// pixel stride
        int a1bo = afa1.getBandOffset(0);	// band 0 offsets

        /* Second source alpha channel (if any). */
        byte[] a2 = null;
        int a2ss = 0;
        int a2ps = 0;
        int a2bo = 0;
        if (afa2 != null) {
            a2 = afa2.getByteDataArray(0);	// use band 0
            a2ss = afa2.getScanlineStride();	// scanline stride
            a2ps = afa2.getPixelStride();	// pixel stride
            a2bo = afa2.getBandOffset(0);	// band 0 offset
        }

        /* Destination color and alpha channels. */
        byte[][] d = dst.getByteDataArrays();
        int dss = dst.getScanlineStride();	// scanline stride
        int dps = dst.getPixelStride();	// pixel stride
        int[] dbo = dst.getBandOffsets();	// band offsets

        int s1so = 0, s2so = 0, a1so = 0, a2so = 0, dso = 0;
        int s1po, s2po, a1po, a2po, dpo;	// po = pixel offset
        if (alphaPremultiplied) {
            if (afa2 == null) {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        float t = 1.0F - (a1[a1po+a1bo] & 0xFF) * invMaxValue;

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = maxValueByte;

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = (byte)
                                ((s1[b][s1po+s1bo[b]] & 0xFF) +
                                 (s2[b][s2po+s2bo[b]] & 0xFF) * t);
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    dso += dss;
                }
            } else {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    a2po = a2so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        int t1 = a1[a1po+a1bo] & 0xFF;	// a1
                        float t2 = 1.0F - t1 * invMaxValue;	// 1-a1/maxValue

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = (byte)
                            (t1 + (a2[a2po+a2bo] & 0xFF) * t2);

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = (byte)
                                ((s1[b][s1po+s1bo[b]] & 0xFF) +
                                 (s2[b][s2po+s2bo[b]] & 0xFF) * t2);
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        a2po += a2ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    a2so += a2ss;
                    dso += dss;
                }
            }
        } else {
            if (afa2 == null) {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        float t1 = (a1[a1po+a1bo] & 0xFF) * invMaxValue;
                        float t2 = 1.0F - t1;		// 1-a1/maxValue

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = maxValueByte;

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = (byte)
                                ((s1[b][s1po+s1bo[b]] & 0xFF) * t1 +
                                 (s2[b][s2po+s2bo[b]] & 0xFF) * t2);
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    dso += dss;
                }
            } else {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    a2po = a2so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        int t1 = a1[a1po+a1bo] & 0xFF;	// a1
                        float t2 = (1.0F - t1 * invMaxValue) *
                                   (a2[a2po+a2bo] & 0xFF); // a2*(1-a1/maxValue)
                        float t3 = t1 + t2;		// d[alpha]
                        float t4, t5;
                        if (t3 == 0.0F) {
                            t4 = 0.0F;
                            t5 = 0.0F;
                        } else {
                            t4 = t1 / t3;
                            t5 = t2 / t3;
                        }

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = (byte)t3;

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = (byte)
                                ((s1[b][s1po+s1bo[b]] & 0xFF) * t4 +
                                 (s2[b][s2po+s2bo[b]] & 0xFF) * t5);
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        a2po += a2ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    a2so += a2ss;
                    dso += dss;
                }
            }
        }
    }

    private void ushortLoop(RasterAccessor src1, RasterAccessor src2,
                            RasterAccessor afa1, RasterAccessor afa2,
                            RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int numBands = src1.getNumBands();

        /* First source color channels. */
        short[][] s1 = src1.getShortDataArrays();
        int s1ss = src1.getScanlineStride();	// scanline stride
        int s1ps = src1.getPixelStride();	// pixel stride
        int[] s1bo = src1.getBandOffsets();	// band offsets

        /* Second source color channels. */
        short[][] s2 = src2.getShortDataArrays();
        int s2ss = src2.getScanlineStride();	// scanline stride
        int s2ps = src2.getPixelStride();	// pixel stride
        int[] s2bo = src2.getBandOffsets();	// band offsets

        /* First source alpha channel. */
        short[] a1 = afa1.getShortDataArray(0);	// use band 0
        int a1ss = afa1.getScanlineStride();	// scanline stride
        int a1ps = afa1.getPixelStride();	// pixel stride
        int a1bo = afa1.getBandOffset(0);	// band 0 offsets

        /* Second source alpha channel (if any). */
        short[] a2 = null;
        int a2ss = 0;
        int a2ps = 0;
        int a2bo = 0;
        if (afa2 != null) {
            a2 = afa2.getShortDataArray(0);	// use band 0
            a2ss = afa2.getScanlineStride();	// scanline stride
            a2ps = afa2.getPixelStride();	// pixel stride
            a2bo = afa2.getBandOffset(0);	// band 0 offset
        }

        /* Destination color and alpha channels. */
        short[][] d = dst.getShortDataArrays();
        int dss = dst.getScanlineStride();	// scanline stride
        int dps = dst.getPixelStride();	// pixel stride
        int[] dbo = dst.getBandOffsets();	// band offsets

        int s1so = 0, s2so = 0, a1so = 0, a2so = 0, dso = 0;
        int s1po, s2po, a1po, a2po, dpo;	// po = pixel offset
        if (alphaPremultiplied) {
            if (afa2 == null) {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        float t = 1.0F - (a1[a1po+a1bo] & 0xFFFF) * invMaxValue;

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = maxValueShort;

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = (short)
                                ((s1[b][s1po+s1bo[b]] & 0xFFFF) +
                                 (s2[b][s2po+s2bo[b]] & 0xFFFF) * t);
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    dso += dss;
                }
            } else {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    a2po = a2so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        int t1 = a1[a1po+a1bo] & 0xFFFF;	// a1
                        float t2 = 1.0F - t1 * invMaxValue;	// 1-a1/maxValue

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = (short)
                            (t1 + (a2[a2po+a2bo] & 0xFFFF) * t2);

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = (short)
                                ((s1[b][s1po+s1bo[b]] & 0xFFFF) +
                                 (s2[b][s2po+s2bo[b]] & 0xFFFF) * t2);
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        a2po += a2ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    a2so += a2ss;
                    dso += dss;
                }
            }
        } else {
            if (afa2 == null) {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        float t1 = (a1[a1po+a1bo] & 0xFFFF) * invMaxValue;
                        float t2 = 1.0F - t1;		// 1-a1/maxValue

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = maxValueShort;

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = (short)
                                ((s1[b][s1po+s1bo[b]] & 0xFFFF) * t1 +
                                 (s2[b][s2po+s2bo[b]] & 0xFFFF) * t2);
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    dso += dss;
                }
            } else {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    a2po = a2so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        int t1 = a1[a1po+a1bo] & 0xFFFF;	// a1
                        float t2 = (1.0F - t1 * invMaxValue) *
                                   (a2[a2po+a2bo] & 0xFFFF); // a2*(1-a1/maxValue)
                        float t3 = t1 + t2;		// d[alpha]
                        float t4, t5;
                        if (t3 == 0.0F) {
                            t4 = 0.0F;
                            t5 = 0.0F;
                        } else {
                            t4 = t1 / t3;
                            t5 = t2 / t3;
                        }

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = (short)t3;

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = (short)
                                ((s1[b][s1po+s1bo[b]] & 0xFFFF) * t4 +
                                 (s2[b][s2po+s2bo[b]] & 0xFFFF) * t5);
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        a2po += a2ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    a2so += a2ss;
                    dso += dss;
                }
            }
        }
    }

    private void shortLoop(RasterAccessor src1, RasterAccessor src2,
                           RasterAccessor afa1, RasterAccessor afa2,
                           RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int numBands = src1.getNumBands();

        /* First source color channels. */
        short[][] s1 = src1.getShortDataArrays();
        int s1ss = src1.getScanlineStride();	// scanline stride
        int s1ps = src1.getPixelStride();	// pixel stride
        int[] s1bo = src1.getBandOffsets();	// band offsets

        /* Second source color channels. */
        short[][] s2 = src2.getShortDataArrays();
        int s2ss = src2.getScanlineStride();	// scanline stride
        int s2ps = src2.getPixelStride();	// pixel stride
        int[] s2bo = src2.getBandOffsets();	// band offsets

        /* First source alpha channel. */
        short[] a1 = afa1.getShortDataArray(0);	// use band 0
        int a1ss = afa1.getScanlineStride();	// scanline stride
        int a1ps = afa1.getPixelStride();	// pixel stride
        int a1bo = afa1.getBandOffset(0);	// band 0 offsets

        /* Second source alpha channel (if any). */
        short[] a2 = null;
        int a2ss = 0;
        int a2ps = 0;
        int a2bo = 0;
        if (afa2 != null) {
            a2 = afa2.getShortDataArray(0);	// use band 0
            a2ss = afa2.getScanlineStride();	// scanline stride
            a2ps = afa2.getPixelStride();	// pixel stride
            a2bo = afa2.getBandOffset(0);	// band 0 offset
        }

        /* Destination color and alpha channels. */
        short[][] d = dst.getShortDataArrays();
        int dss = dst.getScanlineStride();	// scanline stride
        int dps = dst.getPixelStride();	// pixel stride
        int[] dbo = dst.getBandOffsets();	// band offsets

        int s1so = 0, s2so = 0, a1so = 0, a2so = 0, dso = 0;
        int s1po, s2po, a1po, a2po, dpo;	// po = pixel offset
        if (alphaPremultiplied) {
            if (afa2 == null) {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        float t = 1.0F - a1[a1po+a1bo] * invMaxValue;

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = maxValueShort;

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = (short)
                                (s1[b][s1po+s1bo[b]] + s2[b][s2po+s2bo[b]] * t);
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    dso += dss;
                }
            } else {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    a2po = a2so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        int t1 = a1[a1po+a1bo];	// a1
                        float t2 = 1.0F - t1 * invMaxValue;	// 1-a1/maxValue

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = (short)
                            (t1 + a2[a2po+a2bo] * t2);

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = (short)
                                (s1[b][s1po+s1bo[b]] +
                                 s2[b][s2po+s2bo[b]] * t2);
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        a2po += a2ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    a2so += a2ss;
                    dso += dss;
                }
            }
        } else {
            if (afa2 == null) {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        float t1 = a1[a1po+a1bo] * invMaxValue;
                        float t2 = 1.0F - t1;		// 1-a1/maxValue

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = maxValueShort;

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = (short)
                                (s1[b][s1po+s1bo[b]] * t1 +
                                 s2[b][s2po+s2bo[b]] * t2);
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    dso += dss;
                }
            } else {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    a2po = a2so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        int t1 = a1[a1po+a1bo];	// a1
                        float t2 = (1.0F - t1 * invMaxValue) *
                                   a2[a2po+a2bo]; // a2*(1-a1/maxValue)
                        float t3 = t1 + t2;		// d[alpha]
                        float t4, t5;
                        if (t3 == 0.0F) {
                            t4 = 0.0F;
                            t5 = 0.0F;
                        } else {
                            t4 = t1 / t3;
                            t5 = t2 / t3;
                        }

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = (short)t3;

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = (short)
                                (s1[b][s1po+s1bo[b]] * t4 +
                                 s2[b][s2po+s2bo[b]] * t5);
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        a2po += a2ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    a2so += a2ss;
                    dso += dss;
                }
            }
        }
    }

    private void intLoop(RasterAccessor src1, RasterAccessor src2,
                         RasterAccessor afa1, RasterAccessor afa2,
                         RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int numBands = src1.getNumBands();

        /* First source color channels. */
        int[][] s1 = src1.getIntDataArrays();
        int s1ss = src1.getScanlineStride();	// scanline stride
        int s1ps = src1.getPixelStride();	// pixel stride
        int[] s1bo = src1.getBandOffsets();	// band offsets

        /* Second source color channels. */
        int[][] s2 = src2.getIntDataArrays();
        int s2ss = src2.getScanlineStride();	// scanline stride
        int s2ps = src2.getPixelStride();	// pixel stride
        int[] s2bo = src2.getBandOffsets();	// band offsets

        /* First source alpha channel. */
        int[] a1 = afa1.getIntDataArray(0);	// use band 0
        int a1ss = afa1.getScanlineStride();	// scanline stride
        int a1ps = afa1.getPixelStride();	// pixel stride
        int a1bo = afa1.getBandOffset(0);	// band 0 offsets

        /* Second source alpha channel (if any). */
        int[] a2 = null;
        int a2ss = 0;
        int a2ps = 0;
        int a2bo = 0;
        if (afa2 != null) {
            a2 = afa2.getIntDataArray(0);	// use band 0
            a2ss = afa2.getScanlineStride();	// scanline stride
            a2ps = afa2.getPixelStride();	// pixel stride
            a2bo = afa2.getBandOffset(0);	// band 0 offset
        }

        /* Destination color and alpha channels. */
        int[][] d = dst.getIntDataArrays();
        int dss = dst.getScanlineStride();	// scanline stride
        int dps = dst.getPixelStride();	// pixel stride
        int[] dbo = dst.getBandOffsets();	// band offsets

        int s1so = 0, s2so = 0, a1so = 0, a2so = 0, dso = 0;
        int s1po, s2po, a1po, a2po, dpo;	// po = pixel offset
        if (alphaPremultiplied) {
            if (afa2 == null) {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        float t = 1.0F - a1[a1po+a1bo] * invMaxValue;

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = maxValue;

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = (int)(s1[b][s1po+s1bo[b]] +
                                                     s2[b][s2po+s2bo[b]] * t);
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    dso += dss;
                }
            } else {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    a2po = a2so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        int t1 = a1[a1po+a1bo];		// a1
                        float t2 = 1.0F - t1 * invMaxValue;	// 1-a1/maxValue

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = (int)
                            (t1 + a2[a2po+a2bo] * t2);

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = (int)(s1[b][s1po+s1bo[b]] +
                                                     s2[b][s2po+s2bo[b]] * t2);
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        a2po += a2ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    a2so += a2ss;
                    dso += dss;
                }
            }
        } else {
            if (afa2 == null) {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        float t1 = a1[a1po+a1bo] * invMaxValue;	// a1/maxValue
                        float t2 = 1.0F - t1;	// 1-a1/maxValue

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = maxValue;

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = (int)
                                (s1[b][s1po+s1bo[b]] * t1 +
                                 s2[b][s2po+s2bo[b]] * t2);
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    dso += dss;
                }
            } else {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    a2po = a2so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        int t1 = a1[a1po+a1bo];		// a1
                        float t2 = (1.0F - t1 * invMaxValue) * a2[a2po+a2bo];
                        				// a2*(1-a1/maxValue)
                        float t3 = t1 + t2;		// d[alpha]
                        float t4, t5;
                        if (t3 == 0.0F) {
                            t4 = 0.0F;
                            t5 = 0.0F;
                        } else {
                            t4 = t1 / t3;
                            t5 = t2 / t3;
                        }

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = (int)t3;

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = (int)(s1[b][s1po+s1bo[b]] * t4 +
                                                     s2[b][s2po+s2bo[b]] * t5);
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        a2po += a2ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    a2so += a2ss;
                    dso += dss;
                }
            }
        }
    }

    private void floatLoop(RasterAccessor src1, RasterAccessor src2,
                           RasterAccessor afa1, RasterAccessor afa2,
                           RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int numBands = src1.getNumBands();

        /* First source color channels. */
        float[][] s1 = src1.getFloatDataArrays();
        int s1ss = src1.getScanlineStride();    // scanline stride
        int s1ps = src1.getPixelStride();       // pixel stride
        int[] s1bo = src1.getBandOffsets();     // band offsets

        /* Second source color channels. */
        float[][] s2 = src2.getFloatDataArrays();
        int s2ss = src2.getScanlineStride();    // scanline stride
        int s2ps = src2.getPixelStride();       // pixel stride
        int[] s2bo = src2.getBandOffsets();     // band offsets

        /* First source alpha channel. */
        float[] a1 = afa1.getFloatDataArray(0); // use band 0
        int a1ss = afa1.getScanlineStride();    // scanline stride
        int a1ps = afa1.getPixelStride();       // pixel stride
        int a1bo = afa1.getBandOffset(0);       // band 0 offsets

        /* Second source alpha channel (if any). */
        float[] a2 = null;
        int a2ss = 0;
        int a2ps = 0;
        int a2bo = 0;
        if (afa2 != null) {
            a2 = afa2.getFloatDataArray(0);     // use band 0
            a2ss = afa2.getScanlineStride();    // scanline stride
            a2ps = afa2.getPixelStride();       // pixel stride
            a2bo = afa2.getBandOffset(0);       // band 0 offset
        }

        /* Destination color and alpha channels. */
        float[][] d = dst.getFloatDataArrays();
        int dss = dst.getScanlineStride();      // scanline stride
        int dps = dst.getPixelStride();         // pixel stride
        int[] dbo = dst.getBandOffsets();       // band offsets

        int s1so = 0, s2so = 0, a1so = 0, a2so = 0, dso = 0;
        int s1po, s2po, a1po, a2po, dpo;        // po = pixel offset
	float invMaxValue = 1.0F / Float.MAX_VALUE;
        if (alphaPremultiplied) {
            if (afa2 == null) {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        float t = 1.0F - a1[a1po+a1bo] * invMaxValue;

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = Float.MAX_VALUE;

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = s1[b][s1po+s1bo[b]] +
                                               s2[b][s2po+s2bo[b]] * t;
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    dso += dss;
                }
            } else {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    a2po = a2so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        float t1 = a1[a1po+a1bo];         // a1
                        float t2 = 1.0F - t1 * invMaxValue;     // 1-a1/maxValue

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = t1 + a2[a2po+a2bo] * t2;

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = s1[b][s1po+s1bo[b]] +
                                               s2[b][s2po+s2bo[b]] * t2;
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        a2po += a2ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    a2so += a2ss;
                    dso += dss;
                }
            }
        } else {
            if (afa2 == null) {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        float t1 = a1[a1po+a1bo] * invMaxValue; // a1/maxValue
                        float t2 = 1.0F - t1;   // 1-a1/maxValue

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = Float.MAX_VALUE;

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = s1[b][s1po+s1bo[b]] * t1 +
                                               s2[b][s2po+s2bo[b]] * t2;
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    dso += dss;
                }
            } else {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    a2po = a2so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        float t1 = a1[a1po+a1bo];         // a1
                        float t2 = (1.0F - t1 * invMaxValue) * a2[a2po+a2bo];
                                                        // a2*(1-a1/maxValue)
                        float t3 = t1 + t2;             // d[alpha]
                        float t4, t5;
                        if (t3 == 0.0F) {
                            t4 = 0.0F;
                            t5 = 0.0F;
                        } else {
                            t4 = t1 / t3;
                            t5 = t2 / t3;
                        }

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = t3;

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = s1[b][s1po+s1bo[b]] * t4 +
                                               s2[b][s2po+s2bo[b]] * t5;
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        a2po += a2ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    a2so += a2ss;
                    dso += dss;
                }
            }
        }
    }

    private void doubleLoop(RasterAccessor src1, RasterAccessor src2,
                            RasterAccessor afa1, RasterAccessor afa2,
                            RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int numBands = src1.getNumBands();

        /* First source color channels. */
        double[][] s1 = src1.getDoubleDataArrays();
        int s1ss = src1.getScanlineStride();    // scanline stride
        int s1ps = src1.getPixelStride();       // pixel stride
        int[] s1bo = src1.getBandOffsets();     // band offsets

        /* Second source color channels. */
        double[][] s2 = src2.getDoubleDataArrays();
        int s2ss = src2.getScanlineStride();    // scanline stride
        int s2ps = src2.getPixelStride();       // pixel stride
        int[] s2bo = src2.getBandOffsets();     // band offsets

        /* First source alpha channel. */
        double[] a1 = afa1.getDoubleDataArray(0); // use band 0
        int a1ss = afa1.getScanlineStride();    // scanline stride
        int a1ps = afa1.getPixelStride();       // pixel stride
        int a1bo = afa1.getBandOffset(0);       // band 0 offsets

        /* Second source alpha channel (if any). */
        double[] a2 = null;
        int a2ss = 0;
        int a2ps = 0;
        int a2bo = 0;
        if (afa2 != null) {
            a2 = afa2.getDoubleDataArray(0);     // use band 0
            a2ss = afa2.getScanlineStride();    // scanline stride
            a2ps = afa2.getPixelStride();       // pixel stride
            a2bo = afa2.getBandOffset(0);       // band 0 offset
        }

        /* Destination color and alpha channels. */
        double[][] d = dst.getDoubleDataArrays();
        int dss = dst.getScanlineStride();      // scanline stride
        int dps = dst.getPixelStride();         // pixel stride
        int[] dbo = dst.getBandOffsets();       // band offsets

        int s1so = 0, s2so = 0, a1so = 0, a2so = 0, dso = 0;
        int s1po, s2po, a1po, a2po, dpo;        // po = pixel offset
	double invMaxValue = 1.0D / Double.MAX_VALUE;
        if (alphaPremultiplied) {
            if (afa2 == null) {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        double t = 1.0D - a1[a1po+a1bo] * invMaxValue;

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = Double.MAX_VALUE;

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = s1[b][s1po+s1bo[b]] +
                                               s2[b][s2po+s2bo[b]] * t;
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    dso += dss;
                }
            } else {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    a2po = a2so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        double t1 = a1[a1po+a1bo];         // a1
                        double t2 = 1.0D - t1 * invMaxValue;     // 1-a1/maxValue

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = t1 + a2[a2po+a2bo] * t2;

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = s1[b][s1po+s1bo[b]] +
                                               s2[b][s2po+s2bo[b]] * t2;
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        a2po += a2ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    a2so += a2ss;
                    dso += dss;
                }
            }
        } else {
            if (afa2 == null) {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        double t1 = a1[a1po+a1bo] * invMaxValue; // a1/maxValue
                        double t2 = 1.0D - t1;   // 1-a1/maxValue

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = Double.MAX_VALUE;

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = s1[b][s1po+s1bo[b]] * t1 +
                                               s2[b][s2po+s2bo[b]] * t2;
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    dso += dss;
                }
            } else {
                for (int h = 0; h < dheight; h++) {
                    s1po = s1so;
                    s2po = s2so;
                    a1po = a1so;
                    a2po = a2so;
                    dpo = dso;

                    for (int w = 0; w < dwidth; w++) {
                        double t1 = a1[a1po+a1bo];         // a1
                        double t2 = (1.0D - t1 * invMaxValue) * a2[a2po+a2bo];
                                                        // a2*(1-a1/maxValue)
                        double t3 = t1 + t2;             // d[alpha]
                        double t4, t5;
                        if (t3 == 0.0D) {
                            t4 = 0.0D;
                            t5 = 0.0D;
                        } else {
                            t4 = t1 / t3;
                            t5 = t2 / t3;
                        }

                        /* Destination alpha channel. */
                        d[aOffset][dpo+dbo[aOffset]] = t3;

                        /* Destination color channels. */
                        for (int b = 0; b < numBands; b++) {
                            int i = b + cOffset;
                            d[i][dpo+dbo[i]] = s1[b][s1po+s1bo[b]] * t4 +
                                               s2[b][s2po+s2bo[b]] * t5;
                        }

                        s1po += s1ps;
                        s2po += s2ps;
                        a1po += a1ps;
                        a2po += a2ps;
                        dpo += dps;
                    }

                    s1so += s1ss;
                    s2so += s2ss;
                    a1so += a1ss;
                    a2so += a2ss;
                    dso += dss;
                }
            }
        }
    }

//     public static void main(String args[]) {
//         System.out.println("AddOpImage Test");
//         ImageLayout layoutSrc, layoutAlpha;
//         OpImage src1, src2, afa1, afa2, dst;
//         Rectangle rect = new Rectangle(0, 0, 10, 5);

//         layoutAlpha = OpImageTester.createImageLayout(
//             0, 0, 800, 800, 0, 0, 200, 200, DataBuffer.TYPE_BYTE, 1, false);
//         afa1 = OpImageTester.createRandomOpImage(layoutAlpha);
//         afa2 = OpImageTester.createRandomOpImage(layoutAlpha);
//         OpImageTester.printPixels("Alpha 1", afa1, rect);
//         OpImageTester.printPixels("Alpha 2", afa2, rect);

//         System.out.println("1. PixelInterleaved byte 3-band");

//         layoutSrc = OpImageTester.createImageLayout(
//             0, 0, 800, 800, 0, 0, 200, 200, DataBuffer.TYPE_BYTE, 3, false);
//         src1 = OpImageTester.createRandomOpImage(layoutSrc);
//         src2 = OpImageTester.createRandomOpImage(layoutSrc);

//         System.out.println("1a. Alpha premultiplied, source2 opaque");
//         dst = new CompositeOpImage(src1, src2, null, null,
//                                    afa1, null, true, true);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("1b. Alpha premultiplied, source2 not opaque");
//         dst = new CompositeOpImage(src1, src2, null, null,
//                                    afa1, afa2, true, true);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("1c. Alpha not premultiplied, source2 opaque");
//         dst = new CompositeOpImage(src1, src2, null, null,
//                                    afa1, null, false, true);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("1d. Alpha not premultiplied, source2 not opaque");
//         dst = new CompositeOpImage(src1, src2, null, null,
//                                    afa1, afa2, false, true);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("2. Banded byte 3-band");

//         layoutSrc = OpImageTester.createImageLayout(
//             0, 0, 800, 800, 0, 0, 200, 200, DataBuffer.TYPE_BYTE, 3, true);
//         src1 = OpImageTester.createRandomOpImage(layoutSrc);
//         src2 = OpImageTester.createRandomOpImage(layoutSrc);

//         System.out.println("2b. Alpha premultiplied, source2 not opaque");
//         dst = new CompositeOpImage(src1, src2, null, null,
//                                    afa1, afa2, true, false);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("2d. Alpha not premultiplied, source2 not opaque");
//         dst = new CompositeOpImage(src1, src2, null, null,
//                                    afa1, afa2, false, true);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         layoutAlpha = OpImageTester.createImageLayout(
//             0, 0, 800, 800, 0, 0, 200, 200, DataBuffer.TYPE_USHORT, 1, true);
//         afa1 = OpImageTester.createRandomOpImage(layoutAlpha);
//         afa2 = OpImageTester.createRandomOpImage(layoutAlpha);
//         OpImageTester.printPixels("Alpha 1", afa1, rect);
//         OpImageTester.printPixels("Alpha 2", afa2, rect);

//         System.out.println("3. PixelInterleaved ushort 3-band");

//         layoutSrc = OpImageTester.createImageLayout(
//             0, 0, 800, 800, 0, 0, 200, 200, DataBuffer.TYPE_USHORT, 3, false);
//         src1 = OpImageTester.createRandomOpImage(layoutSrc);
//         src2 = OpImageTester.createRandomOpImage(layoutSrc);

//         System.out.println("3a. Alpha premultiplied, source2 opaque");
//         dst = new CompositeOpImage(src1, src2, null, null,
//                                    afa1, null, true, false);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("3d. Alpha not premultiplied, source2 not opaque");
//         dst = new CompositeOpImage(src1, src2, null, null,
//                                    afa1, afa2, false, false);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);
//     }
}
