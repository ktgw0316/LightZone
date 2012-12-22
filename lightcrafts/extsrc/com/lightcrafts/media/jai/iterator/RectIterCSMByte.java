/*
 * $RCSfile: RectIterCSMByte.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:43 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.iterator;
import java.awt.Rectangle;
import java.awt.image.DataBufferByte;
import java.awt.image.RenderedImage;

/**
 */
public class RectIterCSMByte extends RectIterCSM {

    byte[][] bankData;
    byte[] bank;

    public RectIterCSMByte(RenderedImage im, Rectangle bounds) {
        super(im, bounds);

        this.bankData = new byte[numBands + 1][];
        dataBufferChanged();
    }

    protected final void dataBufferChanged() {
        if (bankData == null) {
            return;
        }

        byte[][] bd = ((DataBufferByte)dataBuffer).getBankData();
        for (int i = 0; i < numBands; i++) {
            bankData[i] = bd[bankIndices[i]];
        }
        bank = bankData[b];

        adjustBandOffsets();
    }

    public void startBands() {
        super.startBands();
        bank = bankData[0];
    }

    public void nextBand() {
        super.nextBand();
        bank = bankData[b];
    }

    public final int getSample() {
        return bank[offset + bandOffset] & 0xff;
    }

    public final int getSample(int b) {
        return bankData[b][offset + bandOffsets[b]] & 0xff;
    }

    public final float getSampleFloat() {
        return (float)(bank[offset + bandOffset] & 0xff);
    }

    public final float getSampleFloat(int b) {
        return (float)(bankData[b][offset + bandOffsets[b]] & 0xff);
    }

    public final double getSampleDouble() {
        return (double)(bank[offset + bandOffset] & 0xff);
    }

    public final double getSampleDouble(int b) {
        return (double)(bankData[b][offset + bandOffsets[b]] & 0xff);
    }

    public int[] getPixel(int[] iArray) {
        if (iArray == null) {
            iArray = new int[numBands];
        }
        for (int b = 0; b < numBands; b++) {
            iArray[b] = bankData[b][offset + bandOffsets[b]] & 0xff;
        }
        return iArray;
    }

    public float[] getPixel(float[] fArray) {
        if (fArray == null) {
            fArray = new float[numBands];
        }
        for (int b = 0; b < numBands; b++) {
            fArray[b] = (float)(bankData[b][offset + bandOffsets[b]] & 0xff);
        }
        return fArray;
    }

    public double[] getPixel(double[] dArray) {
        if (dArray == null) {
            dArray = new double[numBands];
        }
        for (int b = 0; b < numBands; b++) {
            dArray[b] = (double)(bankData[b][offset + bandOffsets[b]] & 0xff);
        }
        return dArray;
    }
}
