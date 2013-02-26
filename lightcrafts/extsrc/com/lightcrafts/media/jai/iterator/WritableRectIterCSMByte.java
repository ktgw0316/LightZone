/*
 * $RCSfile: WritableRectIterCSMByte.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:45 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.iterator;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import com.lightcrafts.mediax.jai.iterator.WritableRectIter;

/**
 */
public class WritableRectIterCSMByte extends RectIterCSMByte 
    implements WritableRectIter {

    public WritableRectIterCSMByte(RenderedImage im, Rectangle bounds) {
        super(im, bounds);
    }

    public void setSample(int s) {
        bank[offset + bandOffset] = (byte)s;
    }

    public void setSample(int b, int s) {
        bankData[b][offset + bandOffsets[b]] = (byte)s;
    }

    public void setSample(float s) {
        bank[offset + bandOffset] = (byte)s;
    }

    public void setSample(int b, float s) {
        bankData[b][offset + bandOffsets[b]] = (byte)s;
    }

    public void setSample(double s) {
        bank[offset + bandOffset] = (byte)s;
    }

    public void setSample(int b, double s) {
        bankData[b][offset + bandOffsets[b]] = (byte)s;
    }

    public void setPixel(int[] iArray) {
        for (int b = 0; b < numBands; b++) {
            bankData[b][offset + bandOffsets[b]] = (byte)iArray[b];
        }
    }

    public void setPixel(float[] fArray) {
        for (int b = 0; b < numBands; b++) {
            bankData[b][offset + bandOffsets[b]] = (byte)fArray[b];
        }
    }

    public void setPixel(double[] dArray) {
        for (int b = 0; b < numBands; b++) {
            bankData[b][offset + bandOffsets[b]] = (byte)dArray[b];
        }
    }
}
