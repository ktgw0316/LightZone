/*
 * $RCSfile: RandomIterCSM.java,v $
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
import java.awt.image.ComponentSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

/**
 * @since EA2
 */
public abstract class RandomIterCSM extends RandomIterFallback {

    protected ComponentSampleModel sampleModel;
    protected int pixelStride;
    protected int scanlineStride;
    protected int[] bandOffsets;
    protected int numBands;

    public RandomIterCSM(RenderedImage im, Rectangle bounds) {
        super(im, bounds);
        this.sampleModel = (ComponentSampleModel)im.getSampleModel();
        this.numBands = sampleModel.getNumBands();
        this.pixelStride = sampleModel.getPixelStride();
        this.scanlineStride = sampleModel.getScanlineStride();        
    }

    protected void dataBufferChanged() {}

    /**
     * Sets dataBuffer to the correct buffer for the pixel
     * (x, y) = (xLocal + boundsRect.x, yLocal + boundsRect.y).
     *
     * @param xLocal the X coordinate in the local coordinate system.
     * @param yLocal the Y coordinate in the local coordinate system.
     */
    protected final void makeCurrent(int xLocal, int yLocal) {
        int xIDNew = xTiles[xLocal];
        int yIDNew = yTiles[yLocal];

        if ((xIDNew != xID) || (yIDNew != yID) || (dataBuffer == null)) {
            xID = xIDNew;
            yID = yIDNew;
            Raster tile = im.getTile(xID, yID);

            this.dataBuffer = tile.getDataBuffer();
            dataBufferChanged();

            this.bandOffsets = dataBuffer.getOffsets();
        }
    }

    public float getSampleFloat(int x, int y, int b) {
        return (float)getSample(x, y, b);
    }

    public double getSampleDouble(int x, int y, int b) {
        return (double)getSample(x, y, b);
    }

    public int[] getPixel(int x, int y, int[] iArray) {
        if (iArray == null) {
            iArray = new int[numBands];
        }
        for (int b = 0; b < numBands; b++) {
            iArray[b] = getSample(x, y, b);
        }
        return iArray;
    }

    public float[] getPixel(int x, int y, float[] fArray) {
        if (fArray == null) {
            fArray = new float[numBands];
        }
        for (int b = 0; b < numBands; b++) {
            fArray[b] = getSampleFloat(x, y, b);
        }
        return fArray;
    }

    public double[] getPixel(int x, int y, double[] dArray) {
        if (dArray == null) {
            dArray = new double[numBands];
        }
        for (int b = 0; b < numBands; b++) {
            dArray[b] = getSampleDouble(x, y, b);
        }
        return dArray;
    }
}
