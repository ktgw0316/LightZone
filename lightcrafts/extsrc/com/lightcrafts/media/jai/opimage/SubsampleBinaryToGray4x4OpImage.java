/*
 * $RCSfile: SubsampleBinaryToGray4x4OpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:44 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.PixelInterleavedSampleModel;
import com.lightcrafts.mediax.jai.ImageLayout;
import java.util.Map;
import com.lightcrafts.mediax.jai.GeometricOpImage;
import com.lightcrafts.mediax.jai.PackedImageData;
import com.lightcrafts.mediax.jai.PixelAccessor;

/**
 * A class extending <code>GeometricOpImage</code> to
 * subsample binary images to gray scale images.  Image scaling operations
 * require rectilinear backwards mapping and padding by the resampling
 * filter dimensions.
 *
 * <p> When applying scale factors of scaleX, scaleY to a source image
 * with width of src_width and height of src_height, the resulting image
 * is defined to have the following bounds:
 *
 * <code>
 *       dst minX  = floor(src minX  * scaleX + transX)
 *       dst minY  = floor(src minY  * scaleY + transY)
 *       dst width  =  floor(src width  * scaleX)
 *       dst height =  floor(src height * scaleY)
 * </code>
 *
 * <p> When interpolations which require padding the source such as Bilinear
 * or Bicubic interpolation are specified, the source needs to be extended
 * such that it has the extra pixels needed to compute all the destination
 * pixels. This extension is performed via the <code>BorderExtender</code>
 * class. The type of border extension can be specified as a
 * <code>RenderingHint</code> to the <code>JAI.create</code> method.
 *
 * <p> If no <code>BorderExtender</code> is specified, the source will
 * not be extended.  The scaled image size is still calculated
 * according to the formula specified above. However since there is not
 * enough source to compute all the destination pixels, only that
 * subset of the destination image's pixels which can be computed,
 * will be written in the destination. The rest of the destination
 * will be set to zeros.
 *
 * @see ScaleOpImage
 *
 */

class SubsampleBinaryToGray4x4OpImage extends GeometricOpImage {
    private int blockX = 4;
    private int blockY = 4;

    /** destination image width */
    private int dWidth;
    /** destination image height*/
    private int dHeight;

    /** the 1st pixel location for destination pixels, i.e.,
     *  the source pixel matrix
     *       [yValues[j] yValues[j]+blockY] by [xValues[i] xValues[i]+blockX]
     *  will be condensed to form pixel <code>i</code>th pixel in row <code>j</code>
     */
    private int[] xValues;
    private int[] yValues;

    // a look up table; lut[i] counts 1s in binary expression of i
    private int[] lut;

    // convert from number of bits on count to gray value, with
    // scaling, i.e. if invScaleX,Y=3,3, then the possible bit
    // counts are 0..9, hence the lookup tables are [0..9] * 255/9.
    // there are 4 kinds of scaling, depending on area size
    // when invScaleX,Y are non integers,
    //   [floor(invScaleY), ceil(invScaleY)] x [floor(invScaleX), ceil(invScaleX)]
    private byte[] lutGray;

    /**
     * Constructs a <code>SubsampleBinaryToGray4x4OpImage</code> from a <code>RenderedImage</code>
     * source, an optional <code>BorderExtender</code>, x and y scale
     * and translation factors, and an <code>Interpolation</code>
     * object.  The image dimensions are determined by forward-mapping
     * the source bounds, and are passed to the superclass constructor
     * by means of the <code>layout</code> parameter.  Other fields of
     * the layout are passed through unchanged.  If
     * <code>layout</code> is <code>null</code>, a new
     * <code>ImageLayout</code> will be constructor to hold the bounds
     * information.
     *
     * Note that the scale factors are represented internally as Rational
     * numbers in order to workaround inexact device specific representation
     * of floating point numbers. For instance the floating point number 1.2
     * is internally represented as 1.200001, which can throw the
     * calculations off during a forward/backward map.
     *
     * <p> The Rational approximation is valid upto the sixth decimal place.
     *
     * @param layout an <code>ImageLayout</code> optionally containing
     *        the tile grid layout, <code>SampleModel</code>, and
     *        <code>ColorModel</code>, or <code>null</code>.
     * @param source a <code>RenderedImage</code>.

     *        from this <code>OpImage</code>, or <code>null</code>.  If
     *        <code>null</code>, no caching will be performed.
     * @param cobbleSources a boolean indicating whether
     *        <code>computeRect</code> expects contiguous sources.
     * @param extender a <code>BorderExtender</code>, or <code>null</code>.
     * @param interp an <code>Interpolation</code> object to use for
     *        resampling.
     * @param scaleX scale factor along x axis.
     * @param scaleY scale factor along y axis.
     *
     * @throws IllegalArgumentException if combining the
     *         source bounds with the layout parameter results in negative
     *         output width or height.
     */
    public SubsampleBinaryToGray4x4OpImage(RenderedImage source,
					   ImageLayout layout,
					   Map config){

        super(vectorize(source),
              SubsampleBinaryToGrayOpImage.layoutHelper(source,
							1.0F/4,
							1.0F/4,
							layout,
							config),
              config,
              true, // cobbleSources,
	      null, // extender
              null,  // interpolation
	      null);

	int srcWidth = source.getWidth();
	int srcHeight= source.getHeight();

	blockX = blockY = 4;

	dWidth  = srcWidth / blockX;
	dHeight = srcHeight/ blockY;

	if (extender == null) {
	    computableBounds = new Rectangle(0, 0, dWidth, dHeight);
	} else {
	    // If extender is present we can write the entire destination.
	    computableBounds = getBounds();
	}


	// these can be delayed, such as placed in computeRect()
	buildLookupTables();

	// compute the begining bit position of each row and column
	computeXYValues(dWidth, dHeight);
    }

    /**
     * Computes the source point corresponding to the supplied point.
     *
     * @param destPt the position in destination image coordinates
     * to map to source image coordinates.
     *
     * @return a <code>Point2D</code> of the same class as
     * <code>destPt</code>.
     *
     * @throws IllegalArgumentException if <code>destPt</code> is
     * <code>null</code>.
     *
     * @since JAI 1.1.2
     */
    public Point2D mapDestPoint(Point2D destPt) {
        if (destPt == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        Point2D pt = (Point2D)destPt.clone();

        pt.setLocation(destPt.getX()*4.0, destPt.getY()*4.0);

        return pt;
    }

    /**
     * Computes the destination point corresponding to the supplied point.
     *
     * @param sourcePt the position in source image coordinates
     * to map to destination image coordinates.
     *
     * @return a <code>Point2D</code> of the same class as
     * <code>sourcePt</code>.
     *
     * @throws IllegalArgumentException if <code>sourcePt</code> is
     * <code>null</code>.
     *
     * @since JAI 1.1.2
     */
    public Point2D mapSourcePoint(Point2D sourcePt) {
        if (sourcePt == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        Point2D pt = (Point2D)sourcePt.clone();

        pt.setLocation(sourcePt.getX()/4.0, sourcePt.getY()/4.0);

        return pt;
    }

    /**
     * Returns the minimum bounding box of the region of the destination
     * to which a particular <code>Rectangle</code> of the specified source
     * will be mapped.
     *
     * @param sourceRect the <code>Rectangle</code> in source coordinates.
     * @param sourceIndex the index of the source image.
     *
     * @return a <code>Rectangle</code> indicating the destination
     *         bounding box, or <code>null</code> if the bounding box
     *         is unknown.
     *
     * @throws IllegalArgumentException if <code>sourceIndex</code> is
     *         negative or greater than the index of the last source.
     * @throws IllegalArgumentException if <code>sourceRect</code> is
     *         <code>null</code>.
     */
    protected Rectangle forwardMapRect(Rectangle sourceRect,
                                       int sourceIndex) {

        if ( sourceRect == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sourceIndex != 0) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic1"));
        }

	// Get the source dimensions
	int x0 = sourceRect.x;
	int y0 = sourceRect.y;
	int dx0 = x0 / blockX;
	int dy0 = y0 / blockY;

	int x1 = sourceRect.x + sourceRect.width - 1;
	int y1 = sourceRect.y + sourceRect.height- 1;

	int dx1 = x1 / blockX;
	int dy1 = y1 / blockY;

        // Return the writable destination area
	return new Rectangle(dx0, dy0, dx1-dx0+1, dy1-dy0+1);
    }

    /**
     * Returns the minimum bounding box of the region of the specified
     * source to which a particular <code>Rectangle</code> of the
     * destination will be mapped.
     *
     * @param destRect the <code>Rectangle</code> in destination coordinates.
     * @param sourceIndex the index of the source image.
     *
     * @return a <code>Rectangle</code> indicating the source bounding box,
     *         or <code>null</code> if the bounding box is unknown.
     *
     * @throws IllegalArgumentException if <code>sourceIndex</code> is
     *         negative or greater than the index of the last source.
     * @throws IllegalArgumentException if <code>destRect</code> is
     *         <code>null</code>.
     */
    protected Rectangle backwardMapRect(Rectangle destRect,
                                        int sourceIndex) {

        if ( destRect == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sourceIndex != 0) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic1"));
        }

        // Get the destination rectangle coordinates and dimensions
        int sx0 = destRect.x * blockX;
        int sy0 = destRect.y * blockY;
        int sx1 = (destRect.x + destRect.width -1) * blockX;
        int sy1 = (destRect.y + destRect.height-1) * blockY;

	return new Rectangle(sx0, sy0, sx1 - sx0 + blockX, sy1 - sy0 + blockY);
    }

    /**
     * Performs a subsamplebinarytogray operation on a specified rectangle.
     * The sources are cobbled.
     *
     * @param sources  an array of source Rasters, guaranteed to provide all
     *                 necessary source data for computing the output.
     * @param dest     a WritableRaster  containing the area to be computed.
     * @param destRect the rectangle within dest to be processed.
     */
    protected void computeRect(Raster[]   sources,
                               WritableRaster dest,
                               Rectangle  destRect) {
	Raster source = sources[0];

        switch (source.getSampleModel().getDataType()) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_INT:
	        byteLoop4x4(source, dest, destRect);
            break;
        default:
            throw new
		RuntimeException(JaiI18N.getString("SubsampleBinaryToGrayOpImage0"));
        }
    }


    // speed up for the case of 4x4
    // and data buffer bitOffset is 0 or 4
    private void byteLoop4x4(Raster source, WritableRaster dest, Rectangle  destRect) {
        PixelAccessor pa = new PixelAccessor(source.getSampleModel(), null);
	PackedImageData pid =
            pa.getPackedPixels(source, source.getBounds(), false, false);

	if(pid.bitOffset % 4 !=0){
	   // special treatment only for offsets 0 and 4
	   byteLoop(source, dest, destRect);
	   return;
	}

	byte[] sourceData   = pid.data;
        int sourceDBOffset  = pid.offset;
	int dx  = destRect.x;        int dy  = destRect.y;
	int dwi = destRect.width;    int dhi = destRect.height;
        int sourceTransX = pid.rect.x;   // source.getSampleModelTranslateX();
        int sourceTransY = pid.rect.y;   // source.getSampleModelTranslateY();
        int sourceDataBitOffset  = pid.bitOffset;
        int sourceScanlineStride = pid.lineStride;

        PixelInterleavedSampleModel destSM =
            (PixelInterleavedSampleModel)dest.getSampleModel();
        DataBufferByte destDB =
            (DataBufferByte)dest.getDataBuffer();
        int destTransX = dest.getSampleModelTranslateX();
        int destTransY = dest.getSampleModelTranslateY();
        int destScanlineStride = destSM.getScanlineStride();

        byte[] destData  = destDB.getData();
        int destDBOffset = destDB.getOffset();

	int[] sAreaBitsOn = new int[2];

	  for(int j = 0; j < dhi; j++) {
	      int y = (dy + j) << 2;   //int y = (dy + j) * blockY;
              int sourceYOffset =
                  (y - sourceTransY)*sourceScanlineStride + sourceDBOffset;

	      int destYOffset =
  		  (j + dy - destTransY)*destScanlineStride + destDBOffset;
	      destYOffset += dx - destTransX;

	      int  selement, sbitnumi, sstartbiti, sbytenumi;
	      // sbitnumi   - the 1st bit position from the minX of the raster
	      // sstartbiti - the 1st bit position in the byte data
	      //sbitnumi = blockX * dx - sourceTransX + sourceDataBitOffset;
	      sbitnumi = (dx<<2) - sourceTransX + sourceDataBitOffset;

	      for(int i=0; i < dwi; ){
		 sbytenumi = sbitnumi >> 3;
		 sstartbiti  = sbitnumi % 8;
		 int byteindex = sourceYOffset + sbytenumi;
		 sAreaBitsOn[0] = sAreaBitsOn[1] = 0;
		 for(int k=0; k < 4; k++, byteindex += sourceScanlineStride){
		   selement = 0x00ff & (int)sourceData[byteindex];
		   sAreaBitsOn[1] += lut[selement & 0x000f];
		   sAreaBitsOn[0] += lut[selement>>4];

		 }
		 // set dest elements
		 // count in 4s
		 // sstartbiti = 0 means the 0th of sAreaBitsOn is added to
		 //     current dest position, ie destYOffset + i;
		 // sstartbiti = 4 means the 1th of sAreaBitsOn is added to
		 //     current dest position, ie destYOffset + i;
		 // sstartbiti now means different
		 // sstartbiti = sstartbiti / 4;
		 sstartbiti >>= 2;

		 while (sstartbiti < 2 && i < dwi){
		   destData[destYOffset + i] = lutGray[sAreaBitsOn[sstartbiti]];
		   sstartbiti++;
		   i++;
		   sbitnumi += blockX;
		 }
	      }
	  }
    }



    // same as SubsampleBinaryToGray, and change byteLoop to protected in superclass?
    // extends that and save this? using prote
    private void byteLoop(Raster source, WritableRaster dest, Rectangle  destRect) {
        PixelAccessor pa = new PixelAccessor(source.getSampleModel(), null);
	PackedImageData pid =
            pa.getPackedPixels(source, source.getBounds(), false, false);
	byte[] sourceData   = pid.data;
        int sourceDBOffset  = pid.offset;
	int dx  = destRect.x;        int dy  = destRect.y;
	int dwi = destRect.width;    int dhi = destRect.height;
        int sourceTransX = pid.rect.x;   // source.getSampleModelTranslateX();
        int sourceTransY = pid.rect.y;   // source.getSampleModelTranslateY();
        int sourceDataBitOffset  = pid.bitOffset;
        int sourceScanlineStride = pid.lineStride;

        PixelInterleavedSampleModel destSM =
            (PixelInterleavedSampleModel)dest.getSampleModel();
        DataBufferByte destDB =
            (DataBufferByte)dest.getDataBuffer();
        int destTransX = dest.getSampleModelTranslateX();
        int destTransY = dest.getSampleModelTranslateY();
        int destScanlineStride = destSM.getScanlineStride();

        byte[] destData  = destDB.getData();
        int destDBOffset = destDB.getOffset();

        int[] sbytenum = new int[dwi];
        int[] sstartbit= new int[dwi];
	int[] sAreaBitsOn   = new int[dwi];
        for (int i = 0; i < dwi; i++) {
            int x = xValues[dx+i];
            int sbitnum = sourceDataBitOffset + (x - sourceTransX);
            sbytenum[i] = sbitnum >> 3;
            sstartbit[i] = sbitnum % 8;
        }

        for(int j = 0; j < dhi; j++) {

	   for(int i=0; i < dwi; i++){
	      sAreaBitsOn[i] = 0;
	   }

	   for(int y = yValues[dy+j]; y < yValues[dy+j]+blockY; y++){

              int sourceYOffset =
                (y - sourceTransY)*sourceScanlineStride + sourceDBOffset;

	      int  delement, selement, sendbiti, sendbytenumi;
	      for(int i=0; i < dwi; i++){
	         delement = 0;
		 sendbiti = sstartbit[i] + blockX - 1;
		 sendbytenumi = sbytenum[i] + (sendbiti >> 3); // byte num of the end bit
		 sendbiti %= 8;  // true src end bit position
		 selement = 0x00ff & (int)sourceData[sourceYOffset + sbytenum[i]];

		 int swingBits = 24 + sstartbit[i];
		 if(sbytenum[i]==sendbytenumi){
		   // selement  <<= 24 + sstartbit[i];
		    selement  <<= swingBits;
		    selement >>>= 31 - sendbiti + sstartbit[i];
		    delement += lut[selement];
		 }else{
		    selement  <<= swingBits;
		    selement >>>= swingBits;
		    // selement >>>= 24;

		    delement += lut[selement];
		    for(int b=sbytenum[i]+1; b< sendbytenumi; b++){
		      selement  = 0x00ff & (int)sourceData[sourceYOffset + b];
		      delement += lut[selement];
		    }
		    selement = 0x00ff & (int)sourceData[sourceYOffset + sendbytenumi];
		    selement >>>= 7-sendbiti;
		    delement += lut[selement];
		 }
		 sAreaBitsOn[i] += delement;
	      }
	   }
	   int destYOffset =
	       (j + dy - destTransY)*destScanlineStride + destDBOffset;

	   destYOffset += dx - destTransX;

	   // update dest values for row j in raster

	   for(int i = 0; i < dwi; i++){
	      destData[destYOffset + i] = lutGray[sAreaBitsOn[i]];
	   }
	}
    }



    // buildLookupTables()
    // initializes variabes bitSet and lut
    // to be called mainly in the constructor
    private final void buildLookupTables(){
	// lut
        lut = new int[16];
	lut[0] = 0; lut[1] = 1; lut[2] = 1; lut[3] = 2;
	lut[4] = 1; lut[5] = 2; lut[6] = 2; lut[7] = 3;
	for(int i =8; i < 16; i++)   lut[i] = 1 + lut[i-8];
	//for(int i= 16; i < 256; i++) lut[i]  = lut[i&(0x0f)] + lut[(i>>4)&(0x0f)];

	// lutGray
	if (lutGray != null) return;
	lutGray = new byte[blockX * blockY +1];
	for (int i=0; i < lutGray.length; i++){
	    int tmp =  (int)Math.round(255.0F*i/(lutGray.length-1.0F));
	    lutGray[i] = tmp>255? (byte)0xff : (byte)tmp;
	}

	// switch black-white if needed
	if (SubsampleBinaryToGrayOpImage.isMinWhite(
		   this.getSourceImage(0).getColorModel())){
	    for(int i=0; i < lutGray.length; i++)
	       lutGray[i]= (byte)(255-(0xff &lutGray[i]));
	}
    }

    private void computeXYValues(int dstWidth, int dstHeight){
      if (xValues==null || yValues == null){
	 xValues = new int[dstWidth];
	 yValues = new int[dstHeight];
      }

      for(int i= 0; i < dstWidth; i++){
	  //xValues[i] = blockX * i;
	  xValues[i] = i << 2;
      }

      for(int i=0;  i < dstHeight; i++){
	  //yValues[i] =  blockY * i;
	  yValues[i] =  i << 2;
      }
    }
}
