/*
 * $RCSfile: ErodeBinaryOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/12/08 20:27:59 $
 * $State: Exp $
 */ 
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.AreaOpImage;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.KernelJAI;
import java.util.Map;
import com.lightcrafts.mediax.jai.PixelAccessor;
import com.lightcrafts.mediax.jai.PackedImageData;


/**
 *
 * An OpImage class to perform erosion on a source image.
 *
 * <p> This class implements an erosion operation.
 * 
 * <p> <b>Grey Scale Erosion</b>
 * is a spatial operation that computes
 * each output sample by subtract elements of a kernel to the samples
 * surrounding a particular source sample with some care.
 * A mathematical expression is:
 *
 * <p> For a kernel K with a key position (xKey, yKey), the erosion
 * of image I at (x,y) is given by:
 * <pre>
 *     max{a:  a + K(xKey+i, yKey+j) <= I(x+i,y+j): all (i,j) }
 *
 *      all possible (i,j) means that both I(x+i,y+j) and K(xKey+i, yKey+j)
 *      are in bounds. Otherwise, the value is set to 0.
 *
 * </pre> 
 * <p> Intuitively, the kernel is like an unbrella and the key point
 * is the handle. At every point, you try to push the umbrella up as high
 * as possible but still underneath the image surface. The final height
 * of the handle is the value after erosion. Thus if you want the image
 * to erode from the upper right to bottom left, the following would do.
 *
 * <p><center>
 * <table border=1>
 * <tr align=center><td>0</td><td>0</td><td>X</td> </tr>
 * <tr align=center><td>0</td><td>X</td><td>0</td> </tr>
 * <tr align=center><td><b>X</b></td><td>0</td><td>0</td> </tr>
 * </table></center>
 *
 * <p> Note that zero kernel erosion has effects on the image, the
 * location of the key position and size of kernel all matter.
 * 
 * <p> Pseudo code for the erosion operation is as follows.
 * Assuming the kernel K is of size M rows x N cols
 * and the key position is (xKey, yKey).
 * 
 * <pre>
 * 
 * // erosion
 * for every dst pixel location (x,y){
 *    tmp = infinity;
 *    for (i = -xKey; i < M - xKey; i++){
 *       for (j = -yKey; j < N - yKey; j++){
 *          if((x+i, y+j) are in bounds of src){
 *             tmp = min{tmp, src[x + i][y + j] - K[xKey + i][yKey + j]};
 *          }
 *       }
 *    }
 *    dst[x][y] = tmp;
 *    if (dst[x][y] == infinity)
 *        dst[x][y] = 0;
 * }
 * </pre>
 *
 * <p> The kernel cannot be bigger in any dimension than the image data.
 *
 * <p> <b>Binary Image Erosion</b>
 * requires the kernel to be binary as well.
 * Intuitively, binary erosion slides the kernel
 * key position and place it at every non-zero point (x,y) in the src image.
 * The dst value at this position is set to 1 if all the kernel
 * are fully supported by the src image, and the src image value is 1
 * whenever the kernel has value 1.
 * Otherwise, the value after erosion at (x,y) is set to 0.
 * Erosion usually shrinks images, but it can fill holes
 * with kernels like 
 * <pre> [1 0 1] </pre>
 * and the key position at the center.
 *
 * <p> Pseudo code for the erosion operation is as follows.
 * 
 * <pre>
 * // erosion
 * for every dst pixel location (x,y){
 *    dst[x][y] = 1;
 *    for (i = -xKey; i < M - xKey; i++){
 *       for (j = -yKey; j < N - yKey; j++){
 *         if((x+i,y+j) is out of bounds of src ||
 *             src(x+i, y+j)==0 && Key(xKey+i, yKey+j)==1){
 *            dst[x][y] = 0; break;
 *          }
 *       }
 *    }
 * }
 * </pre>
 *
 * <p> Reference: An Introduction to Nonlinear Image Processing,
 * by Edward R. Bougherty and Jaakko Astola,
 * Spie Optical Engineering Press, 1994.
 *
 *
 * @see KernelJAI
 */


final class ErodeBinaryOpImage extends AreaOpImage {

    /**
     * The kernel with which to do the erode operation.
     */
    protected KernelJAI kernel;

    /** Kernel variables. */
    private int kw, kh, kx, ky;
    private int[]   kdataPack; // Pack kernel into integers;
    private int     kwPack;

    // for factoring things out
    private int dwidth, dheight;
    private int dnumBands;     // should be 1; gray images

    private int bits;          // per packed unit, 8, 16 or 32

    //private int dstBandOffsets[];
    private int dstDBOffset;
    private int dstScanlineStride;
    private int dstScanlineStrideBits;
    private int dstMinX, dstMinY, dstTransX, dstTransY;
    private int dstDataBitOffset;

    //private int srcBandOffsets[];
    private int srcDBOffset;
    private int srcScanlineStride;
    private int srcScanlineStrideBits;
    private int srcMinX, srcMinY, srcTransX, srcTransY;
    private int srcDataBitOffset;

    // Since this operation deals with packed binary data, we do not need
    // to expand the IndexColorModel
    private static Map configHelper(Map configuration) {

	Map config;

	if (configuration == null) {
	    config = new RenderingHints(JAI.KEY_REPLACE_INDEX_COLOR_MODEL,
					Boolean.FALSE);
	} else {

	    config = configuration;

	    if (!(config.containsKey(JAI.KEY_REPLACE_INDEX_COLOR_MODEL))) {
		RenderingHints hints = (RenderingHints)configuration;
		config = (RenderingHints)hints.clone();
		config.put(JAI.KEY_REPLACE_INDEX_COLOR_MODEL, Boolean.FALSE);
	    }
	}

	return config;
    }

    /**
     * Creates a ErodeBinaryOpImage given a ParameterBlock containing the image
     * source and pre-rotated erosion kernel.  The image dimensions are 
     * derived
     * from the source image.  The tile grid layout, SampleModel, and
     * ColorModel may optionally be specified by an ImageLayout
     * object.
     *
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param kernel the pre-rotated erosion KernelJAI.
     */
    public ErodeBinaryOpImage(RenderedImage source,
                           BorderExtender extender,
                           Map config,
                           ImageLayout layout,
                           KernelJAI kernel) {
	super(source,
              layout,
              configHelper(config),
              true,
              extender,
              kernel.getLeftPadding(),
              kernel.getRightPadding(),
              kernel.getTopPadding(),
              kernel.getBottomPadding());
        
	this.kernel = kernel;
	kw = kernel.getWidth();
	kh = kernel.getHeight();
	kx = kernel.getXOrigin();
	ky = kernel.getYOrigin();
	
	kwPack = (kw+31)/32;
	kdataPack  = packKernel(kernel);   
    }

    /**
     * Performs erosion on a specified rectangle. The sources are
     * cobbled.
     *
     * @param sources an array of source Rasters, guaranteed to provide all
     *                necessary source data for computing the output.
     * @param dest a WritableRaster tile containing the area to be computed.
     * @param destRect the rectangle within dest to be processed.
     */
     protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {

        Raster source = sources[0];

	PixelAccessor pa = new PixelAccessor(source.getSampleModel(), null);
	PackedImageData srcIm =
            pa.getPackedPixels(source, source.getBounds(), false, false);

	pa = new PixelAccessor(dest.getSampleModel(), null);
	PackedImageData dstIm =
            pa.getPackedPixels(dest, destRect, true, false);

        // src data under kernel, packed in int.
	int[] srcUK = new int [kwPack * kh];

	// sliding the kernel row by row
	// general the packed matrix under the row
	int dheight = destRect.height;
	int dwidth  = destRect.width;

	int sOffset = srcIm.offset;
	int dOffset = dstIm.offset;
	for (int j = 0; j < dheight; j++) {
            int selement, val, dindex, delement;

	    // reset srcUK for each row beginning
	    // src[sOffset +[-kx:kw-kx, -ky:kh-ky]] placed in srcUK
	    //
	    for (int m = 0; m < srcUK.length; m++){
	        srcUK[m] = 0;
	    }
	    
	    // initial srcUK
	    // first shift left the packed bits under the sliding kernel by 1 bit
	    // then fill (compute) in the last bit of each row
	    for(int i = 0; i < kw -1; i++){
	        bitShiftMatrixLeft(srcUK, kh, kwPack); // expand for speedup?
		int lastCol = kwPack - 1;
		int bitLoc  = srcIm.bitOffset + i;
		int byteLoc = bitLoc >> 3;
		bitLoc = 7 - (bitLoc & 7);
		for(int m=0, sOffsetB = sOffset; 
		    m < kh; 
		    m++, sOffsetB += srcIm.lineStride){

		  selement = (int)srcIm.data[sOffsetB + byteLoc];
		  val = (selement >> bitLoc) & 0x1;
		  srcUK[lastCol] |= val;
		  lastCol += kwPack;
		}
	    }

	    // same as above
	    // also setting dest 
	    for (int i = 0; i < dwidth; i++){

	        bitShiftMatrixLeft(srcUK, kh, kwPack); // expand for speedup?
		int lastCol = kwPack - 1;
		int bitLoc  = srcIm.bitOffset + i + kw -1;
		int byteLoc = bitLoc >> 3;
		bitLoc = 7 - (bitLoc & 7);
		for(int m=0, sOffsetB = sOffset;
		    m < kh;
		    m++, sOffsetB += srcIm.lineStride){

		  selement = (int)srcIm.data[sOffsetB + byteLoc];
		  val = (selement >> bitLoc) & 0x1;
		  srcUK[lastCol] |= val;
		  lastCol += kwPack;
		}


		int dBitLoc = dstIm.bitOffset + i;
		int dshift  = 7 - (dBitLoc & 7);
		int dByteLoc= (dBitLoc >> 3) + dOffset;
		delement  = (int)dstIm.data[dByteLoc];
		delement |= (0x1) << dshift;

		for (int m = 0; m < srcUK.length; m++){
		    if ((srcUK[m] & kdataPack[m]) != kdataPack[m]){
		      delement &= ~((0x1) << dshift);
		      break;
		    }
		}
		dstIm.data[dByteLoc] = (byte)delement;
	    }
            sOffset += srcIm.lineStride;
            dOffset += dstIm.lineStride;
        }
	pa.setPackedPixels(dstIm);
    }


    /** pack kernel into integers by row, aligned to the right;
     *  extra bits on the left are filled with 0 bits
     *  @params  kernel - the given kernel (already rotated)
     *  @returns an integer array of ints from packed kernel data
     */
    private final int[] packKernel(KernelJAI kernel){
	int kw = kernel.getWidth();
	int kh = kernel.getHeight();
        int kwPack = (31+kw)/32;
	int kerPacked[] = new int[kwPack * kh];
        float[] kdata = kernel.getKernelData();
	for (int j=0; j<kw; j++){
	  int m = j;
	  int lastCol = kwPack - 1;
	  bitShiftMatrixLeft(kerPacked, kh, kwPack);
	  for (int i=0; i< kh; i++, lastCol+=kwPack, m+= kw){
	    if (kdata[m] > .9F){     	  // same as == 1.0F
	       kerPacked[lastCol] |= 0x1;
	    }
	  }
	}
	return kerPacked;
    }

    // to shift an integer matrix one bit left
    // assuming that the matrix is row oriented
    // each row is viewed as a long bit array
    // rows and cols are the dimention after packing
    private final static void bitShiftMatrixLeft(int[] mat, int rows, int cols){
        int m = 0;
	for (int i=0; i<rows; i++){
	  for (int j=0; j< cols-1; j++){
	    mat[m] = (mat[m]<< 1) | (mat[m+1] >>> 31);
	    m++;
	  }
	  mat[m] <<= 1;
	  m++;
	}
    }

}
