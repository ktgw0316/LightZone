/*
 * $RCSfile: ScaleGeneralOpImage.java,v $
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
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import com.lightcrafts.mediax.jai.ScaleOpImage;
import java.util.Map;
import com.lightcrafts.media.jai.util.Rational;

/**
 * An <code>OpImage</code> that performs scaling using a general interpolation.
 *
 */
final class ScaleGeneralOpImage extends ScaleOpImage {

    /* The number of subsampleBits */
    private int subsampleBits;

    /* 2 ^ subsampleBits */
    private int one;

    Rational half = new Rational(1, 2);

    // Interpolation kernel related information.
    private int interp_width, interp_height, interp_left, interp_top;

    long invScaleYInt, invScaleYFrac;
    long invScaleXInt, invScaleXFrac;
    
    /**
     * Constructs a ScaleGeneralOpImage from a RenderedImage source,
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
    public ScaleGeneralOpImage(RenderedImage source,
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

	// Internal precision required for position calculations
	one = 1 << subsampleBits;

	// Get the width and height and padding of the Interpolation kernel.
	interp_width = interp.getWidth();
	interp_height = interp.getHeight();
	interp_left = interp.getLeftPadding();
	interp_top = interp.getTopPadding();

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

        RasterAccessor srcAccessor = 
            new RasterAccessor(source, srcRect,  
                               formatTags[0], getSource(0).getColorModel());

        RasterAccessor dstAccessor = 
            new RasterAccessor(dest, destRect, formatTags[1], getColorModel());

	int dwidth = destRect.width;
	int dheight = destRect.height;
        int srcPixelStride = srcAccessor.getPixelStride();
	int srcScanlineStride = srcAccessor.getScanlineStride();

	int[] ypos = new int[dheight];
	int[] xpos = new int[dwidth];

	int   xfracvalues[] = null, yfracvalues[] = null;
	float xfracvaluesFloat[] = null, yfracvaluesFloat[] = null;

        switch (dstAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_INT:
	     yfracvalues = new int[dheight];
	     xfracvalues = new int[dwidth];
	     preComputePositionsInt(destRect, srcRect.x, srcRect.y,
			srcPixelStride, srcScanlineStride,
			xpos, ypos, xfracvalues, yfracvalues);
	     break;

	case DataBuffer.TYPE_FLOAT:
	case DataBuffer.TYPE_DOUBLE:
	     yfracvaluesFloat = new float[dheight];
	     xfracvaluesFloat = new float[dwidth];
	     preComputePositionsFloat(destRect, srcRect.x, srcRect.y,
			srcPixelStride, srcScanlineStride,
			xpos, ypos, xfracvaluesFloat, yfracvaluesFloat);
             break;

        default:
            throw 
		new RuntimeException(JaiI18N.getString("OrderedDitherOpImage0"));
        }

        switch (dstAccessor.getDataType()) {

        case DataBuffer.TYPE_BYTE:
            byteLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvalues, yfracvalues);
            break;

        case DataBuffer.TYPE_SHORT:
	    shortLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvalues, yfracvalues);
            break;

        case DataBuffer.TYPE_USHORT:
	    ushortLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvalues, yfracvalues);
            break;

        case DataBuffer.TYPE_INT:
	    intLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvalues, yfracvalues);
            break;

	case DataBuffer.TYPE_FLOAT:
	    floatLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvaluesFloat, yfracvaluesFloat);
	    break;

	case DataBuffer.TYPE_DOUBLE:
	    doubleLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvaluesFloat, yfracvaluesFloat);
	    break;

        default:
	    throw 
		new RuntimeException(JaiI18N.getString("OrderedDitherOpImage0"));
        }

        // If the RasterAccessor object set up a temporary buffer for the 
        // op to write to, tell the RasterAccessor to write that data
        // to the raster now that we're done with it.
        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }


    private void preComputePositionsInt(Rectangle destRect, 
			int srcRectX, int srcRectY,
			int srcPixelStride, int srcScanlineStride,
			int xpos[], int ypos[],
			int xfracvalues[], int yfracvalues[]) {

	int dwidth = destRect.width;
	int dheight = destRect.height;

	// Loop variables based on the destination rectangle to be calculated.
	int dx = destRect.x;
	int dy = destRect.y;

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

	// Precalculate the x positions and store them in an array.
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
    }

    private void preComputePositionsFloat(Rectangle destRect, 
			int srcRectX, int srcRectY,
			int srcPixelStride, int srcScanlineStride,
			int xpos[], int ypos[],
			float xfracvaluesFloat[], float yfracvaluesFloat[]) {

	int dwidth = destRect.width;
	int dheight = destRect.height;

	// Loop variables based on the destination rectangle to be calculated.
	int dx = destRect.x;
	int dy = destRect.y;

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

	// Precalculate the x positions and store them in an array.
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
	    xfracvaluesFloat[i] = (float)srcXFrac/(float)commonXDenom;

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
	    yfracvaluesFloat[i] = (float)srcYFrac/(float)commonYDenom;

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
	
	int dstOffset = 0;
	
	// Number of samples required for the interpolation
	int samples[][] = new int[interp_height][interp_width];
	int xfrac, yfrac;
	int s;
	int posx, posy;
	
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

		for (int i = 0; i < dwidth; i++) {
		    xfrac = xfracvalues[i];
		    posx = xpos[i];
		    
		    // Get the required number of surrounding sample values
		    // and put them in the samples array

		    int start = interp_left * srcPixelStride + 
			interp_top * srcScanlineStride;
		    start = posx + posy - start;
		    int countH = 0, countV = 0;

		    for (int yloop = 0; yloop < interp_height; yloop++) {

			int startY = start;
			
			for (int xloop = 0; xloop < interp_width; xloop++) {
			    samples[countV][countH++] = srcData[start] & 0xff;
			    start += srcPixelStride;
			}

			countV++;
			countH = 0;
			start = startY + srcScanlineStride;
		    }
		    
		    // Perform the interpolation
		    s = interp.interpolate(samples, xfrac, yfrac);
			
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

	// Number of samples required for the interpolation
 	int samples[][] = new int[interp_height][interp_width];
	
	int posy, posx;
	int xfrac, yfrac;
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

		for (int i = 0; i < dwidth; i++) {
		    xfrac = xfracvalues[i];
		    posx = xpos[i];

		    // Get the required number of surrounding sample values
		    int start = interp_left * srcPixelStride + 
			interp_top * srcScanlineStride;
		    start = posx + posy - start;
		    int countH = 0, countV = 0;

		    for (int yloop = 0; yloop < interp_height; yloop++) {

			int startY = start;
			
			for (int xloop = 0; xloop < interp_width; xloop++) {
			    samples[countV][countH++] = srcData[start];
			    start += srcPixelStride;
			}

			countV++;
			countH = 0;
			start = startY + srcScanlineStride;
		    }
		    
		    s = interp.interpolate(samples, xfrac, yfrac);
			
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
	
	// Number of samples required for the interpolation
	int samples[][] = new int[interp_height][interp_width];
	int posy, posx;
	int xfrac, yfrac;
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

		for (int i = 0; i < dwidth; i++) {

		    xfrac = xfracvalues[i];
		    posx = xpos[i];
		    
		    // Get the required number of surrounding sample values
		    int start = interp_left * srcPixelStride + 
			interp_top * srcScanlineStride;
		    start = posx + posy - start;
		    int countH = 0, countV = 0;
		    for (int yloop = 0; yloop < interp_height; yloop++) {

			int startY = start;
			
			for (int xloop = 0; xloop < interp_width; xloop++) {
			    samples[countV][countH++] = srcData[start] & 0xffff;
			    start += srcPixelStride;
			}

			countV++;
			countH = 0;
			start = startY + srcScanlineStride;
		    }
		    
		    s = interp.interpolate(samples, xfrac, yfrac);
			
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

	// Number of samples required for the interpolation
	int samples[][] = new int[interp_height][interp_width];
	int posy, posx;
	int xfrac, yfrac;
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

		for (int i = 0; i < dwidth; i++) {

		    xfrac = xfracvalues[i];
		    posx = xpos[i];

		    // Get the required number of surrounding sample values
		    int start = interp_left * srcPixelStride + 
			interp_top * srcScanlineStride;
		    start = posx + posy - start;
		    int countH = 0, countV = 0;
		    for (int yloop = 0; yloop < interp_height; yloop++) {

			int startY = start;
			
			for (int xloop = 0; xloop < interp_width; xloop++) {
			    samples[countV][countH++] = srcData[start];
			    start += srcPixelStride;
			}

			countV++;
			countH = 0;
			start = startY + srcScanlineStride;
		    }
		    
		    s = interp.interpolate(samples, xfrac, yfrac);

		    dstData[dstPixelOffset] = s;
                    dstPixelOffset += dstPixelStride;
		}
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void floatLoop(RasterAccessor src, Rectangle destRect,
			  RasterAccessor dst, int xpos[], int ypos[],
			  float xfracvaluesFloat[], float yfracvaluesFloat[]) {

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

	// Number of samples required for the interpolation
	float samples[][] = new float[interp_height][interp_width];

	int posy, posx;
	float xfrac, yfrac;
	float s;
 
	// Putting band loop outside
	for (int k = 0; k < dnumBands; k++)  {

            float dstData[] = dstDataArrays[k];
            float srcData[] = srcDataArrays[k];
	    int dstScanlineOffset = dstBandOffsets[k];
	    int bandOffset = bandOffsets[k];
	    
	    for (int j = 0; j < dheight; j++) {

                int dstPixelOffset = dstScanlineOffset;
		yfrac = yfracvaluesFloat[j];
		posy = ypos[j] + bandOffset;

		for (int i = 0; i < dwidth; i++) {

		    xfrac = xfracvaluesFloat[i];
		    posx = xpos[i];

		    // Get the required number of surrounding sample values
		    int start = interp_left * srcPixelStride + 
			interp_top * srcScanlineStride;
		    start = posx + posy - start;
		    int countH = 0, countV = 0;
		    for (int yloop = 0; yloop < interp_height; yloop++) {

			int startY = start;
			
			for (int xloop = 0; xloop < interp_width; xloop++) {
			    samples[countV][countH++] = srcData[start];
			    start += srcPixelStride;
			}

			countV++;
			countH = 0;
			start = startY + srcScanlineStride;
		    }
		    
		    s = interp.interpolate(samples, xfrac, yfrac);

		    if (s > Float.MAX_VALUE) {
			s = Float.MAX_VALUE;
		    } else if (s < -Float.MAX_VALUE) {
			s = -Float.MAX_VALUE;
		    }

		    dstData[dstPixelOffset] = (float)s;
                    dstPixelOffset += dstPixelStride;
		}
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void doubleLoop(RasterAccessor src, Rectangle destRect,
			  RasterAccessor dst, int xpos[], int ypos[],
			  float xfracvaluesFloat[], float yfracvaluesFloat[]) {

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

	// Number of samples required for the interpolation
	double samples[][] = new double[interp_height][interp_width];

	int posy, posx;
	double s;
	float xfrac, yfrac;

	// Putting band loop outside
	for (int k = 0; k < dnumBands; k++)  {

            double dstData[] = dstDataArrays[k];
            double srcData[] = srcDataArrays[k];
	    int dstScanlineOffset = dstBandOffsets[k];
	    int bandOffset = bandOffsets[k];
	    
	    for (int j = 0; j < dheight; j++) {

                int dstPixelOffset = dstScanlineOffset;
		yfrac = yfracvaluesFloat[j];
		posy = ypos[j] + bandOffset;

		for (int i = 0; i < dwidth; i++) {

		    xfrac = xfracvaluesFloat[i];
		    posx = xpos[i];

		    // Get the required number of surrounding sample values
		    int start = interp_left * srcPixelStride + 
			interp_top * srcScanlineStride;
		    start = posx + posy - start;
		    int countH = 0, countV = 0;
		    for (int yloop = 0; yloop < interp_height; yloop++) {

			int startY = start;
			
			for (int xloop = 0; xloop < interp_width; xloop++) {
			    samples[countV][countH++] = srcData[start];
			    start += srcPixelStride;
			}

			countV++;
			countH = 0;
			start = startY + srcScanlineStride;
		    }
		    
		    s = interp.interpolate(samples, xfrac, yfrac);

		    dstData[dstPixelOffset] = s;
                    dstPixelOffset += dstPixelStride;
		}
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

}

