/*
 * $RCSfile: RectIterCSMFloat.java,v $
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
import java.awt.image.RenderedImage;
import com.lightcrafts.media.jai.util.DataBufferUtils;

/**
 */
public class RectIterCSMFloat extends RectIterCSM {

    float[][] bankData;
    float[] bank;

    public RectIterCSMFloat(RenderedImage im, Rectangle bounds) {
        super(im, bounds);

        this.bankData = new float[numBands + 1][];
        dataBufferChanged();
    }

    protected final void dataBufferChanged() {
        if (bankData == null) {
            return;
        }

        float[][] bd = DataBufferUtils.getBankDataFloat(dataBuffer);
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
        return (int)bank[offset + bandOffset];
    }

    public final int getSample(int b) {
        return (int)bankData[b][offset + bandOffsets[b]];
    }

    public final float getSampleFloat() {
        return bank[offset + bandOffset];
    }

    public final float getSampleFloat(int b) {
        return bankData[b][offset + bandOffsets[b]];
    }

    public final double getSampleDouble() {
        return (double)bank[offset + bandOffset];
    }

    public final double getSampleDouble(int b) {
        return (double)bankData[b][offset + bandOffsets[b]];
    }

    public int[] getPixel(int[] iArray) {
        if (iArray == null) {
            iArray = new int[numBands];
        }
        for (int b = 0; b < numBands; b++) {
            iArray[b] = (int)bankData[b][offset + bandOffsets[b]];
        }
        return iArray;
    }

    public float[] getPixel(float[] fArray) {
        if (fArray == null) {
            fArray = new float[numBands];
        }
        for (int b = 0; b < numBands; b++) {
            fArray[b] = bankData[b][offset + bandOffsets[b]];
        }
        return fArray;
    }

    public double[] getPixel(double[] dArray) {
        if (dArray == null) {
            dArray = new double[numBands];
        }
        for (int b = 0; b < numBands; b++) {
            dArray[b] = (double)bankData[b][offset + bandOffsets[b]];
        }
        return dArray;
    }
}
