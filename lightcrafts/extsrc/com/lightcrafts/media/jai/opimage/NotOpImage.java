/*
 * $RCSfile: NotOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:37 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;

import com.lightcrafts.mediax.jai.ColormapOpImage;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import java.util.Map;
// import com.lightcrafts.media.jai.test.OpImageTester;

/**
 * An <code>OpImage</code> implementing the "Not" operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.NotDescriptor</code>.
 *
 * <p>This <code>OpImage</code> performs a logical "not" operation on
 * the the pixel values of the source image on a per-band basis. 
 *
 * <p>The value of the pixel (x, y) in the destination image is defined as:
 * <pre>
 * for (b = 0; b < numBands; b++) {
 *     dst[y][x][b] = ~(src[y][x][b]);
 * }
 * </pre>
 *
 * The data type <code>byte</code> is treated as unsigned, with maximum
 * value as 255 and minimum value as 0.
 *
 * @since EA2
 * @see com.lightcrafts.mediax.jai.operator.NotDescriptor
 * @see NotCRIF
 *
 */
final class NotOpImage extends ColormapOpImage {

    /**
     * Constructs an <code>NotOpImage</code>.
     *
     * @param source  The source image.
     * @param layout   The destination image layout.
     */
    public NotOpImage(RenderedImage source,
                      Map config,
		      ImageLayout layout) {	
        super(source, layout, config, true);

        // Set flag to permit in-place operation.
        permitInPlaceOperation();

        // Initialize the colormap if necessary.
        initializeColormapOperation();
    }

    /**
     * Transform the colormap according to the rescaling parameters.
     */
    protected void transformColormap(byte[][] colormap) {

        for(int b = 0; b < 3; b++) {
            byte[] map = colormap[b];
            int mapSize = map.length;

            for(int i = 0; i < mapSize; i++) {
                map[i] = (byte)(~map[i]);
            }
        }
    }

    /**
     * Nots the pixel values of the source image within a specified
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

        /* For ColormapOpImage, srcRect = destRect. */
        RasterAccessor src = new RasterAccessor(sources[0], destRect,  
                                                formatTags[0], 
                                                getSource(0).getColorModel());
        RasterAccessor dst = new RasterAccessor(dest, destRect,  
                                               formatTags[1], getColorModel());

        if(dst.isBinary()) {
            byte[] srcBits = src.getBinaryDataArray();
            byte[] dstBits = dst.getBinaryDataArray();

            int length = dstBits.length;
            for(int i = 0; i < length; i++) {
                dstBits[i] = (byte)(~(srcBits[i]));
            }

            dst.copyBinaryDataToRaster();

	    return;
        }

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();

        int dstNumBands = dst.getNumBands();
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();

        switch (dst.getDataType()) {
	    
        case DataBuffer.TYPE_BYTE:
            byteLoop(dstNumBands, dstWidth, dstHeight,
                     srcLineStride, srcPixelStride,
                     srcBandOffsets, src.getByteDataArrays(),
                     dstLineStride, dstPixelStride,
                     dstBandOffsets, dst.getByteDataArrays());
            break;
	    
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            shortLoop(dstNumBands, dstWidth, dstHeight,
		      srcLineStride, srcPixelStride,
		      srcBandOffsets, src.getShortDataArrays(),
		      dstLineStride, dstPixelStride,
		      dstBandOffsets, dst.getShortDataArrays());
            break;
	    
        case DataBuffer.TYPE_INT:
            intLoop(dstNumBands, dstWidth, dstHeight,
                    srcLineStride, srcPixelStride,
                    srcBandOffsets, src.getIntDataArrays(),
                    dstLineStride, dstPixelStride,
                    dstBandOffsets, dst.getIntDataArrays());
            break;
        }
	
        dst.copyDataToRaster();
    }

    private void byteLoop(int dstNumBands, int dstWidth, int dstHeight,
                          int srcLineStride, int srcPixelStride,
                          int[] srcBandOffsets, byte[][] srcData,
                          int dstLineStride, int dstPixelStride,
                          int[] dstBandOffsets, byte[][] dstData) {

	for (int b = 0; b < dstNumBands; b++) {
            byte[] s = srcData[b];
            byte[] d = dstData[b];
            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;
                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < dstWidth; w++) {
		    d[dstPixelOffset] = (byte)(~(s[srcPixelOffset]));
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }
   
    private void shortLoop(int dstNumBands, int dstWidth, int dstHeight,
			   int srcLineStride, int srcPixelStride,
			   int[] srcBandOffsets, short[][] srcData,
			   int dstLineStride, int dstPixelStride,
                            int[] dstBandOffsets, short[][] dstData) {
	
	for (int b = 0; b < dstNumBands; b++) {
            short[] s = srcData[b];
            short[] d = dstData[b];
            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;
                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    d[dstPixelOffset] = (short)(~(s[srcPixelOffset]));
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    private void intLoop(int dstNumBands, int dstWidth, int dstHeight,
                         int srcLineStride, int srcPixelStride,
                         int[] srcBandOffsets, int[][] srcData,
                         int dstLineStride, int dstPixelStride,
                         int[] dstBandOffsets, int[][] dstData) {

	for (int b = 0; b < dstNumBands; b++) {
	    int[] s = srcData[b];
	    int[] d = dstData[b];
	    int srcLineOffset = srcBandOffsets[b];
	    int dstLineOffset = dstBandOffsets[b];

	    for (int h = 0; h < dstHeight; h++) {
		int srcPixelOffset = srcLineOffset;
		int dstPixelOffset = dstLineOffset;
		srcLineOffset += srcLineStride;
		dstLineOffset += dstLineStride;
		
		for (int w = 0; w < dstWidth; w++) {
		    d[dstPixelOffset] = ~(s[srcPixelOffset]);
		    srcPixelOffset += srcPixelStride;
		    dstPixelOffset += dstPixelStride;
		}
	    }
	}
    }

//     public static void main(String args[]) {
//         System.out.println("NotOpImage Test");
//         ImageLayout layout;
//         OpImage src, dst;
//         Rectangle rect = new Rectangle(0, 0, 5, 5);

//         System.out.println("1. PixelInterleaved byte 3-band");
//         layout = OpImageTester.createImageLayout(
//             0, 0, 800, 800, 0, 0, 200, 200, DataBuffer.TYPE_BYTE, 3, false);
//         src = OpImageTester.createRandomOpImage(layout);
//         dst = new NotOpImage(src, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("2. Banded byte 3-band");
//         layout = OpImageTester.createImageLayout(
//            0, 0, 800, 800, 0, 0, 200, 200, DataBuffer.TYPE_BYTE, 3, true);
//         src = OpImageTester.createRandomOpImage(layout);
//         dst = new NotOpImage(src, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("3. PixelInterleaved int 3-band");
//         layout = OpImageTester.createImageLayout(
//             0, 0, 512, 512, 0, 0, 200, 200, DataBuffer.TYPE_INT, 3, false);
//         src = OpImageTester.createRandomOpImage(layout);
//         dst = new NotOpImage(src, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("4. Banded int 3-band");
//         layout = OpImageTester.createImageLayout(
//             0, 0, 512, 512, 0, 0, 200, 200, DataBuffer.TYPE_INT, 3, true);
//         src = OpImageTester.createRandomOpImage(layout);
//         dst = new NotOpImage(src, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);
//     }
}
