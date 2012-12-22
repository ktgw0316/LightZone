/*
 * $RCSfile: DivideOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:24 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
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
 * An <code>OpImage</code> implementing the "Divide" operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.DivideDescriptor</code>.
 *
 * <p>This <code>OpImage</code> divides the pixel values of the first source
 * image by the second source image pixels on a per-band basis. In case
 * the two source images have different number of bands, the number of
 * bands for the destination image is the smaller of the number of bands
 * of the two source images. That is
 * <code>dstNumBands = Math.min(src1NumBands, src2NumBands)</code>.
 * In case the two source images have different data types, the data type
 * for the destination image is the bigger data type of the two source
 * images.
 *
 * <p>The value of the pixel (x, y) in the destination image is defined as:
 * <pre>
 * for (b = 0; b < numBands; b++) {
 *     dst[y][x][b] = src1[y][x][b] / src2[y][x][b];
 * }
 * </pre>
 *
 * <p>If the result of the division overflows/underflows the
 * maximum/minimum value supported by the destination image, then it
 * will be clamped to the maximum/minimum value respectively. The
 * data type <code>byte</code> is treated as unsigned, with maximum
 * value as 255 and minimum value as 0.
 *
 * @since EA2
 * @see com.lightcrafts.mediax.jai.operator.DivideDescriptor
 * @see DivideCRIF
 *
 */
final class DivideOpImage extends PointOpImage {

    private byte[][] divideTableByte;

    /* Source 1 band increment */
    private int s1bd = 1;

    /* Source 2 band increment */
    private int s2bd = 1;

    /**
     * Constructs an <code>DivideOpImage</code>.
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
    public DivideOpImage(RenderedImage source1,
			 RenderedImage source2,
                         Map config,
			 ImageLayout layout) {
        super(source1, source2, layout, config, true);

        // Get the source band counts.
        int numBands1 = source1.getSampleModel().getNumBands();
        int numBands2 = source2.getSampleModel().getNumBands();

        // Handle the special case of dividing each band of an N-band
        // image by a 1-band image.
        int numBandsDst;
        if(layout != null && layout.isValid(ImageLayout.SAMPLE_MODEL_MASK)) {
            SampleModel sm = layout.getSampleModel(null);
            numBandsDst = sm.getNumBands();

            // The second source must be single-banded and the first must
            // be multi-banded.
            if(numBandsDst > 1 &&
               ((numBands1 > 1 && numBands2 == 1) ||
                (numBands1 == 1 && numBands2 > 1))) {
                // Clamp the destination band count to the number of
                // bands in the multi-band source.
                numBandsDst = Math.min(Math.max(numBands1, numBands2),
                                       numBandsDst);

                // Create a new SampleModel if necessary.
                if(numBandsDst != sampleModel.getNumBands()) {
                    sampleModel =
                        RasterFactory.createComponentSampleModel(
                            sm,
                            sampleModel.getTransferType(),
                            sampleModel.getWidth(),
                            sampleModel.getHeight(),
                            numBandsDst);

                    if(colorModel != null &&
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

        if (sampleModel.getTransferType() == DataBuffer.TYPE_BYTE) {
            /* Initialize divideTableByte. */
            divideTableByte = new byte[256][256];
            for (int j = 0; j < 256; j++) {
                byte[] array = divideTableByte[j];

		if (j > 0) {
		    array[0] = (byte)255;
		} else {
		    array[0] = 0;
		}

                for (int i = 1; i < 256; i++) {
                    array[i] = ImageUtil.clampRoundByte((float)j/(float)i);
                }
            }
        }

        // Set flag to permit in-place operation.
        permitInPlaceOperation();
    }

    /**
     * Divides the pixel values of two source images within a specified
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

        /* For PointOpImage, srcRect = destRect. */
        RasterAccessor s1 = new RasterAccessor(sources[0], destRect, 
                                               formatTags[0], 
                                               getSourceImage(0).getColorModel());
        RasterAccessor s2 = new RasterAccessor(sources[1], destRect,
                                               formatTags[1], 
                                               getSourceImage(1).getColorModel());
        RasterAccessor d = new RasterAccessor(dest, destRect,
                                              formatTags[2], getColorModel());
 
        if(d.isBinary()) {
            byte[] dstBits = d.getBinaryDataArray();

            // Subtraction in this case boils down to copying image 1.
            System.arraycopy(s1.getBinaryDataArray(), 0,
                             dstBits, 0, dstBits.length);

            d.copyBinaryDataToRaster();

	    return;
        }

        int src1LineStride = s1.getScanlineStride();
        int src1PixelStride = s1.getPixelStride();
        int[] src1BandOffsets = s1.getBandOffsets();

        int src2LineStride = s2.getScanlineStride();
        int src2PixelStride = s2.getPixelStride();
        int[] src2BandOffsets = s2.getBandOffsets();

        int dstNumBands = d.getNumBands();
        int dstWidth = d.getWidth();
        int dstHeight = d.getHeight();
        int dstLineStride = d.getScanlineStride();
        int dstPixelStride = d.getPixelStride();
        int[] dstBandOffsets = d.getBandOffsets();

        switch (d.getDataType()) {

        case DataBuffer.TYPE_BYTE:
            byteLoop(dstNumBands, dstWidth, dstHeight,
                     src1LineStride, src1PixelStride,
                     src1BandOffsets, s1.getByteDataArrays(),
                     src2LineStride, src2PixelStride,
                     src2BandOffsets, s2.getByteDataArrays(),
                     dstLineStride, dstPixelStride,
                     dstBandOffsets, d.getByteDataArrays());
            break;

	case DataBuffer.TYPE_USHORT:
            ushortLoop(dstNumBands, dstWidth, dstHeight,
                       src1LineStride, src1PixelStride,
                       src1BandOffsets, s1.getShortDataArrays(),
                       src2LineStride, src2PixelStride,
                       src2BandOffsets, s2.getShortDataArrays(),
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, d.getShortDataArrays());
            break;

	case DataBuffer.TYPE_SHORT:
            shortLoop(dstNumBands, dstWidth, dstHeight,
                      src1LineStride, src1PixelStride,
                      src1BandOffsets, s1.getShortDataArrays(),
                      src2LineStride, src2PixelStride,
                      src2BandOffsets, s2.getShortDataArrays(),
                      dstLineStride, dstPixelStride,
                      dstBandOffsets, d.getShortDataArrays());
            break;

	case DataBuffer.TYPE_INT:
            intLoop(dstNumBands, dstWidth, dstHeight,
                    src1LineStride, src1PixelStride,
                    src1BandOffsets, s1.getIntDataArrays(),
                    src2LineStride, src2PixelStride,
                    src2BandOffsets, s2.getIntDataArrays(),
                    dstLineStride, dstPixelStride,
                    dstBandOffsets, d.getIntDataArrays());
            break;

	case DataBuffer.TYPE_FLOAT:
            floatLoop(dstNumBands, dstWidth, dstHeight,
                      src1LineStride, src1PixelStride,
                      src1BandOffsets, s1.getFloatDataArrays(),
                      src2LineStride, src2PixelStride,
                      src2BandOffsets, s2.getFloatDataArrays(),
                      dstLineStride, dstPixelStride,
                      dstBandOffsets, d.getFloatDataArrays());
            break;

        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(dstNumBands, dstWidth, dstHeight,
                       src1LineStride, src1PixelStride,
                       src1BandOffsets, s1.getDoubleDataArrays(),
                       src2LineStride, src2PixelStride,
                       src2BandOffsets, s2.getDoubleDataArrays(),
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, d.getDoubleDataArrays());
            break;
        }

        if (d.needsClamping()) {
            d.clampDataArrays();
        }

        d.copyDataToRaster();
    }

    private void byteLoop(int dstNumBands, int dstWidth, int dstHeight,
                          int src1LineStride, int src1PixelStride,
                          int[] src1BandOffsets, byte[][] src1Data,
                          int src2LineStride, int src2PixelStride,
                          int[] src2BandOffsets, byte[][] src2Data,
                          int dstLineStride, int dstPixelStride,
                          int[] dstBandOffsets, byte[][] dstData) {

        for (int b = 0, s1b = 0, s2b = 0; b < dstNumBands;
             b++, s1b += s1bd, s2b += s2bd) {
            byte[] s1 = src1Data[s1b];
            byte[] s2 = src2Data[s2b];
            byte[] d = dstData[b];
            int src1LineOffset = src1BandOffsets[s1b];
            int src2LineOffset = src2BandOffsets[s2b];
            int dstLineOffset = dstBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int src1PixelOffset = src1LineOffset;
                int src2PixelOffset = src2LineOffset;
                int dstPixelOffset = dstLineOffset;
                src1LineOffset += src1LineStride;
                src2LineOffset += src2LineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    d[dstPixelOffset] =
			divideTableByte[s1[src1PixelOffset]
				       & 0xFF][s2[src2PixelOffset]&0xFF];
                    src1PixelOffset += src1PixelStride;
                    src2PixelOffset += src2PixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    private void ushortLoop(int dstNumBands, int dstWidth, int dstHeight,
                            int src1LineStride, int src1PixelStride,
                            int[] src1BandOffsets, short[][] src1Data,
                            int src2LineStride, int src2PixelStride,
                            int[] src2BandOffsets, short[][] src2Data,
                            int dstLineStride, int dstPixelStride,
                            int[] dstBandOffsets, short[][] dstData) {

	for (int b = 0, s1b = 0, s2b = 0; b < dstNumBands;
             b++, s1b += s1bd, s2b += s2bd) {
            short[] s1 = src1Data[s1b];
            short[] s2 = src2Data[s2b];
            short[] d = dstData[b];
	    float f1, f2;
            int src1LineOffset = src1BandOffsets[s1b];
            int src2LineOffset = src2BandOffsets[s2b];
            int dstLineOffset = dstBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int src1PixelOffset = src1LineOffset;
                int src2PixelOffset = src2LineOffset;
                int dstPixelOffset = dstLineOffset;
                src1LineOffset += src1LineStride;
                src2LineOffset += src2LineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < dstWidth; w++) {
		    f1 = (float)(s1[src1PixelOffset] & 0xffff);
		    f2 = (float)(s2[src2PixelOffset] & 0xffff);

                    if (f1 == 0) {
			// 0 divided by any value is defined to return 0
			d[dstPixelOffset] = 0;
                    } else if (f2 == 0) {
			// Anything other than 0 divided by zero, returns
			// the max value
			d[dstPixelOffset] = (short)0xffff;
                    } else {
			d[dstPixelOffset] = ImageUtil.clampRoundUShort(f1/f2);
                    }

                    src1PixelOffset += src1PixelStride;
                    src2PixelOffset += src2PixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    private void shortLoop(int dstNumBands, int dstWidth, int dstHeight,
                           int src1LineStride, int src1PixelStride,
                           int[] src1BandOffsets, short[][] src1Data,
                           int src2LineStride, int src2PixelStride,
                           int[] src2BandOffsets, short[][] src2Data,
                           int dstLineStride, int dstPixelStride,
                           int[] dstBandOffsets, short[][] dstData) {

        for (int b = 0, s1b = 0, s2b = 0; b < dstNumBands;
             b++, s1b += s1bd, s2b += s2bd) {
            short[] s1 = src1Data[s1b];
            short[] s2 = src2Data[s2b];
            short[] d = dstData[b];
	    float f1, f2;
            int src1LineOffset = src1BandOffsets[s1b];
            int src2LineOffset = src2BandOffsets[s2b];
            int dstLineOffset = dstBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int src1PixelOffset = src1LineOffset;
                int src2PixelOffset = src2LineOffset;
                int dstPixelOffset = dstLineOffset;
                src1LineOffset += src1LineStride;
                src2LineOffset += src2LineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < dstWidth; w++) {
		    f1 = (float)s1[src1PixelOffset];
		    f2 = (float)s2[src2PixelOffset];

                    if (f1 == 0) {
			// 0 divided by any value is defined to return 0
			d[dstPixelOffset] = 0;
                    } else if (f2 == 0) {
			if ( f1 < 0 ) {
			    d[dstPixelOffset] = Short.MIN_VALUE;
			} else {
			    d[dstPixelOffset] = Short.MAX_VALUE;
			}
                    } else {
			d[dstPixelOffset] = ImageUtil.clampRoundShort(f1/f2);
                    }

                    src1PixelOffset += src1PixelStride;
                    src2PixelOffset += src2PixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    private void intLoop(int dstNumBands, int dstWidth, int dstHeight,
                         int src1LineStride, int src1PixelStride,
                         int[] src1BandOffsets, int[][] src1Data,
                         int src2LineStride, int src2PixelStride,
                         int[] src2BandOffsets, int[][] src2Data,
                         int dstLineStride, int dstPixelStride,
                         int[] dstBandOffsets, int[][] dstData) {
        /*
         * The destination data type may be any of the integral data types.
         * The "clamp" function must clamp to the appropriate range for
         * that data type.
         */
        switch (sampleModel.getTransferType()) {

        case DataBuffer.TYPE_BYTE:
            for (int b = 0, s1b = 0, s2b = 0; b < dstNumBands;
                 b++, s1b += s1bd, s2b += s2bd) {
                int[] s1 = src1Data[s1b];
                int[] s2 = src2Data[s2b];
                int[] d = dstData[b];
		float f1, f2;
                int src1LineOffset = src1BandOffsets[s1b];
                int src2LineOffset = src2BandOffsets[s2b];
                int dstLineOffset = dstBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int src1PixelOffset = src1LineOffset;
                    int src2PixelOffset = src2LineOffset;
                    int dstPixelOffset = dstLineOffset;
                    src1LineOffset += src1LineStride;
                    src2LineOffset += src2LineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < dstWidth; w++) {
			f1 = (float)(s1[src1PixelOffset] & 0xFF);
			f2 = (float)(s2[src2PixelOffset] & 0xFF);

			if ( f1 == 0 ) {
			    // since bytes are unsigned, lowest value is 0
			    d[dstPixelOffset] = 0;
			} else if (f2 == 0) {
			    // +ve no divided by 0 = datatype's max value
			    d[dstPixelOffset] = 255;
			} else {
			    d[dstPixelOffset] = ImageUtil.clampRoundByte(f1/f2);
			}

                        src1PixelOffset += src1PixelStride;
                        src2PixelOffset += src2PixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }
            break;

	case DataBuffer.TYPE_USHORT:
            for (int b = 0, s1b = 0, s2b = 0; b < dstNumBands;
                 b++, s1b += s1bd, s2b += s2bd) {
                int[] s1 = src1Data[s1b];
                int[] s2 = src2Data[s2b];
                int[] d = dstData[b];
		float f1, f2;
                int src1LineOffset = src1BandOffsets[s1b];
                int src2LineOffset = src2BandOffsets[s2b];
                int dstLineOffset = dstBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int src1PixelOffset = src1LineOffset;
                    int src2PixelOffset = src2LineOffset;
                    int dstPixelOffset = dstLineOffset;
                    src1LineOffset += src1LineStride;
                    src2LineOffset += src2LineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < dstWidth; w++) {
			f1 = (float)(s1[src1PixelOffset] & 0xFFFF);
			f2 = (float)(s2[src2PixelOffset] & 0xFFFF);

			if (f1 == 0) {
			    // The minimum value for ushort is 0
			    d[dstPixelOffset] = 0;
			} else if (f2 == 0) {
			    // +ve no divided by 0 = datatype's max value
			    d[dstPixelOffset] = (short) 0xffff;
			} else {
			    d[dstPixelOffset] = ImageUtil.clampRoundUShort(f1/f2);
			}

                        src1PixelOffset += src1PixelStride;
                        src2PixelOffset += src2PixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }
            break;
	    
        case DataBuffer.TYPE_SHORT:
            for (int b = 0, s1b = 0, s2b = 0; b < dstNumBands;
                 b++, s1b += s1bd, s2b += s2bd) {
                int[] s1 = src1Data[s1b];
                int[] s2 = src2Data[s2b];
                int[] d = dstData[b];
		float f1, f2;
                int src1LineOffset = src1BandOffsets[s1b];
                int src2LineOffset = src2BandOffsets[s2b];
                int dstLineOffset = dstBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int src1PixelOffset = src1LineOffset;
                    int src2PixelOffset = src2LineOffset;
                    int dstPixelOffset = dstLineOffset;
                    src1LineOffset += src1LineStride;
                    src2LineOffset += src2LineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < dstWidth; w++) {
			f1 = (float)s1[src1PixelOffset];
			f2 = (float)s2[src2PixelOffset];

			if (f1 == 0 ) {
			    d[dstPixelOffset] = 0;
			} else if (f2 == 0) {
			    if ( f1 < 0 ) {
			        // 0 divided by 0 is defined to return 0
			        // -ve no divided by 0 = datatype's min value
			        d[dstPixelOffset] = Short.MIN_VALUE;
			    } else {
				d[dstPixelOffset] = Short.MAX_VALUE;
			    }
			} else {
			    d[dstPixelOffset] = ImageUtil.clampRoundShort(f1/f2);
			}

                        src1PixelOffset += src1PixelStride;
                        src2PixelOffset += src2PixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }
            break;
	    
        case DataBuffer.TYPE_INT:
            for (int b = 0, s1b = 0, s2b = 0; b < dstNumBands;
                 b++, s1b += s1bd, s2b += s2bd) {
                int[] s1 = src1Data[s1b];
                int[] s2 = src2Data[s2b];
                int[] d = dstData[b];
		float f1, f2;
                int src1LineOffset = src1BandOffsets[s1b];
                int src2LineOffset = src2BandOffsets[s2b];
                int dstLineOffset = dstBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int src1PixelOffset = src1LineOffset;
                    int src2PixelOffset = src2LineOffset;
                    int dstPixelOffset = dstLineOffset;
                    src1LineOffset += src1LineStride;
                    src2LineOffset += src2LineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < dstWidth; w++) {
			f1 = (float)s1[src1PixelOffset];
			f2 = (float)s2[src2PixelOffset];

			if (f1 == 0) {
			    // 0 divided by 0 or any number is defined to return 0
			    d[dstPixelOffset] = 0;
			} else if (f2 == 0) {
			    if ( f1 < 0 ) {
				d[dstPixelOffset] = Integer.MIN_VALUE;
			    } else {
				d[dstPixelOffset] = Integer.MAX_VALUE;
			    }
			} else {
			    d[dstPixelOffset] = ImageUtil.clampRoundInt(f1/f2);
			}

                        src1PixelOffset += src1PixelStride;
                        src2PixelOffset += src2PixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }
            break;
        }
    }

    private void floatLoop(int dstNumBands, int dstWidth, int dstHeight,
                           int src1LineStride, int src1PixelStride,
                           int[] src1BandOffsets, float[][] src1Data,
                           int src2LineStride, int src2PixelStride,
                           int[] src2BandOffsets, float[][] src2Data,
                           int dstLineStride, int dstPixelStride,
                           int[] dstBandOffsets, float[][] dstData) {

        for (int b = 0, s1b = 0, s2b = 0; b < dstNumBands;
             b++, s1b += s1bd, s2b += s2bd) {
            float[] s1 = src1Data[s1b];
            float[] s2 = src2Data[s2b];
            float[] d = dstData[b];
            int src1LineOffset = src1BandOffsets[s1b];
            int src2LineOffset = src2BandOffsets[s2b];
            int dstLineOffset = dstBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int src1PixelOffset = src1LineOffset;
                int src2PixelOffset = src2LineOffset;
                int dstPixelOffset = dstLineOffset;
                src1LineOffset += src1LineStride;
                src2LineOffset += src2LineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < dstWidth; w++) {
		    // follows IEEE-754 standard
		    d[dstPixelOffset] = s1[src1PixelOffset] / s2[src2PixelOffset];

                    src1PixelOffset += src1PixelStride;
                    src2PixelOffset += src2PixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    private void doubleLoop(int dstNumBands, int dstWidth, int dstHeight,
                            int src1LineStride, int src1PixelStride,
                            int[] src1BandOffsets, double[][] src1Data,
                            int src2LineStride, int src2PixelStride,
                            int[] src2BandOffsets, double[][] src2Data,
                            int dstLineStride, int dstPixelStride,
                            int[] dstBandOffsets, double[][] dstData) {

        for (int b = 0, s1b = 0, s2b = 0; b < dstNumBands;
             b++, s1b += s1bd, s2b += s2bd) {
            double[] s1 = src1Data[s1b];
            double[] s2 = src2Data[s2b];
            double[] d = dstData[b];
            int src1LineOffset = src1BandOffsets[s1b];
            int src2LineOffset = src2BandOffsets[s2b];
            int dstLineOffset = dstBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int src1PixelOffset = src1LineOffset;
                int src2PixelOffset = src2LineOffset;
                int dstPixelOffset = dstLineOffset;
                src1LineOffset += src1LineStride;
                src2LineOffset += src2LineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < dstWidth; w++) {
		    // follows IEEE-754 standard
		    d[dstPixelOffset] = s1[src1PixelOffset] / s2[src2PixelOffset];

                    src1PixelOffset += src1PixelStride;
                    src2PixelOffset += src2PixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

//     public static void main(String args[]) {
//         System.out.println("DivideOpImage Test");
//         ImageLayout layout;
//         OpImage src1, src2, dst;
//         Rectangle rect = new Rectangle(0, 0, 5, 5);

//         System.out.println("1. PixelInterleaved byte 3-band");
//         layout = OpImageTester.createImageLayout(
//             0, 0, 800, 800, 0, 0, 200, 200, DataBuffer.TYPE_BYTE, 3, false);
//         src1 = OpImageTester.createRandomOpImage(layout);
//         src2 = OpImageTester.createRandomOpImage(layout);
//         dst = new DivideOpImage(src1, src2, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("2. Banded byte 3-band");
//         layout = OpImageTester.createImageLayout(
//            0, 0, 800, 800, 0, 0, 200, 200, DataBuffer.TYPE_BYTE, 3, true);
//         src1 = OpImageTester.createRandomOpImage(layout);
//         src2 = OpImageTester.createRandomOpImage(layout);
//         dst = new DivideOpImage(src1, src2, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("3. PixelInterleaved int 3-band");
//         layout = OpImageTester.createImageLayout(
//             0, 0, 512, 512, 0, 0, 200, 200, DataBuffer.TYPE_INT, 3, false);
//         src1 = OpImageTester.createRandomOpImage(layout);
//         src2 = OpImageTester.createRandomOpImage(layout);
//         dst = new DivideOpImage(src1, src2, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("4. Banded int 3-band");
//         layout = OpImageTester.createImageLayout(
//             0, 0, 512, 512, 0, 0, 200, 200, DataBuffer.TYPE_INT, 3, true);
//         src1 = OpImageTester.createRandomOpImage(layout);
//         src2 = OpImageTester.createRandomOpImage(layout);
//         dst = new DivideOpImage(src1, src2, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("5. PixelInterleaved float 3-band");
//         layout = OpImageTester.createImageLayout(
//             0, 0, 512, 512, 0, 0, 200, 200, DataBuffer.TYPE_FLOAT, 3, false);
//         src1 = OpImageTester.createRandomOpImage(layout);
//         src2 = OpImageTester.createRandomOpImage(layout);
//         dst = new DivideOpImage(src1, src2, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("6. Banded float 3-band");
//         layout = OpImageTester.createImageLayout(
//             0, 0, 512, 512, 0, 0, 200, 200, DataBuffer.TYPE_FLOAT, 3, true);
//         src1 = OpImageTester.createRandomOpImage(layout);
//         src2 = OpImageTester.createRandomOpImage(layout);
//         dst = new DivideOpImage(src1, src2, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("7. PixelInterleaved double 3-band");
//         layout = OpImageTester.createImageLayout(
//             0, 0, 512, 512, 0, 0, 200, 200, DataBuffer.TYPE_DOUBLE, 3, false);
//         src1 = OpImageTester.createRandomOpImage(layout);
//         src2 = OpImageTester.createRandomOpImage(layout);
//         dst = new DivideOpImage(src1, src2, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("8. Banded double 3-band");
//         layout = OpImageTester.createImageLayout(
//             0, 0, 512, 512, 0, 0, 200, 200, DataBuffer.TYPE_DOUBLE, 3, true);
//         src1 = OpImageTester.createRandomOpImage(layout);
//         src2 = OpImageTester.createRandomOpImage(layout);
//         dst = new DivideOpImage(src1, src2, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);
//     }
}
