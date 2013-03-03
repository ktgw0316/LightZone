/*
 * $RCSfile: MlibScaleBicubicOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/12/15 18:35:46 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.InterpolationBicubic2;
import java.util.Map;
import com.sun.medialib.mlib.*;

// import com.lightcrafts.media.jai.test.OpImageTester;

/**
 * An OpImage class that scales an image using bicubic interpolation.
 *
 */
final class MlibScaleBicubicOpImage extends MlibScaleOpImage {

    /**
     * Constructs an MlibScaleBicubicOpImage. The image dimensions are copied
     * from the source image.  The tile grid layout, SampleModel, and
     * ColorModel may optionally be specified by an ImageLayout object.
     *
     * @param source    a RenderedImage.
     * @param extender  a BorderExtender, or null.
     * @param layout    an ImageLayout optionally containing the tile
     *                  grid layout, SampleModel, and ColorModel, or null.
     * @param xScale    the x scaling factor.
     * @param yScale    the y scaling factor.
     * @param xTrans    the x translation factor.
     * @param yTrans    the y translation factor.
     * @param interp    the Bicubic interpolation object.
     */
    public MlibScaleBicubicOpImage(RenderedImage source,
                                   BorderExtender extender,
                                   Map config,
				   ImageLayout layout,
				   float xScale, float yScale,
				   float xTrans, float yTrans,
				   Interpolation interp) {
        super(source, extender, config,
              layout, xScale, yScale, xTrans, yTrans, interp, true);
    }

    /**
     * Scale the given rectangle by the specified scale and translation 
     * factors. The sources are cobbled.
     *
     * @param sources   an array of sources, guarantee to provide all
     *                  necessary source data for computing the rectangle.
     * @param dest      a tile that contains the rectangle to be computed.
     * @param destRect  the rectangle within this OpImage to be processed.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {

	// The default interpolation for this class is Bicubic
	int mlibInterpType = Constants.MLIB_BICUBIC;

	// Unless a Bicubic2 object is passed in as the interpolation
	if (interp instanceof InterpolationBicubic2) {
	    mlibInterpType = Constants.MLIB_BICUBIC2;
	}

	Raster source = sources[0];
	Rectangle srcRect = source.getBounds();
	
        int formatTag = MediaLibAccessor.findCompatibleTag(sources, dest);

        MediaLibAccessor srcAccessor = new MediaLibAccessor(source, srcRect,
							    formatTag);
        MediaLibAccessor dstAccessor = new MediaLibAccessor(dest, destRect,
							    formatTag);

	// Get the scale factors in double precision
	double mlibScaleX = 
	    (double)scaleXRationalNum / (double)scaleXRationalDenom;
	double mlibScaleY = 
	    (double)scaleYRationalNum / (double)scaleYRationalDenom;

        // Translation to be specified to Medialib. Note that we have to 
	// specify an additional translation since all images are 0 based
	// in Medialib.   Note that scale and translation scalars have
	// rational representations.
	    
	// Calculate intermediate values using rational arithmetic.
	long tempDenomX = scaleXRationalDenom * transXRationalDenom;
        long tempDenomY = scaleYRationalDenom * transYRationalDenom;
        long tempNumerX = 
	    (srcRect.x * scaleXRationalNum * transXRationalDenom) +
	    (transXRationalNum * scaleXRationalDenom) -
	    (destRect.x * tempDenomX);
        long tempNumerY = 
	    (srcRect.y * scaleYRationalNum * transYRationalDenom) +
	    (transYRationalNum * scaleYRationalDenom) -
	    (destRect.y * tempDenomY);
	
        double tx = (double)tempNumerX/(double)tempDenomX;
	double ty = (double)tempNumerY/(double)tempDenomY;

	mediaLibImage srcML[], dstML[];
	
        switch (dstAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
            srcML = srcAccessor.getMediaLibImages();
            dstML = dstAccessor.getMediaLibImages();
            for (int i = 0 ; i < dstML.length; i++) {
		Image.ZoomTranslate(dstML[i], srcML[i],
				    mlibScaleX,
				    mlibScaleY,
				    tx, ty, 
				    mlibInterpType,
				    Constants.MLIB_EDGE_DST_NO_WRITE);
                MlibUtils.clampImage(dstML[i], getColorModel());
            }
            break;

        case DataBuffer.TYPE_FLOAT:
        case DataBuffer.TYPE_DOUBLE:
	    srcML = srcAccessor.getMediaLibImages();
            dstML = dstAccessor.getMediaLibImages();
            for (int i = 0 ; i < dstML.length; i++) {
		Image.ZoomTranslate_Fp(dstML[i], srcML[i],
				       mlibScaleX,
				       mlibScaleY,
				       tx, ty, 
				       mlibInterpType,
				       Constants.MLIB_EDGE_DST_NO_WRITE);
            }
            break;

        default:
            String className = this.getClass().getName();
            throw new RuntimeException(JaiI18N.getString("Generic2"));
        }

        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }

//     public static OpImage createTestImage(OpImageTester oit) {
// 	Interpolation interp = new InterpolationBicubic(8);
//         return new MlibScaleBicubicOpImage(oit.getSource(), null, null,
// 					   new ImageLayout(oit.getSource()),
// 					   2, 2, 0, 0, interp); 
//     }

//     // Calls a method on OpImage that uses introspection, to make this
//     // class, discover it's createTestImage() call, call it and then
//     // benchmark the performance of the created OpImage chain.
//     public static void main (String args[]) {
//         String classname = "com.lightcrafts.media.jai.mlib.MlibScaleBicubicOpImage";
//         OpImageTester.performDiagnostics(classname, args);
//     }
}
