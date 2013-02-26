/*
 * $RCSfile: AndOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:14 $
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
// import com.lightcrafts.media.jai.test.OpImageTester;

/**
 * An <code>OpImage</code> implementing the "And" operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.AndDescriptor</code>.
 *
 * <p>This <code>OpImage</code> logically "ands" the pixel values of two source
 * images on a per-band basis. In case the two source images have different
 * number of bands, the number of bands for the destination image is the
 * smaller band number of the two source images. That is
 * <code>dstNumBands = Math.min(src1NumBands, src2NumBands)</code>.
 * In case the two source images have different data types, the data type
 * for the destination image is the bigger data type of the two source
 * images.
 *
 * <p>The value of the pixel (x, y) in the destination image is defined as:
 * <pre>
 * for (b = 0; b < numBands; b++) {
 *     dst[y][x][b] = src1[y][x][b] & src2[y][x][b];
 * }
 * </pre>
 *
 * The data type <code>byte</code> is treated as unsigned, with maximum
 * value as 255 and minimum value as 0.
 *
 * @since EA2
 * @see com.lightcrafts.mediax.jai.operator.AndDescriptor
 * @see AndCRIF
 *
 */
final class AndOpImage extends PointOpImage {

    /**
     * Constructs an <code>AndOpImage</code>.
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
    public AndOpImage(RenderedImage source1,
                      RenderedImage source2,
                      Map config,
		      ImageLayout layout) {
        super(source1, source2, layout, config, true);

        // Set flag to permit in-place operation.
        permitInPlaceOperation();
    }

    /**
     * Multiplies the pixel values of two source images within a specified
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
            byte[] src1Bits = s1.getBinaryDataArray();
            byte[] src2Bits = s2.getBinaryDataArray();
            byte[] dstBits = d.getBinaryDataArray();

            int length = dstBits.length;
            for(int i = 0; i < length; i++) {
                dstBits[i] = (byte)(src1Bits[i] & src2Bits[i]);
            }

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

	for (int b = 0; b < dstNumBands; b++) {
            byte[] s1 = src1Data[b];
            byte[] s2 = src2Data[b];
            byte[] d = dstData[b];
            int src1LineOffset = src1BandOffsets[b];
            int src2LineOffset = src2BandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int src1PixelOffset = src1LineOffset;
                int src2PixelOffset = src2LineOffset;
                int dstPixelOffset = dstLineOffset;
                src1LineOffset += src1LineStride;
                src2LineOffset += src2LineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < dstWidth; w++) {
		    d[dstPixelOffset] = (byte)(s1[src1PixelOffset] &
					       s2[src2PixelOffset]);
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
	
	for (int b = 0; b < dstNumBands; b++) {
            short[] s1 = src1Data[b];
            short[] s2 = src2Data[b];
            short[] d = dstData[b];
            int src1LineOffset = src1BandOffsets[b];
            int src2LineOffset = src2BandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int src1PixelOffset = src1LineOffset;
                int src2PixelOffset = src2LineOffset;
                int dstPixelOffset = dstLineOffset;
                src1LineOffset += src1LineStride;
                src2LineOffset += src2LineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    d[dstPixelOffset] = (short)(s1[src1PixelOffset] &
						s2[src2PixelOffset]);

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

	for (int b = 0; b < dstNumBands; b++) {
	    int[] s1 = src1Data[b];
	    int[] s2 = src2Data[b];
	    int[] d = dstData[b];
	    int src1LineOffset = src1BandOffsets[b];
	    int src2LineOffset = src2BandOffsets[b];
	    int dstLineOffset = dstBandOffsets[b];

	    for (int h = 0; h < dstHeight; h++) {
		int src1PixelOffset = src1LineOffset;
		int src2PixelOffset = src2LineOffset;
		int dstPixelOffset = dstLineOffset;
		src1LineOffset += src1LineStride;
		src2LineOffset += src2LineStride;
		dstLineOffset += dstLineStride;
		
		for (int w = 0; w < dstWidth; w++) {
		    d[dstPixelOffset] = s1[src1PixelOffset] & s2[src2PixelOffset];
		    
		    src1PixelOffset += src1PixelStride;
		    src2PixelOffset += src2PixelStride;
		    dstPixelOffset += dstPixelStride;
		}
	    }
	}
    }

//     public static void main(String args[]) {
//         System.out.println("AndOpImage Test");
//         ImageLayout layout;
//         OpImage src1, src2, dst;
//         Rectangle rect = new Rectangle(0, 0, 5, 5);

//         System.out.println("1. PixelInterleaved byte 3-band");
//         layout = OpImageTester.createImageLayout(
//             0, 0, 800, 800, 0, 0, 200, 200, DataBuffer.TYPE_BYTE, 3, false);
//         src1 = OpImageTester.createRandomOpImage(layout);
//         src2 = OpImageTester.createRandomOpImage(layout);
//         dst = new AndOpImage(src1, src2, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("2. Banded byte 3-band");
//         layout = OpImageTester.createImageLayout(
//            0, 0, 800, 800, 0, 0, 200, 200, DataBuffer.TYPE_BYTE, 3, true);
//         src1 = OpImageTester.createRandomOpImage(layout);
//         src2 = OpImageTester.createRandomOpImage(layout);
//         dst = new AndOpImage(src1, src2, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("3. PixelInterleaved int 3-band");
//         layout = OpImageTester.createImageLayout(
//             0, 0, 512, 512, 0, 0, 200, 200, DataBuffer.TYPE_INT, 3, false);
//         src1 = OpImageTester.createRandomOpImage(layout);
//         src2 = OpImageTester.createRandomOpImage(layout);
//         dst = new AndOpImage(src1, src2, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("4. Banded int 3-band");
//         layout = OpImageTester.createImageLayout(
//             0, 0, 512, 512, 0, 0, 200, 200, DataBuffer.TYPE_INT, 3, true);
//         src1 = OpImageTester.createRandomOpImage(layout);
//         src2 = OpImageTester.createRandomOpImage(layout);
//         dst = new AndOpImage(src1, src2, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);
//     }
}
