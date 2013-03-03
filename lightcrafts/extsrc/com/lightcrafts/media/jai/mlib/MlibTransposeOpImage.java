/*
 * $RCSfile: MlibTransposeOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/12/13 21:23:07 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PlanarImage;
import java.util.Map;

import com.lightcrafts.media.jai.opimage.TransposeOpImage;
import com.sun.medialib.mlib.*;
// import com.lightcrafts.media.jai.test.OpImageTester;

/**
 * An OpImage class to perform a transpose (flip) of an image.
 *
 * @since EA3
 *
 */
final class MlibTransposeOpImage extends TransposeOpImage {

    /**
     * Constructs an TransposeOpImage from a RenderedImage source,
     * and Transpose type.  The image dimensions are determined by
     * forward-mapping the source bounds.
     * The tile grid layout, SampleModel, and ColorModel are specified
     * by the image source, possibly overridden by values from the
     * ImageLayout parameter.
     *
     * @param source a RenderedImage.

     *        or null.  If null, a default cache will be used.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param type the desired Tranpose type.
     */
    public MlibTransposeOpImage(RenderedImage source,
                                Map config,
                                ImageLayout layout,
                                int type) {
        super(source, config, layout, type);
    }

    public Raster computeTile(int tileX, int tileY) {
        //
        // Create a new WritableRaster to represent this tile.
        //
        Point org = new Point(tileXToX(tileX), tileYToY(tileY));
        WritableRaster dest = createWritableRaster(sampleModel, org);

        //
        // Clip output rectangle to image bounds.
        //
        Rectangle destRect =
            getTileRect(tileX, tileY).intersection(getBounds());

        //
        // Get source
        //
        PlanarImage src = getSourceImage(0);

        //
        // Determine effective source bounds.
        //
        Rectangle srcRect =
            mapDestRect(destRect, 0).intersection(src.getBounds());

        Raster[] sources = new Raster[1];
        sources[0] = src.getData(srcRect);

        //
        // Compute the destination.
        //
        computeRect(sources, dest, destRect);

        // Recycle the source tile
        if(src.overlapsMultipleTiles(srcRect)) {
            recycleTile(sources[0]);
        }

        return dest;
    }

    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        Raster source = sources[0];

        Rectangle srcRect = source.getBounds();

        int formatTag = MediaLibAccessor.findCompatibleTag(sources, dest);

        MediaLibAccessor srcAccessor =
            new MediaLibAccessor(source,srcRect,formatTag);
        MediaLibAccessor dstAccessor =
            new MediaLibAccessor(dest,destRect,formatTag);
        int numBands = getSampleModel().getNumBands();

	mediaLibImage srcML[], dstML[];

        switch (dstAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
	    srcML = srcAccessor.getMediaLibImages();
	    dstML = dstAccessor.getMediaLibImages();
            switch (type) {
            case 0: // FLIP_VERTICAL
                Image.FlipX(dstML[0], srcML[0]);
                break;

            case 1: // FLIP_HORIZONTAL
                Image.FlipY(dstML[0], srcML[0]);
                break;

            case 2: // FLIP_DIAGONAL
                Image.FlipMainDiag(dstML[0], srcML[0]);
                break;

            case 3: // FLIP_ANTIDIAGONAL
                Image.FlipAntiDiag(dstML[0], srcML[0]);
                break;

            case 4: // ROTATE_90
                Image.Rotate90(dstML[0], srcML[0]);
                break;

            case 5: // ROTATE_180
                Image.Rotate180(dstML[0], srcML[0]);
                break;

            case 6: // ROTATE_270
                Image.Rotate270(dstML[0], srcML[0]);
                break;
            }
            break;

        case DataBuffer.TYPE_FLOAT:
        case DataBuffer.TYPE_DOUBLE:
	    srcML = srcAccessor.getMediaLibImages();
	    dstML = dstAccessor.getMediaLibImages();
            switch (type) {
            case 0: // FLIP_VERTICAL
                Image.FlipX_Fp(dstML[0], srcML[0]);
                break;

            case 1: // FLIP_HORIZONTAL
                Image.FlipY_Fp(dstML[0], srcML[0]);
                break;

            case 2: // FLIP_DIAGONAL
                Image.FlipMainDiag_Fp(dstML[0], srcML[0]);
                break;

            case 3: // FLIP_ANTIDIAGONAL
                Image.FlipAntiDiag_Fp(dstML[0], srcML[0]);
                break;

            case 4: // ROTATE_90
                Image.Rotate90_Fp(dstML[0], srcML[0]);
                break;

            case 5: // ROTATE_180
                Image.Rotate180_Fp(dstML[0], srcML[0]);
                break;

            case 6: // ROTATE_270
                Image.Rotate270_Fp(dstML[0], srcML[0]);
                break;
            }
            break;

        default:
            throw new RuntimeException(JaiI18N.getString("Generic2"));
        }

        //
        // If the RasterAccessor object set up a temporary buffer for the
        // op to write to, tell the RasterAccessor to write that data
        // to the raster, that we're done with it.
        //
        if (dstAccessor.isDataCopy()) {
            dstAccessor.copyDataToRaster();
        }
    }

//     public static OpImage createTestImage(OpImageTester oit) {
//         int type = 1;
//         return new MlibTransposeOpImage(oit.getSource(), null,
//                                         new ImageLayout(oit.getSource()),
//                                         type);
//     }

//     public static void main (String args[]) {
//         String classname = "com.lightcrafts.media.jai.mlib.MlibTransposeOpImage";
//         OpImageTester.performDiagnostics(classname,args);
//     }
}
