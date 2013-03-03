/*
 * $RCSfile: SubsampleBinaryToGray2x2OpImage.java,v $
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
 * subsample binary images to gray scale images.
 * This class provides an acceleration for a special
 * case of SubsampleBinaryToGrayOpImage, when the
 * scaling factors in x and y directions are 1/2.
 *
 * <code>
 *       dst minX  = floor(src minX /2 )
 *       dst minY  = floor(src minY /2 )
 *       dst width  =  floor(src width  / 2)
 *       dst height =  floor(src height / 2)
 * </code>
 *
 * @see ScaleOpImage
 * @see SubsampleBinaryToGrayOpImage
 *
 */
class SubsampleBinaryToGray2x2OpImage extends GeometricOpImage {

    /** block pixel size; to shrink to one pixel */
    private int blockX;
    private int blockY;

    /** destination image width */
    private int dWidth;
    /** destination image height*/
    private int dHeight;

    /** the 1st pixel location for destination pixels, i.e.,
     *  the source pixel matrix
     *       [yValues[j] yValues[j]+blockY] by [xValues[i] xValues[i]+blockX]
     *  will be condensed to form pixel <code>i</code>th pixel in row <code>j</code>
     */

    // a look up table; lut[i] counts 1s in binary expression of i
    // lut4_45 counts 1s in bit 4,5 in i&0x0f, the last 8 bits of i
    // lut4_67 counts 1s in bit 6,7 in i&0x0f, the last 8 bits of i
    private int[] lut4_45;
    private int[] lut4_67;

    // convert from number of bits on count to gray value, with
    // scaling, i.e. if invScaleX,Y=3,3, then the possible bit
    // counts are 0..9, hence the lookup tables are [0..9] * 255/9.
    // there are 4 kinds of scaling, depending on area size
    // when invScaleX,Y are non integers,
    //   [floor(invScaleY), ceil(invScaleY)] x [floor(invScaleX), ceil(invScaleX)]
    private byte[] lutGray;

    /**
     * Constructs a <code>SubsampleBinaryToGray2x2OpImage</code> from a <code>RenderedImage</code>
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
    public SubsampleBinaryToGray2x2OpImage(RenderedImage source,
					ImageLayout layout,
					Map config){

        super(vectorize(source),
	      SubsampleBinaryToGrayOpImage.layoutHelper(source,
							1.0F/2,
							1.0F/2,
							layout,
							config),
              config,
              true, // cobbleSources,
	      null, // extender
              null,  // interpolation
	      null);

	blockX = 2;
	blockY = 2;
	int srcWidth = source.getWidth();
	int srcHeight= source.getHeight();

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

        pt.setLocation(destPt.getX()*2.0, destPt.getY()*2.0);

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

        pt.setLocation(sourcePt.getX()/2.0, sourcePt.getY()/2.0);

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

	int dx0 = sourceRect.x / blockX;
	int dy0 = sourceRect.y / blockY;
	int dx1 = (sourceRect.x + sourceRect.width -1) / blockX;
	int dy1 = (sourceRect.y + sourceRect.height-1) / blockY;

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
            byteLoop2x2(source, dest, destRect);
            break;
        default:
            throw new
	       RuntimeException(JaiI18N.getString("SubsampleBinaryToGrayOpImage0"));
        }
    }

    private void byteLoop2x2(Raster source, WritableRaster dest, Rectangle  destRect) {
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

	int[] sAreaBitsOn = new int[4];

	if ((sourceDataBitOffset & 0x01) == 0){

	  for(int j = 0; j < dhi; j++) {
	      int y = (dy + j) << 1;  // y = (dy+j) * blockY;
              int sourceYOffset =
                  (y - sourceTransY)*sourceScanlineStride + sourceDBOffset;
	      int sourceYOffset2= sourceYOffset + sourceScanlineStride;

	      int destYOffset =
  		  (j + dy - destTransY)*destScanlineStride + destDBOffset;
	      destYOffset += dx - destTransX;

	      int  selement, sbitnumi, sstartbiti, sbytenumi;
	      // sbitnumi   - the 1st bit position from the minX of the raster
	      // sstartbiti - the 1st bit position in the byte data
	      //sbitnumi = blockX * dx - sourceTransX + sourceDataBitOffset;
	      sbitnumi = (dx<<1) - sourceTransX + sourceDataBitOffset;
	      for(int i=0; i < dwi; ){
		 sbytenumi = sbitnumi >> 3;

		 sstartbiti  = sbitnumi % 8;
		 selement = 0x00ff & (int)sourceData[sourceYOffset + sbytenumi];

		 sAreaBitsOn[2] = lut4_45[selement & 0x000f];
		 sAreaBitsOn[3] = lut4_67[selement & 0x000f];
		 selement >>= 4;
		 sAreaBitsOn[0] = lut4_45[selement];
		 sAreaBitsOn[1] = lut4_67[selement];

		 // next line
		 selement = 0x00ff & (int)sourceData[sourceYOffset2 + sbytenumi];
		 sAreaBitsOn[2] += lut4_45[selement & 0x000f];
		 sAreaBitsOn[3] += lut4_67[selement & 0x000f];
		 selement >>= 4;
		 sAreaBitsOn[0] += lut4_45[selement];
		 sAreaBitsOn[1] += lut4_67[selement];

		 // set dest elements
		 // count in 2s
		 // sstartbiti = 0 means the 0th of sAreaBitsOn is added to
		 //     current dest position, i.e. destYOffset + i;
		 // sstartbiti = 2 means the 1th of sAreaBitsOn is added to
		 //     current dest position, i.e. destYOffset + i;
		 // sstartbiti now means different
		 sstartbiti >>= 1;	 // sstartbiti = sstartbiti / 2;

		 while (sstartbiti < 4 && i < dwi){
		   destData[destYOffset + i] = lutGray[sAreaBitsOn[sstartbiti]];
		   sstartbiti++;
		   i++;
		   sbitnumi += blockX;
		 }
	      }
	  }
	}else{
	  // need to shift one bit a lot of the time
	  for(int j = 0; j < dhi; j++) {
	      int y = (dy + j)<< 1;    // y = (dy+j) * blockY;
              int sourceYOffset =
                  (y - sourceTransY)*sourceScanlineStride + sourceDBOffset;
	      int sourceYOffset2= sourceYOffset + sourceScanlineStride;

	      int destYOffset =
  		  (j + dy - destTransY)*destScanlineStride + destDBOffset;
	      destYOffset += dx - destTransX;

	      int  selement, sbitnumi, sstartbiti, sbytenumi;
	      // sbitnumi   - the 1st bit position from the minX of the raster
	      // sstartbiti - the 1st bit position in the byte data
	      // sbitnumi = blockX * dx - sourceTransX + sourceDataBitOffset;
	      sbitnumi = (dx<<1) - sourceTransX + sourceDataBitOffset;

	      for(int i=0; i < dwi; ){
		 sbytenumi = sbitnumi >> 3;

		 sstartbiti  = sbitnumi % 8;
		 // shift one bit, so that we can use almost the same code
		 // as even bitOffset cases as above
		 selement = 0x00ff & (sourceData[sourceYOffset + sbytenumi]<< 1);

		 sAreaBitsOn[2] = lut4_45[selement & 0x000f];
		 sAreaBitsOn[3] = lut4_67[selement & 0x000f];
		 selement >>= 4;
		 sAreaBitsOn[0] = lut4_45[selement];
		 sAreaBitsOn[1] = lut4_67[selement];

		 // next line
		 // shift one bit
		 selement = 0x00ff & (sourceData[sourceYOffset2 + sbytenumi]<<1);
		 sAreaBitsOn[2] += lut4_45[selement & 0x000f];
		 sAreaBitsOn[3] += lut4_67[selement & 0x000f];
		 selement >>= 4;
		 sAreaBitsOn[0] += lut4_45[selement];
		 sAreaBitsOn[1] += lut4_67[selement];

		 // taking care the extra bit that is in the next byte (<0 means 1 for the 1st bit)
		 // when there is one more byte to go on this line
		 // as long as there is more data in the buffer
		 // adding to the last one is ok; will not be used if out side of raster bounds
		 sbytenumi += 1;   // move to next byte
		 if(sbytenumi < sourceData.length - sourceYOffset2){
		    sAreaBitsOn[3] += sourceData[sourceYOffset  + sbytenumi] < 0? 1: 0;
		    sAreaBitsOn[3] += sourceData[sourceYOffset2 + sbytenumi] < 0? 1: 0;
		 }

		 // set dest elements
		 // count in 2s, this corresponds to i th dest
		 // sstartbiti now means different

		 sstartbiti >>= 1; // sstartbiti = sstartbiti / 2;

		 while ( sstartbiti < 4 && i < dwi){
		   destData[destYOffset + i] = lutGray[sAreaBitsOn[sstartbiti]];
		   sstartbiti++;
		   i++;
		   sbitnumi += blockX;
		 }

	      }
	  }
	}
    }

    // shortLoop and intLoop are not needed, due to PixelAccessor or RasterAccessor's
    // returns byte packing for binary data
    // private void shortLoop(Raster source, WritableRaster dest, Rectangle  destRect) {;}
    // private void   intLoop(Raster source, WritableRaster dest, Rectangle  destRect) {;}

    // buildLookupTables()
    // initializes variabes bitSet and lut
    // to be called mainly in the constructor
    private final void buildLookupTables(){
        lut4_45 = new int[16];
	lut4_67 = new int[16];
	// 6-7th bits on
	lut4_67[0] = 0; lut4_67[1] = 1; lut4_67[2] = 1; lut4_67[3] = 2;
	for(int i= 4; i < 16; i++) lut4_67[i] = lut4_67[i&0x03];
	// 4 and 5 th bits
	for(int i= 0; i < 16; i++) lut4_45[i] = lut4_67[i>>2];

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
}
