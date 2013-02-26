/*
 * $RCSfile: MlibMinOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:00 $
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

/**
 * An <code>OpImage</code> implementing the "Min" operation
 * using MediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.MinDescriptor
 * @see MlibMinRIF
 *
 * @since 1.0
 *
 */
final class MlibMinOpImage extends PointOpImage {

    /** Constructor. */
    public MlibMinOpImage(RenderedImage source1,
                          RenderedImage source2,
                          Map config,
                          ImageLayout layout) {
        super(source1, source2, layout, config, true);
        // Set flag to permit in-place operation.
        permitInPlaceOperation();
    }

    /**
     * Performs the "Min" operation on a rectangular region of
     * the same.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        int formatTag = MediaLibAccessor.findCompatibleTag(sources, dest);

        MediaLibAccessor srcMA1 =
            new MediaLibAccessor(sources[0], destRect, formatTag);
        MediaLibAccessor srcMA2 =
            new MediaLibAccessor(sources[1], destRect, formatTag);
        MediaLibAccessor dstMA =
            new MediaLibAccessor(dest, destRect, formatTag);

        mediaLibImage[] srcMLI1 = srcMA1.getMediaLibImages();
        mediaLibImage[] srcMLI2 = srcMA2.getMediaLibImages();
        mediaLibImage[] dstMLI = dstMA.getMediaLibImages();

        switch (dstMA.getDataType()) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
            for (int i = 0 ; i < dstMLI.length; i++) {
                Image.Min(dstMLI[i], srcMLI1[i], srcMLI2[i]);
            }
            break;

        case DataBuffer.TYPE_FLOAT:
        case DataBuffer.TYPE_DOUBLE:
            for (int i = 0 ; i < dstMLI.length; i++) {
                Image.Min_Fp(dstMLI[i], srcMLI1[i], srcMLI2[i]);
            }
            break;

        default:
            throw new RuntimeException(JaiI18N.getString("Generic2"));
        }

        if (dstMA.isDataCopy()) {
            dstMA.clampDataArrays();
            dstMA.copyDataToRaster();
        }
    }
}
