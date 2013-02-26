/*
 * $RCSfile: MlibDilate3PlusOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/12/09 00:20:28 $
 * $State: Exp $
 */ 
package com.lightcrafts.media.jai.mlib;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.AreaOpImage;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.KernelJAI;
import java.util.Map;
import com.sun.medialib.mlib.*;
// import com.lightcrafts.media.jai.test.OpImageTester;

/**
 * An OpImage class to perform dilation on a source image
 * for the specific case when the kernel is a 3x3 plus shape
 * with the key position in the middle,
 * using mediaLib, of course :-).
 *
 * @see com.lightcrafts.mediax.jai.operator.DilateDescriptor
 * @see KernelJAI
 */
final class MlibDilate3PlusOpImage extends AreaOpImage {

    // Since medialib expects single banded data with IndexColorModel, we
    // should not expand the indexed data
    private static Map configHelper(Map configuration) {

        Map config;

        if (configuration == null) {

            config = new RenderingHints(JAI.KEY_REPLACE_INDEX_COLOR_MODEL,
                                        Boolean.FALSE);

        } else {

            config = configuration;

	    // If the user has specified a hint for this, then we don't
	    // want to change it, so change only if this hint is not 
	    // already specified
            if (!(config.containsKey(JAI.KEY_REPLACE_INDEX_COLOR_MODEL))) {
                RenderingHints hints = (RenderingHints)configuration;
                config = (RenderingHints)hints.clone();
                config.put(JAI.KEY_REPLACE_INDEX_COLOR_MODEL, Boolean.FALSE);
            }
        }

        return config;
    }

    /**
     * Creates a MlibDilate3PlusOpImage given the image source
     * The image dimensions are
     * derived from the source image.  The tile grid layout,
     * SampleModel, and ColorModel may optionally be specified by an
     * ImageLayout object.
     *
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.

     *        or null.  If null, a default cache will be used.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     */
    public MlibDilate3PlusOpImage(RenderedImage source,
                                  BorderExtender extender,
                                  Map config,
                                  ImageLayout layout
                                  ) {
	super(source,
              layout,
              configHelper(config),
              true,
              extender,
              1, //kernel.getLeftPadding(),
              1, //kernel.getRightPadding(),
              1, //kernel.getTopPadding(),
              1  //kernel.getBottomPadding()
	      );
    }


    /**
     * Performs dilation on a specified rectangle. The sources are
     * cobbled.
     *
     * @param sources an array of source Rasters, guaranteed to provide all
     *                necessary source data for computing the output.
     * @param dest a WritableRaster tile containing the area to be computed.
     * @param destRect the rectangle within dest to be processed.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {

        Raster source = sources[0];
        Rectangle srcRect = mapDestRect(destRect, 0);

        int formatTag = MediaLibAccessor.findCompatibleTag(sources,dest);

        MediaLibAccessor srcAccessor =
            new MediaLibAccessor(source,srcRect,formatTag,true);
        MediaLibAccessor dstAccessor =
            new MediaLibAccessor(dest,destRect,formatTag,true);
        int numBands = getSampleModel().getNumBands();


        mediaLibImage[] srcML = srcAccessor.getMediaLibImages();
        mediaLibImage[] dstML = dstAccessor.getMediaLibImages();
        for (int i = 0; i < dstML.length; i++) {
            switch (dstAccessor.getDataType()) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
                Image.Dilate4(dstML[i], srcML[i]);
                break;
            case DataBuffer.TYPE_FLOAT:
            case DataBuffer.TYPE_DOUBLE:
                Image.Dilate4_Fp(dstML[i], srcML[i]);
                break;
            default:
                String className = this.getClass().getName();
                throw new RuntimeException(JaiI18N.getString("Generic2"));
            }
        }
 
        if (dstAccessor.isDataCopy()) {
            dstAccessor.copyDataToRaster();
        }
    }

//     public static OpImage createTestImage(OpImageTester oit) {
//         float data[] = {0.05f,0.10f,0.05f,
//                         0.10f,0.40f,0.10f,
//                         0.05f,0.10f,0.05f};
//         KernelJAI kJAI = new KernelJAI(3,3,1,1,data);
//         return new MlibDilate4OpImage(oit.getSource(), null, null,
//                                           new ImageLayout(oit.getSource()),
//                                           kJAI);
//     }
 
//     public static void main (String args[]) {
//         String classname = "com.lightcrafts.media.jai.mlib.MlibDilate4OpImage";
//         OpImageTester.performDiagnostics(classname,args);
//     }
}
