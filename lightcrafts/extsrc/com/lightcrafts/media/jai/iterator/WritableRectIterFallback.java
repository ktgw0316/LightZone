/*
 * $RCSfile: WritableRectIterFallback.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:46 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.iterator;
import java.awt.Rectangle;
import java.awt.image.WritableRenderedImage;
import com.lightcrafts.mediax.jai.iterator.WritableRectIter;

/**
 * @since EA2
 */
public class WritableRectIterFallback extends RectIterFallback
    implements WritableRectIter {

    protected WritableRenderedImage wim;

    public WritableRectIterFallback(WritableRenderedImage im,
                                    Rectangle bounds) {
        super(im, bounds);
        this.wim = im;
    }

    public void setSample(int s) {
        sampleModel.setSample(localX, localY, b, s, dataBuffer);
    }

    public void setSample(int b, int s) {
        sampleModel.setSample(localX, localY, b, s, dataBuffer);
    }

    public void setSample(float s) {
        sampleModel.setSample(localX, localY, b, s, dataBuffer);
    }

    public void setSample(int b, float s) {
        sampleModel.setSample(localX, localY, b, s, dataBuffer);
    }

    public void setSample(double s) {
        sampleModel.setSample(localX, localY, b, s, dataBuffer);
    }

    public void setSample(int b, double s) {
        sampleModel.setSample(localX, localY, b, s, dataBuffer);
    }

    public void setPixel(int[] iArray) {
        sampleModel.setPixel(localX, localY, iArray, dataBuffer);
    }

    public void setPixel(float[] fArray) {
        sampleModel.setPixel(localX, localY, fArray, dataBuffer);
    }

    public void setPixel(double[] dArray) {
        sampleModel.setPixel(localX, localY, dArray, dataBuffer);
    }
}
