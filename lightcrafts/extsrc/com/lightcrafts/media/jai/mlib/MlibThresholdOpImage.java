/*
 * $RCSfile: MlibThresholdOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:07 $
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
 * An <code>OpImage</code> implementing the "Threshold" operation
 * using MediaLib.
 *
 * @see com.lightcrafts.mediax.jai.operator.ThresholdDescriptor
 * @see MlibThresholdRIF
 *
 * @since 1.0
 *
 */
final class MlibThresholdOpImage extends PointOpImage {

    /** The lower bound, one for each band. */
    private double[] low;

    /** The integer version of lower bound. */
    private int[] lowInt;

    /** The upper bound, one for each band. */
    private double[] high;

    /** The integer version of upper bound. */
    private int[] highInt;

    /** The constants to be mapped, one for each band. */
    private double[] constants;

    /** The integer version of constants. */
    private int[] constantsInt;

    /** Constructor. */
    public MlibThresholdOpImage(RenderedImage source,
                                Map config,
                                ImageLayout layout,
                                double[] low,
                                double[] high,
                                double[] constants) {
        super(source, layout, config, true);

        int numBands = getSampleModel().getNumBands();
        this.low = new double[numBands];
        this.lowInt = new int[numBands];
        this.high = new double[numBands];
        this.highInt = new int[numBands];
        this.constants = new double[numBands];
        this.constantsInt = new int[numBands];

        for (int i = 0; i < numBands; i++) {
            if (low.length < numBands) {
                this.low[i] = low[0];
            } else {
                this.low[i] = low[i];
            }
            this.lowInt[i] = ImageUtil.clampInt((int)Math.ceil(this.low[i]));

            if (high.length < numBands) {
                this.high[i] = high[0];
            } else {
                this.high[i] = high[i];
            }
            this.highInt[i] = ImageUtil.clampInt((int)Math.floor(this.high[i]));

            if (constants.length < numBands) {
                this.constants[i] = constants[0];
            } else {
                this.constants[i] = constants[i];
            }
            this.constantsInt[i] = ImageUtil.clampRoundInt(this.constants[i]);
        }
        // Set flag to permit in-place operation.
        permitInPlaceOperation();
    }

    /**
     * Performs the "Threshold" operation on a rectangular region of
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
                int[] mlConstants = dstMA.getIntParameters(i, constantsInt);
                Image.Thresh5(dstMLI[i], srcMLI[i],
                                              mlHigh, mlLow, mlConstants);
            }
            break;

        case DataBuffer.TYPE_FLOAT:
        case DataBuffer.TYPE_DOUBLE:
            for (int i = 0 ; i < dstMLI.length; i++) {
                double[] mlLow = dstMA.getDoubleParameters(i, low);
                double[] mlHigh = dstMA.getDoubleParameters(i, high);
                double[] mlConstants = dstMA.getDoubleParameters(i, constants);
                Image.Thresh5_Fp(dstMLI[i], srcMLI[i],
                                                 mlHigh, mlLow, mlConstants);
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
