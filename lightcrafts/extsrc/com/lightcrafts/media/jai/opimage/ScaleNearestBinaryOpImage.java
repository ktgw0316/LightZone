/*
 * $RCSfile: ScaleNearestBinaryOpImage.java,v $
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
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.ScaleOpImage;
import java.util.Map;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.media.jai.util.Rational;

/**
 * An OpImage subclass that performs nearest-neighbor scaling
 * for binary images with a MultiPixelPackedSampleModel
 * and byte, short, or int DataBuffers.
 *
 */
final class ScaleNearestBinaryOpImage extends ScaleOpImage {

    long invScaleXInt, invScaleXFrac;
    long invScaleYInt, invScaleYFrac;

    /**
     * Constructs a ScaleNearestBinaryOpImage from a RenderedImage source,
     * 
     * @param source a RenderedImage.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param xScale scale factor along x axis.
     * @param yScale scale factor along y axis.
     * @param xTrans translation factor along x axis.
     * @param yTrans translation factor along y axis.
     * @param interp an Interpolation object to use for resampling.
     */
    public ScaleNearestBinaryOpImage(RenderedImage source,
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

        // Propagate source's ColorModel
        if (layout != null) {
            colorModel = layout.getColorModel(source);
        } else {
            colorModel = source.getColorModel();
        }
        sampleModel =
            source.getSampleModel().createCompatibleSampleModel(tileWidth,
                                                                tileHeight);

	if (invScaleXRational.num > invScaleXRational.denom) {
	    invScaleXInt = invScaleXRational.num / invScaleXRational.denom;
	    invScaleXFrac = invScaleXRational.num % invScaleXRational.denom;
	} else {
	    invScaleXInt = 0;
	    invScaleXFrac = invScaleXRational.num;
	}

	if (invScaleYRational.num > invScaleYRational.denom) {
	    invScaleYInt = invScaleYRational.num / invScaleYRational.denom;
	    invScaleYFrac = invScaleYRational.num % invScaleYRational.denom;
	} else {
	    invScaleYInt = 0;
	    invScaleYFrac = invScaleYRational.num;
	}
    }

    /**
     * Performs a scale operation on a specified rectangle. The sources are
     * cobbled.
     *
     * @param sources  an array of source Rasters, guaranteed to provide all
     *                 necessary source data for computing the output.
     * @param dest     a WritableRaster  containing the area to be computed.
     * @param destRect the rectangle within dest to be processed.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
	Raster source = sources[0];

	// Get the source rectangle
        Rectangle srcRect = source.getBounds();

	int srcRectX = srcRect.x;
	int srcRectY = srcRect.y;

	// Destination rectangle dimensions.
	int dx = destRect.x;
	int dy = destRect.y;
	int dwidth = destRect.width;
	int dheight = destRect.height;

	// Precalculate the x positions and store them in an array.
	int[] xvalues = new int[dwidth];

	long sxNum = dx, sxDenom = 1;

	// Subtract the X translation factor sx -= transX
	sxNum = sxNum * transXRationalDenom - transXRationalNum * sxDenom;
	sxDenom *= transXRationalDenom;
	
	// Add 0.5
	sxNum = 2*sxNum + sxDenom;
	sxDenom *= 2;

	// Multply by invScaleX
	sxNum *= invScaleXRationalNum;
	sxDenom *= invScaleXRationalDenom;

	// Separate the x source coordinate into integer and fractional part
	// int part is floor(sx), frac part is sx - floor(sx)
	int srcXInt = Rational.floor(sxNum , sxDenom);
	long srcXFrac = sxNum % sxDenom;
	if (srcXInt < 0) {
	    srcXFrac = sxDenom + srcXFrac;
	}

	// Normalize - Get a common denominator for the fracs of 
	// src and invScaleX
	long commonXDenom = sxDenom*invScaleXRationalDenom;
	srcXFrac *= invScaleXRationalDenom;
	long newInvScaleXFrac = invScaleXFrac*sxDenom;

	for (int i = 0; i < dwidth; i++) {
	    // Calculate the position
	    xvalues[i] = srcXInt; 

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

	// Precalculate the y positions and store them in an array.       
	int[] yvalues = new int[dheight];

	long syNum = dy, syDenom = 1;

	// Subtract the X translation factor sy -= transY
	syNum = syNum*transYRationalDenom - transYRationalNum*syDenom;
	syDenom *= transYRationalDenom;
	
	// Add 0.5
	syNum = 2*syNum + syDenom;
	syDenom *= 2;

	// Multply by invScaleX
	syNum *= invScaleYRationalNum;
	syDenom *= invScaleYRationalDenom;

	// Separate the x source coordinate into integer and fractional part
	int srcYInt = Rational.floor(syNum, syDenom);
	long srcYFrac = syNum % syDenom;
	if (srcYInt < 0) {
	    srcYFrac = syDenom + srcYFrac;
	}

	// Normalize - Get a common denominator for the fracs of 
	// src and invScaleY
	long commonYDenom = syDenom * invScaleYRationalDenom;
	srcYFrac *= invScaleYRationalDenom;
	long newInvScaleYFrac = invScaleYFrac * syDenom;

	for (int i = 0; i < dheight; i++) {
	    // Calculate the position
	    yvalues[i] = srcYInt;

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

        switch (source.getSampleModel().getDataType()) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(source, dest, destRect, xvalues, yvalues);
            break;

        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
            shortLoop(source, dest, destRect, xvalues, yvalues);
            break;

        case DataBuffer.TYPE_INT:
            intLoop(source, dest, destRect, xvalues, yvalues);
            break;

        default:
            throw new 
		RuntimeException(JaiI18N.getString("OrderedDitherOpImage0"));
        }
    }

    private void byteLoop(Raster source, WritableRaster dest,
			Rectangle destRect, int xvalues[], int yvalues[]) {

	int dx = destRect.x;
	int dy = destRect.y;
	int dwidth = destRect.width;
	int dheight = destRect.height;

        MultiPixelPackedSampleModel sourceSM = 
            (MultiPixelPackedSampleModel)source.getSampleModel();
        DataBufferByte sourceDB =
            (DataBufferByte)source.getDataBuffer();
        int sourceTransX = source.getSampleModelTranslateX();
        int sourceTransY = source.getSampleModelTranslateY();
        int sourceDataBitOffset = sourceSM.getDataBitOffset();
        int sourceScanlineStride = sourceSM.getScanlineStride();

        MultiPixelPackedSampleModel destSM = 
            (MultiPixelPackedSampleModel)dest.getSampleModel();
        DataBufferByte destDB =
            (DataBufferByte)dest.getDataBuffer();
        int destMinX = dest.getMinX();
        int destMinY = dest.getMinY();
        int destTransX = dest.getSampleModelTranslateX();
        int destTransY = dest.getSampleModelTranslateY();
        int destDataBitOffset = destSM.getDataBitOffset();
        int destScanlineStride = destSM.getScanlineStride();

        byte[] sourceData = sourceDB.getData();
        int sourceDBOffset = sourceDB.getOffset();

        byte[] destData = destDB.getData();
        int destDBOffset = destDB.getOffset();

        int[] sbytenum = new int[dwidth];
        int[] sshift = new int[dwidth];

        for (int i = 0; i < dwidth; i++) {
            int x = xvalues[i];
            int sbitnum = sourceDataBitOffset + (x - sourceTransX);
            sbytenum[i] = sbitnum >> 3;
            sshift[i] = 7 - (sbitnum & 7);
        }

        for (int j = 0; j < dheight; j++) {
            int y = yvalues[j];

            int sourceYOffset =
                (y - sourceTransY)*sourceScanlineStride + sourceDBOffset;
            int destYOffset =
                (j + dy - destTransY)*destScanlineStride +
                destDBOffset;
            int dbitnum = destDataBitOffset + (dx - destTransX);

            int selement, val, dindex, dshift, delement;

	    int i = 0;
	    while ((i < dwidth) && ((dbitnum & 7) != 0)) {
	        selement = sourceData[sourceYOffset + sbytenum[i]];
	        val = (selement >> sshift[i]) & 0x1;
                dindex = destYOffset + (dbitnum >> 3);
		dshift = 7 - (dbitnum & 7);
		delement = destData[dindex];
		delement |= val << dshift;
		destData[dindex] = (byte)delement;
		++dbitnum;
		++i;
	    }

	    dindex = destYOffset + (dbitnum >> 3);
	    int nbytes = (dwidth - i + 1) >>3;

	    if (nbytes > 0 && (j > 0) && (y == yvalues[j - 1])) {
	        // Copy central portion of previous scanline
		    System.arraycopy(destData, dindex - destScanlineStride,
			   	     destData, dindex,
				     nbytes);
		    i += nbytes * 8;
		    dbitnum += nbytes * 8;
	    } else {
                while (i < dwidth - 7) {
                    selement = sourceData[sourceYOffset + sbytenum[i]];
                    val = (selement >> sshift[i]) & 0x1;

                    delement = val << 7; // Set initial value
                    ++i;

                    selement = sourceData[sourceYOffset + sbytenum[i]];
                    val = (selement >> sshift[i]) & 0x1;

                    delement |= val << 6;
                    ++i;

                    selement = sourceData[sourceYOffset + sbytenum[i]];
                    val = (selement >> sshift[i]) & 0x1;

                    delement |= val << 5;
                    ++i;

                    selement = sourceData[sourceYOffset + sbytenum[i]];
                    val = (selement >> sshift[i]) & 0x1;

                    delement |= val << 4;
                    ++i;

                    selement = sourceData[sourceYOffset + sbytenum[i]];
                    val = (selement >> sshift[i]) & 0x1;

                    delement |= val << 3;
                    ++i;

                    selement = sourceData[sourceYOffset + sbytenum[i]];
                    val = (selement >> sshift[i]) & 0x1;

                    delement |= val << 2;
                    ++i;

                    selement = sourceData[sourceYOffset + sbytenum[i]];
                    val = (selement >> sshift[i]) & 0x1;

                    delement |= val << 1;
                    ++i;

                    selement = sourceData[sourceYOffset + sbytenum[i]];
                    val = (selement >> sshift[i]) & 0x1;

                    delement |= val;
                    ++i;

                    destData[dindex++] = (byte)delement;
                    dbitnum += 8;
                }
	    }

	    if (i < dwidth) {
	        dindex = destYOffset + (dbitnum >> 3);
		delement = destData[dindex];
		while (i < dwidth) {
		    selement = sourceData[sourceYOffset + sbytenum[i]];
		    val = (selement >> sshift[i]) & 0x1;

                    dshift = 7 - (dbitnum & 7);
		    delement |= val << dshift;
		    ++dbitnum;
		    ++i;
		}
	        destData[dindex] = (byte)delement;
	    }
        }
    }

    private void shortLoop(Raster source, WritableRaster dest,
			Rectangle destRect, int xvalues[], int yvalues[]) {

	int dx = destRect.x;
	int dy = destRect.y;
	int dwidth = destRect.width;
	int dheight = destRect.height;

        MultiPixelPackedSampleModel sourceSM =
            (MultiPixelPackedSampleModel)source.getSampleModel();
        int sourceTransX = source.getSampleModelTranslateX();
        int sourceTransY = source.getSampleModelTranslateY();
        int sourceDataBitOffset = sourceSM.getDataBitOffset();
        int sourceScanlineStride = sourceSM.getScanlineStride();

        MultiPixelPackedSampleModel destSM = 
            (MultiPixelPackedSampleModel)dest.getSampleModel();
        int destMinX = dest.getMinX();
        int destMinY = dest.getMinY();
        int destTransX = dest.getSampleModelTranslateX();
        int destTransY = dest.getSampleModelTranslateY();
        int destDataBitOffset = destSM.getDataBitOffset();
        int destScanlineStride = destSM.getScanlineStride();

        DataBufferUShort sourceDB = (DataBufferUShort)source.getDataBuffer();
        short[] sourceData = sourceDB.getData();
        int sourceDBOffset = sourceDB.getOffset();

        DataBufferUShort destDB = (DataBufferUShort)dest.getDataBuffer();
        short[] destData = destDB.getData();
        int destDBOffset = destDB.getOffset();

        int[] sshortnum = new int[dwidth];
        int[] sshift = new int[dwidth];

        for (int i = 0; i < dwidth; i++) {
            int x = xvalues[i];
            int sbitnum = sourceDataBitOffset + (x - sourceTransX);
            sshortnum[i] = sbitnum >> 4;
            sshift[i] = 15 - (sbitnum & 15);
        }

        for (int j = 0; j < dheight; j++) {
            int y = yvalues[j];

            int sourceYOffset =
                (y - sourceTransY)*sourceScanlineStride + sourceDBOffset;
            int destYOffset =
                (j + dy - destTransY)*destScanlineStride +
                destDBOffset;
            int dbitnum = destDataBitOffset + (dx - destTransX);

	    int selement, val, dindex, dshift, delement;

	    int i = 0;
	    while ((i < dwidth) && ((dbitnum & 15) != 0)) {
                    selement = sourceData[sourceYOffset + sshortnum[i]];
                    val = (selement >> sshift[i]) & 0x1;
                    
                    dindex = destYOffset + (dbitnum >> 4);
                    dshift = 15 - (dbitnum & 15);
                    delement = destData[dindex];
                    delement |= val << dshift;
                    destData[dindex] = (short)delement;
                    ++dbitnum;
                    ++i;
	    }

	    dindex = destYOffset + (dbitnum >> 4);

	    int nshorts = (dwidth - i) >> 4;
	       
            if (nshorts > 0 && (j > 0) && (y == yvalues[j - 1])) {
                // Copy previous scanline
                int offset = destYOffset + (dbitnum >> 4);
                System.arraycopy(destData, offset - destScanlineStride,
                                 destData, offset,
                                 nshorts);
		i += nshorts >> 4;
		dbitnum += nshorts >> 4;
            } else {
                while (i < dwidth - 15) {
                    delement = 0;
                    for (int b = 15; b >= 0; b--) {
                        selement = sourceData[sourceYOffset + sshortnum[i]];
                        val = (selement >> sshift[i]) & 0x1;
                        delement |= val << b;
                        ++i;
                    }

                    destData[dindex++] = (short)delement;
                    dbitnum += 16;
                }
	    }

	    if (i < dwidth) {
	        dindex = destYOffset + (dbitnum >> 4);
		delement = destData[dindex];
		while (i < dwidth) {
		    selement = sourceData[sourceYOffset + sshortnum[i]];
		    val = (selement >> sshift[i]) & 0x1;
		    
		    dshift = 15 - (dbitnum & 15);
		    delement |= val << dshift;
		    ++dbitnum;
		    ++i;
		}
		destData[dindex] = (short)delement;
	    }
	}
    }

    private void intLoop(Raster source, WritableRaster dest,
			Rectangle destRect, int xvalues[], int yvalues[]) {

	int dx = destRect.x;
	int dy = destRect.y;
	int dwidth = destRect.width;
	int dheight = destRect.height;

        MultiPixelPackedSampleModel sourceSM =
            (MultiPixelPackedSampleModel)source.getSampleModel();
        DataBufferInt sourceDB =
            (DataBufferInt)source.getDataBuffer();
        int sourceTransX = source.getSampleModelTranslateX();
        int sourceTransY = source.getSampleModelTranslateY();
        int sourceDataBitOffset = sourceSM.getDataBitOffset();
        int sourceScanlineStride = sourceSM.getScanlineStride();

        MultiPixelPackedSampleModel destSM = 
            (MultiPixelPackedSampleModel)dest.getSampleModel();
        DataBufferInt destDB =
            (DataBufferInt)dest.getDataBuffer();
        int destMinX = dest.getMinX();
        int destMinY = dest.getMinY();
        int destTransX = dest.getSampleModelTranslateX();
        int destTransY = dest.getSampleModelTranslateY();
        int destDataBitOffset = destSM.getDataBitOffset();
        int destScanlineStride = destSM.getScanlineStride();

        int[] sourceData = sourceDB.getData();
        int sourceDBOffset = sourceDB.getOffset();

        int[] destData = destDB.getData();
        int destDBOffset = destDB.getOffset();

        int[] sintnum = new int[dwidth];
        int[] sshift = new int[dwidth];

        for (int i = 0; i < dwidth; i++) {
            int x = xvalues[i];
            int sbitnum = sourceDataBitOffset + (x - sourceTransX);
            sintnum[i] = sbitnum >> 5;
            sshift[i] = 31 - (sbitnum & 31);
        }

        for (int j = 0; j < dheight; j++) {
            int y = yvalues[j];

            int sourceYOffset =
                (y - sourceTransY)*sourceScanlineStride + sourceDBOffset;
            int destYOffset =
                (j + dy - destTransY)*destScanlineStride +
                destDBOffset;
            int dbitnum = destDataBitOffset + (dx - destTransX);


	    int selement, val, dindex, dshift, delement;
	    
	    int i = 0;
	    while ((i < dwidth) && ((dbitnum & 31) != 0)) {
                    selement = sourceData[sourceYOffset + sintnum[i]];
                    val = (selement >> sshift[i]) & 0x1;
                    
                    dindex = destYOffset + (dbitnum >> 5);
                    dshift = 31 - (dbitnum & 31);
                    delement = destData[dindex];
                    delement |= val << dshift;
                    destData[dindex] = delement;
                    ++dbitnum;
                    ++i;
	    }

	    dindex = destYOffset + (dbitnum >> 5);
	    int nints = (dwidth - i) >> 5;

            if (nints > 0 && (j > 0) && (y == yvalues[j - 1])) {
                // Copy previous scanline
                int offset = destYOffset + (dbitnum >> 5);
                System.arraycopy(destData, offset - destScanlineStride,
                                 destData, offset,
                                 nints);
		i += nints >> 5;
		dbitnum += nints >> 5;
            } else {
                while (i < dwidth - 31) {
                    delement = 0;
                    for (int b = 31; b >= 0; b--) {
                        selement = sourceData[sourceYOffset + sintnum[i]];
                        val = (selement >> sshift[i]) & 0x1;
                        delement |= val << b;
                        ++i;
                    }

                    destData[dindex++] = delement;
                    dbitnum += 32;
                }
	    }

	    if (i < dwidth) {
	      dindex = destYOffset + (dbitnum >> 5);
	      delement = destData[dindex];
	      while (i < dwidth) {
                        selement = sourceData[sourceYOffset + sintnum[i]];
                        val = (selement >> sshift[i]) & 0x1;
                        
                        dshift = 31 - (dbitnum & 31);
                        delement |= val << dshift;
                        ++dbitnum;
                        ++i;
	      }
	      destData[dindex] = delement;
	    }
	}
    }
}
