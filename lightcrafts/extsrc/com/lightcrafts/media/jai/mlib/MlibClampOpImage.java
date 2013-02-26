/*
 * $RCSfile: MlibClampOpImage.java,v $
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
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PointOpImage;
import java.util.Map;
import com.lightcrafts.media.jai.util.ImageUtil;
import com.sun.medialib.mlib.*;

/**
 * An <code>OpImage</code> implementing the "Clamp" operation
 * using MediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.ClampDescriptor
 * @see MlibClampRIF
 *
 * @since 1.0
 *
 */
final class MlibClampOpImage extends PointOpImage {

    /** The lower bound, one for each band. */
    private double[] low;

    /** The integer version of lower bound. */
    private int[] lowInt;

    /** The upper bound, one for each band. */
    private double[] high;

    /** The integer version of upper bound. */
    private int[] highInt;

    /** Constructor. */
    public MlibClampOpImage(RenderedImage source,
                                Map config,
                                ImageLayout layout,
                                double[] low,
                                double[] high) {
        super(source, layout, config, true);

        int numBands = getSampleModel().getNumBands();
        this.low = new double[numBands];
        this.lowInt = new int[numBands];
        this.high = new double[numBands];
        this.highInt = new int[numBands];

        for (int i = 0; i < numBands; i++) {
            if (low.length < numBands) {
                this.low[i] = low[0];
            } else {
                this.low[i] = low[i];
            }
            this.lowInt[i] = ImageUtil.clampRoundInt(this.low[i]);

            if (high.length < numBands) {
                this.high[i] = high[0];
            } else {
                this.high[i] = high[i];
            }
            this.highInt[i] = ImageUtil.clampRoundInt(this.high[i]);
        }
        // Set flag to permit in-place operation.
        permitInPlaceOperation();
    }

    /**
     * Performs the "Clamp" operation on a rectangular region of
     * the same.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        int formatTag = MediaLibAccessor.findCompatibleTag(sources, dest);

        MediaLibAccessor srcMA =
            new MediaLibAccessor(sources[0], destRect, formatTag);
        MediaLibAccessor dstMA =
            new MediaLibAccessor(dest, destRect, formatTag);

        mediaLibImage[] srcMLI = srcMA.getMediaLibImages();
        mediaLibImage[] dstMLI = dstMA.getMediaLibImages();

        switch (dstMA.getDataType()) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
            for (int i = 0 ; i < dstMLI.length; i++) {
                int[] mlLow = dstMA.getIntParameters(i, lowInt);
                int[] mlHigh = dstMA.getIntParameters(i, highInt);
                Image.Thresh4(dstMLI[i], srcMLI[i],
                                              mlHigh, mlLow, mlHigh, mlLow);
            }
            break;

        case DataBuffer.TYPE_FLOAT:
        case DataBuffer.TYPE_DOUBLE:
            for (int i = 0 ; i < dstMLI.length; i++) {
                double[] mlLow = dstMA.getDoubleParameters(i, low);
                double[] mlHigh = dstMA.getDoubleParameters(i, high);
                Image.Thresh4_Fp(dstMLI[i], srcMLI[i],
                                                 mlHigh, mlLow, mlHigh, mlLow);
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
