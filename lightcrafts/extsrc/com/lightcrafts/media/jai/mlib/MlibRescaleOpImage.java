/*
 * $RCSfile: MlibRescaleOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:03 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PointOpImage;
import java.util.Map;
import com.sun.medialib.mlib.*;
// import com.lightcrafts.media.jai.test.OpImageTester;

/**
 * An OpImage class that multiplies pixels in an
 * image by a constant, then adds an offset.
 *
 */
final class MlibRescaleOpImage extends PointOpImage {
    private double[] constants;
    private double[] offsets;

    // XXX This cloning of the ImageLayout object should be centraliZed
    // into the superclass PointOpImage and removed from the mlib subclasses.
    private static ImageLayout layoutHelper(ImageLayout layout) {
	if (layout == null) {
	    return null;
	} else {
	    return (ImageLayout)layout.clone();
	}
    }

    /**
     * Constructs an MlibRescaleOpImage. The image dimensions are copied
     * from the source image.  The tile grid layout, SampleModel, and
     * ColorModel may optionally be specified by an ImageLayout object.
     *
     * @param source    a RenderedImage.
     * @param layout    an ImageLayout optionally containing the tile
     *                  grid layout, SampleModel, and ColorModel, or null.
     */
    public MlibRescaleOpImage(RenderedImage source,
                              Map config,
                              ImageLayout layout,
                              double[] constants,
                              double[] offsets) {
        super(source, layoutHelper(layout), config, true);
        int numBands = getSampleModel().getNumBands();
        this.constants = MlibUtils.initConstants(constants, numBands);
        this.offsets = MlibUtils.initConstants(offsets, numBands);
        // Set flag to permit in-place operation.
        permitInPlaceOperation();
    }

    /**
     * Multiply the pixel values of a rectangle with a given constant,
     * then add an offset.  The sources are cobbled.
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
        Rectangle srcRect = mapDestRect(destRect, 0);

        int formatTag = MediaLibAccessor.findCompatibleTag(sources,dest);

        MediaLibAccessor srcAccessor = 
            new MediaLibAccessor(source,srcRect,formatTag);
        MediaLibAccessor dstAccessor = 
            new MediaLibAccessor(dest,destRect,formatTag);

        mediaLibImage[] srcML = srcAccessor.getMediaLibImages();
        mediaLibImage[] dstML = dstAccessor.getMediaLibImages();

        switch (dstAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
            for (int i = 0; i < dstML.length; i++) {
                double[] mlconstants = dstAccessor.getDoubleParameters(i, constants);
                double[] mloffsets   = dstAccessor.getDoubleParameters(i, offsets);
                Image.Scale2(dstML[i], srcML[i], mlconstants, mloffsets);
            }
            break;

        case DataBuffer.TYPE_FLOAT:
        case DataBuffer.TYPE_DOUBLE:
            for (int i = 0; i < dstML.length; i++) {
                double[] mlconstants = dstAccessor.getDoubleParameters(i, constants);
                double[] mloffsets   = dstAccessor.getDoubleParameters(i, offsets);
                Image.Scale_Fp(dstML[i], srcML[i], mlconstants, mloffsets);
            }
            break;

        default:
            String className = this.getClass().getName();
            throw new RuntimeException(className + JaiI18N.getString("Generic2"));
        }

        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }

//     public static OpImage createTestImage(OpImageTester oit) {
//         double[] consts  = { 5.0D, 5.0D, 5.0D };
//         double[] offsets = { 0.0D, 0.0D, 0.0D };
//         return new MlibRescaleOpImage(oit.getSource(), null,
//                                        new ImageLayout(oit.getSource()),
//                                        consts,
//                                        offsets);
//     }

//     // Calls a method on OpImage that uses introspection, to make this
//     // class, discover it's createTestImage() call, call it and then
//     // benchmark the performance of the created OpImage chain.
//     public static void main (String args[]) {
//         String classname = "com.lightcrafts.media.jai.mlib.MlibRescaleOpImage";
//         OpImageTester.performDiagnostics(classname,args);
//     }
}
