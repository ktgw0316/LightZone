/*
 * $RCSfile: SubsampleBinaryToGrayOpImage.java,v $
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
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.SampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.SinglePixelPackedSampleModel;

import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.JDKWorkarounds;
import com.lightcrafts.mediax.jai.ImageLayout;
import java.util.Map;
import com.lightcrafts.mediax.jai.GeometricOpImage;
import com.lightcrafts.mediax.jai.JAI;
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
 * <code></pre>
 *       dst minX  = floor(src minX  * scaleX)
 *       dst minY  = floor(src minY  * scaleY)
 *       dst width  =  floor(src width  * scaleX)
 *       dst height =  floor(src height * scaleY)
 * </pre></code>
 *
 * @see ScaleOpImage
 *
 */
public class SubsampleBinaryToGrayOpImage extends GeometricOpImage {

    /** The horizontal scale factor. */
    protected float scaleX;

    /** The vertical scale factor. */
    protected float scaleY;

    /** Cached value equal to 1/scaleX. */
    protected float invScaleX;

    /** Cached value equal to 1/scaleY. */
    protected float invScaleY;

    /** Used to determine whether a float is close to an int */
    private float floatTol;

    /** same as ceil(invScaleX), ceil(invScaleY) */
    private int blockX;
    private int blockY;

    /** destination image width */
    private int dWidth;
    /** destination image height*/
    private int dHeight;


    /** the 1st pixel location for destination pixels, i.e.,
     *  the source pixel matrix
     *  Note the index runs from 0..dstWidth-1 and 0..dstHeight-1
     *       [yValues[j] yValues[j]+blockY-1] by [xValues[i] xValues[i]+blockX-1]
     *  will be condensed to form pixel <code>i</code>th pixel in row <code>j</code>
     */
    private int[] xValues;
    private int[] yValues;

    // a look up table; lut[i] counts 1s in binary expression of i
    private int[] lut = new int[256];

    /**
     * Convert from number of bits on count to gray value, with
     * scaling, i.e. if invScaleX,Y=3,3, then the possible bit
     * counts are 0..9, hence the lookup tables are [0..9] * 255/9.
     */
    protected byte[] lutGray;

    // package accessible for SubsampleBinaryToGrayOpImage4x4, etc...
    static ImageLayout layoutHelper(RenderedImage source,
				    float scaleX,
				    float scaleY,
				    ImageLayout il,
                                    Map config) {

        ImageLayout layout = (il == null) ?
            new ImageLayout() : (ImageLayout)il.clone();

	// to compute dWidth and dHeight
	// fTol and dWi, dHi must be the same as in computeDestInfo(..)
	// due to static method, a few lines of coding are repeated
	int srcWidth = source.getWidth();
	int srcHeight= source.getHeight();

	float f_dw = scaleX * srcWidth;
	float f_dh = scaleY * srcHeight;
	float fTol = .1F * Math.min(scaleX/(f_dw+1.0F),scaleY/(f_dh+1.0F));

	int dWi = (int)(f_dw);
	int dHi = (int)(f_dh);

	// let it be int in the almost int case
	//   espacially in the true int case with float calculation errors
	if(Math.abs(Math.round(f_dw)-f_dw) < fTol){
	    dWi = Math.round(f_dw);
	}

	if(Math.abs(Math.round(f_dh)-f_dh) < fTol){
	    dHi= Math.round(f_dh);
	}

	// Set the top left coordinate of the destination
	layout.setMinX((int)(scaleX * source.getMinX()));
	layout.setMinY((int)(scaleY * source.getMinY()));

	layout.setWidth(dWi);
	layout.setHeight(dHi);

	// sample model
	SampleModel sm = layout.getSampleModel(null);

	if (sm == null ||
	    sm.getDataType() != DataBuffer.TYPE_BYTE ||
	    !(sm instanceof PixelInterleavedSampleModel  ||
	      sm instanceof SinglePixelPackedSampleModel &&
              sm.getNumBands()==1)){

	    // Width and height will be corrected in OpImage.layoutHelper
            sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
						 1,
						 1,
						 1,
						 1,
						 new int[] {0});
	}

        layout.setSampleModel(sm);

	ColorModel cm = layout.getColorModel(null);

        if(cm == null ||
           !JDKWorkarounds.areCompatibleDataModels(sm, cm)) {

            layout.setColorModel(ImageUtil.getCompatibleColorModel(sm,
                                                                   config));
        }

        return layout;
    }

    // Since this operation deals with packed bits in a binary image, we
    // do not need to expand the IndexColorModel
    private static Map configHelper(Map configuration) {

	Map config;

	if (configuration == null) {
	    config = new RenderingHints(JAI.KEY_REPLACE_INDEX_COLOR_MODEL,
					Boolean.FALSE);
	} else {

	    config = configuration;

	    if (!config.containsKey(JAI.KEY_REPLACE_INDEX_COLOR_MODEL)) {
		RenderingHints hints = (RenderingHints)configuration;
		config = (RenderingHints)hints.clone();
		config.put(JAI.KEY_REPLACE_INDEX_COLOR_MODEL, Boolean.FALSE);
	    }
	}

	return config;
    }

    /**
     * Constructs a <code>SubsampleBinaryToGrayOpImage</code>
     * from a <code>RenderedImage</code> source, x and y scale
     * object.  The image dimensions are determined by forward-mapping
     * the source bounds, and are passed to the superclass constructor
     * by means of the <code>layout</code> parameter.  Other fields of
     * the layout are passed through unchanged.  If
     * <code>layout</code> is <code>null</code>, a new
     * <code>ImageLayout</code> will be constructor to hold the bounds
     * information.
     *
     * The float rounding errors, such as 1.2 being
     * internally represented as 1.200001, are dealt with
     * the floatTol, which is set up so that only 1/10 of pixel
     * error will occur at the end of a line, which yields correct
     * results with Math.round() operation.
     * The repeatability is guaranteed with a one-time computed
     * table xvalues and yvalues.
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
    public SubsampleBinaryToGrayOpImage(RenderedImage source,
					ImageLayout layout,
					Map config,
					float scaleX,
					float scaleY){

        super(vectorize(source),
              layoutHelper(source, scaleX, scaleY, layout, config),
              configHelper(config),
              true, // cobbleSources,
	      null, // extender
              null,  // interpolation
	      null);

	this.scaleX = scaleX;
	this.scaleY = scaleY;
	int srcMinX = source.getMinX();
	int srcMinY = source.getMinY();
	int srcWidth = source.getWidth();
	int srcHeight= source.getHeight();

	// compute floatTol, invScaleX, blockX, dWidth, dHeight,...
	computeDestInfo(srcWidth, srcHeight);

	if (extender == null) {
	    computableBounds = new Rectangle(0, 0, dWidth, dHeight);
	} else {
	    // If extender is present we can write the entire destination.
	    computableBounds = getBounds();
	}


	// these can be delayed, such as placed in computeRect()
	buildLookupTables();

	// compute the begining bit position of each row and column
	computeXYValues(srcWidth, srcHeight, srcMinX, srcMinY);
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

        pt.setLocation(destPt.getX()/scaleX, destPt.getY()/scaleY);

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

        pt.setLocation(sourcePt.getX()*scaleX, sourcePt.getY()*scaleY);

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
	int x0 = sourceRect.x - blockX +1;
	int y0 = sourceRect.y - blockY +1;
	x0 = x0 < 0? 0: x0;
	y0 = y0 < 0? 0: y0;

	int dx0 = (int)(x0 * scaleX);
	int dy0 = (int)(y0 * scaleY);
	while (xValues[dx0] > x0 && dx0 > 0){
	  dx0--;
	}
	while(yValues[dy0] > y0 && dy0 > 0){
	  dy0--;
	}

	int x1 = sourceRect.x + sourceRect.width - 1;
	int y1 = sourceRect.y + sourceRect.height- 1;

	int dx1 = (int)Math.round(x1 * scaleX);
	int dy1 = (int)Math.round(y1 * scaleY);
	dx1 = dx1 >= dWidth ? dWidth -1 : dx1;
	dy1 = dy1 >= dHeight? dHeight-1 : dy1;
	while (xValues[dx1] < x1 && dx1 < dWidth -1){
	  dx1++;
	}
	while (yValues[dy1] < y1 && dy1 < dHeight-1){
	  dy1++;
	}

        dx0 += this.minX;
        dy0 += this.minY;
        dx1 += this.minX;
        dy1 += this.minY;

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
	// Note: indices starting from 0, thus minX/Y should be considered
        int sx0 = xValues[destRect.x - this.minX];
        int sy0 = yValues[destRect.y - this.minY];
        int sx1 = xValues[destRect.x - this.minX + destRect.width -1];
        int sy1 = yValues[destRect.y - this.minY + destRect.height-1];

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
            byteLoop(source, dest, destRect);
	    break;
        default:
            throw new
		RuntimeException(JaiI18N.getString("SubsampleBinaryToGrayOpImage0"));
        }
    }

    private void byteLoop(Raster source, WritableRaster dest, Rectangle  destRect){
        PixelAccessor pa = new PixelAccessor(source.getSampleModel(), null);
	PackedImageData pid =
            pa.getPackedPixels(source, source.getBounds(), false, false);
	byte[] sourceData   = pid.data;
        int sourceDBOffset  = pid.offset;
	int dx  = destRect.x;
	int dy  = destRect.y;
	int dwi = destRect.width;
	int dhi = destRect.height;
        int sourceTransX = pid.rect.x;   // source.getSampleModelTranslateX();
        int sourceTransY = pid.rect.y;   // source.getSampleModelTranslateY();

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
            int x = xValues[dx+i-this.minX];
            int sbitnum = pid.bitOffset + (x - sourceTransX);
            sbytenum[i] = sbitnum >> 3;
            sstartbit[i]   = sbitnum % 8;
        }

        for(int j = 0; j < dhi; j++) {

	   for(int i=0; i < dwi; i++){
	      sAreaBitsOn[i] = 0;
	   }

	   for(int y = yValues[dy+j-this.minY]; y < yValues[dy+j-this.minY]+blockY; y++){

              int sourceYOffset =
                (y - sourceTransY)*pid.lineStride + sourceDBOffset;

	      int  delement=0, selement, sendbiti, sendbytenumi;
	      for(int i=0; i < dwi; i++){
	         delement = 0;
		 sendbiti     = sstartbit[i] + blockX - 1;
		 sendbytenumi = sbytenum[i]+(sendbiti>>3);
		 sendbiti    %= 8;

		 selement = 0x00ff & (int)sourceData[sourceYOffset + sbytenum[i]];

		 if(sbytenum[i]==sendbytenumi){
		    selement  <<= 24 + sstartbit[i];
		    selement >>>= 31 - sendbiti + sstartbit[i];
		    delement += lut[selement];
		 }else{
		    selement  <<= 24 + sstartbit[i];
		    selement >>>= 24;
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


    private void computeDestInfo(int srcWidth, int srcHeight){

	// Inverse scale factors
	invScaleX = 1.0F / scaleX;
	invScaleY = 1.0F / scaleY;
	blockX = (int)Math.ceil(invScaleX);
	blockY = (int)Math.ceil(invScaleY);

	// calculate dst width and height
	float f_dw = scaleX * srcWidth;
	float f_dh = scaleY * srcHeight;
	floatTol = .1F * Math.min(scaleX/(f_dw+1.0F),scaleY/(f_dh+1.0F));

	dWidth = (int)(f_dw);
	dHeight= (int)(f_dh);

	// let it be int in the almost int case
	//   espacially in the true int case with float calculation errors
	if (Math.abs(Math.round(f_dw)-f_dw) < floatTol){
	   dWidth = Math.round(f_dw);
	}

	if (Math.abs(Math.round(f_dh)-f_dh) < floatTol){
	   dHeight= Math.round(f_dh);
	}

	if (Math.abs(Math.round(invScaleX) - invScaleX) < floatTol){
	   invScaleX = Math.round(invScaleX);
	   blockX    = (int)invScaleX;
	}

	if (Math.abs(Math.round(invScaleY) - invScaleY) < floatTol){
	   invScaleY = Math.round(invScaleY);
	   blockY = (int)invScaleY;
	}

    }


    // buildLookupTables()
    // initializes variabes bitSet and lut
    // to be called mainly in the constructor
    private final void buildLookupTables(){
	// lut
	lut[0] = 0; lut[1] = 1; lut[2] = 1; lut[3] = 2;
	lut[4] = 1; lut[5] = 2; lut[6] = 2; lut[7] = 3;
	lut[8] = 1; lut[9] = 2; lut[10]= 2; lut[11]= 3;
	lut[12]= 2; lut[13]= 3; lut[14]= 3; lut[15]= 4;
	for(int i= 16; i < 256; i++){
	   lut[i]  = lut[i&(0x0f)] + lut[(i>>4)&(0x0f)];
	}

	// lutGray
	if (lutGray != null) return;
	lutGray = new byte[blockX * blockY +1];
	for (int i=0; i < lutGray.length; i++){
	    int tmp =  (int)Math.round(255.0F*i/(lutGray.length-1.0F));
	    lutGray[i] = tmp>255? (byte)0xff : (byte)tmp;
	}

	// switch black-white if needed
	if (isMinWhite(this.getSourceImage(0).getColorModel()))
	    for(int i=0; i < lutGray.length; i++)
	       lutGray[i]= (byte)(255-(0xff &lutGray[i]));
    }

    // this function can be called
    // only after dWidth and dHeight has been set in the constructor
    // XY values should be computed and stored for repeatable behavior
    // taking care of non-zero minX, minY
    private void computeXYValues(int srcWidth, int srcHeight,
				 int srcMinX,  int srcMinY){
      if (xValues==null || yValues == null){
	 xValues = new int[dWidth];
	 yValues = new int[dHeight];
      }

      float tmp;
      for(int i= 0; i < dWidth; i++){
          tmp = invScaleX * i;
	  xValues[i] = (int)Math.round(tmp);
      }
      if (xValues[dWidth-1]+blockX > srcWidth){
	 xValues[dWidth-1]--;
      }

      for(int i=0;  i < dHeight; i++){
  	 tmp = invScaleY * i;
	 yValues[i] = Math.round(tmp);
      }
      if (yValues[dHeight-1] + blockY > srcHeight){
	 yValues[dHeight-1]--;
      }

      // if case the source MinX/Y are not zeros
      if (srcMinX != 0)
	  for (int i = 0; i < dWidth;  i++)  xValues[i] += srcMinX;
      if (srcMinY != 0)
	  for (int i = 0; i < dHeight; i++)  yValues[i] += srcMinY;
    }

    // check to see whether an indexed colormodel is inverted
    // returns false if cm not IndexColorModel
    // red[0] = 0     returns false
    // red[0] = 255   returns true
    static boolean isMinWhite(ColorModel cm){
      if (cm == null || !(cm instanceof IndexColorModel)) return false;

      byte[] red = new byte[256];
      ((IndexColorModel)cm).getReds(red);
      return (red[0]==(byte)255?true:false);
    }
}
