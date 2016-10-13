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
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PointOpImage;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import com.lightcrafts.mediax.jai.RasterFactory;
import java.util.Map;
import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.JDKWorkarounds;
/// import com.lightcrafts.media.jai.test.OpImageTester;

/**
 * An <code>OpImage</code> implementing the "Add" operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.AddDescriptor</code>.
 *
 * <p>This <code>OpImage</code> adds the pixel values of two source
 * images on a per-band basis. In case the two source images have different
 * number of bands, the number of bands for the destination image is the
 * smaller band number of the two source images. That is
 * <code>dstNumBands = Math.min(src1NumBands, src2NumBands)</code>.
 * In case the two source images have different data types, the data type
 * for the destination image is the higher data type of the two source
 * images.
 *
 * <p>The value of the pixel (x, y) in the destination image is defined as:
 * <pre>
 * for (b = 0; b < numBands; b++) {
 *     dst[y][x][b] = src1[y][x][b] + src2[y][x][b];
 * }
 * </pre>
 *
 * <p>If the result of the addition overflows/underflows the
 * maximum/minimum value supported by the destination image, then it
 * will be clamped to the maximum/minimum value respectively. The
 * data type <code>byte</code> is treated as unsigned, with maximum
 * value as 255 and minimum value as 0.
 *
 * @see com.lightcrafts.mediax.jai.operator.AddDescriptor
 * @see AddCRIF
 *
 */
final class MultiplyAddOpImage extends PointOpImage {

    /* Source 1 band increment */
    private int s1bd = 1;

    /* Source 2 band increment */
    private int s2bd = 1;

    protected double[] constants;

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
    public MultiplyAddOpImage(RenderedImage source1,
                      RenderedImage source2,
                      Map config,
		      ImageLayout layout,
                      double[] constants) {
        super(source1, source2, layout, config, true);

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

                if (constants.length < numBandsDst) {
                    this.constants = new double[numBandsDst];
                    for (int i = 0; i < numBandsDst; i++) {
                        this.constants[i] = constants[0];
                    }
                } else {
                    this.constants = constants.clone();
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

        for (int b = 0, s1b = 0, s2b = 0; b < bands;
             b++, s1b += s1bd, s2b += s2bd) {
            byte[] s1 = s1Data[s1b];
            byte[] s2 = s2Data[s2b];
            byte[] d = dData[b];
            double c = constants[b];

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
                    d[dPixelOffset] = ImageUtil.clampRoundByte((s1[s1PixelOffset] & 0xFF) +
                                                               c * (s2[s2PixelOffset] & 0xFF));

                    s1PixelOffset += s1PixelStride;
                    s2PixelOffset += s2PixelStride;
                    dPixelOffset += dPixelStride;
                }
            }
        }
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

        for (int b = 0, s1b = 0, s2b = 0; b < bands;
             b++, s1b += s1bd, s2b += s2bd) {
            short[] s1 = s1Data[s1b];
            short[] s2 = s2Data[s2b];
            short[] d = dData[b];
            double c = constants[b];

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
                    d[dPixelOffset] = ImageUtil.clampRoundUShort((s1[s1PixelOffset] & 0xFFFF) +
                                                                 c * (s2[s2PixelOffset] & 0xFFFF));

                    s1PixelOffset += s1PixelStride;
                    s2PixelOffset += s2PixelStride;
                    dPixelOffset += dPixelStride;
                }
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
            double c = constants[b];

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
                    d[dPixelOffset] = ImageUtil.clampRoundShort(s1[s1PixelOffset] + c * s2[s2PixelOffset]);

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
                double c = constants[b];

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
                        d[dPixelOffset] = ImageUtil.clampRoundByte((s1[s1PixelOffset] & 0xFF) +
                                                               c * (s2[s2PixelOffset] & 0xFF));

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
                double c = constants[b];

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
                        d[dPixelOffset] = ImageUtil.clampRoundUShort((s1[s1PixelOffset] & 0xFFFF) +
                                                                    c * (s2[s2PixelOffset] & 0xFFFF));

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
                double c = constants[b];

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
                        d[dPixelOffset] = ImageUtil.clampRoundShort(s1[s1PixelOffset] + c * s2[s2PixelOffset]);

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
                double c = constants[b];

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
                        d[dPixelOffset] = ImageUtil.clampRoundInt(s1[s1PixelOffset] + c * s2[s2PixelOffset]);

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
            float c = (float) constants[b];

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
                    d[dPixelOffset] = s1[s1PixelOffset] + c * s2[s2PixelOffset];

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
            double c = constants[b];

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
                    d[dPixelOffset] = s1[s1PixelOffset] + c * s2[s2PixelOffset];

                    s1PixelOffset += s1PixelStride;
                    s2PixelOffset += s2PixelStride;
                    dPixelOffset += dPixelStride;
                }
            }
        }
    }

//     public static void main(String args[]) {
//         System.out.println("AddOpImage Test");
//         ImageLayout layout;
//         OpImage src1, src2, dst;
//         Rectangle rect = new Rectangle(0, 0, 5, 5);

//         System.out.println("1. PixelInterleaved byte 3-band");
//         layout = OpImageTester.createImageLayout(
//             0, 0, 800, 800, 0, 0, 200, 200, DataBuffer.TYPE_BYTE, 3, false);
//         src1 = OpImageTester.createRandomOpImage(layout);
//         src2 = OpImageTester.createRandomOpImage(layout);
//         dst = new AddOpImage(src1, src2, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("2. Banded byte 3-band");
//         layout = OpImageTester.createImageLayout(
//            0, 0, 800, 800, 0, 0, 200, 200, DataBuffer.TYPE_BYTE, 3, true);
//         src1 = OpImageTester.createRandomOpImage(layout);
//         src2 = OpImageTester.createRandomOpImage(layout);
//         dst = new AddOpImage(src1, src2, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("3. PixelInterleaved int 3-band");
//         layout = OpImageTester.createImageLayout(
//             0, 0, 512, 512, 0, 0, 200, 200, DataBuffer.TYPE_INT, 3, false);
//         src1 = OpImageTester.createRandomOpImage(layout);
//         src2 = OpImageTester.createRandomOpImage(layout);
//         dst = new AddOpImage(src1, src2, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("4. Banded int 3-band");
//         layout = OpImageTester.createImageLayout(
//             0, 0, 512, 512, 0, 0, 200, 200, DataBuffer.TYPE_INT, 3, true);
//         src1 = OpImageTester.createRandomOpImage(layout);
//         src2 = OpImageTester.createRandomOpImage(layout);
//         dst = new AddOpImage(src1, src2, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("5. PixelInterleaved float 3-band");
//         layout = OpImageTester.createImageLayout(
//             0, 0, 512, 512, 0, 0, 200, 200, DataBuffer.TYPE_FLOAT, 3, false);
//         src1 = OpImageTester.createRandomOpImage(layout);
//         src2 = OpImageTester.createRandomOpImage(layout);
//         dst = new AddOpImage(src1, src2, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("6. Banded float 3-band");
//         layout = OpImageTester.createImageLayout(
//             0, 0, 512, 512, 0, 0, 200, 200, DataBuffer.TYPE_FLOAT, 3, true);
//         src1 = OpImageTester.createRandomOpImage(layout);
//         src2 = OpImageTester.createRandomOpImage(layout);
//         dst = new AddOpImage(src1, src2, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("7. PixelInterleaved double 3-band");
//         layout = OpImageTester.createImageLayout(
//             0, 0, 512, 512, 0, 0, 200, 200, DataBuffer.TYPE_DOUBLE, 3, false);
//         src1 = OpImageTester.createRandomOpImage(layout);
//         src2 = OpImageTester.createRandomOpImage(layout);
//         dst = new AddOpImage(src1, src2, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("8. Banded double 3-band");
//         layout = OpImageTester.createImageLayout(
//             0, 0, 512, 512, 0, 0, 200, 200, DataBuffer.TYPE_DOUBLE, 3, true);
//         src1 = OpImageTester.createRandomOpImage(layout);
//         src2 = OpImageTester.createRandomOpImage(layout);
//         dst = new AddOpImage(src1, src2, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);
//     }
}
