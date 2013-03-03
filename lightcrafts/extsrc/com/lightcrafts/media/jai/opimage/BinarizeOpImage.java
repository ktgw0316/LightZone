/*
 * $RCSfile: BinarizeOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:15 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.Map;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PointOpImage;
import com.lightcrafts.mediax.jai.PixelAccessor;
import com.lightcrafts.mediax.jai.PackedImageData;
import com.lightcrafts.mediax.jai.UnpackedImageData;
import com.lightcrafts.media.jai.util.JDKWorkarounds;
import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * An <code>OpImage</code> implementing the "Binarize" operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.BinarizeDescriptor</code>.
 *
 * <p>This <code>OpImage</code> maps all the pixels of an image
 * whose value falls within a given range to a constant on a per-band basis.
 * Each of the lower bound, upper bound, and constant arrays may have only
 * one value in it. If that is the case, that value is used for all bands.
 *
 * @see com.lightcrafts.mediax.jai.operator.BinarizeDescriptor
 * @see BinarizeCRIF
 *
 * @since version 1.1
 */
final class BinarizeOpImage extends PointOpImage {

    /**
     * Lookup table for ORing bytes of output.
     */
    private static byte[] byteTable = new byte[] {
        (byte)0x80, (byte)0x40, (byte)0x20, (byte)0x10,
        (byte)0x08, (byte)0x04, (byte)0x02, (byte)0x01,
    };

    /** 
     *  bitsOn[j + (i<<3)]
     *  sets bits on from i to j 
     */
    private static int[] bitsOn = null;

    /** The threshold. */
    private double threshold;

    /**
     * Constructor.
     *
     * @param source     The source image.
     * @param layout     The destination image layout.
     * @param threshold  The threshold value for binarization.
     */
    public BinarizeOpImage(RenderedImage source,
			   Map config,
			   ImageLayout layout,
			   double threshold) {
        super(source, layoutHelper(source, layout, config), config, true);

        if(source.getSampleModel().getNumBands() != 1) {
	    throw new IllegalArgumentException(JaiI18N.getString("BinarizeOpImage0"));
        }

	this.threshold = threshold;
    }

    // set the OpImage's SM to be MultiPixelPackedSampleModel
    private static ImageLayout layoutHelper(RenderedImage source,
                                            ImageLayout il,
                                            Map config) {

        ImageLayout layout = (il == null) ?
            new ImageLayout() : (ImageLayout)il.clone();

        SampleModel sm = layout.getSampleModel(source);
        if(!ImageUtil.isBinary(sm)) {
            sm = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE,
                                                 layout.getTileWidth(source),
                                                 layout.getTileHeight(source),
                                                 1);
            layout.setSampleModel(sm);
        }

        ColorModel cm = layout.getColorModel(null);
        if(cm == null ||
           !JDKWorkarounds.areCompatibleDataModels(sm, cm)) {
            layout.setColorModel(ImageUtil.getCompatibleColorModel(sm,
                                                                   config));
        }
           
        return layout;
    }


    /**
     * Map the pixels inside a specified rectangle whose value is within a 
     * rang to a constant on a per-band basis.
     *
     * @param sources   Cobbled sources, guaranteed to provide all the
     *                  source data necessary for computing the rectangle.
     * @param dest      The tile containing the rectangle to be computed.
     * @param destRect  The rectangle within the tile to be computed.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        switch (sources[0].getSampleModel().getDataType()) {
        case DataBuffer.TYPE_BYTE:
	     byteLoop(sources[0], dest, destRect);
	     break;

        case DataBuffer.TYPE_SHORT:
	     shortLoop(sources[0], dest, destRect);
	     break;
        case DataBuffer.TYPE_USHORT:
	     ushortLoop(sources[0], dest, destRect);
	     break;
        case DataBuffer.TYPE_INT:
	     intLoop(sources[0], dest, destRect);
	     break;

        case DataBuffer.TYPE_FLOAT:
	     floatLoop(sources[0], dest, destRect);
	     break;
        case DataBuffer.TYPE_DOUBLE:
	     doubleLoop(sources[0], dest, destRect);
	     break;

        default:
	    throw new RuntimeException(JaiI18N.getString("BinarizeOpImage1"));
        }
    }

    private void byteLoop(Raster source,
			  WritableRaster dest,
			  Rectangle destRect){

        if(threshold <= 0.0D){
	    // every bit is 1
	    setTo1(dest, destRect);
	    return;
	}else if (threshold > 255.0D){
	    //every bit is zeros;
	    return;
	}
	
	short thresholdI = (short)Math.ceil(threshold);
	// computation can be done in integer
	// even though threshold is of double type
	// int thresholdI = (int)Math.ceil(this.threshold);
	// or through a lookup table for byte case

	Rectangle srcRect = mapDestRect(destRect,0); // should be identical to destRect

        PixelAccessor   pa  = new PixelAccessor(dest.getSampleModel(), null);
        PackedImageData pid = pa.getPackedPixels(dest, destRect, true, false);
	int offset = pid.offset;
        PixelAccessor   srcPa  = new PixelAccessor(source.getSampleModel(), null);

        UnpackedImageData srcImD = srcPa.getPixels(source, srcRect, DataBuffer.TYPE_BYTE, false);
	int srcOffset  = srcImD.bandOffsets[0];
	byte[] srcData = ((byte[][])srcImD.data)[0];
	int pixelStride= srcImD.pixelStride;

	int ind0 = pid.bitOffset;
	for(int h = 0; h < destRect.height; h++){
	   int indE = ind0 + destRect.width;
	   for(int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride){
               if((srcData[s]&0xFF) >= thresholdI) {
                   pid.data[offset + (b >> 3)] |= byteTable[b%8];
               }
	   }
	   offset += pid.lineStride;
	   srcOffset += srcImD.lineStride;
	}
	pa.setPackedPixels(pid);
    }


    // computation in short
    private void shortLoop(Raster source,
			  WritableRaster dest,
			  Rectangle destRect){

        if(threshold <= Short.MIN_VALUE){
	    // every bit is 1
	    setTo1(dest, destRect);
	    return;
	}else if (threshold > Short.MAX_VALUE){
	    //every bit is zeros;
	    return;
	}
	 
	short thresholdS = (short)( Math.ceil(threshold));
	// computation can be done in integer
	// even though threshold is of double type
	// int thresholdI = (int)Math.ceil(this.threshold);
	// or through a lookup table for byte case

	Rectangle srcRect = mapDestRect(destRect,0); // should be identical to destRect

        PixelAccessor   pa  = new PixelAccessor(dest.getSampleModel(), null);
        PackedImageData pid = pa.getPackedPixels(dest, destRect, true, false);
	int offset = pid.offset;
        PixelAccessor   srcPa  = new PixelAccessor(source.getSampleModel(), null);

        UnpackedImageData srcImD = srcPa.getPixels(source, srcRect, DataBuffer.TYPE_SHORT, false);
	int srcOffset  = srcImD.bandOffsets[0];
	short[] srcData = ((short[][])srcImD.data)[0];
	int pixelStride= srcImD.pixelStride;

	int ind0 = pid.bitOffset;
	for(int h = 0; h < destRect.height; h++){
	   int indE = ind0 + destRect.width;
	   for(int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride){
               if(srcData[s] >= thresholdS) {
                   pid.data[offset + (b >> 3)] |= byteTable[b%8];
               }
	   }
	   offset += pid.lineStride;
	   srcOffset += srcImD.lineStride;
	}
	pa.setPackedPixels(pid);
    }


    // computation in short
    private void ushortLoop(Raster source,
			  WritableRaster dest,
			  Rectangle destRect){

        if(threshold <= 0.0D){
	    // every bit is 1
	    setTo1(dest, destRect);
	    return;
	}else if (threshold > (double)(0xFFFF)){
	    //every bit is zeros;
	    return;
	}
	 
	int thresholdI = (int)( Math.ceil(threshold));
	// computation can be done in integer
	// even though threshold is of double type
	// int thresholdI = (int)Math.ceil(this.threshold);
	// or through a lookup table for byte case

	Rectangle srcRect = mapDestRect(destRect,0); // should be identical to destRect

        PixelAccessor   pa  = new PixelAccessor(dest.getSampleModel(), null);
        PackedImageData pid = pa.getPackedPixels(dest, destRect, true, false);
	int offset = pid.offset;
        PixelAccessor   srcPa  = new PixelAccessor(source.getSampleModel(), null);

        UnpackedImageData srcImD = srcPa.getPixels(source, srcRect, DataBuffer.TYPE_USHORT, false);
	int srcOffset  = srcImD.bandOffsets[0];
	short[] srcData = ((short[][])srcImD.data)[0];
	int pixelStride= srcImD.pixelStride;

	int ind0 = pid.bitOffset;
	for(int h = 0; h < destRect.height; h++){
	   int indE = ind0 + destRect.width;
	   for(int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride){
               if((srcData[s]&0xFFFF) >= thresholdI) {
                   pid.data[offset + (b >> 3)] |= byteTable[b%8];
               }
	   }
	   offset += pid.lineStride;
	   srcOffset += srcImD.lineStride;
	}
	pa.setPackedPixels(pid);
    }



    private void intLoop(Raster source,
			  WritableRaster dest,
			  Rectangle destRect){

        if(threshold <= Integer.MIN_VALUE){
	    // every bit is 1
	    setTo1(dest, destRect);
	    return;
	}else if (threshold > (double)Integer.MAX_VALUE){
	    //every bit is zeros;
	    return;
	}
	 
	// computation can be done in integer
	// even though threshold is of double type
	int thresholdI = (int)Math.ceil(this.threshold);

	// computation can be done in integer
	// even though threshold is of double type
	// int thresholdI = (int)Math.ceil(this.threshold);

	Rectangle srcRect = mapDestRect(destRect,0); // should be identical to destRect

        PixelAccessor   pa  = new PixelAccessor(dest.getSampleModel(), null);
        PackedImageData pid = pa.getPackedPixels(dest, destRect, true, false);
	int offset = pid.offset;
        PixelAccessor   srcPa  = new PixelAccessor(source.getSampleModel(), null);

        UnpackedImageData srcImD = srcPa.getPixels(source, srcRect, DataBuffer.TYPE_INT, false);
	int srcOffset  = srcImD.bandOffsets[0];
	int[] srcData = ((int[][])srcImD.data)[0];
	int pixelStride= srcImD.pixelStride;

	int ind0 = pid.bitOffset;
	for(int h = 0; h < destRect.height; h++){
	   int indE = ind0 + destRect.width;
	   for(int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride){
               if(srcData[s] >= threshold) {
                   pid.data[offset + (b >> 3)] |= byteTable[b%8];
               }
           }
	   offset += pid.lineStride;
	   srcOffset += srcImD.lineStride;
	}
	pa.setPackedPixels(pid);
    }


    // computation in float
    private void floatLoop(Raster source,
			  WritableRaster dest,
			  Rectangle destRect){

	Rectangle srcRect = mapDestRect(destRect,0); // should be identical to destRect

        PixelAccessor   pa  = new PixelAccessor(dest.getSampleModel(), null);
        PackedImageData pid = pa.getPackedPixels(dest, destRect, true, false);
	int offset = pid.offset;
        PixelAccessor   srcPa  = new PixelAccessor(source.getSampleModel(), null);

        UnpackedImageData srcImD = srcPa.getPixels(source, srcRect, DataBuffer.TYPE_FLOAT, false);
	int srcOffset  = srcImD.bandOffsets[0];
	float[] srcData = ((float[][])srcImD.data)[0];
	int pixelStride= srcImD.pixelStride;

	int ind0 = pid.bitOffset;
	for(int h = 0; h < destRect.height; h++){
	   int indE = ind0 + destRect.width;
	   for(int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride){
	     if (srcData[s]>threshold) {
                 pid.data[offset + (b >> 3)] |= byteTable[b%8];
             }
	   }
	   offset += pid.lineStride;
	   srcOffset += srcImD.lineStride;
	}
	pa.setPackedPixels(pid);
    }

    // computation in double
    private void doubleLoop(Raster source,
			  WritableRaster dest,
			  Rectangle destRect){

	Rectangle srcRect = mapDestRect(destRect,0); // should be identical to destRect

        PixelAccessor   pa  = new PixelAccessor(dest.getSampleModel(), null);
        PackedImageData pid = pa.getPackedPixels(dest, destRect, true, false);
	int offset = pid.offset;
        PixelAccessor   srcPa  = new PixelAccessor(source.getSampleModel(), null);

        UnpackedImageData srcImD = srcPa.getPixels(source, srcRect, DataBuffer.TYPE_DOUBLE, false);
	int srcOffset  = srcImD.bandOffsets[0];
	double[] srcData = ((double[][])srcImD.data)[0];
	int pixelStride= srcImD.pixelStride;

	int ind0 = pid.bitOffset;
	for(int h = 0; h < destRect.height; h++){
	   int indE = ind0 + destRect.width;
	   for(int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride){
	     if (srcData[s]>threshold) {
                 pid.data[offset + (b >> 3)] |= byteTable[b%8];
             }
	   }
	   offset += pid.lineStride;
	   srcOffset += srcImD.lineStride;
	}
	pa.setPackedPixels(pid);
    }

    // set all bits in a rectangular region to be 1
    // need to be sure that paddings not changing
    private void setTo1(Raster dest, Rectangle destRect){
        initBitsOn();
        PixelAccessor   pa  = new PixelAccessor(dest.getSampleModel(), null);
        PackedImageData pid = pa.getPackedPixels(dest, destRect, true, false);
	int offset = pid.offset;

	for(int h = 0; h < destRect.height; h++){
	   int ind0 = pid.bitOffset;
	   int indE = ind0 + destRect.width - 1;
	   if (indE < 8){
	      // the entire row in data[offset]
	      pid.data[offset] = (byte)(pid.data[offset] | bitsOn[indE]); // (0<<3) + indE
	   }else{
    	      //1st byte
	      pid.data[offset] = (byte)(pid.data[offset] | bitsOn[7]); // (0<<3) + 7
	      //middle bytes
	      for(int b = offset + 1; b <= offset +  (indE-7)/8; b++){
		 pid.data[b] = (byte)(0xff);
	      }
	      //last byte

	      int remBits = indE % 8;
	      if(remBits % 8 != 7){
		  indE = offset + indE/8;
		  pid.data[indE] = (byte)(pid.data[indE] | bitsOn[remBits]); // (0<<3)+remBits
	      }
	   }
	   offset += pid.lineStride;
	}
	pa.setPackedPixels(pid);
    }

    // setting bits i to j to 1;
    //  i <= j
    private static synchronized void initBitsOn() {

       if(bitsOn != null)
	  return;
      
       bitsOn = new int[64];
       for(int i = 0; i < 8; i++){
	  for(int j = i; j< 8; j++){
	     int bi = (0x00ff) >> i;
	     int bj = (0x00ff) << (7-j);
	     bitsOn[j + (i<<3)] = bi & bj;
	  }
       }
    }
}
