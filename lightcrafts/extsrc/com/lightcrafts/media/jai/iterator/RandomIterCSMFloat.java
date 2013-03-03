/*
 * $RCSfile: RandomIterCSMFloat.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:42 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.iterator;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import com.lightcrafts.media.jai.util.DataBufferUtils;

/**
 * @since EA2
 */
public class RandomIterCSMFloat extends RandomIterCSM {

    float[][] bankData;

    public RandomIterCSMFloat(RenderedImage im, Rectangle bounds) {
        super(im, bounds);
    }

    protected final void dataBufferChanged() {
        this.bankData = DataBufferUtils.getBankDataFloat(dataBuffer);
    }

    public final int getSample(int x, int y, int b) {
        makeCurrent(x - boundsX, y - boundsX);
        return (int)bankData[b][(x - sampleModelTranslateX)*pixelStride + 
                               (y - sampleModelTranslateY)*scanlineStride +
                               bandOffsets[b]];
    }

    public final float getSampleFloat(int x, int y, int b) {
        makeCurrent(x - boundsX, y - boundsX);
        return bankData[b][(x - sampleModelTranslateX)*pixelStride + 
                          (y - sampleModelTranslateY)*scanlineStride +
                          bandOffsets[b]];
    }

    public final double getSampleDouble(int x, int y, int b) {
        makeCurrent(x - boundsX, y - boundsX);
        return (double)bankData[b][(x - sampleModelTranslateX)*pixelStride + 
                                  (y - sampleModelTranslateY)*scanlineStride +
                                  bandOffsets[b]];
    }

    public float[] getPixel(int x, int y, float[] fArray) {
        if (fArray == null) {
            fArray = new float[numBands];
        }

        int offset = (x - sampleModelTranslateX)*pixelStride + 
            (y - sampleModelTranslateY)*scanlineStride;
        for (int b = 0; b < numBands; b++) {
            fArray[b] = bankData[b][offset + bandOffsets[b]];
        }
        return fArray;
    }
}
