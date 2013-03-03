/*
 * $RCSfile: RandomIterCSMByte.java,v $
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
import java.awt.image.DataBufferByte;
import java.awt.image.RenderedImage;

/**
 * @since EA2
 */
public class RandomIterCSMByte extends RandomIterCSM {

    byte[][] bankData;

    public RandomIterCSMByte(RenderedImage im, Rectangle bounds) {
        super(im, bounds);
    }

    protected final void dataBufferChanged() {
        this.bankData = ((DataBufferByte)dataBuffer).getBankData();
    }

    public final int getSample(int x, int y, int b) {
        makeCurrent(x - boundsX, y - boundsY);
        return bankData[b][(x - sampleModelTranslateX)*pixelStride + 
                          (y - sampleModelTranslateY)*scanlineStride +
                          bandOffsets[b]] & 0xff;
    }

    public final float getSampleFloat(int x, int y, int b) {
        makeCurrent(x - boundsX, y - boundsX);
        return (float)(bankData[b][(x - sampleModelTranslateX)*pixelStride + 
                                  (y - sampleModelTranslateY)*scanlineStride +
                                  bandOffsets[b]] & 0xff);
    }

    public final double getSampleDouble(int x, int y, int b) {
        makeCurrent(x - boundsX, y - boundsX);
        return (double)(bankData[b][(x - sampleModelTranslateX)*pixelStride + 
                                   (y - sampleModelTranslateY)*scanlineStride +
                                   bandOffsets[b]] & 0xff);
    }

    public int[] getPixel(int x, int y, int[] iArray) {
        if (iArray == null) {
            iArray = new int[numBands];
        }

        int offset = (x - sampleModelTranslateX)*pixelStride +
            (y - sampleModelTranslateY)*scanlineStride;
        for (int b = 0; b < numBands; b++) {
            iArray[b] = bankData[b][offset + bandOffsets[b]] & 0xff;
        }
        return iArray;
    }
}
