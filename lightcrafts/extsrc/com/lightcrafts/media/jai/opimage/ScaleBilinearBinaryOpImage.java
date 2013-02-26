/*
 * $RCSfile: ScaleBilinearBinaryOpImage.java,v $
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
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.media.jai.util.Rational;
import java.util.Map;

/**
 * An OpImage subclass that performs bilinear scaling
 * for binary images with a MultiPixelPackedSampleModel
 * and byte, short, or int DataBuffers.
 *
 */

final public class ScaleBilinearBinaryOpImage extends ScaleOpImage 
{

  /* The number of SubsampleBits */
  private int subsampleBits;

  /* Subsampling related variables */
  int one, shift2, round2;

  long invScaleXInt, invScaleXFrac;
  long invScaleYInt, invScaleYFrac;
  
  /**
   * Constructs a ScaleBilinearBinaryOpImage from a RenderedImage source,
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
  
  public ScaleBilinearBinaryOpImage(RenderedImage source,
				    BorderExtender extender,
				    Map config,
				    ImageLayout layout,
				    float xScale,
				    float yScale,
				    float xTrans,
				    float yTrans,
				    Interpolation interp) 
  {
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

    subsampleBits =  interp.getSubsampleBitsH ();

    // Numnber of subsampling positions
    one = 1 << subsampleBits;

    //Subsampling related variables
    shift2 = 2 * subsampleBits;
    round2 = 1 << (shift2 - 1);

    // Propagate source's ColorModel
    if (layout != null) 
    {
      colorModel = layout.getColorModel(source);
    } 
    else 
    {
      colorModel = source.getColorModel();
    }
    
    sampleModel = source.getSampleModel().createCompatibleSampleModel(tileWidth, tileHeight);
    
    if (invScaleXRational.num > invScaleXRational.denom) 
    {
      invScaleXInt = invScaleXRational.num / invScaleXRational.denom;
      invScaleXFrac = invScaleXRational.num % invScaleXRational.denom;
    } 
    else 
    {
      invScaleXInt = 0;
      invScaleXFrac = invScaleXRational.num;
    }

    if (invScaleYRational.num > invScaleYRational.denom) 
    {
      invScaleYInt = invScaleYRational.num / invScaleYRational.denom;
      invScaleYFrac = invScaleYRational.num % invScaleYRational.denom;
    } 
    else 
    {
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

  protected void computeRect (Raster[] sources,
			      WritableRaster dest,
			      Rectangle destRect) 
  {
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
    int[] yvalues = new int[dheight];

    int[] xfracvalues = new int[dwidth];
    int[] yfracvalues = new int[dheight];


    long sxNum = dx, sxDenom = 1;
    long syNum = dy, syDenom = 1;

    // Subtract the X translation factor sx -= transX
    sxNum = sxNum * transXRationalDenom - transXRationalNum * sxDenom;
    sxDenom *= transXRationalDenom;

    syNum = syNum*transYRationalDenom - transYRationalNum*syDenom;
    syDenom *= transYRationalDenom;
    
    // Add 0.5
    sxNum = 2*sxNum + sxDenom;
    sxDenom *= 2;

    syNum = 2*syNum + syDenom;
    syDenom *= 2;
    
    // Multply by invScaleX & Y

    sxNum *= invScaleXRationalNum;
    sxDenom *= invScaleXRationalDenom;

    syNum *= invScaleYRationalNum;
    syDenom *= invScaleYRationalDenom;


    // Subtract 0.5
    // jxz
    sxNum = 2*sxNum - sxDenom;
    sxDenom *= 2;

    syNum = 2*syNum - syDenom;
    syDenom *= 2;


    // Separate the x source coordinate into integer and fractional part

    int srcXInt = Rational.floor(sxNum , sxDenom);
    long srcXFrac = sxNum % sxDenom;
    if (srcXInt < 0) 
    {
      srcXFrac = sxDenom + srcXFrac;
    }
    
    int srcYInt = Rational.floor(syNum, syDenom);
    long srcYFrac = syNum % syDenom;
    if (srcYInt < 0) 
    {
      srcYFrac = syDenom + srcYFrac;
    }

    // Normalize - Get a common denominator for the fracs of 
    // src and invScaleX
    long commonXDenom = sxDenom*invScaleXRationalDenom;
    srcXFrac *= invScaleXRationalDenom;
    long newInvScaleXFrac = invScaleXFrac*sxDenom;

    long commonYDenom = syDenom * invScaleYRationalDenom;
    srcYFrac *= invScaleYRationalDenom;
    long newInvScaleYFrac = invScaleYFrac * syDenom;

    for (int i = 0; i < dwidth; i++) 
    {
      // Calculate the position
      // xfracvalues is the fractional part of x position in terms
      // of the nuber of subpixel points

      xvalues[i] = srcXInt; 

      // added by jxz; for the case frac is less then 1/2,
      // the previous location is used
      // e.g. 24.25 is between the two half points 23.5 and 24.5
      // thus 23rd and 24th are the pixel rows
      // XXX watch for side effects associated with sfracvalues

      //if(2 * srcXFrac < commonXDenom && xvalues[i] > 0){
      //--xvalues[i];
      //}

      xfracvalues[i] = (int) ( ( (float) srcXFrac / (float) commonXDenom) * one);
      
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
      if (srcXFrac >= commonXDenom) 
      {
	srcXInt += 1;
	srcXFrac -= commonXDenom;
      }
    }

    // Precalculate the y positions and store them in an array. 

    for (int i = 0; i < dheight; i++) 
    {
      // Calculate the position
      yvalues[i] = srcYInt;
      yfracvalues[i] = (int) ( ( ( float) srcYFrac / (float) commonYDenom) * one );

      // added by jxz; for the case frac is less then 1/2,
      // the previous location is used
      // e.g. 24.25 is between the two half points 23.5 and 24.5
      // thus 23rd and 24th are the pixel rows
      // XXX watch for side effects associated with yfracvalues

      // if(2 * srcYFrac < commonYDenom && yvalues[i] > 0){
      //      --yvalues[i];
      // }


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
      if (srcYFrac >= commonYDenom) 
      {
	srcYInt += 1;
	srcYFrac -= commonYDenom;
      }
    }

    switch (source.getSampleModel().getDataType()) 
    {
    case DataBuffer.TYPE_BYTE:
      byteLoop(source, dest, dx, dy, dwidth, dheight,
               xvalues, yvalues, xfracvalues, yfracvalues);
      break;
      
    case DataBuffer.TYPE_SHORT:
    case DataBuffer.TYPE_USHORT:
      shortLoop(source, dest, dx, dy, dwidth, dheight,
                xvalues, yvalues, xfracvalues, yfracvalues);
      break;

    case DataBuffer.TYPE_INT:
      intLoop(source, dest, dx, dy, dwidth, dheight,
              xvalues, yvalues, xfracvalues, yfracvalues);
      break;
      
    default:
      throw new 
	RuntimeException(JaiI18N.getString("OrderedDitherOpImage0"));
    }
  }

  private void byteLoop(Raster source, WritableRaster dest,
                        int dx, int dy, int dwidth, int dheight,
                        int[] xvalues, int[] yvalues,
                        int[] xfracvalues, int[] yfracvalues) {
    MultiPixelPackedSampleModel sourceSM = (MultiPixelPackedSampleModel)source.getSampleModel();
    DataBufferByte sourceDB = (DataBufferByte)source.getDataBuffer();
    int sourceTransX = source.getSampleModelTranslateX();
    int sourceTransY = source.getSampleModelTranslateY();
    int sourceDataBitOffset = sourceSM.getDataBitOffset();
    int sourceScanlineStride = sourceSM.getScanlineStride();
    
    MultiPixelPackedSampleModel destSM = (MultiPixelPackedSampleModel)dest.getSampleModel();
    DataBufferByte destDB = (DataBufferByte)dest.getDataBuffer();
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
   

    // Since the source data is MultiPixel packed
    // precalculate the byte no and the and the shift
    // after masking required to extract a single pixel
    // sample from a byte

    for (int i = 0; i < dwidth; i++) 
    {
      int x = xvalues[i];
      int sbitnum = sourceDataBitOffset + (x - sourceTransX);
      sbytenum[i] = sbitnum >> 3;
      sshift[i] = 7 - (sbitnum & 7);
    }
      

    int sourceYOffset; 
      
    int s00, s01, s10, s11, s0, s1, s;
    int x =0, y = 0;
    int yfrac, xfrac;
    
    int xNextBitNo;
    int xNextByteNo;
    int xNextShiftNo;

    int destYOffset = (dy - destTransY) * destScanlineStride + destDBOffset;
    int dbitnum = destDataBitOffset + (dx - destTransX);

    int destByteNum;
    int destBitShift;

  
    int i = 0, j = 0;

    // Loop through height of image
    for ( j = 0; j < dheight; j++) 
    {

      y = yvalues[j];
      yfrac = yfracvalues[j];
      
      sourceYOffset = (y - sourceTransY) * sourceScanlineStride + sourceDBOffset;
      dbitnum = destDataBitOffset + ( dx - destTransX );


      // loop through one scan line
      for ( i = 0; i < dwidth; i ++ )
      {
	xfrac = xfracvalues[i];
	x = xvalues[i];
	xNextBitNo = sourceDataBitOffset + (x + 1 - sourceTransX);
	xNextByteNo = xNextBitNo >> 3;
	xNextShiftNo = 7 - ( xNextBitNo & 7);

	/* Four surrounding pixels are needed for Bilinear interpolation.
	 * If the dest pixel to be calculated is at (dx, dy) then the
	 * actual source pixel (sx, sy) required is (dx/scaleX, dy/scaleY).
	 * The four pixels that surround it are at the positions:
	 * s00 = src(sxlow, sylow)
	 * s01 = src(sxhigh, sylow)
	 * s10 = src(sxlow, syhigh)
	 * s11 = src(sxhigh, syhigh)
	 * where sxlow = Math.floor(sx), sxhigh = Math.ceil(sx)
	 * and   sylow = Math.floor(sy), syhigh = Math.ceil(sy)
	 *
	 * The value of the destination pixel can now be calculated as:
	 * s0 = (s01 - s00)*xfrac + s00;
	 * s1 = (s11 - s10)*xfrac + s10;
	 * dst(x,y) = (s1 - s0)*yfrac + s0;
	 */


	//Obtain sample values for 4 adjacent pixels in the source
	s00 = (sourceData[sourceYOffset + sbytenum[i]] >> sshift[i]) & 0x01;
	s01 = (sourceData[sourceYOffset + xNextByteNo] >> xNextShiftNo ) & 0x01;
	s10 = (sourceData[sourceYOffset + sourceScanlineStride + sbytenum[i]] >> sshift[i]) & 0x01;
	s11 = (sourceData[sourceYOffset + sourceScanlineStride + xNextByteNo] >> xNextShiftNo ) & 0x01;

	// perform the bilinear interpolation
	s0 = ( s01 - s00 ) * xfrac + (s00 << subsampleBits);
	s1 = ( s11 - s10 ) * xfrac + (s10 << subsampleBits);

	// The bilinear intrerpolated value
	s = ( ( s1 - s0  ) * yfrac + ( (s0 << subsampleBits) + round2) ) >> shift2;


	destByteNum = dbitnum >> 3;
	destBitShift = 7 - (dbitnum & 7);

	if ( s == 1 )
	{
	  //the destBit must be set
	  destData[destYOffset + destByteNum] |= ( 0x01 << destBitShift );
	}
	else
	{
	  //the destBit must be cleared
	  destData[destYOffset + destByteNum] &= ( 0xff - ( 0x01 << destBitShift ) );
	}
	dbitnum ++;	  
      }
      destYOffset += destScanlineStride;
    }
  }
  
  
  private void shortLoop(Raster source, WritableRaster dest,
                        int dx, int dy, int dwidth, int dheight,
                        int[] xvalues, int[] yvalues,
                        int[] xfracvalues, int[] yfracvalues) 
  {
    MultiPixelPackedSampleModel sourceSM =  (MultiPixelPackedSampleModel)source.getSampleModel();
    int sourceTransX = source.getSampleModelTranslateX();
    int sourceTransY = source.getSampleModelTranslateY();
    int sourceDataBitOffset = sourceSM.getDataBitOffset();
    int sourceScanlineStride = sourceSM.getScanlineStride();
    
    MultiPixelPackedSampleModel destSM = (MultiPixelPackedSampleModel) dest.getSampleModel();
    int destMinX = dest.getMinX();
    int destMinY = dest.getMinY();
    int destTransX = dest.getSampleModelTranslateX();
    int destTransY = dest.getSampleModelTranslateY();
    int destDataBitOffset = destSM.getDataBitOffset();
    int destScanlineStride = destSM.getScanlineStride();
    
    DataBufferUShort sourceDB = (DataBufferUShort) source.getDataBuffer();
    short[] sourceData = sourceDB.getData();
    int sourceDBOffset = sourceDB.getOffset();
    
    DataBufferUShort destDB = (DataBufferUShort) dest.getDataBuffer();
    short[] destData = destDB.getData();
    int destDBOffset = destDB.getOffset();
    
    int[] sshortnum = new int[dwidth];
    int[] sshift = new int[dwidth];
    
    for (int i = 0; i < dwidth; i++) 
    {
      int x = xvalues[i];
      int sbitnum = sourceDataBitOffset + (x - sourceTransX);
      sshortnum[i] = sbitnum >> 4;
      sshift[i] = 15 - (sbitnum & 15);
    }
    
    int sourceYOffset;

    int s00, s01, s10, s11, s0, s1, s;

    int x, y;
    int yfrac, xfrac;
    
    int xNextBitNo;
    int xNextShortNo;
    int xNextShiftNo;

    int destYOffset = (dy - destTransY) * destScanlineStride + destDBOffset;
    int dbitnum = destDataBitOffset + (dx - destTransX);

    int destShortNum;
    int destBitShift;
    
    for (int j = 0; j < dheight; j++) 
    {
      y = yvalues[j];
      yfrac = yfracvalues[j];

      sourceYOffset = (y - sourceTransY)*sourceScanlineStride + sourceDBOffset;
      dbitnum = destDataBitOffset + (dx - destTransX);

      for ( int i = 0; i < dwidth; i++)
      {
	xfrac = xfracvalues[i];
	x = xvalues[i];
	xNextBitNo = sourceDataBitOffset + (x + 1 - sourceTransX);
	xNextShortNo = xNextBitNo >> 4;
	xNextShiftNo = 15 - (xNextBitNo & 15);

	s00 = (sourceData[sourceYOffset + sshortnum[i]] >> sshift[i]) & 0x01;
	s01 = (sourceData[sourceYOffset + xNextShortNo] >> xNextShiftNo ) & 0x01;
	s10 = (sourceData[sourceYOffset + sourceScanlineStride + sshortnum[i]] >> sshift[i]) & 0x01;
	s11 = (sourceData[sourceYOffset + sourceScanlineStride + xNextShortNo] >> xNextShiftNo ) & 0x01;

	s0 = ( s01 - s00 ) * xfrac + (s00 << subsampleBits);
	s1 = ( s11 - s10 ) * xfrac + (s10 << subsampleBits);
	s = ( ( s1 - s0  ) * yfrac + (s0 << subsampleBits) + round2 ) >> shift2;

	destShortNum = dbitnum >> 4;
	destBitShift = 15 - (dbitnum & 15);
	
	if ( s == 1)
	{
	  destData [destYOffset + destShortNum] |= ( 0x01 << destBitShift );
	}
	else
	{
	  destData [destYOffset + destShortNum] &= ( 0xffff - ( 0x01 << destBitShift ) );
	}
	dbitnum ++;
      }
      destYOffset += destScanlineStride;
    }
  }
  
  private void intLoop(Raster source, WritableRaster dest,
                        int dx, int dy, int dwidth, int dheight,
                        int[] xvalues, int[] yvalues,
                        int[] xfracvalues, int[] yfracvalues) 
  {
    MultiPixelPackedSampleModel sourceSM = (MultiPixelPackedSampleModel)source.getSampleModel();
    DataBufferInt sourceDB = (DataBufferInt)source.getDataBuffer();
    int sourceTransX = source.getSampleModelTranslateX();
    int sourceTransY = source.getSampleModelTranslateY();
    int sourceDataBitOffset = sourceSM.getDataBitOffset();
    int sourceScanlineStride = sourceSM.getScanlineStride();
    
    MultiPixelPackedSampleModel destSM =  (MultiPixelPackedSampleModel)dest.getSampleModel();
    DataBufferInt destDB = (DataBufferInt)dest.getDataBuffer();
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
    
    for (int i = 0; i < dwidth; i++) 
    {
      int x = xvalues[i];
      int sbitnum = sourceDataBitOffset + (x - sourceTransX);
      sintnum[i] = sbitnum >> 5;
      sshift[i] = 31 - (sbitnum & 31);
    }
    
    int sourceYOffset; 
      
    int s00, s01, s10, s11, s0, s1, s;
    int x, y;
    int yfrac, xfrac;
    
    int xNextBitNo;
    int xNextIntNo;
    int xNextShiftNo;

    int destYOffset = (dy - destTransY) * destScanlineStride + destDBOffset;
    int dbitnum = destDataBitOffset + (dx - destTransX);

    int destIntNum;
    int destBitShift;
    
    for (int j = 0; j < dheight; j++) 
    {
      y = yvalues[j];
      yfrac = yfracvalues[j];

      sourceYOffset = (y - sourceTransY)*sourceScanlineStride + sourceDBOffset;
      dbitnum = destDataBitOffset + ( dx - destTransX );

      for ( int i = 0; i < dwidth; i ++)
      {
	xfrac = xfracvalues[i];
	x = xvalues[i];
	
	xNextBitNo = sourceDataBitOffset + (x + 1 - sourceTransX);
	xNextIntNo = xNextBitNo >> 5;
	xNextShiftNo = 31 - ( xNextBitNo & 31 );

	s00 = (sourceData[sourceYOffset + sintnum[i]] >> sshift[i]) & 0x01;
	s01 = (sourceData[sourceYOffset + xNextIntNo] >> xNextShiftNo ) & 0x01;
	s10 = (sourceData[sourceYOffset + sourceScanlineStride + sintnum[i]] >> sshift[i]) & 0x01;
	s11 = (sourceData[sourceYOffset + sourceScanlineStride + xNextIntNo] >> xNextShiftNo ) & 0x01;

	s0 = ( s01 - s00 ) * xfrac + (s00 << subsampleBits);
	s1 = ( s11 - s10 ) * xfrac + (s10 << subsampleBits);
	s = ( ( s1 - s0  ) * yfrac + (s0 << subsampleBits) + round2 ) >> shift2;

	destIntNum = dbitnum >> 5;
	destBitShift = 31 - ( dbitnum & 31 );

	if ( s == 1 )
	{
	  //Is above the threshold, the destBit must be set
	  destData [destYOffset + destIntNum] |= ( 0x01 << destBitShift );
	}
	else
	{
	  destData [destYOffset + destIntNum] &= ( 0xff - ( 0x01 << destBitShift ) );
	}
	dbitnum ++;	  
      }
      destYOffset += destScanlineStride;
    }

  }
}

