/*
 * $RCSfile: DilateBinaryOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/12/08 20:27:58 $
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
 * An OpImage class to perform dilation on a source image.
 *
 * Dilation for grey scale images can be charaterized by "slide, add and max",
 * while for binary images by "slide and set". As always, the kernel
 * is expected to come with a key position.
 *
 * <p> <b> Grey scale dilation</b> is a spatial operation that computes
 * each output sample by adding elements of a kernel to the samples
 * surrounding a particular source sample and taking the maximum.
 * A mathematical expression is:
 *
 * <p> For a kernel K with a key position (xKey,yKey), the dilation
 * of image I at (x,y) is given by:
 * <pre>
 *     max{ I(x-i, y-j) + K(xKey+i, yKey+j): some (i,j) restriction }
 *
 *      where the (i,j) restriction means:
 *      all possible (i,j) so that both I(x-i,y-j) and K(xKey+i, yKey+j)
 *      are defined, that is, these indecies are in bounds.
 *
 * </pre>
 * <p>Intuitively in 2D, the kernel is like
 * an unbrella and the key point is the handle. When the handle moves
 * all over the image surface, the upper outbounds of all the umbrella
 * positions is the dilation. Thus if you want the image to dilate in
 * the upper right direction, the following kernel would do with
 * the bold face key position.
 *
 * <p><center>
 * <table border=1>
 * <tr align=center><td>0</td><td>0</td><td>50</td> </tr>
 * <tr align=center><td>0</td><td>50</td><td>0</td> </tr>
 * <tr align=center><td><b>0</b></td><td>0</td><td>0</td> </tr>
 * </table></center>
 *
 * <p> Note also that zero kernel have effects on the dilation!
 * That is because of the "max" in the add and max process. Thus
 * a 3 x 1 zero kernel with the key persion at the bottom of the kernel
 * dilates the image upwards.
 *
 * <p>
 * After the kernel is rotated 180 degrees, Pseudo code for dilation operation
 * is as follows. Of course, you should provide the kernel in its
 * (unrotated) original form. Assuming the kernel K is of size M rows x N cols
 * and the key position is (xKey, yKey).
 *
 * // dilation
 * for every dst pixel location (x,y){
 *    dst[x][y] = -infinity;
 *    for (i = -xKey; i < M - xKey; i++){
 *       for (j = -yKey; j < N - yKey; j++){
 *          if((x+i, y+j) are in bounds of src &&
 *	      (xKey+i, yKey+j) are in bounds of K){
 *             tmp = src[x + i][y + j]+ K[xKey + i][yKey + j];
 *	       dst[x][y] = max{tmp, dst[x][y]};
 *          }
 *       }
 *    }
 * }
 * </pre>
 *
 * <p> Dilation, unlike convolution and most neighborhood operations,
 * actually can grow the image region. But to conform with other
 * image neighborhood operations, the border pixels are set to 0.
 * For a 3 x 3 kernel with the key point at the center, there will
 * be a pixel wide 0 stripe around the border.
 *
 * <p> The kernel cannot be bigger in any dimension than the image data.
 *
 * <p> <b>Binary Image Dilation</b>
 * requires the kernel K to be binary.
 * Intuitively, starting from dst image being a duplicate of src,
 * binary dilation slides the kernel K to place the key position
 * at every non-zero point (x,y) in src image and set dst positions
 * under ones of K to 1.
 *
 * <p> After the kernel is rotated 180 degrees, the pseudo code for
 * dilation operation is as follows. (Of course, you should provide
 * the kernel in its original unrotated form.)
 *
 * <pre>
 *
 * // dilating
 * for every dst pixel location (x,y){
 *    dst[x][y] = src[x][y];
 *    for (i = -xKey; i < M - xKey; i++){
 *       for (j = -yKey; j < N - yKey; j++){
 *         if(src[x+i,y+i]==1 && Key(xKey+i, yKey+j)==1){
 *            dst[x][y] = 1; break;
 *          }
 *       }
 *    }
 * }
 * </pre>

 * <p> Reference: An Introduction to Nonlinear Image Processing,
 * by Edward R. Bougherty and Jaakko Astola,
 * Spie Optical Engineering Press, 1994.
 *
 *
 * @see KernelJAI
 */
final class DilateBinaryOpImage extends AreaOpImage {

    /**
     * The kernel with which to do the dilate operation.
     */
    protected KernelJAI kernel;

    /** Kernel variables. */
    private int kw, kh, kx, ky;
    private int[]   kdataPack; // Pack kernel into int;
    private int     kwPack;    // num of int needed to pack each row of the kernel

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
		config.put(JAI.KEY_REPLACE_INDEX_COLOR_MODEL, Boolean.FALSE);
		RenderingHints hints = (RenderingHints)configuration;
		config = (RenderingHints)hints.clone();
	    }
	}

	return config;
    }

    /**
     * Creates a DilateBinaryOpImage given a ParameterBlock containing the 
     * image source and pre-rotated dilation kernel.  The image dimensions 
     * are derived from the source image.  The tile grid layout, SampleModel, 
     * and ColorModel may optionally be specified by an ImageLayout object.
     *
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param kernel the pre-rotated dilation KernelJAI.
     */
    public DilateBinaryOpImage(RenderedImage source,
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
     * Performs dilation on a specified rectangle. The sources are
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

		// set dest bits
		for (int m = 0; m < srcUK.length; m++){
		    if ((srcUK[m] & kdataPack[m]) != 0){
    		      int dBitLoc = dstIm.bitOffset + i;
		      int dshift  = 7 - (dBitLoc & 7);
		      int dByteLoc= (dBitLoc >> 3) + dOffset;
		      delement  = (int)dstIm.data[dByteLoc];
		      delement |= (0x1) << dshift;
		      dstIm.data[dByteLoc] = (byte)delement;
		      break;
		    }
		}

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
