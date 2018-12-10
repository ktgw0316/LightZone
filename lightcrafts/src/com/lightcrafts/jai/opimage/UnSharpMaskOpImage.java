/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * $RCSfile: AddOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:12 $
 * $State: Exp $
 */
package com.lightcrafts.jai.opimage;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.RasterFactory;
import java.util.Map;

import com.sun.media.jai.util.ImageUtil;
import com.sun.media.jai.util.JDKWorkarounds;

final class UnSharpMaskOpImage extends PointOpImage {

    /* Source 1 band increment */
    private int s1bd = 1;

    /* Source 2 band increment */
    private int s2bd = 1;

    protected double gain;
    protected int threshold;

    /**
     * Constructs an <code>AddOpImage</code>.
     *
     * <p>The <code>layout</code> parameter may optionally contains the
     * tile grid layout, sample model, and/or color model. The image
     * dimension is determined by the intersection of the bounding boxes
     * of the two source images.
     *
     * <p>The image layout of the first source image, <code>source1</code>,
     * is used as the fall-back for the image layout of the destination
     * image. Any layout parameters not specified in the <code>layout</code>
     * argument are set to the same value as that of <code>source1</code>.
     *
     * @param source1  The first source image.
     * @param source2  The second source image.
     * @param layout   The destination image layout.
     */
    public UnSharpMaskOpImage(RenderedImage source1,
                              RenderedImage source2,
                              Map config,
                              ImageLayout layout,
                              double gain, int threshold) {
        super(source1, source2, layout, config, true);

        this.gain = gain;
        this.threshold = threshold;

        // Get the source band counts.
        int numBands1 = source1.getSampleModel().getNumBands();
        int numBands2 = source2.getSampleModel().getNumBands();

        // Handle the special case of adding a single band image to
        // each band of a multi-band image.
        int numBandsDst;
        if (layout != null && layout.isValid(ImageLayout.SAMPLE_MODEL_MASK)) {
            SampleModel sm = layout.getSampleModel(null);
            numBandsDst = sm.getNumBands();

            // One of the sources must be single-banded and the other must
            // have at most the number of bands in the SampleModel hint.
            if (numBandsDst > 1 &&
                ((numBands1 == 1 && numBands2 > 1) ||
                 (numBands2 == 1 && numBands1 > 1))) {
                // Clamp the destination band count to the number of
                // bands in the multi-band source.
                numBandsDst = Math.min(Math.max(numBands1, numBands2),
                                       numBandsDst);

                // Create a new SampleModel if necessary.
                if (numBandsDst != sampleModel.getNumBands()) {
                    sampleModel =
                            RasterFactory.createComponentSampleModel(sm,
                                                                     sampleModel.getTransferType(),
                                                                     sampleModel.getWidth(),
                                                                     sampleModel.getHeight(),
                                                                     numBandsDst);

                    if (colorModel != null &&
                        !JDKWorkarounds.areCompatibleDataModels(sampleModel,
                                                                colorModel)) {
                        colorModel =
                                ImageUtil.getCompatibleColorModel(sampleModel,
                                                                  config);
                    }
                }

                // Set the source band increments.
                s1bd = numBands1 == 1 ? 0 : 1;
                s2bd = numBands2 == 1 ? 0 : 1;
            }
        }

        // Set flag to permit in-place operation.
        permitInPlaceOperation();
    }

    /**
     * Adds the pixel values of two source images within a specified
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

        RasterAccessor s1 = new RasterAccessor(sources[0], destRect,
                                               formatTags[0],
                                               getSourceImage(0).getColorModel());
        RasterAccessor s2 = new RasterAccessor(sources[1], destRect,
                                               formatTags[1],
                                               getSourceImage(1).getColorModel());
        RasterAccessor d = new RasterAccessor(dest, destRect,
                                              formatTags[2], getColorModel());

        switch (d.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            computeRectByte(s1, s2, d);
            break;
        case DataBuffer.TYPE_USHORT:
            computeRectUShort(s1, s2, d);
            break;
        case DataBuffer.TYPE_SHORT:
            computeRectShort(s1, s2, d);
            break;
        case DataBuffer.TYPE_INT:
            computeRectInt(s1, s2, d);
            break;
        case DataBuffer.TYPE_FLOAT:
            computeRectFloat(s1, s2, d);
            break;
        case DataBuffer.TYPE_DOUBLE:
            computeRectDouble(s1, s2, d);
            break;
        }

        if (d.needsClamping()) {
            d.clampDataArrays();
        }
        d.copyDataToRaster();
    }

    private void computeRectByte(RasterAccessor src1,
                                 RasterAccessor src2,
                                 RasterAccessor dst) {
        int s1LineStride = src1.getScanlineStride();
        int s1PixelStride = src1.getPixelStride();
        int[] s1BandOffsets = src1.getBandOffsets();
        byte[][] s1Data = src1.getByteDataArrays();

        int s2LineStride = src2.getScanlineStride();
        int s2PixelStride = src2.getPixelStride();
        int[] s2BandOffsets = src2.getBandOffsets();
        byte[][] s2Data = src2.getByteDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int bands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        byte[][] dData = dst.getByteDataArrays();

        int c = (int) (gain * 0x100);

        for (int b = 0, s1b = 0, s2b = 0; b < bands;
             b++, s1b += s1bd, s2b += s2bd) {
            byte[] s1 = s1Data[s1b];
            byte[] s2 = s2Data[s2b];
            byte[] d = dData[b];

            int s1LineOffset = s1BandOffsets[s1b];
            int s2LineOffset = s2BandOffsets[s2b];
            int dLineOffset = dBandOffsets[b];

            for (int h = 0; h < dheight; h++) {
                int s1PixelOffset = s1LineOffset;
                int s2PixelOffset = s2LineOffset;
                int dPixelOffset = dLineOffset;

                s1LineOffset += s1LineStride;
                s2LineOffset += s2LineStride;
                dLineOffset += dLineStride;

                for (int w = 0; w < dwidth; w++) {
                    int src = s1[s1PixelOffset] & 0xFF;
                    d[dPixelOffset] = ImageUtil.clampByte(src + c * (src - (s2[s2PixelOffset] & 0xFF)) / 0x100);

                    s1PixelOffset += s1PixelStride;
                    s2PixelOffset += s2PixelStride;
                    dPixelOffset += dPixelStride;
                }
            }
        }
    }

    private static double sigmoid(double x) {
        final double s = 0.05;
        return 1 / (1 + Math.exp(- s * (x + 60)));
    }

    static final int sigmoidTableLenght = 16 * 1024;
    static final float sigmoidTable[] = new float[sigmoidTableLenght];

    static {
        for (int i = 0; i < sigmoidTableLenght; i++)
            sigmoidTable[i] = (float) sigmoid(0.02 * (i - sigmoidTableLenght / 2));
    }

    private static double sigmoidT(double x) {
        int idx = (int) (50 * x + 0.5) + sigmoidTableLenght / 2;
        if (idx < 0)
            return 0;
        else if (idx >= sigmoidTableLenght)
            return 1;
        else
            return sigmoidTable[idx];
    }

    public static void main( String[] args ) {
        System.out.println("Here: ");
    }

    private void computeRectUShort(RasterAccessor src1,
                                   RasterAccessor src2,
                                   RasterAccessor dst) {
        int s1LineStride = src1.getScanlineStride();
        int s1PixelStride = src1.getPixelStride();
        int[] s1BandOffsets = src1.getBandOffsets();
        short[][] s1Data = src1.getShortDataArrays();

        int s2LineStride = src2.getScanlineStride();
        int s2PixelStride = src2.getPixelStride();
        int[] s2BandOffsets = src2.getBandOffsets();
        short[][] s2Data = src2.getShortDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int bands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        short[][] dData = dst.getShortDataArrays();

        int c = (int) (gain * 256);
        int t = 256 * threshold;

        short[] s1 = s1Data[0];
        short[] s2 = s2Data[0];
        short[] d = dData[0];

        int s1LineOffset = s1BandOffsets[0];
        int s2LineOffset = s2BandOffsets[0];
        int dLineOffset = dBandOffsets[0];

        for (int h = 0; h < dheight; h++) {
            int s1PixelOffset = s1LineOffset;
            int s2PixelOffset = s2LineOffset;
            int dPixelOffset = dLineOffset;

            s1LineOffset += s1LineStride;
            s2LineOffset += s2LineStride;
            dLineOffset += dLineStride;

            for (int w = 0; w < dwidth; w++) {
                if (bands == 3) {
                    int s10 = s1[s1PixelOffset+0] & 0xFFFF;
                    int s20 = s2[s2PixelOffset+0] & 0xFFFF;
                    int d0 = s10 - s20;

                    int s11 = s1[s1PixelOffset+1] & 0xFFFF;
                    int s21 = s2[s1PixelOffset+1] & 0xFFFF;
                    int d1 = s11 - s21;

                    int s12 = s1[s1PixelOffset+2] & 0xFFFF;
                    int s22 = s2[s1PixelOffset+2] & 0xFFFF;
                    int d2 = s12 - s22;

                    double diff = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                    double s = sigmoidT(20 * diff - t);

                    d[dPixelOffset+0] = ImageUtil.clampUShort(s10 + (int) (c * d0 * s / 256.));
                    d[dPixelOffset+1] = ImageUtil.clampUShort(s11 + (int) (c * d1 * s / 256.));
                    d[dPixelOffset+2] = ImageUtil.clampUShort(s12 + (int) (c * d2 * s / 256.));
                } else {
                    int ss1 = s1[s1PixelOffset] & 0xFFFF;
                    int ss2 = s2[s1PixelOffset] & 0xFFFF;
                    int dd = ss1 - ss2;

                    double s = sigmoidT(20 * Math.abs(ss1 - ss2) - t);

                    d[dPixelOffset] = ImageUtil.clampUShort(ss1 + (int) (c * dd * s / 256.));
                }
                s1PixelOffset += s1PixelStride;
                s2PixelOffset += s2PixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }

    private void computeRectShort(RasterAccessor src1,
                                  RasterAccessor src2,
                                  RasterAccessor dst) {
        int s1LineStride = src1.getScanlineStride();
        int s1PixelStride = src1.getPixelStride();
        int[] s1BandOffsets = src1.getBandOffsets();
        short[][] s1Data = src1.getShortDataArrays();

        int s2LineStride = src2.getScanlineStride();
        int s2PixelStride = src2.getPixelStride();
        int[] s2BandOffsets = src2.getBandOffsets();
        short[][] s2Data = src2.getShortDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int bands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        short[][] dData = dst.getShortDataArrays();

        for (int b = 0, s1b = 0, s2b = 0; b < bands;
             b++, s1b += s1bd, s2b += s2bd) {
            short[] s1 = s1Data[s1b];
            short[] s2 = s2Data[s2b];
            short[] d = dData[b];
            int c = (int) (gain * (Short.MAX_VALUE + 1) + 0.5);

            int s1LineOffset = s1BandOffsets[s1b];
            int s2LineOffset = s2BandOffsets[s2b];
            int dLineOffset = dBandOffsets[b];

            for (int h = 0; h < dheight; h++) {
                int s1PixelOffset = s1LineOffset;
                int s2PixelOffset = s2LineOffset;
                int dPixelOffset = dLineOffset;

                s1LineOffset += s1LineStride;
                s2LineOffset += s2LineStride;
                dLineOffset += dLineStride;

                for (int w = 0; w < dwidth; w++) {
                    int src = s1[s1PixelOffset];
                    d[dPixelOffset] = ImageUtil.clampRoundShort(src + c * (src - s2[s2PixelOffset]) / (Short.MAX_VALUE + 1));

                    s1PixelOffset += s1PixelStride;
                    s2PixelOffset += s2PixelStride;
                    dPixelOffset += dPixelStride;
                }
            }
        }
    }

    private void computeRectInt(RasterAccessor src1,
                                RasterAccessor src2,
                                RasterAccessor dst) {
        int s1LineStride = src1.getScanlineStride();
        int s1PixelStride = src1.getPixelStride();
        int[] s1BandOffsets = src1.getBandOffsets();
        int[][] s1Data = src1.getIntDataArrays();

        int s2LineStride = src2.getScanlineStride();
        int s2PixelStride = src2.getPixelStride();
        int[] s2BandOffsets = src2.getBandOffsets();
        int[][] s2Data = src2.getIntDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int bands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        int[][] dData = dst.getIntDataArrays();

        /*
         * The destination data type may be any of the integral data types.
         * The "clamp" function must clamp to the appropriate range for
         * that data type.
         */
        switch (sampleModel.getTransferType()) {
        case DataBuffer.TYPE_BYTE:
            for (int b = 0, s1b = 0, s2b = 0; b < bands;
                 b++, s1b += s1bd, s2b += s2bd) {
                int[] s1 = s1Data[s1b];
                int[] s2 = s2Data[s2b];
                int[] d = dData[b];
                int c = (int) (gain * 0x100 + 0.5);

                int s1LineOffset = s1BandOffsets[s1b];
                int s2LineOffset = s2BandOffsets[s2b];
                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    int s1PixelOffset = s1LineOffset;
                    int s2PixelOffset = s2LineOffset;
                    int dPixelOffset = dLineOffset;

                    s1LineOffset += s1LineStride;
                    s2LineOffset += s2LineStride;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {
                        int src = s1[s1PixelOffset] & 0xFF;
                        d[dPixelOffset] = ImageUtil.clampRoundByte(src + c * (src - (s2[s2PixelOffset] & 0xFF)) / 0x100);

                        s1PixelOffset += s1PixelStride;
                        s2PixelOffset += s2PixelStride;
                        dPixelOffset += dPixelStride;
                    }
                }
            }
            break;

        case DataBuffer.TYPE_USHORT:
            for (int b = 0, s1b = 0, s2b = 0; b < bands;
                 b++, s1b += s1bd, s2b += s2bd) {
                int[] s1 = s1Data[s1b];
                int[] s2 = s2Data[s2b];
                int[] d = dData[b];
                long c = (long) (gain * 0x10000 + 0.5);

                int s1LineOffset = s1BandOffsets[s1b];
                int s2LineOffset = s2BandOffsets[s2b];
                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    int s1PixelOffset = s1LineOffset;
                    int s2PixelOffset = s2LineOffset;
                    int dPixelOffset = dLineOffset;

                    s1LineOffset += s1LineStride;
                    s2LineOffset += s2LineStride;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {
                        int src = s1[s1PixelOffset] & 0xFFFF;
                        d[dPixelOffset] = ImageUtil.clampRoundUShort(src + c * (src - (s2[s2PixelOffset] & 0xFFFF)) / 0x10000);

                        s1PixelOffset += s1PixelStride;
                        s2PixelOffset += s2PixelStride;
                        dPixelOffset += dPixelStride;
                    }
                }
            }
            break;

        case DataBuffer.TYPE_SHORT:
            for (int b = 0, s1b = 0, s2b = 0; b < bands;
                 b++, s1b += s1bd, s2b += s2bd) {
                int[] s1 = s1Data[s1b];
                int[] s2 = s2Data[s2b];
                int[] d = dData[b];
                int c = (int) (gain * (Short.MAX_VALUE + 1) + 0.5);

                int s1LineOffset = s1BandOffsets[s1b];
                int s2LineOffset = s2BandOffsets[s2b];
                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    int s1PixelOffset = s1LineOffset;
                    int s2PixelOffset = s2LineOffset;
                    int dPixelOffset = dLineOffset;

                    s1LineOffset += s1LineStride;
                    s2LineOffset += s2LineStride;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {
                        int src = s1[s1PixelOffset];
                        d[dPixelOffset] = ImageUtil.clampRoundShort(src + c * (src - s2[s2PixelOffset]) / (Short.MAX_VALUE + 1));

                        s1PixelOffset += s1PixelStride;
                        s2PixelOffset += s2PixelStride;
                        dPixelOffset += dPixelStride;
                    }
                }
            }
            break;

        case DataBuffer.TYPE_INT:
            for (int b = 0, s1b = 0, s2b = 0; b < bands;
                 b++, s1b += s1bd, s2b += s2bd) {
                int[] s1 = s1Data[s1b];
                int[] s2 = s2Data[s2b];
                int[] d = dData[b];
                long c = (long) (gain * ((long) Integer.MAX_VALUE + 1) + 0.5);

                int s1LineOffset = s1BandOffsets[s1b];
                int s2LineOffset = s2BandOffsets[s2b];
                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    int s1PixelOffset = s1LineOffset;
                    int s2PixelOffset = s2LineOffset;
                    int dPixelOffset = dLineOffset;

                    s1LineOffset += s1LineStride;
                    s2LineOffset += s2LineStride;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {
                        int src = s1[s1PixelOffset];
                        d[dPixelOffset] = ImageUtil.clampRoundInt(src + c * (src - s2[s2PixelOffset]) / ((long) Integer.MAX_VALUE + 1));

                        s1PixelOffset += s1PixelStride;
                        s2PixelOffset += s2PixelStride;
                        dPixelOffset += dPixelStride;
                    }
                }
            }
            break;
        }
    }

    private void computeRectFloat(RasterAccessor src1,
                                  RasterAccessor src2,
                                  RasterAccessor dst) {
        int s1LineStride = src1.getScanlineStride();
        int s1PixelStride = src1.getPixelStride();
        int[] s1BandOffsets = src1.getBandOffsets();
        float[][] s1Data = src1.getFloatDataArrays();

        int s2LineStride = src2.getScanlineStride();
        int s2PixelStride = src2.getPixelStride();
        int[] s2BandOffsets = src2.getBandOffsets();
        float[][] s2Data = src2.getFloatDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int bands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        float[][] dData = dst.getFloatDataArrays();

        for (int b = 0, s1b = 0, s2b = 0; b < bands;
             b++, s1b += s1bd, s2b += s2bd) {
            float[] s1 = s1Data[s1b];
            float[] s2 = s2Data[s2b];
            float[] d = dData[b];
            float c = (float) gain;

            int s1LineOffset = s1BandOffsets[s1b];
            int s2LineOffset = s2BandOffsets[s2b];
            int dLineOffset = dBandOffsets[b];

            for (int h = 0; h < dheight; h++) {
                int s1PixelOffset = s1LineOffset;
                int s2PixelOffset = s2LineOffset;
                int dPixelOffset = dLineOffset;

                s1LineOffset += s1LineStride;
                s2LineOffset += s2LineStride;
                dLineOffset += dLineStride;

                for (int w = 0; w < dwidth; w++) {
                    float src = s1[s1PixelOffset];
                    d[dPixelOffset] = src + c * (src - s2[s2PixelOffset]);

                    s1PixelOffset += s1PixelStride;
                    s2PixelOffset += s2PixelStride;
                    dPixelOffset += dPixelStride;
                }
            }
        }
    }

    private void computeRectDouble(RasterAccessor src1,
                                   RasterAccessor src2,
                                   RasterAccessor dst) {
        int s1LineStride = src1.getScanlineStride();
        int s1PixelStride = src1.getPixelStride();
        int[] s1BandOffsets = src1.getBandOffsets();
        double[][] s1Data = src1.getDoubleDataArrays();

        int s2LineStride = src2.getScanlineStride();
        int s2PixelStride = src2.getPixelStride();
        int[] s2BandOffsets = src2.getBandOffsets();
        double[][] s2Data = src2.getDoubleDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int bands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        double[][] dData = dst.getDoubleDataArrays();

        for (int b = 0, s1b = 0, s2b = 0; b < bands;
             b++, s1b += s1bd, s2b += s2bd) {
            double[] s1 = s1Data[s1b];
            double[] s2 = s2Data[s2b];
            double[] d = dData[b];
            double c = gain;

            int s1LineOffset = s1BandOffsets[s1b];
            int s2LineOffset = s2BandOffsets[s2b];
            int dLineOffset = dBandOffsets[b];

            for (int h = 0; h < dheight; h++) {
                int s1PixelOffset = s1LineOffset;
                int s2PixelOffset = s2LineOffset;
                int dPixelOffset = dLineOffset;

                s1LineOffset += s1LineStride;
                s2LineOffset += s2LineStride;
                dLineOffset += dLineStride;

                for (int w = 0; w < dwidth; w++) {
                    double src = s1[s1PixelOffset];
                    d[dPixelOffset] = src + c * (src - s2[s2PixelOffset]);

                    s1PixelOffset += s1PixelStride;
                    s2PixelOffset += s2PixelStride;
                    dPixelOffset += dPixelStride;
                }
            }
        }
    }
}
