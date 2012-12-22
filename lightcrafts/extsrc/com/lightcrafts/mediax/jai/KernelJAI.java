/*
 * $RCSfile: KernelJAI.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:11 $
 * $State: Exp $
 */ 
package com.lightcrafts.mediax.jai;
import java.awt.image.Kernel;
import java.io.Serializable;
import com.lightcrafts.mediax.jai.JaiI18N;
 
/**
 * A kernel representing a matrix with a key position,
 *  used by operators such as <code> Convolve </code>.
 *
 * <p> A <code>KernelJAI</code> is characterized by its width, height, and
 * origin, or key element. The key element is the element which is placed
 * over the current source pixel to perform convolution or error diffusion.
 * 
 * <p>A kernel K is separable it the outer product of two one-dimensional
 * vectors. It can speed up computation. One can construct a kernel
 * from two one-dimensional vectors. 
 *
 * <>The symmetry can be useful (such as computation speedup). Currently
 * the protected instance variables isHorizonallySymmetric
 * and  isVerticallySymmetric are set to false.
 *
 *
 * @see com.lightcrafts.mediax.jai.operator.ConvolveDescriptor
 * @see com.lightcrafts.mediax.jai.operator.OrderedDitherDescriptor
 * @see com.lightcrafts.mediax.jai.operator.ErrorDiffusionDescriptor
 */
public class KernelJAI extends Object implements Serializable {

    /**
     * Floyd and Steinberg error filter (1975).
     * <pre>
     * (1/16 x)  [   * 7 ]
     *           [ 3 5 1 ]
     * </pre>
     */
    public static final KernelJAI ERROR_FILTER_FLOYD_STEINBERG =
        new KernelJAI(3, 2, 1, 0,
                      new float[] {0.0F/16.0F, 0.0F/16.0F, 7.0F/16.0F,
                                   3.0F/16.0F, 5.0F/16.0F, 1.0F/16.0F});

    /**
     * Jarvis, Judice, and Ninke error filter (1976).
     * <pre>
     *           [     * 7 5 ]
     * (1/48 x)  [ 3 5 7 5 3 ]
     *           [ 1 3 5 3 1 ]
     * </pre>
     */
    public static final KernelJAI ERROR_FILTER_JARVIS =
        new KernelJAI(5, 3, 2, 0,
    new float[] {0.0F,       0.0F,       0.0F,       7.0F/48.0F, 5.0F/48.0F,
                 3.0F/48.0F, 5.0F/48.0F, 7.0F/48.0F, 5.0F/48.0F, 3.0F/48.0F,
                 1.0F/48.0F, 3.0F/48.0F, 5.0F/48.0F, 3.0F/48.0F, 1.0F/48.0F});

    /**
     * Stucki error filter (1981).
     * <pre>
     *           [     * 7 5 ]
     * (1/42 x)  [ 2 4 8 4 2 ]
     *           [ 1 2 4 2 1 ]
     * </pre>
     */
    public static final KernelJAI ERROR_FILTER_STUCKI =
        new KernelJAI(5, 3, 2, 0,
    new float[] {0.0F,       0.0F,       0.0F,       7.0F/42.0F, 5.0F/42.0F,
                 2.0F/42.0F, 4.0F/42.0F, 8.0F/42.0F, 4.0F/42.0F, 2.0F/42.0F,
                 1.0F/42.0F, 2.0F/42.0F, 4.0F/42.0F, 2.0F/42.0F, 1.0F/42.0F});

    /**
     * 4x4x1 mask useful for dithering 8-bit grayscale images to 1-bit images.
     */
    public static final KernelJAI[] DITHER_MASK_441 = new KernelJAI[] {
        new KernelJAI(4, 4, 1, 1,
                      new float[] {0.9375F, 0.4375F, 0.8125F, 0.3125F,
                                   0.1875F, 0.6875F, 0.0625F, 0.5625F,
                                   0.7500F, 0.2500F, 0.8750F, 0.3750F,
                                   0.0000F, 0.5000F, 0.1250F, 0.6250F})
            };

    /**
     * 4x4x3 mask useful for dithering 24-bit color images to 8-bit
     * pseudocolor images.
     */
    public static final KernelJAI[] DITHER_MASK_443 = new KernelJAI[] {
        new KernelJAI(4, 4, 1, 1,
                      new float[] {0.0000F, 0.5000F, 0.1250F, 0.6250F,
                                   0.7500F, 0.2500F, 0.8750F, 0.3750F,
                                   0.1875F, 0.6875F, 0.0625F, 0.5625F,
                                   0.9375F, 0.4375F, 0.8125F, 0.3125F}),
        new KernelJAI(4, 4, 1, 1,
                      new float[] {0.6250F, 0.1250F, 0.5000F, 0.0000F,
                                   0.3750F, 0.8750F, 0.2500F, 0.7500F,
                                   0.5625F, 0.0625F, 0.6875F, 0.1875F,
                                   0.3125F, 0.8125F, 0.4375F, 0.9375F}),
        new KernelJAI(4, 4, 1, 1,
                      new float[] {0.9375F, 0.4375F, 0.8125F, 0.3125F,
                                   0.1875F, 0.6875F, 0.0625F, 0.5625F,
                                   0.7500F, 0.2500F, 0.8750F, 0.3750F,
                                   0.0000F, 0.5000F, 0.1250F, 0.6250F})
            };

    /**
     * Gradient Mask for SOBEL_VERTICAL.
     */
    public static final KernelJAI GRADIENT_MASK_SOBEL_VERTICAL =
        new KernelJAI(3, 3, 1, 1,
                      new float[] {-1, -2 , -1,
                                    0,  0,   0,
                                    1,  2,   1});

    /**
     * Gradient Mask for SOBEL_HORIZONTAL.
     */
    public static final KernelJAI GRADIENT_MASK_SOBEL_HORIZONTAL =
        new KernelJAI(3, 3, 1, 1,
                      new float[] {-1, 0, 1,
                                   -2, 0, 2,
                                   -1, 0, 1});

    /** The width of the kernel. */
    protected int width;

    /** The height of the kernel. */
    protected int height;

    /** The X coordinate of the key element. */
    protected int xOrigin;

    /** The Y coordinate of the key element. */
    protected int yOrigin;

    /** The kernel data in row-major format. */
    protected float[] data = null;

    /** The horizontal data for a separable kernel */
    protected float[] dataH = null;
   
    /** The vertical data for a separable kernel */
    protected float[] dataV = null;

    /** True if the kernel is separable. */
    protected boolean isSeparable = false;

    /** True if the kernel has horizontal (Y axis) symmetry. */
    protected boolean isHorizontallySymmetric = false;

    /** True if the kernel has vertical (X axis) symmetry. */
    protected boolean isVerticallySymmetric = false;
 
    /** Variable to cache a copy of the rotated kernel */
    protected KernelJAI rotatedKernel = null;


    private synchronized void checkSeparable() {
        // Define a local constant for single precision floating
        // point tolerance.
        float floatZeroTol = (float)1.0E-5;

        if (isSeparable)  { return; }   	    // already separable
        if (width <= 1 || height <= 1)  { return; } 
	// 1D kernel is non-separable unless constructed to explicitly so
	// (either dataH or dataV will be a 1x1.

        //  else:
        //  Check to see if given kernel can be factored into separable kernels
        //  previous approach: if data[0]==0, then not separable;
        //  new approach: find the largest element (and its row number) first then
        //      check to see if rows are multiples of that row
        //  Normalize is also important: separable kernel implimentation has
	//  hash table look ups... and expecting things in range

	float maxData = 0.0F;
	int   imax = 0, jmax = 0;

	for (int k=0; k < this.data.length; k++){
	  float tmp = Math.abs(this.data[k]);
	  if (tmp > maxData){
	     imax = k;
	     maxData = tmp;
	  }
	}


	// check for 0 kernel
	// a case that should not happen in meaningful convolution
	if (maxData < floatZeroTol/(float)data.length){
	  isSeparable = false;
	  return;
	}

	float tmpRow[] = new float[width];
	float fac  = 1.0F / data[imax];

	// position of the max data element in the kernel matrix
	jmax = imax%width;   
	imax = imax/width;


	for(int j = 0; j < width; j++){
	  tmpRow[j] = data[imax*width + j] * fac;
	}

	//
	//  Rank 1 checking: every row should be a multiple of tmpRow
	//  if separable (a rank one kernel matrix)
	for(int i = 0, i0 = 0; i < height; i++, i0 += width) {
	  for(int j = 0; j < width; j++ ) {
	    float tmp = Math.abs(data[i0+jmax]*tmpRow[j]-data[i0+j]);
	    if (tmp > floatZeroTol) {
	      isSeparable = false;
	      return;
	    }
	  }
	}


	dataH = tmpRow;
	dataV = new float[height];
	for (int i = 0; i < height; i++) {
	  dataV[i] = data[jmax + i * width];
	}
	isSeparable = true;

	// normalizing - so that dataH and dataV add up to 1
	// in some cases, it may not be possible for both if
	// the original kernel does not add up to 1.
	// Row adds up to 1 as 1st choice.
	// If both dataH and dataV add up small,
	// no normalization is done.
	// NOTE: non-positive kernels, normalization may be skipped
	float sumH = 0.0F, sumV =0.0F;
	for (int j = 0; j < width;  j++)  { sumH += dataH[j]; }
	for (int j = 0; j < height;  j++) { sumV += dataV[j]; }
	
	if (Math.abs(sumH)>= Math.abs(sumV) && Math.abs(sumH) > floatZeroTol){
	  fac = 1.0F/sumH;
	  for (int j = 0; j < width;  j++) { dataH[j] *= fac; }
	  for (int j = 0; j < height; j++) { dataV[j] *= sumH; }
	}else if (Math.abs(sumH)< Math.abs(sumV) &&
                  Math.abs(sumV) > floatZeroTol){
	  fac = 1.0F/sumV;
	  for (int j = 0; j < width;  j++) { dataH[j] *= sumV; }
	  for (int j = 0; j < height; j++) { dataV[j] *= fac;  }	
	}
    }


    
    private void classifyKernel() {
        if (isSeparable == false) {
           checkSeparable();
        }
        isHorizontallySymmetric = false;
        isVerticallySymmetric = false;
    }

    /**
     * Constructs a KernelJAI with the given parameters.  The data
     * array is copied.
     *
     * @param width    the width of the kernel.
     * @param height   the height of the kernel.
     * @param xOrigin  the X coordinate of the key kernel element.
     * @param yOrigin  the Y coordinate of the key kernel element.
     * @param data     the float data in row-major format.
     *
     * @throws IllegalArgumentException if data is null.
     * @throws IllegalArgumentException if width is not a positive number.
     * @throws IllegalArgumentException if height is not a positive number.
     * @throws IllegalArgumentException if kernel data array does not have
     * width * height number of elements.
     * @classifies as non-separable if width or height is 1.
     */
    public KernelJAI(int width,
                     int height,
                     int xOrigin,
                     int yOrigin,
                     float[] data) {

        if ( data == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

	this.width = width;
	this.height = height;
	this.xOrigin = xOrigin;
	this.yOrigin = yOrigin;
        this.data = (float[])data.clone();
        if (width <= 0) {
            throw new IllegalArgumentException(JaiI18N.getString("KernelJAI0"));
        }
        if (height <= 0) {
            throw new IllegalArgumentException(JaiI18N.getString("KernelJAI1"));
        }
        if (width*height != data.length) {
            throw new IllegalArgumentException(JaiI18N.getString("KernelJAI2"));
        }
        classifyKernel();
    }

    /**
     * Constructs a separable KernelJAI from two float arrays.
     * The data arrays are copied.
     *
     * A Separable kernel K = dataH * dataV^T, the outer product of two
     * one dimensional vectors dataH and dataV. It can often speed up
     * compution.
     *
     * @param width    the width of the kernel.
     * @param height   the height of the kernel.
     * @param xOrigin  the X coordinate of the key kernel element.
     * @param yOrigin  the Y coordinate of the key kernel element.
     * @param dataH    the float data for the horizontal direction.
     * @param dataV    the float data for the vertical direction.
     *
     * @throws IllegalArgumentException if dataH is null.
     * @throws IllegalArgumentException if dataV is null.
     * @throws IllegalArgumentException if width is not a positive number.
     * @throws IllegalArgumentException if height is not a positive number.
     * @throws IllegalArgumentException if dataH does not have width elements.
     * @throws IllegalArgumentException if dataV does not have height elements.
     * @must   use the other constructor when dataH or dataV is null
     */
    public KernelJAI(int width,
                     int height,
                     int xOrigin,
                     int yOrigin,
                     float[] dataH,
                     float[] dataV) {

        if ( dataH == null || dataV == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (width <= 0) {
            throw new IllegalArgumentException(JaiI18N.getString("KernelJAI0"));
        }

        if (height <= 0) {
            throw new IllegalArgumentException(JaiI18N.getString("KernelJAI1"));
        }

        if (width != dataH.length) {
            throw new IllegalArgumentException(JaiI18N.getString("KernelJAI3"));
        }

        if (height != dataV.length) {
            throw new IllegalArgumentException(JaiI18N.getString("KernelJAI4"));
        }

        this.width = width;
        this.height = height;
        this.xOrigin = xOrigin;
        this.yOrigin = yOrigin;
        this.dataH = (float[])dataH.clone();
        this.dataV = (float[])dataV.clone();
        this.data = new float[dataH.length*dataV.length];

        int rowOffset = 0;
        for (int i = 0; i < dataV.length; i++) {
            float vValue = dataV[i];
            for (int j = 0; j < dataH.length; j++) { 
                data[rowOffset+j] = vValue*dataH[j];
            }
            rowOffset += dataH.length;
        }
        isSeparable = true;
        classifyKernel();
    }

    /**
     * Constructs a kernel with the given parameters.  The data
     * array is copied.  The key element is set to
     * (trunc(width/2), trunc(height/2)).
     *
     * @param width    the width of the kernel.
     * @param height   the height of the kernel.
     * @param data     the float data in row-major format.
     *
     * @throws IllegalArgumentException if data is null.
     * @throws IllegalArgumentException if width is not a positive number.
     * @throws IllegalArgumentException if height is not a positive number.
     * @throws IllegalArgumentException if data does not have
     * width * height number of elements.
     */
    public KernelJAI(int width, int height, float[] data) {
        this(width, height, width/2, height/2, data);
    }

    /**
     * Constructs a KernelJAI from a java.awt.image.Kernel
     * object.
     *
     * @throws NullPointerException if k is null.
     */
    public KernelJAI(Kernel k) {
        // XXX - NullPointerException (inconsistent style)
        this(k.getWidth(), k.getHeight(),
             k.getXOrigin(), k.getYOrigin(), k.getKernelData(null));
    }

    /** Returns the width of the kernel. */
    public int getWidth() {
	return width;
    }

    /** Returns the height of the kernel. */
    public int getHeight() {
	return height;
    }

    /** Returns the X coordinate of the key kernel element. */
    public int getXOrigin() {
	return xOrigin;
    }

    /** Returns the Y coordinate of the key kernel element. */
    public int getYOrigin() {
	return yOrigin;
    }

    /** Returns a copy of the kernel data in row-major format. */
    public float[] getKernelData() {
        return (float[])data.clone();
    }

    /**
     * Returns the horizontal portion of the kernel if the
     * kernel is separable, or <code>null</code> otherwise.  The kernel may
     * be tested for separability by calling <code>isSeparable()</code>.
     */
    public float[] getHorizontalKernelData() {
        if (dataH == null) {
            return null;
        }
        return (float[])dataH.clone();
    }

    /**
     * Returns the vertical portion of the kernel if the
     * kernel is separable, or <code>null</code> otherwise.  The kernel may
     * be tested for separability by calling <code>isSeparable()</code>.
     */
    public float[] getVerticalKernelData() {
        if (dataV == null) {
            return null;
        }
        return (float[])dataV.clone();
    }

    /** 
     * Returns a given element of the kernel. 
     *
     * @throws ArrayIndexOutOfBoundsException if either xIndex or yIndex is
     * an invalid index.
     */
    public float getElement(int xIndex, int yIndex) {
        if (!isSeparable) {
	   return data[yIndex*width + xIndex];
        } else {
           return dataH[xIndex]*dataV[yIndex];
        }
    }

    /**
     * Returns true if the kernel is separable. 
     */
    public boolean isSeparable() {
        return isSeparable;
    }

    /** Returns true if the kernel has horizontal (Y axis) symmetry. */
    public boolean isHorizontallySymmetric() {
        return isHorizontallySymmetric;
    }

    /** Returns true if the kernel has vertical (X axis) symmetry. */
    public boolean isVerticallySymmetric() {
        return isVerticallySymmetric;
    }

    /**
     * Returns the number of pixels required to the left of the key element.
     */
    public int getLeftPadding() {
        return xOrigin;
    }

    /**
     * Returns the number of pixels required to the right of the key element.
     */
    public int getRightPadding() {
        return width - xOrigin - 1;
    }

    /**
     * Returns the number of pixels required above the key element.
     */
    public int getTopPadding() {
        return yOrigin;
    }

    /**
     * Returns the number of pixels required below the key element.
     */
    public int getBottomPadding() {
        return height - yOrigin - 1;
    }
 
    /**
     * Returns a 180 degree rotated version of the kernel.  This is
     * needed by most convolve operations to get the correct results.
     * 
     * @return the rotated kernel.
     */
    public KernelJAI getRotatedKernel() {
      if (rotatedKernel == null) {
	if ( this.isSeparable){
            float rotDataH[] = new float[this.width];
            float rotDataV[] = new float[this.height];
            for (int i = 0; i < this.width; i++) {
                rotDataH[i] = this.dataH[width-1-i];
            }
            for (int i = 0; i < this.height; i++) {
                rotDataV[i] = this.dataV[height-1-i];
            }
            rotatedKernel = 
                new KernelJAI(width,
                              height,
                              width-1-xOrigin,
                              height-1-yOrigin,
			      rotDataH,
			      rotDataV);
	}else{
            int length = data.length;
            float newData[] = new float[data.length];
            for (int i = 0; i < length; i++) {
                newData[i] = data[length-1-i];
            }
            rotatedKernel = 
                new KernelJAI(width,
                              height,
                              width-1-xOrigin,
                              height-1-yOrigin,
                              newData);
	}
      }
      return rotatedKernel;
    }
}
