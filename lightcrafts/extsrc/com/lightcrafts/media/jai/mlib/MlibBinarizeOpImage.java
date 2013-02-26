/*
 * $RCSfile: MlibBinarizeOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:51 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.image.RenderedImage;
import com.lightcrafts.mediax.jai.ImageLayout;
import java.util.Map;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.ColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import com.lightcrafts.mediax.jai.PointOpImage;
import com.sun.medialib.mlib.*;
import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.JDKWorkarounds;

/**
 * A mediaLib class extending <code>PointOpImage</code> to
 * binarize images to bileval images.
 * <p> <pre>
 *   dst(i,j) = src(i,j) > threshvalue? 1: 0;
 *
 * @see com.lightcrafts.media.jai.operator.BinarizeOpImage
 * @see com.lightcrafts.mediax.jai.operator.BinarizeDescriptor
 *
 */
class MlibBinarizeOpImage extends PointOpImage {

  /** 
   * threshold value
   */
    private double thresh;

    /**
     * Constructs a <code>MlibBinarizeOpImage</code> 
     * from a <code>RenderedImage</code> source, thresh value.
     * 
     * @param source a <code>RenderedImage</code>.
     * @param layout an <code>ImageLayout</code> optionally containing
     *        the tile grid layout, <code>SampleModel</code>, and
     *        <code>ColorModel</code>, or <code>null</code>.

     *        from this <code>OpImage</code>, or <code>null</code>.  If
     *        <code>null</code>, no caching will be performed.

     * @param thresh Threshold value.
     *
     * @throws IllegalArgumentException if combining the
     *         source bounds with the layout parameter results in negative
     *         output width or height.
     */
    public MlibBinarizeOpImage(RenderedImage source,
			       ImageLayout layout,
			       Map config,
			       double thresh){

        super(source,
              layoutHelper(source, layout, config),
              config,
              true);

	this.thresh = thresh;
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
     * 
     * @param sources   an array of sources, guarantee to provide all
     *                  necessary source data for computing the rectangle.
     * @param dest      a tile that contains the rectangle to be computed.
     * @param destRect  the rectangle within this OpImage to be processed.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {

	Raster source = sources[0];

	Rectangle srcRect = mapDestRect(destRect,0);

        // Hack: derive the source format tag as if it was writing to
        // a destination with the same layout as itself.
        int sourceFormatTag =
            MediaLibAccessor.findCompatibleTag(sources, source);

        // Hard-code the destination format tag as we know that the image
        // has a binary layout.
        int destFormatTag =
            dest.getSampleModel().getDataType() |
            MediaLibAccessor.BINARY |
            MediaLibAccessor.UNCOPIED;

        MediaLibAccessor srcAccessor =
            new MediaLibAccessor(source, srcRect, sourceFormatTag, false);
        MediaLibAccessor dstAccessor =
            new MediaLibAccessor(dest, destRect, destFormatTag, true);

	mediaLibImage srcML[], dstML[];

        switch (srcAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
            srcML = srcAccessor.getMediaLibImages();
            dstML = dstAccessor.getMediaLibImages();
            for (int i = 0 ; i < dstML.length; i++) {
              //XXX
              //medialib version is not inclusive
              //pure java version is inclusive
              //thus medialib version is the same
              //as the java version with thresh value decremented by 1
                Image.Thresh1(dstML[i],
			      srcML[i],
			      new int[]{(int)thresh-1},   // decrease by 1
			      new int[]{1},
			      new int[]{0});
            }
            break;
        case DataBuffer.TYPE_FLOAT:
        case DataBuffer.TYPE_DOUBLE:
            srcML = srcAccessor.getMediaLibImages();
            dstML = dstAccessor.getMediaLibImages();
            for (int i = 0 ; i < dstML.length; i++) {
                Image.Thresh1_Fp(dstML[i],
					       srcML[i],
					       new double[]{thresh},
					       new double[]{1.0D},
					       new double[]{0.0D});
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

}
