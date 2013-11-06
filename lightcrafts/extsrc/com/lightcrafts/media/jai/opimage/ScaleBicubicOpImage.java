/*
 * $RCSfile: ScaleBicubicOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:42 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.InterpolationTable;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import com.lightcrafts.mediax.jai.ScaleOpImage;
import java.util.Map;
import com.lightcrafts.media.jai.util.Rational;
// import com.lightcrafts.media.jai.test.OpImageTester;

/**
 * An <code>OpImage</code> that performs bicubic interpolation scaling.
 *
 */
final class ScaleBicubicOpImage extends ScaleOpImage {

    /* The number of subsampleBits */
    private int subsampleBits;

    /* 2 ^ subsampleBits */
    private int one;

    /** The horizontal coefficient data in fixed-point format. */
    private int[] tableDataHi = null;

    /** The vertical coefficient data in fixed-point format. */
    private int[] tableDataVi = null;

    /** The horizontal coefficient data in floating-point format. */
    private float[] tableDataHf = null;

    /** The vertical coefficient data in floating-point format. */
    private float[] tableDataVf = null;

    /** The horizontal coefficient data in double format. */
    private double[] tableDataHd = null;

    /** The vertical coefficient data in double format. */
    private double[] tableDataVd = null;

    /** Number of fractional bits used to described filter coefficients. */
    private int precisionBits;

    /** The number 1/2 with precisionBits of fractional precision. */
    private int round;

    private Rational half = new Rational(1, 2);

    // The InterpolationTable superclass.
    InterpolationTable interpTable;

    long invScaleYInt, invScaleYFrac;
    long invScaleXInt, invScaleXFrac;

    /**
     * Constructs a ScaleBicubicOpImage from a RenderedImage source,
     * 
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param xScale scale factor along x axis.
     * @param yScale scale factor along y axis.
     * @param xTrans translation factor along x axis.
     * @param yTrans translation factor along y axis.
     * @param interp a Interpolation object to use for resampling.
     */
    public ScaleBicubicOpImage(RenderedImage source,
                               BorderExtender extender,
			       Map config,
                               ImageLayout layout,
			       float xScale,
			       float yScale,
			       float xTrans,
			       float yTrans,
			       Interpolation interp) {
        super(source,
              layout,
              config,
              true,
              extender,
              interp,
              xScale,
              yScale,
              xTrans,
              yTrans);

	subsampleBits = interp.getSubsampleBitsH();
	interpTable = (InterpolationTable)interp;

	// Number of subsample positions
	one = 1 << subsampleBits;
	precisionBits = interpTable.getPrecisionBits();
	if (precisionBits > 0) {
	    round = 1<< (precisionBits - 1);
	}

	if (invScaleYRational.num > invScaleYRational.denom) {
	    invScaleYInt = invScaleYRational.num / invScaleYRational.denom;
	    invScaleYFrac = invScaleYRational.num % invScaleYRational.denom;
	} else {
	    invScaleYInt = 0;
	    invScaleYFrac = invScaleYRational.num;
	}

	if (invScaleXRational.num > invScaleXRational.denom) {
	    invScaleXInt = invScaleXRational.num / invScaleXRational.denom;
	    invScaleXFrac = invScaleXRational.num % invScaleXRational.denom;
	} else {
	    invScaleXInt = 0;
	    invScaleXFrac = invScaleXRational.num;
	}
    }

    /**
     * Performs a scale operation on a specified rectangle. The sources are
     * cobbled.
     *
     * @param sources  an array of source Rasters, guaranteed to provide all
     *                 necessary source data for computing the output.
     * @param dest     a WritableRaster containing the area to be computed.
     * @param destRect the rectangle within dest to be processed.
     */
    protected void computeRect(Raster [] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        Raster source = sources[0];
	// Get the source rectangle
        Rectangle srcRect = source.getBounds();

	int srcRectX = srcRect.x;
	int srcRectY = srcRect.y;

        RasterAccessor srcAccessor = 
            new RasterAccessor(source, srcRect,  
                               formatTags[0], getSource(0).getColorModel());

        RasterAccessor dstAccessor = 
            new RasterAccessor(dest, destRect, formatTags[1], getColorModel());

	// Loop variables based on the destination rectangle to be calculated.
	int dx = destRect.x;
	int dy = destRect.y;
	int dwidth = destRect.width;
	int dheight = destRect.height;
        int srcPixelStride = srcAccessor.getPixelStride();
	int srcScanlineStride = srcAccessor.getScanlineStride();

	int[] ypos = new int[dheight];
	int[] xpos = new int[dwidth];

	// Precalculate the y positions and store them in an array.
	int[] yfracvalues = new int[dheight];
	// Precalculate the x positions and store them in an array.
	int[] xfracvalues = new int[dwidth];

	long syNum = dy, syDenom = 1;

	// Subtract the X translation factor sy -= transY
	syNum = syNum * transYRationalDenom - transYRationalNum * syDenom;
	syDenom *= transYRationalDenom;
	
	// Add 0.5
	syNum = 2 * syNum + syDenom;
	syDenom *= 2;

	// Multply by invScaleX
	syNum *= invScaleYRationalNum;
	syDenom *= invScaleYRationalDenom;

	// Subtract 0.5
	syNum = 2 * syNum - syDenom;
	syDenom *= 2;

	// Separate the x source coordinate into integer and fractional part
	int srcYInt = Rational.floor(syNum , syDenom);
	long srcYFrac = syNum % syDenom;
	if (srcYInt < 0) {
	    srcYFrac = syDenom + srcYFrac;
	}

	// Normalize - Get a common denominator for the fracs of 
	// src and invScaleY
	long commonYDenom = syDenom * invScaleYRationalDenom;
	srcYFrac *= invScaleYRationalDenom;
	long newInvScaleYFrac = invScaleYFrac * syDenom;

	long sxNum = dx, sxDenom = 1;

	// Subtract the X translation factor sx -= transX
	sxNum = sxNum * transXRationalDenom - transXRationalNum * sxDenom;
	sxDenom *= transXRationalDenom;
	
	// Add 0.5
	sxNum = 2 * sxNum + sxDenom;
	sxDenom *= 2;

	// Multply by invScaleX
	sxNum *= invScaleXRationalNum;
	sxDenom *= invScaleXRationalDenom;

	// Subtract 0.5
	sxNum = 2 * sxNum - sxDenom;
	sxDenom *= 2;

	// Separate the x source coordinate into integer and fractional part
	// int part is floor(sx), frac part is sx - floor(sx)
	int srcXInt = Rational.floor(sxNum , sxDenom);
	long srcXFrac = sxNum % sxDenom;
	if (srcXInt < 0) {
	    srcXFrac = sxDenom + srcXFrac;
	}
	
	// Normalize - Get a common denominator for the fracs of 
	// src and invScaleX
	long commonXDenom = sxDenom * invScaleXRationalDenom;
	srcXFrac *= invScaleXRationalDenom;
	long newInvScaleXFrac = invScaleXFrac * sxDenom;

	for (int i=0; i<dwidth; i++) {
 	    xpos[i] = (srcXInt - srcRectX) * srcPixelStride; 
	    xfracvalues[i] = (int)(((float)srcXFrac/(float)commonXDenom) * one);

	    // Move onto the next source pixel.

	    // Add the integral part of invScaleX to the integral part
	    // of srcX
	    srcXInt += invScaleXInt;

	    // Add the fractional part of invScaleX to the fractional part
	    // of srcX
	    srcXFrac += newInvScaleXFrac;

	    // If the fractional part is now greater than equal to the
	    // denominator, divide so as to reduce the numerator to be less
	    // than the denominator and add the overflow to the integral part.
	    if (srcXFrac >= commonXDenom) {
		srcXInt += 1;
		srcXFrac -= commonXDenom;
	    }
	}

	for (int i = 0; i < dheight; i++) {

	    // Calculate the source position in the source data array.
	    ypos[i] = (srcYInt - srcRectY) * srcScanlineStride; 

	    // Calculate the yfrac value
	    yfracvalues[i] = (int)(((float)srcYFrac/(float)commonYDenom) * one);

	    // Move onto the next source pixel.

	    // Add the integral part of invScaleY to the integral part
	    // of srcY
	    srcYInt += invScaleYInt;

	    // Add the fractional part of invScaleY to the fractional part
	    // of srcY
	    srcYFrac += newInvScaleYFrac;

	    // If the fractional part is now greater than equal to the 
	    // denominator, divide so as to reduce the numerator to be less
	    // than the denominator and add the overflow to the integral part.
	    if (srcYFrac >= commonYDenom) {
		srcYInt += 1;
		srcYFrac -= commonYDenom;
	    }
	}

        switch (dstAccessor.getDataType()) {

        case DataBuffer.TYPE_BYTE:
	    initTableDataI();
            byteLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvalues, yfracvalues);
            break;

        case DataBuffer.TYPE_SHORT:
	    initTableDataI();
	    shortLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvalues, yfracvalues);
            break;

        case DataBuffer.TYPE_USHORT:
	    initTableDataI();
	    ushortLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvalues, yfracvalues);
            break;

        case DataBuffer.TYPE_INT:
	    initTableDataI();
	    intLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvalues, yfracvalues);
            break;

	case DataBuffer.TYPE_FLOAT:
	    initTableDataF();
	    floatLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvalues, yfracvalues);
	    break;

	case DataBuffer.TYPE_DOUBLE:
	    initTableDataD();
	    doubleLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvalues, yfracvalues);
	    break;

        default:
	    throw 
		new RuntimeException(JaiI18N.getString("OrderedDitherOpImage0"));
        }

        // If the RasterAccessor object set up a temporary buffer for the 
        // op to write to, tell the RasterAccessor to write that data
        // to the raster no that we're done with it.
        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }

    private void byteLoop(RasterAccessor src, Rectangle destRect,
			  RasterAccessor dst, int xpos[], int ypos[],
			  int xfracvalues[], int yfracvalues[]) {

        int srcPixelStride = src.getPixelStride();
	int srcScanlineStride = src.getScanlineStride();

	int dwidth = destRect.width;
	int dheight = destRect.height;
        int dnumBands = dst.getNumBands();
        byte dstDataArrays[][] = dst.getByteDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
	
        byte srcDataArrays[][] = src.getByteDataArrays(); 
	int bandOffsets[] = src.getBandOffsets();
	
	int posy, posylow, posyhigh, posyhigh2;
	int posx, posxlow, posxhigh, posxhigh2;
	int xfrac, yfrac;
	int s__, s_0, s_1, s_2;
	int s0_, s00, s01, s02;
	int s1_, s10, s11, s12;
	int s2_, s20, s21, s22;
	int s, dstOffset = 0;	

	// Putting band loop outside
	for (int k = 0; k < dnumBands; k++)  {
            byte dstData[] = dstDataArrays[k];
            byte srcData[] = srcDataArrays[k];
	    int dstScanlineOffset = dstBandOffsets[k];
	    int bandOffset = bandOffsets[k];
	    
	    for (int j = 0; j < dheight; j++) {
                int dstPixelOffset = dstScanlineOffset;
		yfrac = yfracvalues[j];
		posy = ypos[j] + bandOffset;
		posylow = posy - srcScanlineStride;
		posyhigh = posy + srcScanlineStride;
		posyhigh2 = posyhigh + srcScanlineStride;
		for (int i = 0; i < dwidth; i++) {
		    xfrac = xfracvalues[i];
		    posx = xpos[i];
		    posxlow = posx - srcPixelStride;
		    posxhigh = posx + srcPixelStride;
		    posxhigh2 = posxhigh + srcPixelStride;
		    
		    // Get the sixteen surrounding pixel values
		    s__ = srcData[posxlow + posylow] & 0xff;
		    s_0 = srcData[posx + posylow] & 0xff;
		    s_1 = srcData[posxhigh + posylow] & 0xff;
		    s_2 = srcData[posxhigh2 + posylow] & 0xff;

		    s0_ = srcData[posxlow + posy] & 0xff;
		    s00 = srcData[posx + posy] & 0xff;
		    s01 = srcData[posxhigh + posy] & 0xff;
		    s02 = srcData[posxhigh2 + posy] & 0xff;
		    
		    s1_ = srcData[posxlow + posyhigh] & 0xff;
		    s10 = srcData[posx + posyhigh] & 0xff;
		    s11 = srcData[posxhigh + posyhigh] & 0xff;
		    s12 = srcData[posxhigh2 + posyhigh] & 0xff;

		    s2_ = srcData[posxlow + posyhigh2] & 0xff;
		    s20 = srcData[posx + posyhigh2] & 0xff;
		    s21 = srcData[posxhigh + posyhigh2] & 0xff;
		    s22 = srcData[posxhigh2 + posyhigh2] & 0xff;

		    // Interpolate in X
		    int offsetX = 4*xfrac;
		    int offsetX1 = offsetX + 1;
		    int offsetX2 = offsetX + 2;
		    int offsetX3 = offsetX + 3;

		    long sum_ = (long)tableDataHi[offsetX]*s__;
		    sum_ += (long)tableDataHi[offsetX1]*s_0;
		    sum_ += (long)tableDataHi[offsetX2]*s_1;
		    sum_ += (long)tableDataHi[offsetX3]*s_2;
		    
		    long sum0 = (long)tableDataHi[offsetX]*s0_;
		    sum0 += (long)tableDataHi[offsetX1]*s00;
		    sum0 += (long)tableDataHi[offsetX2]*s01;
		    sum0 += (long)tableDataHi[offsetX3]*s02;
		    
		    long sum1 = (long)tableDataHi[offsetX]*s1_;
		    sum1 += (long)tableDataHi[offsetX1]*s10;
		    sum1 += (long)tableDataHi[offsetX2]*s11;
		    sum1 += (long)tableDataHi[offsetX3]*s12;
		    
		    long sum2 = (long)tableDataHi[offsetX]*s2_;
		    sum2 += (long)tableDataHi[offsetX1]*s20;
		    sum2 += (long)tableDataHi[offsetX2]*s21;
		    sum2 += (long)tableDataHi[offsetX3]*s22;
		    
		    // Intermediate rounding
		    sum_ = (sum_ + round) >> precisionBits;
		    sum0 = (sum0 + round) >> precisionBits;
		    sum1 = (sum1 + round) >> precisionBits;
		    sum2 = (sum2 + round) >> precisionBits;
		    
		    // Interpolate in Y
		    int offsetY = 4*yfrac;
		    long sum = (long)tableDataVi[offsetY]*sum_;
		    sum += (long)tableDataVi[offsetY + 1]*sum0;
		    sum += (long)tableDataVi[offsetY + 2]*sum1;
		    sum += (long)tableDataVi[offsetY + 3]*sum2;
		    
		    s = (int)((sum + round) >> precisionBits);
			
		    // clamp the value to byte range
		    if (s > 255) {
			s = 255;
		    } else if (s < 0) {
			s = 0;
		    }
		    
		    dstData[dstPixelOffset] = (byte)(s&0xff);
                    dstPixelOffset += dstPixelStride;
		}
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void shortLoop(RasterAccessor src, Rectangle destRect,
			   RasterAccessor dst, int xpos[], int ypos[],
			   int xfracvalues[], int yfracvalues[]) {

        int srcPixelStride = src.getPixelStride();
	int srcScanlineStride = src.getScanlineStride();

	int dwidth = destRect.width;
	int dheight = destRect.height;
        int dnumBands = dst.getNumBands();
	short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
	
        short srcDataArrays[][] = src.getShortDataArrays(); 
	int bandOffsets[] = src.getBandOffsets();
	
	int dstOffset = 0;
	
	int posy, posylow, posyhigh, posyhigh2;
	int posx, posxlow, posxhigh, posxhigh2;
	int xfrac, yfrac;
	int s__, s_0, s_1, s_2;
	int s0_, s00, s01, s02;
	int s1_, s10, s11, s12;
	int s2_, s20, s21, s22;
	int s;	

	// Putting band loop outside
	for (int k = 0; k < dnumBands; k++)  {
            short dstData[] = dstDataArrays[k];
            short srcData[] = srcDataArrays[k];
	    int dstScanlineOffset = dstBandOffsets[k];
	    int bandOffset = bandOffsets[k];
	    
	    for (int j = 0; j < dheight; j++) {
                int dstPixelOffset = dstScanlineOffset;
		yfrac = yfracvalues[j];
		posy = ypos[j] + bandOffset;
		posylow = posy - srcScanlineStride;
		posyhigh = posy + srcScanlineStride;
		posyhigh2 = posyhigh + srcScanlineStride;
		for (int i = 0; i < dwidth; i++) {
		    xfrac = xfracvalues[i];
		    posx = xpos[i];
		    posxlow = posx - srcPixelStride;
		    posxhigh = posx + srcPixelStride;
		    posxhigh2 = posxhigh + srcPixelStride;
		    
		    // Get the sixteen surrounding pixel values
		    s__ = srcData[posxlow + posylow];
		    s_0 = srcData[posx + posylow];
		    s_1 = srcData[posxhigh + posylow];
		    s_2 = srcData[posxhigh2 + posylow];

		    s0_ = srcData[posxlow + posy];
		    s00 = srcData[posx + posy];
		    s01 = srcData[posxhigh + posy];
		    s02 = srcData[posxhigh2 + posy];
		    
		    s1_ = srcData[posxlow + posyhigh];
		    s10 = srcData[posx + posyhigh];
		    s11 = srcData[posxhigh + posyhigh];
		    s12 = srcData[posxhigh2 + posyhigh];

		    s2_ = srcData[posxlow + posyhigh2];
		    s20 = srcData[posx + posyhigh2];
		    s21 = srcData[posxhigh + posyhigh2];
		    s22 = srcData[posxhigh2 + posyhigh2];

		    // Interpolate in X
		    int offsetX = 4*xfrac;
		    int offsetX1 = offsetX + 1;
		    int offsetX2 = offsetX + 2;
		    int offsetX3 = offsetX + 3;

		    long sum_ = (long)tableDataHi[offsetX]*s__;
		    sum_ += (long)tableDataHi[offsetX1]*s_0;
		    sum_ += (long)tableDataHi[offsetX2]*s_1;
		    sum_ += (long)tableDataHi[offsetX3]*s_2;
		    
		    long sum0 = (long)tableDataHi[offsetX]*s0_;
		    sum0 += (long)tableDataHi[offsetX1]*s00;
		    sum0 += (long)tableDataHi[offsetX2]*s01;
		    sum0 += (long)tableDataHi[offsetX3]*s02;
		    
		    long sum1 = (long)tableDataHi[offsetX]*s1_;
		    sum1 += (long)tableDataHi[offsetX1]*s10;
		    sum1 += (long)tableDataHi[offsetX2]*s11;
		    sum1 += (long)tableDataHi[offsetX3]*s12;
		    
		    long sum2 = (long)tableDataHi[offsetX]*s2_;
		    sum2 += (long)tableDataHi[offsetX1]*s20;
		    sum2 += (long)tableDataHi[offsetX2]*s21;
		    sum2 += (long)tableDataHi[offsetX3]*s22;
		    
		    // Intermediate rounding
		    sum_ = (sum_ + round) >> precisionBits;
		    sum0 = (sum0 + round) >> precisionBits;
		    sum1 = (sum1 + round) >> precisionBits;
		    sum2 = (sum2 + round) >> precisionBits;
		    
		    // Interpolate in Y
		    int offsetY = 4*yfrac;
		    long sum = (long)tableDataVi[offsetY]*sum_;
		    sum += (long)tableDataVi[offsetY + 1]*sum0;
		    sum += (long)tableDataVi[offsetY + 2]*sum1;
		    sum += (long)tableDataVi[offsetY + 3]*sum2;
		    
		    s = (int)((sum + round) >> precisionBits);
			
		    // clamp the value to short range
		    if (s > Short.MAX_VALUE) {
			s = Short.MAX_VALUE;
		    } else if (s < Short.MIN_VALUE) {
			s = Short.MIN_VALUE;
		    }
		    
		    dstData[dstPixelOffset] = (short)s;
                    dstPixelOffset += dstPixelStride;
		}
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void ushortLoop(RasterAccessor src, Rectangle destRect,
			    RasterAccessor dst, int xpos[], int ypos[],
			    int xfracvalues[], int yfracvalues[]) {

        int srcPixelStride = src.getPixelStride();
	int srcScanlineStride = src.getScanlineStride();

	int dwidth = destRect.width;
	int dheight = destRect.height;
        int dnumBands = dst.getNumBands();
        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
	
        short srcDataArrays[][] = src.getShortDataArrays(); 
	int bandOffsets[] = src.getBandOffsets();
	
	int dstOffset = 0;
	
	int posy, posylow, posyhigh, posyhigh2;
	int posx, posxlow, posxhigh, posxhigh2;
	int xfrac, yfrac;
	int s__, s_0, s_1, s_2;
	int s0_, s00, s01, s02;
	int s1_, s10, s11, s12;
	int s2_, s20, s21, s22;
	int s;
	
	// Putting band loop outside
	for (int k = 0; k < dnumBands; k++)  {
            short dstData[] = dstDataArrays[k];
            short srcData[] = srcDataArrays[k];
	    int dstScanlineOffset = dstBandOffsets[k];
	    int bandOffset = bandOffsets[k];
	    
	    for (int j = 0; j < dheight; j++) {
                int dstPixelOffset = dstScanlineOffset;
		yfrac = yfracvalues[j];
		posy = ypos[j] + bandOffset;
		posylow = posy - srcScanlineStride;
		posyhigh = posy + srcScanlineStride;
		posyhigh2 = posyhigh + srcScanlineStride;
		for (int i = 0; i < dwidth; i++) {
		    xfrac = xfracvalues[i];
		    posx = xpos[i];
		    posxlow = posx - srcPixelStride;
		    posxhigh = posx + srcPixelStride;
		    posxhigh2 = posxhigh + srcPixelStride;
		    
		    // Get the sixteen surrounding pixel values
		    s__ = srcData[posxlow + posylow] & 0xffff;
		    s_0 = srcData[posx + posylow] & 0xffff;
		    s_1 = srcData[posxhigh + posylow] & 0xffff;
		    s_2 = srcData[posxhigh2 + posylow] & 0xffff;

		    s0_ = srcData[posxlow + posy] & 0xffff;
		    s00 = srcData[posx + posy] & 0xffff;
		    s01 = srcData[posxhigh + posy] & 0xffff;
		    s02 = srcData[posxhigh2 + posy] & 0xffff;
		    
		    s1_ = srcData[posxlow + posyhigh] & 0xffff;
		    s10 = srcData[posx + posyhigh] & 0xffff;
		    s11 = srcData[posxhigh + posyhigh] & 0xffff;
		    s12 = srcData[posxhigh2 + posyhigh] & 0xffff;

		    s2_ = srcData[posxlow + posyhigh2] & 0xffff;
		    s20 = srcData[posx + posyhigh2] & 0xffff;
		    s21 = srcData[posxhigh + posyhigh2] & 0xffff;
		    s22 = srcData[posxhigh2 + posyhigh2] & 0xffff;

		    // Interpolate in X
		    int offsetX = 4*xfrac;
		    int offsetX1 = offsetX + 1;
		    int offsetX2 = offsetX + 2;
		    int offsetX3 = offsetX + 3;

		    long sum_ = (long)tableDataHi[offsetX]*s__;
		    sum_ += (long)tableDataHi[offsetX1]*s_0;
		    sum_ += (long)tableDataHi[offsetX2]*s_1;
		    sum_ += (long)tableDataHi[offsetX3]*s_2;
		    
		    long sum0 = (long)tableDataHi[offsetX]*s0_;
		    sum0 += (long)tableDataHi[offsetX1]*s00;
		    sum0 += (long)tableDataHi[offsetX2]*s01;
		    sum0 += (long)tableDataHi[offsetX3]*s02;
		    
		    long sum1 = (long)tableDataHi[offsetX]*s1_;
		    sum1 += (long)tableDataHi[offsetX1]*s10;
		    sum1 += (long)tableDataHi[offsetX2]*s11;
		    sum1 += (long)tableDataHi[offsetX3]*s12;
		    
		    long sum2 = (long)tableDataHi[offsetX]*s2_;
		    sum2 += (long)tableDataHi[offsetX1]*s20;
		    sum2 += (long)tableDataHi[offsetX2]*s21;
		    sum2 += (long)tableDataHi[offsetX3]*s22;
		    
		    // Intermediate rounding
		    sum_ = (sum_ + round) >> precisionBits;
		    sum0 = (sum0 + round) >> precisionBits;
		    sum1 = (sum1 + round) >> precisionBits;
		    sum2 = (sum2 + round) >> precisionBits;
		    
		    // Interpolate in Y
		    int offsetY = 4*yfrac;
		    long sum = (long)tableDataVi[offsetY]*sum_;
		    sum += (long)tableDataVi[offsetY + 1]*sum0;
		    sum += (long)tableDataVi[offsetY + 2]*sum1;
		    sum += (long)tableDataVi[offsetY + 3]*sum2;
		    
		    s = (int)((sum + round) >> precisionBits);
			
		    // clamp the value to ushort range
		    if (s > 65535) {
			s = 65535;
		    } else if (s < 0) {
			s = 0;
		    }
		    
		    dstData[dstPixelOffset] = (short)(s & 0xffff);
                    dstPixelOffset += dstPixelStride;
		}
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }
    
    // identical to byteLoops, except datatypes have changed.  clumsy,
    // but there's no other way in Java
    private void intLoop(RasterAccessor src, Rectangle destRect,
			 RasterAccessor dst, int xpos[], int ypos[],
			 int xfracvalues[], int yfracvalues[]) {
	
        int srcPixelStride = src.getPixelStride();
	int srcScanlineStride = src.getScanlineStride();

	int dwidth = destRect.width;
	int dheight = destRect.height;
        int dnumBands = dst.getNumBands();
        int dstDataArrays[][] = dst.getIntDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
	
        int srcDataArrays[][] = src.getIntDataArrays(); 
	int bandOffsets[] = src.getBandOffsets();
	
	int dstOffset = 0;
	
	int posy, posylow, posyhigh, posyhigh2;
	int posx, posxlow, posxhigh, posxhigh2;
	long xfrac, yfrac;
	int s__, s_0, s_1, s_2;
	int s0_, s00, s01, s02;
	int s1_, s10, s11, s12;
	int s2_, s20, s21, s22;
	int s;

	// Putting band loop outside
	for (int k = 0; k < dnumBands; k++)  {
            int dstData[] = dstDataArrays[k];
            int srcData[] = srcDataArrays[k];
	    int dstScanlineOffset = dstBandOffsets[k];
	    int bandOffset = bandOffsets[k];
	    
	    for (int j = 0; j < dheight; j++) {
                int dstPixelOffset = dstScanlineOffset;
		yfrac = yfracvalues[j];
		posy = ypos[j] + bandOffset;
		posylow = posy - srcScanlineStride;
		posyhigh = posy + srcScanlineStride;
		posyhigh2 = posyhigh + srcScanlineStride;
		for (int i = 0; i < dwidth; i++) {
		    xfrac = xfracvalues[i];
		    posx = xpos[i];
		    posxlow = posx - srcPixelStride;
		    posxhigh = posx + srcPixelStride;
		    posxhigh2 = posxhigh + srcPixelStride;
		    
		    // Get the sixteen surrounding pixel values
		    s__ = srcData[posxlow + posylow];
		    s_0 = srcData[posx + posylow];
		    s_1 = srcData[posxhigh + posylow];
		    s_2 = srcData[posxhigh2 + posylow];

		    s0_ = srcData[posxlow + posy];
		    s00 = srcData[posx + posy];
		    s01 = srcData[posxhigh + posy];
		    s02 = srcData[posxhigh2 + posy];
		    
		    s1_ = srcData[posxlow + posyhigh];
		    s10 = srcData[posx + posyhigh];
		    s11 = srcData[posxhigh + posyhigh];
		    s12 = srcData[posxhigh2 + posyhigh];

		    s2_ = srcData[posxlow + posyhigh2];
		    s20 = srcData[posx + posyhigh2];
		    s21 = srcData[posxhigh + posyhigh2];
		    s22 = srcData[posxhigh2 + posyhigh2];

		    // Interpolate in X
		    int offsetX = (int)(4*xfrac);
		    int offsetX1 = offsetX + 1;
		    int offsetX2 = offsetX + 2;
		    int offsetX3 = offsetX + 3;

		    long sum_ = (long)tableDataHi[offsetX]*s__;
		    sum_ += (long)tableDataHi[offsetX1]*s_0;
		    sum_ += (long)tableDataHi[offsetX2]*s_1;
		    sum_ += (long)tableDataHi[offsetX3]*s_2;
		    
		    long sum0 = (long)tableDataHi[offsetX]*s0_;
		    sum0 += (long)tableDataHi[offsetX1]*s00;
		    sum0 += (long)tableDataHi[offsetX2]*s01;
		    sum0 += (long)tableDataHi[offsetX3]*s02;
		    
		    long sum1 = (long)tableDataHi[offsetX]*s1_;
		    sum1 += (long)tableDataHi[offsetX1]*s10;
		    sum1 += (long)tableDataHi[offsetX2]*s11;
		    sum1 += (long)tableDataHi[offsetX3]*s12;
		    
		    long sum2 = (long)tableDataHi[offsetX]*s2_;
		    sum2 += (long)tableDataHi[offsetX1]*s20;
		    sum2 += (long)tableDataHi[offsetX2]*s21;
		    sum2 += (long)tableDataHi[offsetX3]*s22;
		    
		    // Intermediate rounding
		    sum_ = (sum_ + round) >> precisionBits;
		    sum0 = (sum0 + round) >> precisionBits;
		    sum1 = (sum1 + round) >> precisionBits;
		    sum2 = (sum2 + round) >> precisionBits;
		    
		    // Interpolate in Y
		    int offsetY = (int)(4*yfrac);
		    long sum = (long)tableDataVi[offsetY]*sum_;
		    sum += (long)tableDataVi[offsetY + 1]*sum0;
		    sum += (long)tableDataVi[offsetY + 2]*sum1;
		    sum += (long)tableDataVi[offsetY + 3]*sum2;
		    
		    s = (int)((sum + round) >> precisionBits);

		    dstData[dstPixelOffset] = s;
                    dstPixelOffset += dstPixelStride;
		}
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void floatLoop(RasterAccessor src, Rectangle destRect,
			   RasterAccessor dst, int xpos[], int ypos[],
			   int xfracvalues[], int yfracvalues[]) {

        int srcPixelStride = src.getPixelStride();
	int srcScanlineStride = src.getScanlineStride();

	int dwidth = destRect.width;
	int dheight = destRect.height;
        int dnumBands = dst.getNumBands();
        float dstDataArrays[][] = dst.getFloatDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
	
        float srcDataArrays[][] = src.getFloatDataArrays(); 
	int bandOffsets[] = src.getBandOffsets();
	
	int dstOffset = 0;

	int posy, posylow, posyhigh, posyhigh2;
	int posx, posxlow, posxhigh, posxhigh2;
	int xfrac, yfrac;

	float s__, s_0, s_1, s_2;
	float s0_, s00, s01, s02;
	float s1_, s10, s11, s12;
	float s2_, s20, s21, s22;

	// Putting band loop outside
	for (int k = 0; k < dnumBands; k++)  {
            float dstData[] = dstDataArrays[k];
            float srcData[] = srcDataArrays[k];
	    int dstScanlineOffset = dstBandOffsets[k];
	    int bandOffset = bandOffsets[k];
	    
	    for (int j = 0; j < dheight; j++) {
                int dstPixelOffset = dstScanlineOffset;
		yfrac = yfracvalues[j];
		posy = ypos[j] + bandOffset;
		posylow = posy - srcScanlineStride;
		posyhigh = posy + srcScanlineStride;
		posyhigh2 = posyhigh + srcScanlineStride;
		for (int i = 0; i < dwidth; i++) {
		    xfrac = xfracvalues[i];
		    posx = xpos[i];
		    posxlow = posx - srcPixelStride;
		    posxhigh = posx + srcPixelStride;
		    posxhigh2 = posxhigh + srcPixelStride;
		    
		    // Get the sixteen surrounding pixel values
		    s__ = srcData[posxlow + posylow];
		    s_0 = srcData[posx + posylow];
		    s_1 = srcData[posxhigh + posylow];
		    s_2 = srcData[posxhigh2 + posylow];

		    s0_ = srcData[posxlow + posy];
		    s00 = srcData[posx + posy];
		    s01 = srcData[posxhigh + posy];
		    s02 = srcData[posxhigh2 + posy];
		    
		    s1_ = srcData[posxlow + posyhigh];
		    s10 = srcData[posx + posyhigh];
		    s11 = srcData[posxhigh + posyhigh];
		    s12 = srcData[posxhigh2 + posyhigh];

		    s2_ = srcData[posxlow + posyhigh2];
		    s20 = srcData[posx + posyhigh2];
		    s21 = srcData[posxhigh + posyhigh2];
		    s22 = srcData[posxhigh2 + posyhigh2];

		    // Perform the bicubic interpolation

		    // Interpolate in X
		    int offsetX = (int)(4 * xfrac);
		    int offsetX1 = offsetX + 1;
		    int offsetX2 = offsetX + 2;
		    int offsetX3 = offsetX + 3;
		    
		    double sum_ = tableDataHf[offsetX]*s__;
		    sum_ += tableDataHf[offsetX1]*s_0;
		    sum_ += tableDataHf[offsetX2]*s_1;
		    sum_ += tableDataHf[offsetX3]*s_2;
		    
		    double sum0 = tableDataHf[offsetX]*s0_;
		    sum0 += tableDataHf[offsetX1]*s00;
		    sum0 += tableDataHf[offsetX2]*s01;
		    sum0 += tableDataHf[offsetX3]*s02;
		    
		    double sum1 = tableDataHf[offsetX]*s1_;
		    sum1 += tableDataHf[offsetX1]*s10;
		    sum1 += tableDataHf[offsetX2]*s11;
		    sum1 += tableDataHf[offsetX3]*s12;
		    
		    double sum2 = tableDataHf[offsetX]*s2_;
		    sum2 += tableDataHf[offsetX1]*s20;
		    sum2 += tableDataHf[offsetX2]*s21;
		    sum2 += tableDataHf[offsetX3]*s22;
		    
		    // Interpolate in Y
		    int offsetY = (int)(4 * yfrac);

		    double sum = tableDataVf[offsetY]*sum_;
		    sum += tableDataVf[offsetY + 1]*sum0;
		    sum += tableDataVf[offsetY + 2]*sum1;
		    sum += tableDataVf[offsetY + 3]*sum2;

		    if (sum > Float.MAX_VALUE) {
			sum = Float.MAX_VALUE;
		    } else if (sum < -Float.MAX_VALUE) {
			sum = -Float.MAX_VALUE;
		    }

		    dstData[dstPixelOffset] = (float)sum;
                    dstPixelOffset += dstPixelStride;
		}
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void doubleLoop(RasterAccessor src, Rectangle destRect,
			    RasterAccessor dst, int xpos[], int ypos[],
			    int xfracvalues[], int yfracvalues[]) {

        int srcPixelStride = src.getPixelStride();
	int srcScanlineStride = src.getScanlineStride();

	int dwidth = destRect.width;
	int dheight = destRect.height;
        int dnumBands = dst.getNumBands();
        double dstDataArrays[][] = dst.getDoubleDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
	
        double srcDataArrays[][] = src.getDoubleDataArrays(); 
	int bandOffsets[] = src.getBandOffsets();
	
	int dstOffset = 0;
	int posy, posylow, posyhigh, posyhigh2;
	int posx, posxlow, posxhigh, posxhigh2;

	double s__, s_0, s_1, s_2;
	double s0_, s00, s01, s02;
	double s1_, s10, s11, s12;
	double s2_, s20, s21, s22;
	double s;
	int xfrac, yfrac;

	// Putting band loop outside
	for (int k = 0; k < dnumBands; k++)  {
            double dstData[] = dstDataArrays[k];
            double srcData[] = srcDataArrays[k];
	    int dstScanlineOffset = dstBandOffsets[k];
	    int bandOffset = bandOffsets[k];
	    
	    for (int j = 0; j < dheight; j++) {
                int dstPixelOffset = dstScanlineOffset;
		yfrac = yfracvalues[j];
		posy = ypos[j] + bandOffset;
		posylow = posy - srcScanlineStride;
		posyhigh = posy + srcScanlineStride;
		posyhigh2 = posyhigh + srcScanlineStride;
		for (int i = 0; i < dwidth; i++) {
		    xfrac = xfracvalues[i];
		    posx = xpos[i];
		    posxlow = posx - srcPixelStride;
		    posxhigh = posx + srcPixelStride;
		    posxhigh2 = posxhigh + srcPixelStride;
		    
		    // Get the sixteen surrounding pixel values
		    s__ = srcData[posxlow + posylow];
		    s_0 = srcData[posx + posylow];
		    s_1 = srcData[posxhigh + posylow];
		    s_2 = srcData[posxhigh2 + posylow];

		    s0_ = srcData[posxlow + posy];
		    s00 = srcData[posx + posy];
		    s01 = srcData[posxhigh + posy];
		    s02 = srcData[posxhigh2 + posy];
		    
		    s1_ = srcData[posxlow + posyhigh];
		    s10 = srcData[posx + posyhigh];
		    s11 = srcData[posxhigh + posyhigh];
		    s12 = srcData[posxhigh2 + posyhigh];

		    s2_ = srcData[posxlow + posyhigh2];
		    s20 = srcData[posx + posyhigh2];
		    s21 = srcData[posxhigh + posyhigh2];
		    s22 = srcData[posxhigh2 + posyhigh2];

		    // Perform the bicubic interpolation

		    // Interpolate in X
		    int offsetX = (int)(4 * xfrac);
		    int offsetX1 = offsetX + 1;
		    int offsetX2 = offsetX + 2;
		    int offsetX3 = offsetX + 3;
		    
		    double sum_ = tableDataHd[offsetX]*s__;
		    sum_ += tableDataHd[offsetX1]*s_0;
		    sum_ += tableDataHd[offsetX2]*s_1;
		    sum_ += tableDataHd[offsetX3]*s_2;
		    
		    double sum0 = tableDataHd[offsetX]*s0_;
		    sum0 += tableDataHd[offsetX1]*s00;
		    sum0 += tableDataHd[offsetX2]*s01;
		    sum0 += tableDataHd[offsetX3]*s02;
		    
		    double sum1 = tableDataHd[offsetX]*s1_;
		    sum1 += tableDataHd[offsetX1]*s10;
		    sum1 += tableDataHd[offsetX2]*s11;
		    sum1 += tableDataHd[offsetX3]*s12;
		    
		    double sum2 = tableDataHd[offsetX]*s2_;
		    sum2 += tableDataHd[offsetX1]*s20;
		    sum2 += tableDataHd[offsetX2]*s21;
		    sum2 += tableDataHd[offsetX3]*s22;
		    
		    // Interpolate in Y
		    int offsetY = (int)(4 * yfrac);
		    s = tableDataVd[offsetY]*sum_;
		    s += tableDataVd[offsetY + 1]*sum0;
		    s += tableDataVd[offsetY + 2]*sum1;
		    s += tableDataVd[offsetY + 3]*sum2;

		    dstData[dstPixelOffset] = s;
                    dstPixelOffset += dstPixelStride;
		}
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private synchronized void initTableDataI() {
	if (tableDataHi == null || tableDataVi == null) {
	    tableDataHi = interpTable.getHorizontalTableData();
	    tableDataVi = interpTable.getVerticalTableData();
	}
    }

    private synchronized void initTableDataF() {
	if (tableDataHf == null || tableDataVf == null) {
	    tableDataHf = interpTable.getHorizontalTableDataFloat();
	    tableDataVf = interpTable.getVerticalTableDataFloat();
	}
    }

    private synchronized void initTableDataD() {
	if (tableDataHd == null || tableDataVd == null) {
	    tableDataHd = interpTable.getHorizontalTableDataDouble();
	    tableDataVd = interpTable.getVerticalTableDataDouble();
	}
    }

//     public static OpImage createTestImage(OpImageTester oit) {
//         Interpolation interp =
//             Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
//         return new ScaleBicubicOpImage(oit.getSource(), null, null,
//                                        new ImageLayout(oit.getSource()),
//                                        2.5F, 2.5F, 0.0F, 0.0F,
//                                        interp);
//     }
    
//     public static void main(String args[]) {

// 	String classname = "com.lightcrafts.media.jai.opimage.ScaleBicubicOpImage";
// 	OpImageTester.performDiagnostics(classname,args);
// 	System.exit(1);
	
// 	System.out.println("ScaleOpImage Test");
//         ImageLayout layout;
//         OpImage src, dst;
//         Rectangle rect = new Rectangle(2, 2, 5, 5);
	
// 	InterpolationBicubic interp = new InterpolationBicubic(8);
	
//         System.out.println("1. PixelInterleaved short 3-band");
//         layout = OpImageTester.createImageLayout(0, 0, 200, 200, 0, 0,
// 						 64, 64, DataBuffer.TYPE_SHORT,
// 						 3, false);
//         src = OpImageTester.createRandomOpImage(layout);
//         dst = new ScaleBicubicOpImage(src, null, null, null,
//                                       2.0F, 2.0F, 0.0F, 0.0F, interp);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);
	
// 	System.out.println("2. PixelInterleaved ushort 3-band");
//         layout = OpImageTester.createImageLayout(0, 0, 512, 512, 0, 0,
// 						 200, 200, 
// 						 DataBuffer.TYPE_USHORT, 
// 						 3, false);
//         src = OpImageTester.createRandomOpImage(layout);
//         dst = new ScaleBicubicOpImage(src, null, null, null,
//                                       2.0F, 2.0F, 0.0F, 0.0F, interp);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);
//     }
}

